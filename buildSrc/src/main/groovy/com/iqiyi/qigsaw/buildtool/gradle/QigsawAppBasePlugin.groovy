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
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TinkerHelper
import com.iqiyi.qigsaw.buildtool.gradle.task.*
import com.iqiyi.qigsaw.buildtool.gradle.transform.ComponentInfoTransform

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.util.VersionNumber

class QigsawAppBasePlugin extends QigsawPlugin {

    public static final String QIGSAW_INTERMEDIATES = "intermediates/qigsaw/"

    public static final String QIGSAW_INTERMEDIATES_MAPPING = "${QIGSAW_INTERMEDIATES}mapping/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_INFO = "${QIGSAW_INTERMEDIATES}split_info/"

    public static final String QIGSAW_INTERMEDIATES_QIGSAW_CONFIG = "${QIGSAW_INTERMEDIATES}qigsaw_config/"

    public static final String QIGSAW_INTERMEDIATES_OLD_APK = "${QIGSAW_INTERMEDIATES}old_apk/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_APK = "${QIGSAW_INTERMEDIATES}split_apk/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_MANIFEST = "${QIGSAW_INTERMEDIATES}split_manifest/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_EXTRACTION = "${QIGSAW_INTERMEDIATES}split_extraction/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_DEPENDENCIES = "${QIGSAW_INTERMEDIATES}split_dependencies/"

    public static final String QIGSAW_INTERMEDIATES_SPLIT_UPLOAD = "${QIGSAW_INTERMEDIATES}split_upload/"

    @Override
    void apply(Project project) {
        //create qigsaw extension.
        project.extensions.create("qigsawSplit", QigsawSplitExtension)
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Qigsaw Error: Android Application plugin required')
        }
        def versionAGP = VersionNumber.parse(AGPCompat.getAndroidGradlePluginVersionCompat())
        if (versionAGP < VersionNumber.parse("3.2.0")) {
            throw new GradleException('Qigsaw Error: Android Gradle Version is required 3.2.0 at least!')
        }
        //extends from AppExtension
        def android = project.extensions.android
        //create ComponentInfo.class to record Android Component of dynamic features.
        ComponentInfoTransform commonInfoCreatorTransform = new ComponentInfoTransform(project)
        android.registerTransform(commonInfoCreatorTransform)
        project.afterEvaluate {
            //if AAPT2 is disable, package id of plugin resources can not be customized.
            if (!AGPCompat.isAapt2EnabledCompat(project)) {
                throw new GradleException('Qigsaw Error: AAPT2 required')
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
                File splitDependenciesOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_SPLIT_DEPENDENCIES + variantName.uncapitalize())
                File qigsawConfigOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_QIGSAW_CONFIG + variantName.uncapitalize())
                File splitUploadOutputDir = new File(project.buildDir, QIGSAW_INTERMEDIATES_SPLIT_UPLOAD + variantName.uncapitalize())
                //compat fot tinker
                String oldApk = TinkerHelper.getOldApk(project)
                if (oldApk == null) {
                    oldApk = QigsawSplitExtensionHelper.getOldApk(project)
                }
                QigsawProcessOldApkTask processOldApkTask = project.tasks.create("qigsawProcess${variantName}OldApk", QigsawProcessOldApkTask)
                processOldApkTask.initArgs(hasQigsawTask, versionName, oldApk == null ? null : new File(oldApk))
                processOldApkTask.outputDir = oldApkOutputDir
                //create QigsawConfig.java
                GenerateQigsawConfig generateQigsawConfigTask = project.tasks.create("generate${variantName}QigsawConfig", GenerateQigsawConfig)
                generateQigsawConfigTask.oldApkOutputDir = oldApkOutputDir
                generateQigsawConfigTask.outputDir = qigsawConfigOutputDir
                generateQigsawConfigTask.sourceOutputDir = variant.variantData.scope.buildConfigSourceOutputDir
                String qigsawId = getQigsawId(project, versionName)
                String splitInfoVersion = versionName + "_" + QigsawSplitExtensionHelper.getSplitInfoVersion(project)
                generateQigsawConfigTask.initArgs(hasQigsawTask, qigsawId, applicationId, versionName, splitInfoVersion, dfNames)
                generateQigsawConfigTask.dependsOn processOldApkTask
                generateQigsawConfigTask.dependsOn generateBuildConfigTask
                generateQigsawConfigTask.setGroup(QIGSAW)
                generateBuildConfigTask.finalizedBy generateQigsawConfigTask

