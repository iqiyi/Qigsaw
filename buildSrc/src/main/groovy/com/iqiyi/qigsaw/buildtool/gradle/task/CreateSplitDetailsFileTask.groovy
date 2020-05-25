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
import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploadException
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploader
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploaderInstance
import org.codehaus.plexus.util.dag.DAG
import org.codehaus.plexus.util.dag.TopologicalSorter
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CreateSplitDetailsFileTask extends ProcessOldOutputsBaseTask {

    @Input
    String qigsawId

    @Input
    String baseVersionName

    @Input
    String completeSplitInfoVersion

    @Input
    Set<String> abiFilters

    @Input
    Set<String> dynamicFeaturesNames

    @Input
    @Optional
    Set<String> splitEntryFragments

    @InputDirectory
    File splitApksDir

    @InputDirectory
    File splitInfoDir

    @OutputFile
    File splitDetailsFile

    @OutputFile
    File updateRecordFile

    @OutputFile
    File baseAppCpuAbiListFile

    @OutputDirectory
    File qigsawMergedAssetsDir

    @OutputDirectory
    File mergedJniLibsBaseDir

    CreateSplitDetailsFileTask() {
        this.splitEntryFragments = QigsawSplitExtensionHelper.getSplitEntryFragments(project)
    }

    @TaskAction
    void doCreation() {
        List<SplitInfo> splitInfoList = new ArrayList<>()
        dynamicFeaturesNames.each {
            File splitInfoFile = new File(splitInfoDir, it + SdkConstants.DOT_JSON)
            if (!splitInfoFile.exists()) {
                throw new GradleException("Qigsaw Error: split-info file ${splitInfoFile.absolutePath} is not existing!")
            }
            SplitInfo splitInfo = TypeClassFileParser.parseFile(splitInfoFile, SplitInfo)
            splitInfoList.add(splitInfo)
        }
        splitInfoList.each {
            it.dependencies = fixSplitDependencies(it.splitName, splitInfoList)
        }
        splitInfoList = rearrangeSplits(splitInfoList)
        File oldSplitDetailsFile = getOldSplitDetailsFile()
        SplitDetails details = createSplitDetails(splitInfoList, oldSplitDetailsFile)
        if (splitDetailsFile.exists()) {
            splitDetailsFile.delete()
        }
        FileUtils.createFileForTypeClass(details, splitDetailsFile)
        if (updateRecordFile.exists()) {
            updateRecordFile.delete()
        }
        FileUtils.createFileForTypeClass(details.updateRecord, updateRecordFile)
        moveOutputsToMergedAssetsDir(oldSplitDetailsFile, details)
    }

    void moveOutputsToMergedAssetsDir(File oldSplitDetailsFile, SplitDetails splitDetails) {
        if (qigsawMergedAssetsDir.exists()) {
            com.android.utils.FileUtils.deleteDirectoryContents(qigsawMergedAssetsDir)
        }
        File destSplitDetailsFile = new File(qigsawMergedAssetsDir, "qigsaw_${completeSplitInfoVersion + SdkConstants.DOT_JSON}")
        if (splitDetails.updateRecord.updateMode == SplitDetails.UpdateRecord.VERSION_NO_CHANGED) {
            FileUtils.copyFile(oldSplitDetailsFile, destSplitDetailsFile)
        } else {
            FileUtils.copyFile(splitDetailsFile, destSplitDetailsFile)
        }
        Set<String> mergedAbiFilters = getMergedAbiFilters()
        if (baseAppCpuAbiListFile.exists()) {
            baseAppCpuAbiListFile.delete()
        }
        baseAppCpuAbiListFile.write("abiList=${mergedAbiFilters.join(",")}")
        FileUtils.copyFile(baseAppCpuAbiListFile, new File(qigsawMergedAssetsDir.parentFile, baseAppCpuAbiListFile.name))
        splitDetails.splits.each { SplitInfo info ->
            if (info.builtIn) {
                String targetAbi = findTargetAbiForBuiltInSplitApk(info.apkData, mergedAbiFilters)
                if (targetAbi == null) {
                    SplitLogger.e("There is no target abi for 'abiFilters' ${mergedAbiFilters}, dynamic-feature ${info.splitName} can't be 'built-in'!")
                } else {
                    File destSplitApk = new File(qigsawMergedAssetsDir, "${info.splitName}-${targetAbi}${SdkConstants.DOT_ZIP}")
                    if (splitDetails.updateRecord.updateMode != SplitDetails.UpdateRecord.DEFAULT) {
                        File oldSplitApk = getOldSplitApk(info.splitName, targetAbi)
                        if (!oldSplitApk.exists()) {
                            throw new GradleException("Old split apk ${oldSplitApk.absolutePath} is not found, make sure oldApk is matching!")
                        }
                        FileUtils.copyFile(oldSplitApk, destSplitApk)
                    } else {
                        File sourceSplitApk = new File(splitApksDir, "${info.splitName}-${targetAbi}-signed${SdkConstants.DOT_ANDROID_PACKAGE}")
                        if (!sourceSplitApk.exists()) {
                            throw new GradleException("Split apk ${sourceSplitApk.absolutePath} is not found!")
                        }
                        FileUtils.copyFile(sourceSplitApk, destSplitApk)
                    }
                }
            }
        }
    }

    static String findTargetAbiForBuiltInSplitApk(List<SplitInfo.SplitApkData> apkDataList, Set<String> mergedAbiFilters) {
        String targetAbi = null
        if (mergedAbiFilters.isEmpty()) {
            if (apkDataList.size() == 1) {
                targetAbi = apkDataList[0].abi
            } else {
                apkDataList.each { SplitInfo.SplitApkData apkData ->
                    if (!QigsawAppBasePlugin.CUSTOM_SUPPORTED_ABIS.contains(apkData.abi)) {
                        targetAbi = apkData.abi
                    }
                }
            }
        } else {
            String joinMergedAbiFilters = mergedAbiFilters.join("-")
            apkDataList.each { SplitInfo.SplitApkData apkData ->
                if (apkData.abi == joinMergedAbiFilters || apkData.abi == "none") {
                    targetAbi = apkData.abi
                }
            }
        }
        return targetAbi
    }

    Set<String> getMergedAbiFilters() {
        File mergedJniLibsDir = getMergedJniLibsDirCompat()
        Set<String> realABIs = new HashSet<>()
        if (mergedJniLibsDir.exists()) {
            mergedJniLibsDir.listFiles(new FileFilter() {
                @Override
                boolean accept(File file) {
                    if (abiFilters.isEmpty()) {
                        realABIs.add(file.name)
                    } else {
                        if (abiFilters.contains(file.name)) {
                            realABIs.add(file.name)
                        }
                    }
                    return false
                }
            })
        }
        return realABIs
    }

    File getMergedJniLibsDirCompat() {
        File mergedJniLibsDir
        File __content__ = new File(mergedJniLibsBaseDir, "__content__.json")
        if (__content__.exists()) {
            List result = TypeClassFileParser.parseFile(__content__, List.class)
            mergedJniLibsDir = new File(mergedJniLibsBaseDir, "${(int) (result.get(0).index)}/lib")
        } else {
            mergedJniLibsDir = new File(mergedJniLibsBaseDir, "lib")
        }
        return mergedJniLibsDir
    }

    SplitDetails createSplitDetails(List<SplitInfo> splitInfoList, File oldSplitDetailsFile) {
        String qigsawId = this.qigsawId
        List<String> updateSplits = null
        SplitDetails.UpdateRecord updateRecord = new SplitDetails.UpdateRecord()
        if (oldSplitDetailsFile != null && oldSplitDetailsFile.exists()) {
            SplitDetails oldSplitDetails = TypeClassFileParser.parseFile(oldSplitDetailsFile, SplitDetails)
            if (hasSplitVersionChanged(oldSplitDetails.splits, splitInfoList)) {
                qigsawId = oldSplitDetails.qigsawId
                updateSplits = analyzeUpdateSplits(oldSplitDetails.splits, splitInfoList)
                updateRecord.updateMode = SplitDetails.UpdateRecord.VERSION_CHANGED
                updateRecord.updateSplits = updateSplits
                SplitLogger.w("Splits ${updateSplits} need to be updated!")
            } else {
                updateRecord.updateMode = SplitDetails.UpdateRecord.VERSION_NO_CHANGED
                SplitLogger.w("No splits need to be updated, just using old Apks!")
                return oldSplitDetails
            }
        }
        splitInfoList.each { SplitInfo info ->
            uploadSplitApkIfNeed(info)
            removeRedundantSplitApkData(info)
        }
        SplitDetails splitDetails = new SplitDetails()
        splitDetails.updateRecord = updateRecord
        splitDetails.qigsawId = qigsawId
        splitDetails.appVersionName = baseVersionName
        splitDetails.updateSplits = updateSplits
        splitDetails.splitEntryFragments = splitEntryFragments
        splitDetails.splits = splitInfoList
        return splitDetails
    }

    static void removeRedundantSplitApkData(SplitInfo splitInfo) {
        if (!splitInfo.builtIn) {
            if (splitInfo.apkData.size() > 1) {
                List<SplitInfo> tempList = new ArrayList<>()
                splitInfo.apkData.each {
                    if (QigsawAppBasePlugin.CUSTOM_SUPPORTED_ABIS.contains(it.abi)) {
                        tempList.add(it)
                    }
                }
                splitInfo.apkData = tempList
            }
        }
    }

    void uploadSplitApkIfNeed(SplitInfo info) {
        if (!info.builtIn) {
            SplitApkUploader uploader = SplitApkUploaderInstance.get()
            if (uploader != null) {
                for (SplitInfo.SplitApkData data : info.apkData) {
                    if (!data.url.startsWith("http")) {
                        //like ["universal", "armeabi-v7a", "arm64-v8a"], "universal" don't need to be uploaded.
                        if (info.apkData.size() > 1 && !QigsawAppBasePlugin.CUSTOM_SUPPORTED_ABIS.contains(data.abi)) {
                            continue
                        }
                        File apkFile = new File(splitApksDir, info.splitName + "-${data.abi}-signed${SdkConstants.DOT_ANDROID_PACKAGE}")
                        if (!apkFile.exists()) {
                            throw new GradleException("Split apk ${apkFile.absolutePath} is not existing!")
                        }
                        String uploadedUrl = uploader.uploadSync(project, apkFile, info.splitName)
                        if (uploadedUrl != null && uploadedUrl.startsWith("http")) {
                            data.url = uploadedUrl
                            SplitLogger.w("Split apk ${apkFile.absolutePath} upload successfully, url: ${uploadedUrl}")
                        } else {
                            throw new SplitApkUploadException("Split apk ${apkFile.absolutePath} upload failed, url: ${uploadedUrl}")
                        }
                    } else {
                        SplitLogger.w("Split ${info.splitName} has been uploaded: ${data.url}")
                    }
                }
            } else {
                SplitLogger.e("SplitApkUploader has not been implemented, just make ${info.splitName} built-in")
                info.builtIn = true
            }
        }
    }

    static List<SplitInfo> rearrangeSplits(List<SplitInfo> splitInfoList) {
        DAG dag = new DAG()
        for (SplitInfo info : splitInfoList) {
            if (info.dependencies != null) {
                for (String dependency : info.dependencies) {
                    dag.addEdge(info.splitName, dependency)
                }
            }
        }
        List<String> sorted = TopologicalSorter.sort(dag)
        SplitLogger.w("> topological sort result: " + sorted)
        List<SplitInfo> ret = new ArrayList<>()
        sorted.each { String name ->
            SplitInfo temp = null
            splitInfoList.each { SplitInfo info ->
                if (info.splitName == name) {
                    temp = info
                    return
                }
            }
            ret.add(temp)
            splitInfoList.remove(temp)
        }
        ret.addAll(splitInfoList)
        return ret
    }

    static Set<String> fixSplitDependencies(String name, List<SplitInfo> splitInfoList) {
        Set<String> dependencies = null
        splitInfoList.each { SplitInfo info ->
            if (info.splitName == name) {
                dependencies = info.dependencies
                return
            }
        }
        if (dependencies == null) {
            return null
        }
        Set<String> fixedDependencies = new HashSet<>()
        fixedDependencies.addAll(dependencies)
        dependencies.each {
            Set<String> ret = fixSplitDependencies(it, splitInfoList)
            if (ret != null) {
                fixedDependencies.addAll(ret)
            }
        }
        return fixedDependencies
    }

    static boolean hasSplitVersionChanged(List<SplitInfo> oldSplits, List<SplitInfo> newSplits) {
        boolean versionChanged = false
        if (oldSplits != null) {
            newSplits.each { SplitInfo newInfo ->
                oldSplits.each { SplitInfo oldInfo ->
                    if (newInfo.splitName == oldInfo.splitName) {
                        if (newInfo.version != oldInfo.version) {
                            versionChanged = true
                        }
                    }
                }
            }
        }
        return versionChanged
    }

    static List<String> analyzeUpdateSplits(List<SplitInfo> oldSplits, List<SplitInfo> newSplits) {
        List<String> updateSplits = new ArrayList<>(0)
        for (SplitInfo newInfo : newSplits) {
            for (SplitInfo oldInfo : oldSplits) {
                if (newInfo.splitName == oldInfo.splitName) {
                    if (newInfo.version == oldInfo.version) {
                        SplitLogger.w("Split ${newInfo.splitName} version ${newInfo.version} is not changed, using old info!")
                        newInfo = oldInfo.clone()
                    } else {
                        SplitLogger.w("Split ${newInfo.splitName} version ${newInfo.version} is changed, it need to be updated!")
                        newInfo.builtIn = false
                        newInfo.onDemand = true
                        updateSplits.add(newInfo.splitName)
                    }
                }
            }
        }
        return updateSplits
    }

}
