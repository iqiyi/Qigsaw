/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.qigsaw.buildtool.gradle.transform

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.SecondaryFile
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReader
import org.gradle.api.Project

import org.apache.commons.io.FileUtils
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

class SplitResourcesLoaderTransform extends Transform {

    final static String NAME = "splitResourcesLoader"

    Project project

    boolean isBaseModule

    WaitableExecutor waitableExecutor

    SplitResourcesLoaderTransform(Project project, boolean isBaseModule) {
        this.project = project
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
        this.isBaseModule = isBaseModule
    }

    SplitResourcesLoaderTransform(Project project) {
        this(project, false)
    }

    @Override
    String getName() {
        return NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    Collection<SecondaryFile> getSecondaryFiles() {
        if (isBaseModule) {
            return super.getSecondaryFiles()
        }
        FileCollection collection = project.files('build/intermediates/merged_manifests')
        return ImmutableSet.of(SecondaryFile.nonIncremental(collection))
    }

    @Override
    Map<String, Object> getParameterInputs() {
        if (isBaseModule) {
            Map<String, Set<String>> baseContainerActivitiesMap = new HashMap<>()
            baseContainerActivitiesMap.put("base_container_activities", QigsawSplitExtensionHelper.getBaseContainerActivities(project))
            return baseContainerActivitiesMap
        }
        return super.getParameterInputs()
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        long startTime = System.currentTimeMillis()
        transformInvocation.getOutputProvider().deleteAll()
        SplitResourcesLoaderInjector resourcesLoaderInjector = null
        if (isBaseModule) {
            Map<String, List<String>> baseContainerActivitiesMap = getParameterInputs()
            Set<String> baseContainerActivities = baseContainerActivitiesMap.get("base_container_activities")
            if (baseContainerActivities != null && !baseContainerActivities.isEmpty()) {
                resourcesLoaderInjector = new SplitResourcesLoaderInjector(waitableExecutor, baseContainerActivities)
            }
        } else {
            Task processManifest = AGPCompat.getProcessManifestTask(project, transformInvocation.context.variantName.capitalize())
            File mergedManifest = AGPCompat.getMergedManifestFileCompat(processManifest)
            ManifestReader manifestReader = new ManifestReader(mergedManifest)
            Set<String> activities = manifestReader.readActivityNames()
            Set<String> services = manifestReader.readServiceNames()
            Set<String> receivers = manifestReader.readReceiverNames()
            resourcesLoaderInjector = new SplitResourcesLoaderInjector(waitableExecutor, activities, services, receivers)
        }
        transformInvocation.inputs.each {
            Collection<DirectoryInput> directoryInputs = it.directoryInputs

            if (directoryInputs != null) {
                directoryInputs.each {
                    File outputDir = transformInvocation.outputProvider.getContentLocation(it.file.absolutePath, it.contentTypes, it.scopes, Format.DIRECTORY)
                    FileUtils.copyDirectory(it.file, outputDir)
                    if (resourcesLoaderInjector != null) {
                        resourcesLoaderInjector.injectDir(outputDir)
                    }
                }
            }
            Collection<JarInput> jarInputs = it.jarInputs
            if (jarInputs != null) {
                jarInputs.each {
                    File outputJar = transformInvocation.outputProvider.getContentLocation(it.file.absolutePath, it.contentTypes, it.scopes, Format.JAR)
                    FileUtils.copyFile(it.file, outputJar)
                    if (resourcesLoaderInjector != null) {
                        resourcesLoaderInjector.injectJar(outputJar)
                    }
                }
            }
        }
        waitableExecutor.waitForTasksWithQuickFail(true)
        System.out.println("SplitComponentTransform cost " + (System.currentTimeMillis() - startTime) + " ms")
    }
}
