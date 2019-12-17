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
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import org.gradle.api.Project

import org.apache.commons.io.FileUtils

class SplitResourcesLoaderTransform extends Transform {

    final static String NAME = "splitResourcesLoader"

    Project project

    File manifest

    WaitableExecutor waitableExecutor

    SplitResourcesLoaderTransform(Project project) {
        this.project = project
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool()
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

    void setManifest(File manifest) {
        this.manifest = manifest
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        long startTime = System.currentTimeMillis()
        transformInvocation.getOutputProvider().deleteAll()
        if (manifest == null || !manifest.exists()) {
            project.logger.error("SplitResourcesLoaderTransform Task of project ${project.name} has no manifest file!")
            return
        }
        SplitResourcesLoaderInjector resourcesLoaderInjector = new SplitResourcesLoaderInjector(waitableExecutor, manifest)
        transformInvocation.inputs.each {
            Collection<DirectoryInput> directoryInputs = it.directoryInputs

            if (directoryInputs != null) {
                directoryInputs.each {
                    File outputDir = transformInvocation.outputProvider.getContentLocation(it.file.absolutePath, it.contentTypes, it.scopes, Format.DIRECTORY)
                    FileUtils.copyDirectory(it.file, outputDir)
                    resourcesLoaderInjector.injectDir(outputDir)
                }
            }
            Collection<JarInput> jarInputs = it.jarInputs
            if (jarInputs != null) {
                jarInputs.each {
                    File outputJar = transformInvocation.outputProvider.getContentLocation(it.file.absolutePath, it.contentTypes, it.scopes, Format.JAR)
                    FileUtils.copyFile(it.file, outputJar)
                    resourcesLoaderInjector.injectJar(outputJar)
                }
            }
        }
        waitableExecutor.waitForTasksWithQuickFail(true)
        System.out.println("SplitComponentTransform cost " + (System.currentTimeMillis() - startTime) + " ms")
    }
}
