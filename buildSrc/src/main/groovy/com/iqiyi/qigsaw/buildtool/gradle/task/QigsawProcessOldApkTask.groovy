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
import com.android.annotations.Nullable
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class QigsawProcessOldApkTask extends DefaultTask {

    static final String OUTPUT_NAME = "qigsaw_old_split_info.json"

    @Input
    boolean hasQigsawTask

    @Optional
    @InputFile
    @Nullable
    File oldApk

    @OutputDirectory
    File outputDir

    @Input
    String versionName

    void initArgs(boolean hasQigsawTask, String versionName, File oldApk) {
        this.hasQigsawTask = hasQigsawTask
        this.versionName = versionName
        this.oldApk = oldApk
    }

    @TaskAction
    void extractOldApk() {
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        outputDir.mkdirs()
        if (oldApk != null && hasQigsawTask) {
            File oldSplitJsonFile = extractSplitJsonFileFromOldApk()
            if (oldSplitJsonFile == null) {
                throw new GradleException("Qigsaw Error: Failed to extract split json file from old apk!")
            }
            SplitDetails splitDetails = TypeClassFileParser.parseFile(oldSplitJsonFile, SplitDetails.class)
            if (splitDetails == null) {
                throw new GradleException("Qigsaw Error: Can't read qigsaw split json file in old apk ${oldApk}")
            }
            if (splitDetails.builtInUrlPrefix == null) {
                throw new GradleException("Qigsaw Error: The applied version of qigsaw-gradle-plugin and old apk ${oldApk} is not matched!")
            }
            boolean assetsBuiltIn = splitDetails.builtInUrlPrefix.equals("assets://")
            if (!assetsBuiltIn && splitDetails.abiFilters == null) {
                throw new GradleException("Qigsaw Error: Can't read 'abiFilters' from old split json file.")
            }
            extractBuiltInSplitApkFromOldApk(assetsBuiltIn, splitDetails.abiFilters, splitDetails.splits)
        }
    }

    void extractBuiltInSplitApkFromOldApk(boolean assetsBuiltIn, Set<String> abiFilters, List<SplitInfo> splits) {
        ZipFile sourceZip = new ZipFile(oldApk)
        Enumeration e = sourceZip.entries()
        String splitApkDirPrefix = assetsBuiltIn ? "assets/" : "lib/${abiFilters.getAt(0)}/"
        String splitApkSuffix = assetsBuiltIn ? SdkConstants.DOT_ZIP : SdkConstants.DOT_NATIVE_LIBS
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement()
            String entryName = entry.getName()
            if (!entryName.startsWith(splitApkDirPrefix)) {
                continue
            }
            if (!entryName.endsWith(splitApkSuffix)) {
                continue
            }
            if (!assetsBuiltIn && !entryName.startsWith("${splitApkDirPrefix}libsplit_")) {
                continue
            }
            for (SplitInfo split : splits) {
                String splitApkName = assetsBuiltIn ? "${split.splitName}${splitApkSuffix}" : "libsplit_${split.splitName}${splitApkSuffix}"
                if (entryName.endsWith(splitApkName)) {
                    File splitApkOutput = new File(outputDir, splitApkName)
                    if (splitApkOutput.exists()) {
                        splitApkOutput.delete()
                    }
                    InputStream inputStream = sourceZip.getInputStream(entry)
                    FileUtils.copyFile(inputStream, new FileOutputStream(splitApkOutput))
                    QigsawLogger.e("Succeed to extract split ${split.splitName} to ${splitApkOutput.absolutePath} from old apk.")
                }
            }
        }
        FileUtils.closeQuietly(sourceZip)
    }

    File extractSplitJsonFileFromOldApk() {
        ZipFile sourceZip = new ZipFile(oldApk)
        Enumeration e = sourceZip.entries()
        File file = null
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement()
            String entryName = entry.getName()
            if (!entryName.startsWith("assets/")) {
                continue
            }
            if (!entryName.contains("qigsaw_${versionName}_") || !entryName.endsWith(SdkConstants.DOT_JSON)) {
                continue
            }
            file = new File(outputDir, OUTPUT_NAME)
            InputStream inputStream = sourceZip.getInputStream(entry)
            FileUtils.copyFile(inputStream, new FileOutputStream(file))
            QigsawLogger.w("Succeed to extract old split json file to ${file.absolutePath} from old apk.")
        }
        FileUtils.closeQuietly(sourceZip)
        return file
    }
}
