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

import com.android.build.api.transform.*
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class ComponentInfoTransform extends SimpleClassCreatorTransform {

    Project project

    List<Project> dfProjects

    ComponentInfoTransform(Project project) {
        this.project = project
    }

    void initArgs(List<Project> dfProjects) {
        this.dfProjects = dfProjects
    }

    @Override
    String getName() {
        return "createComponentInfo"
    }

    @Override
    Collection<SecondaryFile> getSecondaryFiles() {
        FileCollection collection = project.files('build/intermediates/merged_manifests')
        return ImmutableSet.of(SecondaryFile.nonIncremental(collection))
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
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        transformInvocation.getOutputProvider().deleteAll()
        if (dfProjects == null) {
            project.logger.error("Have you invoke ComponentInfoTransform#initArgs(...)?")
            return
        }
        String variantName = transformInvocation.context.variantName.capitalize()

        Map<String, List> addFieldMap = new HashMap<>()
        for (Project dfProject : dfProjects) {
            def dfAndroid = dfProject.extensions.android
            String splitName = dfProject.name
            File splitManifestFile = null
            dfAndroid.applicationVariants.all { ApplicationVariant variant ->
                String dfVariantName = variant.name.capitalize()
                if (dfVariantName.equals(variantName)) {
                    Task processManifestTask = AGPCompat.getProcessManifestTask(dfProject, dfVariantName)
                    splitManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)
                }
            }
            if (splitManifestFile == null || !splitManifestFile.exists()) {
                throw new GradleException("can't get manifest file of project ${splitName}!")
            }
            ManifestReader manifestReader = new ManifestReaderImpl(splitManifestFile)
            List<String> activities = new ArrayList<>()
            List<String> services = new ArrayList<>()
            List<String> receivers = new ArrayList<>()
            List<String> providers = new ArrayList<>()
            List<String> applications = new ArrayList<>()

            String applicationName = manifestReader.readApplicationName().name
            if (applicationName != null && applicationName.length() > 0) {
                applications.add(applicationName)
            }

            manifestReader.readActivities().each {
                activities.add(it.name)
            }

            manifestReader.readServices().each {
                services.add(it.name)
            }
            manifestReader.readReceivers().each {
                receivers.add(it.name)
            }

            manifestReader.readProviders().each {
                providers.add(it.name)
            }

            addFieldMap.put(splitName + "_APPLICATION", applications)
            addFieldMap.put(splitName + "_ACTIVITIES", activities)
            addFieldMap.put(splitName + "_SERVICES", services)
            addFieldMap.put(splitName + "_RECEIVERS", receivers)
            addFieldMap.put(splitName + "_PROVIDERS", providers)
        }

        def dest = prepareToCreateClass(transformInvocation)
        createSimpleClass(dest, "com.iqiyi.android.qigsaw.core.extension.ComponentInfo", "java.lang.Object", new SimpleClassCreatorTransform.OnVisitListener() {

            @Override
            void onVisit(ClassWriter cw) {
                injectCommonInfo(dest, cw, addFieldMap)
            }
        })
    }

    static void injectCommonInfo(def dest, ClassWriter cw, Map<String, List> addFieldMap) {
        addFieldMap.each { entry ->
            List value = entry.value
            if (value.size() <= 0) {
                return
            }
            String name = entry.getKey()
            if (name.endsWith("APPLICATION")) {
                cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                        name, "Ljava/lang/String;", null, value.get(0)).visitEnd()
            } else if (name.endsWith("PROVIDERS")) {
                for (String providerName : value) {
                    String splitName = name.split("_")[0]
                    String providerClassName = providerName + "_Decorated_" + splitName
                    createSimpleClass(dest, providerClassName, "com.iqiyi.android.qigsaw.core.splitload.SplitContentProvider", null)
                }
            } else {
                cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                        name, "Ljava/lang/String;", null,
                        (value as String[]).join(",")).visitEnd()
            }
        }
    }
}