                QigsawAssembleTask qigsawAssembleTask = project.tasks.create("qigsawAssemble${variantName}", QigsawAssembleTask)
                qigsawAssembleTask.outputDir = qigsawAssembleOutputDir
                qigsawAssembleTask.splitApkOutputDir = splitApkOutputDir
                qigsawAssembleTask.splitManifestOutputDir = splitManifestOutputDir
                qigsawAssembleTask.oldApkOutputDir = oldApkOutputDir
                qigsawAssembleTask.splitDependenciesOutputDir = splitDependenciesOutputDir
                Set<String> mergedAbiFilters = new HashSet<>(0)
                if (android.productFlavors != null) {
                    android.productFlavors.each {
                        if (variant.flavorName.contains(it.name.capitalize()) || variant.flavorName.contains(it.name.uncapitalize())) {
                            Set<String> flavorAbiFilter = it.ndk.abiFilters
                            if (flavorAbiFilter != null) {
                                mergedAbiFilters.addAll(flavorAbiFilter)
                            }
                        }
                    }
                }
                Set<String> abiFilters = android.defaultConfig.ndk.abiFilters
                if (abiFilters != null) {
                    mergedAbiFilters.addAll(abiFilters)
                }
                QigsawLogger.w("your abiFilters are: " + mergedAbiFilters)
                qigsawAssembleTask.initArgs(
                        qigsawId,
                        versionAGP,
                        splitInfoVersion,
                        variantName,
                        variant.flavorName,
                        versionName,
                        mergeAssetsDir,
                        mergeJniLibsDir,
                        mergedAbiFilters,
                        dfProjects,
                        dfClassPaths)
                qigsawAssembleTask.setGroup(QIGSAW)
                File apkFile = null
                variant.outputs.each {
                    apkFile = it.outputFile
                }
                QigsawInstallTask qigsawInstallTask = project.tasks.create("qigsawInstall${variantName}", QigsawInstallTask)
                qigsawInstallTask.variantData = variant.variantData
                qigsawInstallTask.installApkFile = apkFile
                qigsawInstallTask.setGroup(QIGSAW)

                boolean isSigningNeed = variant.buildType.signingConfig != null && variant.buildType.signingConfig.isSigningReady()
                QigsawUploadSplitApkTask uploadSplitApkTask = project.tasks.create("qigsawUploadSplit${variantName}", QigsawUploadSplitApkTask)
                uploadSplitApkTask.initArgs(oldApk == null ? null : new File(oldApk), splitUploadOutputDir, packageOutputDir, variantName, isSigningNeed)
                uploadSplitApkTask.setGroup(QIGSAW)
                //set task dependency
                if (hasQigsawTask) {

                    dfProjects.each { Project dfProject ->
                        try {
                            configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask, dfClassPaths,
                                    splitApkOutputDir, splitManifestOutputDir, splitDependenciesOutputDir)
                            QigsawLogger.w("dynamic feature project ${dfProject.name} has been evaluated!")
                        } catch (Throwable ignored) {
                            dfProject.afterEvaluate {
                                configQigsawAssembleTaskDependencies(dfProject, variantName, mergeJniLibsTask, dfClassPaths,
                                        splitApkOutputDir, splitManifestOutputDir, splitDependenciesOutputDir)
                                QigsawLogger.w("dynamic feature project ${dfProject.name} has not been evaluated!")
                            }
                        }
                    }

                    qigsawAssembleTask.dependsOn mergeJniLibsTask
                    mergeJniLibsTask.dependsOn mergeAssetsTask
                    qigsawAssembleTask.finalizedBy assembleTask
                    qigsawInstallTask.dependsOn qigsawAssembleTask
                    qigsawInstallTask.mustRunAfter assembleTask

