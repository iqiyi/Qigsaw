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

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReader
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class SplitComponentTransform extends SimpleClassCreatorTransform {

    static final String NAME = "processSplitComponent"

    Project project

    File splitManifestParentDir

    Set<String> dynamicFeatureNames

    SplitComponentTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return NAME
    }

    @Override
    Collection<SecondaryFile> getSecondaryFiles() {
        FileCollection collection = project.files(splitManifestParentDir)
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
        File splitManifestDir = new File(splitManifestParentDir, transformInvocation.context.variantName.uncapitalize())
        if (!splitManifestDir.exists()) {
            throw new GradleException("${splitManifestDir.absolutePath} is not existing!")
        }
        Map<String, Set> addFieldMap = new HashMap<>()
        dynamicFeatureNames.each { String name ->
            File splitManifest = new File(splitManifestDir, name + SdkConstants.DOT_XML)
            if (!splitManifest.exists()) {
                throw new GradleException("Project ${name} manifest file ${splitManifest.absolutePath} is not found!")
            }
            ManifestReader manifestReader = new ManifestReader(splitManifest)
            Set<String> activities = manifestReader.readActivityNames()
            Set<String> services = manifestReader.readServiceNames()
            Set<String> receivers = manifestReader.readReceiverNames()
            Set<String> providers = manifestReader.readProviderNames()
            Set<String> applications = new HashSet<>()
            String applicationName = manifestReader.readApplicationName()
            if (applicationName != null && applicationName.length() > 0) {
                applications.add(applicationName)
            }
            addFieldMap.put(name + "_APPLICATION", applications)
            addFieldMap.put(name + "_ACTIVITIES", activities)
            addFieldMap.put(name + "_SERVICES", services)
            addFieldMap.put(name + "_RECEIVERS", receivers)
            addFieldMap.put(name + "_PROVIDERS", providers)
        }

        def dest = prepareToCreateClass(transformInvocation)
        createSimpleClass(dest, "com.iqiyi.android.qigsaw.core.extension.ComponentInfo", "java.lang.Object", new SimpleClassCreatorTransform.OnVisitListener() {

            @Override
            void onVisit(ClassWriter cw) {
                injectCommonInfo(dest, cw, addFieldMap)
            }
        })
    }

    static void injectCommonInfo(def dest, ClassWriter cw, Map<String, Set> addFieldMap) {
        addFieldMap.each { entry ->
            Set value = entry.value
            if (value.size() > 0) {
                String name = entry.getKey()
                if (name.endsWith("APPLICATION")) {
                    cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                            name, "Ljava/lang/String;", null, value.getAt(0)).visitEnd()
                } else if (name.endsWith("PROVIDERS")) {
                    //create proxy provider.
                    for (String providerName : value) {
                        String splitName = name.split("_PROVIDERS")[0]
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
}
