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
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.security.MessageDigest

class ComponentInfoCreatorTransform extends Transform {

    Project project

    ComponentInfoCreatorTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "CreateComponentInfoTransform"
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
        BaseAppModuleExtension android = project.extensions.getByType(BaseAppModuleExtension)
        def dynamicFeatures = android.dynamicFeatures
        Map<String, List> addFieldMap = new HashMap<>()

        for (String dynamicFeature : dynamicFeatures) {
            Project dynamicFeatureProject = project.rootProject.project(dynamicFeature)
            AppExtension dynamicAndroid = dynamicFeatureProject.extensions.getByType(AppExtension)
            File splitManifest = null
            dynamicAndroid.applicationVariants.all { variant ->
                ApplicationVariant appVariant = variant
                if (appVariant.name == transformInvocation.context.variantName) {
                    File mergedManifestDir = AGPCompat.getMergedManifestDirCompat(dynamicFeatureProject, appVariant.name.capitalize())
                    splitManifest = new File(mergedManifestDir, "AndroidManifest.xml")
                }
            }
            String splitName = dynamicFeatureProject.getName()
            if (null != splitManifest) {
                ManifestReader manifestReader = new ManifestReaderImpl(splitManifest)

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
        }

        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def md5 = getStringMD5(jarInput.file.getAbsolutePath())
                File dest = transformInvocation.outputProvider.getContentLocation(jarName + md5,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }

        def dest = transformInvocation.outputProvider.getContentLocation("main",
                getOutputTypes(), getScopes(),
                Format.DIRECTORY)
        weave(addFieldMap, dest)
    }

    static String getStringMD5(String str) {
        return MessageDigest.getInstance("MD5").digest(str.bytes).encodeHex().toString()
    }


    static void weave(Map<String, List> addFieldMap, File dest) {
        ClassWriter cw = new ClassWriter(0)
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "com/iqiyi/android/qigsaw/core/extension/ComponentInfo", null, "java/lang/Object", null)
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
                    createSplitProviderClassFile(dest, name, providerName)
                }
            } else {
                cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                        name, "Ljava/lang/String;", null,
                        (value as String[]).join(",")).visitEnd()
            }
        }

        File folder = new File(dest.absolutePath + File.separator + "com" +
                File.separator + "iqiyi" +
                File.separator + "android" +
                File.separator + "qigsaw" +
                File.separator + "core" +
                File.separator + "extension")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        new File(folder.absolutePath + File.separator + "ComponentInfo.class")
                .withOutputStream { os ->
            os.write(cw.toByteArray())
        }
    }

    static void createSplitProviderClassFile(File dest, String name, String providerClassName) {
        String splitName = name.split("_")[0]
        providerClassName = providerClassName + "_Decorated_" + splitName
        ClassWriter cw = new ClassWriter(0)
        String folderName = providerClassName.replace(".", File.separator)
        File providerClassFile = new File(dest.absolutePath + File.separator + folderName + ".class")
        if (!providerClassFile.getParentFile().exists()) {
            providerClassFile.getParentFile().mkdirs()
        }

        cw.visit(Opcodes.V1_7,
                Opcodes.ACC_PUBLIC,
                providerClassName.replace(".", "/"), null,
                "com/iqiyi/android/qigsaw/core/SplitContentProvider",
                null)

        MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V",
                null, null)
        mw.visitVarInsn(Opcodes.ALOAD, 0)
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/iqiyi/android/qigsaw/core/SplitContentProvider", "<init>",
                "()V")
        mw.visitInsn(Opcodes.RETURN)
        mw.visitMaxs(1, 1)
        mw.visitEnd()

        providerClassFile
                .withOutputStream { os ->
            os.write(cw.toByteArray())
        }

    }
}
