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

package com.iqiyi.qigsaw.buildtool.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.iqiyi.qigsaw.buildtool.gradle.compiling.DexReMergeHandler
import com.iqiyi.qigsaw.buildtool.gradle.compiling.FixedMainDexList
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitContentProviderProcessor
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtension
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.task.*
import com.iqiyi.qigsaw.buildtool.gradle.transform.ComponentInfoTransform
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.VersionNumber

class QigsawAppBasePlugin extends QigsawPlugin {

    public static final String QIGSAW_INTERMEDIATES = "build/intermediates/qigsaw/"

    @Override
    void apply(Project project) {
        //create qigsaw extension.
        project.extensions.create("qigsawSplit", QigsawSplitExtension)
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Qigsaw Plugin Error: Android Application plugin required')
        }
        def versionAGP = VersionNumber.parse(AGPCompat.getAndroidGradlePluginVersionCompat())
        if (versionAGP < VersionNumber.parse("3.2.0")) {
            throw new GradleException('generateQigsawApk: Android Gradle Version is required 3.2.0 at least!')
        }
        SplitDependencyStatistics.getInstance().clear()
        //extends from AppExtension
        def android = project.extensions.android
        //create ComponentInfo.class to record Android Component of dynamic features.
        ComponentInfoTransform commonInfoCreatorTransform = new ComponentInfoTransform(project)
        android.registerTransform(commonInfoCreatorTransform)
        project.afterEvaluate {
            //if AAPT2 is disable, package id of plugin resources can not be customized.
            if (!AGPCompat.isAapt2EnabledCompat(project)) {
                throw new GradleException('generateQigsawApk: AAPT2 required')
            }
            boolean hasQigsawTask = hasQigsawTask(project)
            def dynamicFeatures = android.dynamicFeatures
            List<String> dfClassPaths = new ArrayList<>()
            List<Project> dfProjects = new ArrayList<>()
            List<String> dfNames = new ArrayList<>()
            for (String dynamicFeature : dynamicFeatures) {
                Project dfProject = project.rootProject.project(dynamicFeature)
                String classPath = "${dfProject.group}:${dfProject.name}:${dfProject.version}"
                dfClassPaths.add(classPath)
                dfProjects.add(dfProject)
                dfNames.add(dfProject.name)
            }
            commonInfoCreatorTransform.initArgs(dfProjects)
            //config qigsaw tasks
            android.getApplicationVariants().all { ApplicationVariant variant ->
                String versionName = variant.versionName
                String applicationId = variant.applicationId
                if (versionName == null || applicationId == null) {
                    throw new GradleException("Can't read versionName: ${versionName} or applicationId: ${applicationId} from  app project ${project.name}")
                }
                String variantName = variant.name.capitalize()
                //get tasks of Android Gradle Plugin
                Task processManifestTask = AGPCompat.getProcessManifestTask(project, variantName)

                Task mergeAssetsTask = AGPCompat.getMergeAssetsTask(project, variantName)

                Task packageTask = AGPCompat.getPackageTask(project, variantName)

                Task mergeJniLibsTask = AGPCompat.getMergeJniLibsTask(project, variantName)

                Task assembleTask = AGPCompat.getAssemble(variant)

                Task generateBuildConfigTask = AGPCompat.getGenerateBuildConfigTask(project, variantName)

                File baseManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)

                File mergeAssetsDir = new File(AGPCompat.getMergeAssetsBaseDirCompat(mergeAssetsTask))

                File mergeJniLibsDir = AGPCompat.getMergeJniLibsDirCompat(mergeJniLibsTask, versionAGP)

                File packageOutputDir = AGPCompat.getPackageApplicationDirCompat(packageTask)
                //create QigsawConfig.java
                GenerateQigsawConfig generateQigsawConfigTask = project.tasks.create("generate${variantName}QigsawConfig", GenerateQigsawConfig)
                generateQigsawConfigTask.setApplicationId(applicationId)
                generateQigsawConfigTask.setSourceOutputDir(variant.variantData.scope.buildConfigSourceOutputDir)
                String qigsawId = getQigsawId(project, versionName)
                String splitInfoVersion = versionName + "_" + QigsawSplitExtensionHelper.getSplitInfoVersion(project)
                generateQigsawConfigTask.initArgs(hasQigsawTask, qigsawId, versionName, splitInfoVersion, dfNames)
                generateBuildConfigTask.finalizedBy generateQigsawConfigTask
                generateBuildConfigTask.dependsOn processManifestTask

                QigsawAssembleTask qigsawAssembleTask = project.tasks.create("qigsawAssemble${variantName}", QigsawAssembleTask)
                qigsawAssembleTask.outputDir = project.mkdir(QIGSAW_INTERMEDIATES + "split_info" + File.separator + variantName.uncapitalize())

                qigsawAssembleTask.initArgs(
                        qigsawId,
                        splitInfoVersion,
                        variantName,
                        variant.flavorName,
                        versionName,
                        mergeAssetsDir,
                        mergeJniLibsDir,
                        packageOutputDir,
                        baseManifestFile,
                        dfProjects,
                        dfClassPaths)

                generateQigsawConfigTask.setGroup(QIGSAW)
                qigsawAssembleTask.setGroup(QIGSAW)

                //set task dependency
                if (hasQigsawTask) {
                    //config auto-proguard
                    boolean proguardEnable = variant.getBuildType().isMinifyEnabled()
                    if (proguardEnable) {
                        QigsawProguardConfigTask proguardConfigTask = project.tasks.create("qigsawProcess${variantName}Proguard", QigsawProguardConfigTask)
                        proguardConfigTask.applicationVariant = variant
                        Task r8Task = AGPCompat.getR8Task(project, variantName)
                        Task proguardTask = AGPCompat.getProguardTask(project, variantName)
                        if (proguardTask != null) {
                            proguardTask.dependsOn proguardConfigTask
                        } else {
                            if (r8Task != null) {
                                r8Task.dependsOn proguardConfigTask
                            }
                        }
                        proguardConfigTask.mustRunAfter processManifestTask
                    }
                    //remove tinker auto-proguard configuration for 'class * extends android.app.Application', because qigsaw support load split application.
                    boolean multiDexEnabled = variant.variantData.variantConfiguration.isMultiDexEnabled()
                    if (multiDexEnabled) {
                        def multidexTask = AGPCompat.getMultiDexTask(project, variantName)
                        if (multidexTask != null) {
                            removeRulesAboutMultiDex(multidexTask, variant)
                        }
                    }

                    dfProjects.each { Project dfProject ->
                        try {
                            configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask)
                            println("dynamic feature project ${dfProject.name} has been evaluated!")
                        } catch (Throwable ignored) {
                            dfProject.afterEvaluate {
                                configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask)
                                println("dynamic feature project ${dfProject.name} has not been evaluated!")
                            }
                        }
                    }

