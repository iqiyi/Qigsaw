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
import com.google.gson.Gson
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitDetailsCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploader
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploaderInstance
import org.gradle.api.Project
import org.gradle.api.Task

class SplitDetailsCreatorImpl implements SplitDetailsCreator {

    final static String JSON_SUFFIX = ".json"

    Project appProject

    String appVersionName

    String splitDetailsFilePrefix

    String variantName

    String qigsawId

    SplitDetailsCreatorImpl(Project appProject,
                            String variantName,
                            String appVersionName,
                            String qigsawId) {
        this.appProject = appProject
        this.variantName = variantName
        this.appVersionName = appVersionName
        this.splitDetailsFilePrefix = "qigsaw_" + appVersionName + "_"
        this.qigsawId = qigsawId
    }

    @Override
    File createSplitDetailsJsonFile(List<SplitInfo> splits) {
        AppliedSplitInfoJsonFileGetter fileGetter = new AppliedSplitInfoJsonFileGetter(appProject, splitDetailsFilePrefix)
        File splitInfoJsonFromTinker = fileGetter.getSplitInfoJsonFileFromTinkerOldApk()
        if (splitInfoJsonFromTinker != null) {
            return createAppliedSplitInfoJsonFile(splitInfoJsonFromTinker, splits)
        }
        File splitInfoJsonFromQigsaw = fileGetter.getSplitInfoJsonFileFromQigsawOldApk()
        if (splitInfoJsonFromQigsaw != null) {
            return createAppliedSplitInfoJsonFile(splitInfoJsonFromQigsaw, splits)
        }

        for (SplitInfo info : splits) {
            uploadSplitAPKIfNeed(info)
        }
        SplitDetails newSplitDetails = new SplitDetails(qigsawId, appVersionName, splits, null)
        return createNewSplitInfoJsonFile(newSplitDetails)
    }

    File createAppliedSplitInfoJsonFile(File oldSplitInfoJsonFile, List<SplitInfo> splits) {
        SplitDetails splitDetails = createSplitDetails(oldSplitInfoJsonFile)
        if (!hasSplitVersionChanged(splitDetails, splits)) {
            return oldSplitInfoJsonFile
        } else {
            SplitDetails newSplitDetailsForTinker = createSplitDetailsWithApplied(splitDetails, splits)
            return createNewSplitInfoJsonFile(newSplitDetailsForTinker)
        }
    }

    private File createNewSplitInfoJsonFile(SplitDetails splitDetails) {
        Gson gson = new Gson()
        String splitDetailsStr = gson.toJson(splitDetails)
        File outputDir = null
        appProject.extensions.android.applicationVariants.each {
            ApplicationVariant appVariant = it
            if (appVariant.name.equalsIgnoreCase(variantName)) {
                Task packageApplicationTask = AGPCompat.getPackageApplication(appVariant)
                outputDir = packageApplicationTask.outputDirectory
            }
        }
        if (outputDir != null) {
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            String splitInfoVersion = appProject.extensions.qigsawSplit.splitInfoVersion
            String fileName = splitDetailsFilePrefix + splitInfoVersion + JSON_SUFFIX
            File splitDetailsFile = new File(outputDir, fileName)
            if (splitDetailsFile.exists()) {
                splitDetailsFile.delete()
            }
            splitDetailsFile.createNewFile()
            BufferedOutputStream osm = new BufferedOutputStream(new FileOutputStream(splitDetailsFile))
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(osm))
            writer.write(splitDetailsStr)
            writer.close()
            osm.close()
            return splitDetailsFile
        }
        return null
    }


    private static SplitDetails createSplitDetails(File splitInfoJsonFile) {
        String str = readInputStreamContent(new FileInputStream(splitInfoJsonFile))
        return parseSplitDetails(str)
    }

    private static SplitDetails parseSplitDetails(String splitDetailsStr) {
        Gson gson = new Gson()
        return gson.fromJson(splitDetailsStr, SplitDetails)
    }

    private static String readInputStreamContent(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is))
        StringBuilder stringBuffer = new StringBuilder()
        String str
        while ((str = br.readLine()) != null) {
            stringBuffer.append(str)
        }
        FileUtils.closeQuietly(is)
        FileUtils.closeQuietly(br)
        return stringBuffer.toString()
    }

    void uploadSplitAPKIfNeed(SplitInfo splitInfo) throws Exception {
        if (!splitInfo.builtIn) {
            SplitApkUploader uploader = SplitApkUploaderInstance.get()
            if (uploader == null) {
                appProject.logger.error("SplitApkUploader has not been implemented, just make split " + splitInfo.splitName + " built-in")
            } else {
                String uploadedUrl = uploader.uploadSync(appProject, splitInfo.splitApk, splitInfo.splitName)
                if (uploadedUrl != null && uploadedUrl.startsWith("http")) {
                    splitInfo.url = uploadedUrl
                    return
                }
            }
        }
        splitInfo.builtIn = true
        splitInfo.url = "assets://" + splitInfo.splitName + SdkConstants.DOT_ZIP
    }


    private static boolean hasSplitVersionChanged(SplitDetails appliedSplitDetails, List<SplitInfo> splits) {
        boolean versionChanged = false
        if (appliedSplitDetails != null && appliedSplitDetails.splits != null) {
            for (SplitInfo info : splits) {
                for (SplitInfo appliedInfo : appliedSplitDetails.splits) {
                    if (info.splitName.equals(appliedInfo.splitName)) {
                        if (!info.version.equals(appliedInfo.version)) {
                            versionChanged = true
                        }
                    }
                }
            }
        }
        return versionChanged
    }

    private SplitDetails createSplitDetailsWithApplied(SplitDetails appliedSplitDetails, List<SplitInfo> splits) {
        if (appliedSplitDetails != null && appliedSplitDetails.splits != null) {
            List<String> updateSplits = new ArrayList<>()
            for (SplitInfo info : splits) {
                for (SplitInfo appliedInfo : appliedSplitDetails.splits) {
                    if (info.splitName.equals(appliedInfo.splitName)) {
                        if (info.version.equals(appliedInfo.version)) {
                            if (info.md5.equals(appliedInfo.md5)) {
                                info.url = appliedInfo.url
                            } else {
                                uploadSplitAPKIfNeed(info)
                                appProject.logger.error(String.format("Split %s md5 has been changed, but version is not changed!!!!", info.splitName))
                            }
                        } else {
                            info.builtIn = false
                            updateSplits.add(info.splitName)
                            uploadSplitAPKIfNeed(info)
                        }
                    }
                }
            }
            if (updateSplits.isEmpty()) {
                updateSplits = null
            }
            return new SplitDetails(appliedSplitDetails.qigsawId, appVersionName, splits, updateSplits)
        }
        throw new RuntimeException("Can not parse applied split details!")
    }
}
