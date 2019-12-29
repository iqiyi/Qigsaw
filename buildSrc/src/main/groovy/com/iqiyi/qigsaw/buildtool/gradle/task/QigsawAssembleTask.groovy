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

package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.SdkConstants
import com.android.build.gradle.api.ApplicationVariant
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitDetailsProcessorImpl
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitInfoProcessorImpl
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitJsonFileCreatorImpl
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitDetailsProcessor
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitInfoProcessor
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitJsonFileCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitApkSigner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class QigsawAssembleTask extends DefaultTask {

    @Input
    String qigsawId

    @Input
    String splitInfoVersion

    @Input
    String oldApkPath

    @Input
    boolean releaseSplitApk

    @Input
    List<String> restrictWorkProcessesForSplits

    @Input
    List<String> dfClassPaths

    String variantName

    String flavorName

    String appVersionName

    File assetsDir

    File mergeJniLibDir

    List<Project> dfProjects

    @OutputDirectory
    File outputDir

    @InputDirectory
    File splitManifestOutputDir

    @InputDirectory
    File splitApkOutputDir

    @InputDirectory
    File oldApkOutputDir

    QigsawAssembleTask() {
        this.oldApkPath = QigsawSplitExtensionHelper.getOldApk(project)
        this.releaseSplitApk = QigsawSplitExtensionHelper.getReleaseSplitApk(project)
        this.restrictWorkProcessesForSplits = QigsawSplitExtensionHelper.getRestrictWorkProcessesForSplits(project)
    }

    void initArgs(String qigsawId,
                  String splitInfoVersion,
                  String variantName,
                  String flavorName,
                  String appVersionName,
                  File assetsDir,
                  File mergeJniLib,
                  List<Project> dfProjects,
                  List<String> dfClassPaths) {
        this.qigsawId = qigsawId
        this.splitInfoVersion = splitInfoVersion
        this.variantName = variantName
        this.flavorName = flavorName
        this.appVersionName = appVersionName
        this.assetsDir = assetsDir
        this.mergeJniLibDir = mergeJniLib
        this.dfProjects = dfProjects
        this.dfClassPaths = dfClassPaths
    }

    @TaskAction
    void makeSplitJsonFile() {
        makeSplitJsonFileInternal()
    }

    void makeSplitJsonFileInternal() {
        Map<String, SplitInfo> splitInfoMap = new HashMap<>()
        for (Project dfProject : dfProjects) {
            String splitName = dfProject.name
            File splitManifestFile = new File(splitManifestOutputDir, splitName + SdkConstants.DOT_XML)
            File splitApkFile = new File(splitApkOutputDir, splitName + SdkConstants.DOT_ANDROID_PACKAGE)
            String dfFlavorName = null
            String dfVersionName = null
            Integer dfVersionCode = 0
            def dfAndroid = dfProject.extensions.android
            int minApiLevel = dfAndroid.defaultConfig.minSdkVersion.apiLevel
            dfAndroid.applicationVariants.all { ApplicationVariant variant ->
                String dfVariantName = variant.name.capitalize()
                if (dfVariantName.equals(variantName)) {
                    dfFlavorName = variant.flavorName
                    dfVersionName = variant.versionName
                    dfVersionCode = variant.versionCode
                }
            }
            if (dfVersionName == null || dfVersionName.length() == 0) {
                throw new RuntimeException("dynamic feature ${splitName} 'versionName' is not set!")
            }
            if (!splitApkFile.exists() || !splitManifestFile.exists()) {
                if ((flavorName != null && flavorName.length() > 0) && (dfFlavorName == null || dfFlavorName.length() == 0)) {
                    throw new GradleException("Qigsaw Error: Your app project has flavor ${flavorName}, " +
                            "dynamic feature project ${splitName} need set the same flavor config")
                } else {
                    throw new GradleException("Qigsaw Error: Can't find output files of project ${splitName}," +
                            " merged_manifest: ${splitManifestFile}, output_apk: ${splitApkFile}")
                }
            }
            List<String> allDependencies = SplitDependencyStatistics.getInstance().getDependencies(splitName, variantName)
            List<String> dfDependencies = new ArrayList<>()
            if (allDependencies != null) {
                allDependencies.each {
                    if (dfClassPaths.contains(it)) {
                        dfDependencies.add(it.split(":")[1])
                    }
                }
            }
            println("dynamic feature ${splitName} has dependencies: ${dfDependencies.toString()}")
            //sign split apk if in need.
            SplitApkSigner apkSigner = new SplitApkSigner(project, variantName)
            File splitSignedApk = apkSigner.signSplitAPKIfNeed(splitApkFile)
            //create split info
            SplitInfo rawSplitInfo = SplitInfo.newBuilder()
                    .splitApkFile(splitSignedApk)
                    .version(dfVersionName, dfVersionCode)
                    .minSdkVersion(minApiLevel)
                    .dependencies(dfDependencies)
                    .splitName(splitName)
                    .build()

            SplitInfoProcessor infoProcessor = new SplitInfoProcessorImpl(rawSplitInfo,
                    new File(project.buildDir, QigsawAppBasePlugin.QIGSAW_INTERMEDIATES_SPLIT_EXTRACTION))
            SplitInfo splitInfo = infoProcessor.processSplitInfo(
                    splitSignedApk,
                    splitManifestFile,
                    releaseSplitApk,
                    restrictWorkProcessesForSplits)
            splitInfoMap.put(splitInfo.splitName, splitInfo)
        }
        //get Abis that have been merged
        File[] abiDirs = mergeJniLibDir.listFiles()
        Set<String> abiDirNames = null
        boolean copyToAssets = true
        if (abiDirs != null) {
            ImmutableSet.Builder builder = ImmutableSet.builder()
            for (File abiDir : abiDirs) {
                builder.add(abiDir.name)
            }
            abiDirNames = builder.build()
        }
        Set<String> abiFilters = project.android.defaultConfig.ndk.abiFilters
        //check need copy splits to asset dir.
        if (abiFilters == null) {
            if (abiDirNames != null && abiDirNames.size() == 1) {
                copyToAssets = false
            }
        } else {
            if (abiDirNames != null) {
                if (abiFilters.size() == 1 && abiDirNames.containsAll(abiFilters)) {
                    copyToAssets = false
                }
            }
        }
        //fix abiFilters
        Set<String> fixedAbis
        if (abiFilters == null) {
            fixedAbis = abiDirNames
        } else {
            if (abiDirNames != null) {
                ImmutableSet.Builder fixedAbisBuilder = ImmutableSet.builder()
                abiFilters.each {
                    if (abiDirNames.contains(it)) {
                        fixedAbisBuilder.add(it)
                    }
                }
                fixedAbis = fixedAbisBuilder.build()
            } else {
                fixedAbis = abiFilters
            }
        }
        fixedAbis = sortAbis(fixedAbis)
        SplitDetails rawSplitDetails = SplitDetails.newBuilder()
                .qigsawId(qigsawId)
                .appVersionName(appVersionName)
                .builtInUrlPrefix(copyToAssets ? "assets://" : "native://")
                .abiFilters(fixedAbis)
                .build()

        SplitDetailsProcessor detailsProcessor = new SplitDetailsProcessorImpl(project, oldApkOutputDir)
        SplitDetails splitDetails = detailsProcessor.processSplitDetails(rawSplitDetails, splitInfoMap)
        QigsawLogger.e("SplitDetails: \n${splitDetails.toString()}")

        SplitJsonFileCreator fileCreator = new SplitJsonFileCreatorImpl(outputDir, oldApkOutputDir)
        File splitJsonFile = fileCreator.createSplitJsonFile(splitDetails, splitInfoVersion)

        copySplitJsonFileAndSplitAPKs(splitDetails.splits, splitJsonFile, abiDirNames, copyToAssets)
    }

    void copySplitJsonFileAndSplitAPKs(List<SplitInfo> splits, File splitJsonFile, Set<String> abiDirNames, boolean copyToAssets) {
        if (!this.assetsDir.exists()) {
            this.assetsDir.mkdirs()
        }
        File outputJsonFile = new File(assetsDir, splitJsonFile.name)
        if (outputJsonFile.exists()) {
            outputJsonFile.delete()
        }
        FileUtils.copyFile(splitJsonFile, outputJsonFile)
        if (copyToAssets) {
            for (SplitInfo info : splits) {
                File assetsSplitApk = new File(assetsDir, info.splitName + SdkConstants.DOT_ZIP)
                if (assetsSplitApk.exists()) {
                    assetsSplitApk.delete()
                }
                if (info.builtIn) {
                    FileUtils.copyFile(info.splitApk, assetsSplitApk)
                }
            }
        } else {
            abiDirNames.each {
                for (SplitInfo info : splits) {
                    File jniSplitApk = new File(mergeJniLibDir, it + File.separator + "libsplit_" + info.splitName + SdkConstants.DOT_NATIVE_LIBS)
                    if (jniSplitApk.exists()) {
                        jniSplitApk.delete()
                    }
                    if (info.builtIn) {
                        FileUtils.copyFile(info.splitApk, jniSplitApk)
                    }
                }
            }
        }
    }

    static Set<String> sortAbis(Set<String> abis) {
        if (abis == null || abis.isEmpty() || abis.size() == 1) {
            return abis
        }
        ImmutableSet.Builder builder = ImmutableSet.builder()
        if (abis.contains("arm64-v8a")) {
            builder.add("arm64-v8a")
        }
        if (abis.contains("armeabi-v7a")) {
            builder.add("armeabi-v7a")
        }
        if (abis.contains("armeabi")) {
            builder.add("armeabi")
        }
        if (abis.contains("x86")) {
            builder.add("x86")
        }
        if (abis.contains("x86_64")) {
            builder.add("x86_64")
        }
        return builder.build()
    }
}
