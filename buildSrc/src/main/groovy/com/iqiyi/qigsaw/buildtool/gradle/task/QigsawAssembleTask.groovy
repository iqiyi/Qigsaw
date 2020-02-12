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
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

class QigsawAssembleTask extends DefaultTask {

    static final String CONTENT_JSON_FILE_NAME = "__content__.json"

    @Input
    String qigsawId

    @Input
    String splitInfoVersion

    @Input
    boolean releaseSplitApk

    @Input
    @Optional
    List<String> restrictWorkProcessesForSplits

    @Input
    List<String> dfClassPaths

    @Input
    Set<String> abiFilters

    @Input
    String appVersionName

    def versionAGP

    String flavorName

    String variantName

    @OutputDirectory
    File assetsDir

    @OutputDirectory
    File mergeJniLibDir

    File mergeJniLibInternalDir

    List<Project> dfProjects

    @OutputDirectory
    File outputDir

    @InputDirectory
    File splitManifestOutputDir

    @InputDirectory
    File splitApkOutputDir

    @InputDirectory
    File oldApkOutputDir

    @InputDirectory
    File splitDependenciesOutputDir

    QigsawAssembleTask() {
        this.releaseSplitApk = QigsawSplitExtensionHelper.isReleaseSplitApk(project)
        this.restrictWorkProcessesForSplits = QigsawSplitExtensionHelper.getRestrictWorkProcessesForSplits(project)
    }

    void initArgs(String qigsawId,
                  def versionAGP,
                  String splitInfoVersion,
                  String variantName,
                  String flavorName,
                  String appVersionName,
                  File assetsDir,
                  File mergeJniLib,
                  Set<String> abiFilters,
                  List<Project> dfProjects,
                  List<String> dfClassPaths) {
        this.qigsawId = qigsawId
        this.versionAGP = versionAGP
        this.splitInfoVersion = splitInfoVersion
        this.variantName = variantName
        this.flavorName = flavorName
        this.appVersionName = appVersionName
        this.assetsDir = assetsDir
        this.mergeJniLibDir = mergeJniLib
        this.abiFilters = abiFilters
        this.dfProjects = dfProjects
        this.dfClassPaths = dfClassPaths
    }

