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

import com.android.SdkConstants
import com.android.build.gradle.api.ApplicationVariant
import com.iqiyi.qigsaw.buildtool.gradle.compiling.DexReMergeHandler
import com.iqiyi.qigsaw.buildtool.gradle.compiling.FixedMainDexList
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtension
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TinkerHelper
import com.iqiyi.qigsaw.buildtool.gradle.task.*
import com.iqiyi.qigsaw.buildtool.gradle.transform.ComponentInfoTransform

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.VersionNumber

class QigsawAppBasePlugin extends QigsawPlugin {

    public static final String QIGSAW_INTERMEDIATES = "intermediates/qigsaw/"

    public static final String QIGSAW_INTERMEDIATES_MAPPING = "${QIGSAW_INTERMEDIATES}mapping/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_INFO = "${QIGSAW_INTERMEDIATES}split_info/"

    public static final String QIGSAW_INTERMEDIATES_OLD_APK = "${QIGSAW_INTERMEDIATES}old_apk/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_APK = "${QIGSAW_INTERMEDIATES}split_apk/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_MANIFEST = "${QIGSAW_INTERMEDIATES}split_manifest/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_EXTRACTION = "${QIGSAW_INTERMEDIATES}split_extraction/"

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
                //output dir of AGP
                File mergeAssetsDir = new File(AGPCompat.getMergeAssetsBaseDirCompat(mergeAssetsTask))
                File mergeJniLibsDir = AGPCompat.getMergeJniLibsDirCompat(mergeJniLibsTask, versionAGP)
                File packageOutputDir = AGPCompat.getPackageApplicationDirCompat(packageTask)
                //output dir of qigsaw tasks
                File oldApkOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_OLD_APK + variantName.uncapitalize())
                File qigsawProguardOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_MAPPING + variantName.uncapitalize())
                File qigsawAssembleOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_SPLIT_INFO + variantName.uncapitalize())
                File splitApkOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_SPLIT_APK + variantName.uncapitalize())
                File splitManifestOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_SPLIT_MANIFEST + variantName.uncapitalize())
                //compat fot tinker
                String oldApk = TinkerHelper.getOldApk(project)
                if (oldApk == null) {
                    oldApk = QigsawSplitExtensionHelper.getOldApk(project)
                }
                QigsawProcessOldApkTask processOldApkTask = project.tasks.create("qigsawProcess${variantName}OldApk", QigsawProcessOldApkTask)
                processOldApkTask.initArgs(hasQigsawTask, versionName, oldApk)
                processOldApkTask.outputDir = oldApkOutputDir
                //create QigsawConfig.java
                GenerateQigsawConfig generateQigsawConfigTask = project.tasks.create("generate${variantName}QigsawConfig", GenerateQigsawConfig)
                generateQigsawConfigTask.setApplicationId(applicationId)
                generateQigsawConfigTask.setSourceOutputDir(variant.variantData.scope.buildConfigSourceOutputDir)
                String qigsawId = getQigsawId(project, versionName)
                String splitInfoVersion = versionName + "_" + QigsawSplitExtensionHelper.getSplitInfoVersion(project)

                generateQigsawConfigTask.initArgs(hasQigsawTask, qigsawId, versionName, splitInfoVersion, dfNames)
                generateQigsawConfigTask.oldApkOutputDir = oldApkOutputDir
                generateBuildConfigTask.finalizedBy generateQigsawConfigTask
                generateBuildConfigTask.dependsOn processManifestTask
                generateBuildConfigTask.dependsOn processOldApkTask

                QigsawAssembleTask qigsawAssembleTask = project.tasks.create("qigsawAssemble${variantName}", QigsawAssembleTask)
                qigsawAssembleTask.outputDir = qigsawAssembleOutputDir
                qigsawAssembleTask.splitApkOutputDir = splitApkOutputDir
                qigsawAssembleTask.splitManifestOutputDir = splitManifestOutputDir
                qigsawAssembleTask.oldApkOutputDir = oldApkOutputDir
                qigsawAssembleTask.initArgs(
                        qigsawId,
                        splitInfoVersion,
                        variantName,
                        variant.flavorName,
                        versionName,
                        mergeAssetsDir,
                        mergeJniLibsDir,
                        dfProjects,
                        dfClassPaths)

                generateQigsawConfigTask.setGroup(QIGSAW)
                qigsawAssembleTask.setGroup(QIGSAW)

                //set task dependency
                if (hasQigsawTask) {
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
                            configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask, splitApkOutputDir, splitManifestOutputDir)
                            println("dynamic feature project ${dfProject.name} has been evaluated!")
                        } catch (Throwable ignored) {
                            dfProject.afterEvaluate {
                                configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask, splitApkOutputDir, splitManifestOutputDir)
                                println("dynamic feature project ${dfProject.name} has not been evaluated!")
                            }
                        }
                    }
                    qigsawAssembleTask.dependsOn(mergeJniLibsTask)
                    mergeJniLibsTask.dependsOn(mergeAssetsTask)
                    qigsawAssembleTask.finalizedBy(assembleTask)

                    File bundleManifestDir = AGPCompat.getBundleManifestDirCompat(processManifestTask)
                    //3.2.x has no bundle_manifest dir
                    File bundleManifestFile = bundleManifestDir == null ? null : new File(bundleManifestDir, AGPCompat.ANDROIDMANIFEST_DOT_XML)
                    File mergedManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)
                    QigsawProcessManifestTask qigsawProcessManifestTask = project.tasks.create("qigsawProcess${variantName}Manifest", QigsawProcessManifestTask)

                    qigsawProcessManifestTask.initArgs(variantName, mergedManifestFile, bundleManifestFile)
                    qigsawProcessManifestTask.splitManifestOutputDir = splitManifestOutputDir
                    qigsawProcessManifestTask.mustRunAfter processManifestTask
                    generateBuildConfigTask.dependsOn qigsawProcessManifestTask

                    //config auto-proguard
                    boolean proguardEnable = variant.getBuildType().isMinifyEnabled()
                    if (proguardEnable) {
                        QigsawProguardConfigTask proguardConfigTask = project.tasks.create("qigsawProcess${variantName}Proguard", QigsawProguardConfigTask)
                        proguardConfigTask.applicationVariant = variant
                        proguardConfigTask.outputDir = qigsawProguardOutputDir
                        Task r8Task = AGPCompat.getR8Task(project, variantName)
                        Task proguardTask = AGPCompat.getProguardTask(project, variantName)
                        if (proguardTask != null) {
                            proguardTask.dependsOn proguardConfigTask
                        } else {
                            if (r8Task != null) {
                                r8Task.dependsOn proguardConfigTask
                            }
                        }
                        proguardConfigTask.mustRunAfter qigsawProcessManifestTask
                        //set qigsaw proguard file.
                        variant.getBuildType().buildType.proguardFiles(proguardConfigTask.getOutputProguardFile())
                    }
                    packageTask.doFirst {
                        if (versionAGP < VersionNumber.parse("3.5.0")) {
                            Task dexSplitterTask = AGPCompat.getDexSplitterTask(project, variantName)
                            if (dexSplitterTask != null) {
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
                        qigsawAssembleOutputDir.listFiles().each {
                            if (it.name.endsWith(SdkConstants.DOT_JSON)) {
                                File qigsawOutputFile = new File(packageOutputDir, it.name)
                                if (qigsawOutputFile.exists()) {
                                    qigsawOutputFile.delete()
                                }
                                FileUtils.copyFile(it, qigsawOutputFile)
                            }
                        }
                    }
                }
            }
        }
    }

    private static void configQigsawAssembleTaskDependencies(Project dfProject, String baseVariantName,
                                                             Task mergeJniLibsTask, File splitApkOutputDir, File splitManifestOutputDir) {
        dfProject.extensions.android.applicationVariants.all { ApplicationVariant variant ->
            String variantName = variant.name.capitalize()
            if (baseVariantName.equals(variantName)) {
                File splitApkFile = null
                variant.outputs.each {
                    splitApkFile = it.outputFile
                }
                Task dfAssembleTask = AGPCompat.getAssemble(variant)
                mergeJniLibsTask.dependsOn dfAssembleTask
                AnalyzeDependenciesTask analyzeDependenciesTask = dfProject.tasks.create("analyzeDependencies${variantName}", AnalyzeDependenciesTask)
                analyzeDependenciesTask.initArgs(variant.name.capitalize())
                dfAssembleTask.finalizedBy analyzeDependenciesTask
                Task dfProcessManifestTask = AGPCompat.getProcessManifestTask(dfProject, variantName)
                //copy split output files
                dfAssembleTask.doLast {
                    File outputApk = new File(splitApkOutputDir, dfProject.name + SdkConstants.DOT_ANDROID_PACKAGE)
                    if (outputApk.exists()) {
                        outputApk.delete()
                    }
                    if (!splitApkOutputDir.exists()) {
                        splitApkOutputDir.mkdirs()
                    }
                    FileUtils.copyFile(splitApkFile, outputApk)
                }
                File splitManifestFile = AGPCompat.getMergedManifestFileCompat(dfProcessManifestTask)
                dfProcessManifestTask.doLast {
                    File outputManifest = new File(splitManifestOutputDir, dfProject.name + SdkConstants.DOT_XML)
                    if (outputManifest.exists()) {
                        outputManifest.delete()
                    }
                    if (!splitManifestOutputDir.exists()) {
                        splitManifestOutputDir.mkdirs()
                    }
                    FileUtils.copyFile(splitManifestFile, outputManifest)
                }
                println("${dfProject.name} assemble${baseVariantName} has been depended!")
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