                    File bundleManifestDir = AGPCompat.getBundleManifestDirCompat(processManifestTask)
                    //3.2.x has no bundle_manifest dir
                    File bundleManifestFile = bundleManifestDir == null ? null : new File(bundleManifestDir, AGPCompat.ANDROIDMANIFEST_DOT_XML)
                    File mergedManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)
                    QigsawProcessManifestTask qigsawProcessManifestTask = project.tasks.create("qigsawProcess${variantName}Manifest", QigsawProcessManifestTask)

                    qigsawProcessManifestTask.initArgs(variantName, bundleManifestFile)
                    qigsawProcessManifestTask.splitManifestOutputDir = splitManifestOutputDir
                    qigsawProcessManifestTask.mergedManifestFile = mergedManifestFile
                    qigsawProcessManifestTask.mustRunAfter processManifestTask
                    generateQigsawConfigTask.dependsOn qigsawProcessManifestTask

                    Task r8Task = AGPCompat.getR8Task(project, variantName)
                    //remove tinker auto-proguard configuration for 'class * extends android.app.Application', because qigsaw support load split application.
                    boolean multiDexEnabled = variant.variantData.variantConfiguration.isMultiDexEnabled()
                    if (multiDexEnabled) {
                        Task multiDexTask = AGPCompat.getMultiDexTask(project, variantName)
                        //if R8 is enable, there is no multiDex task.
                        if (multiDexTask != null) {
                            QigsawLogger.w("Task ${multiDexTask.name} is found!")
                            removeRulesAboutMultiDex(multiDexTask, variant)
                        } else {
                            if (r8Task != null) {
                                QigsawLogger.w("Task ${r8Task.name} is found!")
                                removeRulesAboutMultiDex(r8Task, variant)
                            }
                        }
                    }
                    //config auto-proguard
                    boolean proguardEnable = variant.getBuildType().isMinifyEnabled()
                    if (proguardEnable) {
                        QigsawProguardConfigTask proguardConfigTask = project.tasks.create("qigsawProcess${variantName}Proguard", QigsawProguardConfigTask)
                        proguardConfigTask.outputDir = qigsawProguardOutputDir
                        proguardConfigTask.initArgs(applicationId)
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
                                                             Task mergeJniLibsTask, List<String> dfClassPaths,
                                                             File splitApkOutputDir, File splitManifestOutputDir, File splitDependenciesOutputDir) {
        dfProject.extensions.android.applicationVariants.all { ApplicationVariant variant ->
            String variantName = variant.name.capitalize()
            if (baseVariantName.equals(variantName)) {
                File splitApkFile = null
                variant.outputs.each {
                    splitApkFile = it.outputFile
                }
                //copy split output files
                CopySplitApkTask copySplitApkTask = dfProject.tasks.create("copySplitApk${variantName}", CopySplitApkTask)
                copySplitApkTask.splitApkFile = splitApkFile
                copySplitApkTask.splitApkOutputDir = splitApkOutputDir

                Task dfProcessManifestTask = AGPCompat.getProcessManifestTask(dfProject, variantName)
                File splitManifestFile = AGPCompat.getMergedManifestFileCompat(dfProcessManifestTask)
                CopySplitManifestTask copySplitManifestTask = dfProject.tasks.create("copySplitManifest${variantName}", CopySplitManifestTask)
                copySplitManifestTask.splitManifestFile = splitManifestFile
                copySplitManifestTask.splitManifestOutputDir = splitManifestOutputDir
                dfProcessManifestTask.finalizedBy copySplitManifestTask
                Task dfAssembleTask = AGPCompat.getAssemble(variant)
                copySplitApkTask.dependsOn dfAssembleTask
                mergeJniLibsTask.dependsOn copySplitApkTask

                List<String> splitDependencies = new ArrayList<>()
                Configuration configuration = dfProject.configurations."${variantName.uncapitalize()}CompileClasspath"
                configuration.incoming.dependencies.each {
                    splitDependencies.add("${it.group}:${it.name}:${it.version}")
                }
                AnalyzeSplitDependenciesTask analyzeDependenciesTask = dfProject.tasks.create("analyzeSplitDependencies${variantName}", AnalyzeSplitDependenciesTask)
                analyzeDependenciesTask.initArgs(splitDependencies, dfClassPaths)
                analyzeDependenciesTask.outputDir = splitDependenciesOutputDir
                analyzeDependenciesTask.mustRunAfter dfProcessManifestTask
                dfAssembleTask.dependsOn analyzeDependenciesTask
                QigsawLogger.w("${dfProject.name} assemble${baseVariantName} has been depended!")
            }
        }
    }

    static void removeRulesAboutMultiDex(Task multiDexTask, ApplicationVariant appVariant) {
        multiDexTask.doFirst {
            FixedMainDexList handler = new FixedMainDexList(appVariant)
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