    @TaskAction
    void makeSplitJsonFile() {
        Task stripDebugSymbolTask = AGPCompat.getStripDebugSymbolTask(project, variantName)
        if (stripDebugSymbolTask != null && !stripDebugSymbolTask.enabled) {
            QigsawLogger.e("stripDebugSymbol task is not enabled!!")
        }
        if (versionAGP >= VersionNumber.parse("3.5.0")) {
            mergeJniLibInternalDir = new File(mergeJniLibDir, "lib")
        } else {
            File contentJsonFile = new File(mergeJniLibDir, CONTENT_JSON_FILE_NAME)
            if (contentJsonFile.exists()) {
                List contents = TypeClassFileParser.parseFile(contentJsonFile, List.class)
                mergeJniLibInternalDir = new File(mergeJniLibDir, "${(int) (contents.get(0).index)}/lib")
                QigsawLogger.e("> Task :${name} mergeJniLibs content_json :" + contents.toString())
            }
        }
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

            List<String> dfDependencies = findAllSplitDependencies(splitName)

            QigsawLogger.w("dynamic feature ${splitName} has dependencies: ${dfDependencies.toString()}")
            //sign split apk if in need.
            SplitApkSigner apkSigner = new SplitApkSigner(project, variantName)
            File splitSignedApk = apkSigner.signAPKIfNeed(splitApkFile, null)
            //create split info
            SplitInfo rawSplitInfo = SplitInfo.newBuilder()
                    .splitApkFile(splitSignedApk)
                    .version(dfVersionName, dfVersionCode)
                    .minSdkVersion(minApiLevel)
                    .dependencies(dfDependencies)
                    .splitName(splitName)
                    .build()

            SplitInfoProcessor infoProcessor = new SplitInfoProcessorImpl(rawSplitInfo,
                    new File(project.buildDir, "${QigsawAppBasePlugin.QIGSAW_INTERMEDIATES_SPLIT_EXTRACTION}/${variantName.uncapitalize()}/${splitName}"))
            SplitInfo splitInfo = infoProcessor.processSplitInfo(
                    splitSignedApk,
                    splitManifestFile,
                    releaseSplitApk,
                    restrictWorkProcessesForSplits)
            splitInfoMap.put(splitInfo.splitName, splitInfo)
        }
        Set<String> abiDirNames = new HashSet<>(0)
        boolean copyToAssets = true
        if (mergeJniLibInternalDir != null && mergeJniLibInternalDir.exists()) {
            //get abi dirs which have been merged
            File[] abiDirs = mergeJniLibInternalDir.listFiles()
            if (abiDirs != null) {
                for (File abiDir : abiDirs) {
                    abiDirNames.add(abiDir.name)
                }
            }
            //check need copy splits to asset dir.
            QigsawLogger.w("> Task :${getName()} abiFilters -> ${abiFilters}")
            if (abiFilters.empty) {
                if (abiDirNames.size() == 1) {
                    copyToAssets = false
                }
            } else {
                if (!abiDirNames.empty) {
                    if (abiFilters.size() == 1 && abiDirNames.containsAll(abiFilters)) {
                        copyToAssets = false
                    }
                }
            }
        }
        QigsawLogger.w("> Task :${getName()} abiDirNames -> ${abiDirNames}")
        //fix abiFilters
        Set<String> fixedAbis = new HashSet<>(0)
        if (abiFilters.empty) {
            fixedAbis = abiDirNames
        } else {
            if (!abiDirNames.empty) {
                abiFilters.each {
                    if (abiDirNames.contains(it)) {
                        fixedAbis.add(it)
                    }
                }
            } else {
                fixedAbis.addAll(abiFilters)
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

        copySplitJsonFileAndSplitAPKs(splitDetails.splits, splitJsonFile, fixedAbis, copyToAssets)
    }

    List<String> findAllSplitDependencies(String splitName) {
        File dfDependenciesFile = new File(splitDependenciesOutputDir, splitName + SdkConstants.DOT_JSON)
        List<String> splitDependencies = new ArrayList<>(0)
        if (dfDependenciesFile.exists()) {
            List<String> result = TypeClassFileParser.parseFile(dfDependenciesFile, List.class)
            splitDependencies.addAll(result)
            result.each {
                splitDependencies.addAll(findAllSplitDependencies(it))
            }
        }
        return splitDependencies
    }

    void copySplitJsonFileAndSplitAPKs(List<SplitInfo> splits, File splitJsonFile, Set<String> fixedAbis, boolean copyToAssets) {
        if (!this.assetsDir.exists()) {
            this.assetsDir.mkdirs()
        }
        //delete old split json files
        File[] oldSplitJsonFiles = assetsDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.startsWith("qigsaw_") && file.name.endsWith(SdkConstants.DOT_JSON)
            }
        })
        if (oldSplitJsonFiles != null) {
            oldSplitJsonFiles.each {
                it.delete()
            }
        }
        File outputJsonFile = new File(assetsDir, splitJsonFile.name)
        if (outputJsonFile.exists()) {
            outputJsonFile.delete()
        }
        FileUtils.copyFile(splitJsonFile, outputJsonFile)
        //delete old split apk files
        splits.each { SplitInfo info ->
            File assetsSplitApk = new File(assetsDir, info.splitName + SdkConstants.DOT_ZIP)
            if (assetsSplitApk.exists()) {
                assetsSplitApk.delete()
            }
        }
        if (fixedAbis != null) {
            fixedAbis.each {
                splits.each { SplitInfo info ->
                    File jniSplitApk = new File(mergeJniLibInternalDir, it + File.separator + "libsplit_" + info.splitName + SdkConstants.DOT_NATIVE_LIBS)
                    if (jniSplitApk.exists()) {
                        jniSplitApk.delete()
                    }
                }
            }
        }
        //copy new split apk files to target dir.
        if (copyToAssets) {
            splits.each { SplitInfo info ->
                File assetsSplitApk = new File(assetsDir, info.splitName + SdkConstants.DOT_ZIP)
                if (info.builtIn) {
                    FileUtils.copyFile(info.splitApk, assetsSplitApk)
                }
            }
        } else {
            fixedAbis.each {
                splits.each { SplitInfo info ->
                    File jniSplitApk = new File(mergeJniLibInternalDir, it + File.separator + "libsplit_" + info.splitName + SdkConstants.DOT_NATIVE_LIBS)
                    if (info.builtIn) {
                        FileUtils.copyFile(info.splitApk, jniSplitApk)
                    }
                }
            }
        }
    }

    static Set<String> sortAbis(Set<String> abis) {
        if (abis.isEmpty() || abis.size() == 1) {
            return abis
        }
        Set<String> ret = new HashSet<>(abis.size())
        if (abis.contains("arm64-v8a")) {
            ret.add("arm64-v8a")
        }
        if (abis.contains("armeabi-v7a")) {
            ret.add("armeabi-v7a")
        }
        if (abis.contains("armeabi")) {
            ret.add("armeabi")
        }
        if (abis.contains("x86")) {
            ret.add("x86")
        }
        if (abis.contains("x86_64")) {
            ret.add("x86_64")
        }
        return ret
    }
}
