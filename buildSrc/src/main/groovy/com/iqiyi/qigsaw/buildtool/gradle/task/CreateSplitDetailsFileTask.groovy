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

    @InputDirectory
    File mergedJniLibsBaseDir

    @OutputFile
    File splitDetailsFile

    @OutputFile
    File updateRecordFile

    @OutputFile
    File baseAppCpuAbiListFile

    @OutputDirectory
    File qigsawMergedAssetsDir

    CreateSplitDetailsFileTask() {
        this.splitEntryFragments = QigsawSplitExtensionHelper.getSplitEntryFragments(project)
    }

    @TaskAction
    void doCreation() {
        if (splitDetailsFile.exists()) {
            splitDetailsFile.delete()
        }
        if (updateRecordFile.exists()) {
            updateRecordFile.delete()
        }
        if (baseAppCpuAbiListFile.exists()) {
            baseAppCpuAbiListFile.delete()
        }
        if (qigsawMergedAssetsDir.exists()) {
            FileUtils.deleteDir(qigsawMergedAssetsDir)
        }
        qigsawMergedAssetsDir.mkdirs()
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
        File oldSplitDetailsFile = getOldSplitDetailsFile()
        SplitDetails details = createSplitDetails(splitInfoList, oldSplitDetailsFile)
        details.splits = rearrangeSplits(details.splits)
        FileUtils.createFileForTypeClass(details, splitDetailsFile)
        FileUtils.createFileForTypeClass(details.updateRecord, updateRecordFile)
        moveOutputsToMergedAssetsDir(oldSplitDetailsFile, details)
    }

    void moveOutputsToMergedAssetsDir(File oldSplitDetailsFile, SplitDetails splitDetails) {
        File destSplitDetailsFile = new File(qigsawMergedAssetsDir, "qigsaw_${completeSplitInfoVersion + SdkConstants.DOT_JSON}")
        if (splitDetails.updateRecord.updateMode == SplitDetails.UpdateRecord.VERSION_NO_CHANGED) {
            FileUtils.copyFile(oldSplitDetailsFile, destSplitDetailsFile)
        } else {
            FileUtils.copyFile(splitDetailsFile, destSplitDetailsFile)
        }
        Set<String> mergedAbiFilters = getMergedAbiFilters()
        baseAppCpuAbiListFile.write("abiList=${mergedAbiFilters.join(",")}")
        FileUtils.copyFile(baseAppCpuAbiListFile, new File(qigsawMergedAssetsDir.parentFile, baseAppCpuAbiListFile.name))
        splitDetails.splits.each { SplitInfo info ->
            if (info.builtIn) {
                info.apkData.each {
                    File destSplitApk = new File(qigsawMergedAssetsDir, "${info.splitName}-${it.abi + SdkConstants.DOT_ZIP}")
                    if (splitDetails.updateRecord.updateMode != SplitDetails.UpdateRecord.DEFAULT) {
                        File oldSplitApk = getOldSplitApk(info.splitName, it.abi)
                        if (!oldSplitApk.exists()) {
                            throw new GradleException("Old split apk ${oldSplitApk.absolutePath} is not found, make sure oldApk is existing!")
                        }
                        FileUtils.copyFile(oldSplitApk, destSplitApk)
                    } else {
                        File sourceSplitApk = new File(splitApksDir, "${info.splitName}-${it.abi + SdkConstants.DOT_ANDROID_PACKAGE}")
                        if (!sourceSplitApk.exists()) {
                            throw new GradleException("Split apk ${sourceSplitApk.absolutePath} is not found!")
                        }
                        FileUtils.copyFile(sourceSplitApk, destSplitApk)
                    }
                }
            }
        }
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
                List<String> updatedSplits = processAndAnalyzeUpdatedSplits(oldSplitDetails.splits, splitInfoList)
                updateRecord.updateMode = SplitDetails.UpdateRecord.VERSION_CHANGED
                updateRecord.updateSplits = updatedSplits
                SplitLogger.w("Splits ${updatedSplits} need to be updated!")
            } else {
                updateRecord.updateMode = SplitDetails.UpdateRecord.VERSION_NO_CHANGED
                SplitLogger.w("No splits need to be updated, just using old Apks!")
                oldSplitDetails.updateRecord = updateRecord
                return oldSplitDetails
            }
        }
        splitInfoList.each { SplitInfo info ->
            uploadSplitApkIfNeed(info)
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

    void uploadSplitApkIfNeed(SplitInfo info) {
        if (!info.builtIn) {
            SplitApkUploader uploader = SplitApkUploaderInstance.get()
            if (uploader != null) {
                for (SplitInfo.SplitApkData data : info.apkData) {
                    if (!data.url.startsWith("http")) {
                        File apkFile = new File(splitApksDir, info.splitName + "-${data.abi + SdkConstants.DOT_ANDROID_PACKAGE}")
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

    static List<String> processAndAnalyzeUpdatedSplits(List<SplitInfo> oldSplits, List<SplitInfo> splits) {
        List<String> updateSplits = new ArrayList<>(0)
        List<SplitInfo> newSplits = new ArrayList<>()
        splits.each { info ->
            oldSplits.each { oldInfo ->
                if (info.splitName == oldInfo.splitName) {
                    if (info.version == oldInfo.version) {
                        newSplits.add(oldInfo)
                        SplitLogger.w("Split ${info.splitName} version ${info.version} is not changed, using old info!")
                    } else {
                        SplitInfo newInfo = info.clone()
                        newInfo.builtIn = false
                        newInfo.onDemand = true
                        newSplits.add(newInfo)
                        updateSplits.add(info.splitName)
                        SplitLogger.w("Split ${info.splitName} version ${info.version} is changed, it need to be updated!")
                    }
                }
            }
        }
        splits.clear()
        splits.addAll(newSplits)
        return updateSplits
    }

}