                    qigsawAssembleTask.dependsOn(mergeJniLibsTask)

                    mergeJniLibsTask.dependsOn(mergeAssetsTask)

                    qigsawAssembleTask.finalizedBy(assembleTask)

                    packageTask.doFirst {
                        if (versionAGP < VersionNumber.parse("3.5.0")) {
                            Task dexSplitterTask = AGPCompat.getDexSplitterTask(project, variantName)
                            if (dexSplitterTask != null) {
                                println("start to remerge dex files!")
                                def startTime = new Date()
                                List<File> dexFiles = new ArrayList<>()
                                inputs.files.each { File file ->
                                    file.listFiles().each { x ->
                                        if (x.name.endsWith(".dex") && x.name.startsWith("classes")) {
                                            dexFiles.add(x)
                                        }
                                    }
                                }
                                DexReMergeHandler handler = new DexReMergeHandler(project, variant)
                                handler.reMerge(dexFiles)
                                def endTime = new Date()
                                println endTime.getTime() - startTime.getTime() + "ms"
                            }
                        }
                    }
                    packageTask.doLast {
                        qigsawAssembleTask.afterPackageApp()
                    }
                    processManifestTask.doLast {
                        SplitContentProviderProcessor providerProcessor = new SplitContentProviderProcessor(variantName, dfProjects)
                        File bundleManifestDir = AGPCompat.getBundleManifestDirCompat(processManifestTask)
                        //3.2.x has no bundle_manifest dir
                        File bundleManifestFile = bundleManifestDir == null ? null : new File(bundleManifestDir, AGPCompat.ANDROIDMANIFEST_DOT_XML)
                        providerProcessor.process(baseManifestFile, bundleManifestFile)
                    }
                }
            }
        }
    }

    private static void configQigsawAssembleTaskDependencies(Project dfProject, String baseVariantName, Task mergeJniLibsTask) {
        dfProject.extensions.android.applicationVariants.all { ApplicationVariant variant ->
            if (baseVariantName.equals(variant.name.capitalize())) {
                Task dfAssembleTask = AGPCompat.getAssemble(variant)
                println("${dfProject.name} assemble${baseVariantName} has been depended!")
                mergeJniLibsTask.dependsOn dfAssembleTask
                AnalyzeDependenciesTask analyzeDependenciesTask = dfProject.tasks.create("analyzeDependencies${variant.name.capitalize()}", AnalyzeDependenciesTask)
                analyzeDependenciesTask.initArgs(variant.name.capitalize())
                dfAssembleTask.finalizedBy analyzeDependenciesTask
            }
        }
    }

    static void removeRulesAboutMultiDex(Task multidexTask, ApplicationVariant appVariant) {
        multidexTask.doFirst {
            FixedMainDexList handler = new FixedMainDexList(project, appVariant)
            handler.execute()
        }
    }

    static String getQigsawId(Project project, String versionName) {
        try {
            String gitRev = 'git rev-parse --short HEAD'.execute(null, project.rootDir).text.trim()
            if (gitRev == null) {
                return "NO_GIT"
            }
            return "${versionName}_${gitRev}"
        } catch (Exception e) {
            return "${versionName}_NO_GIT"
        }
    }

}
