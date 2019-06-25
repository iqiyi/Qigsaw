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

import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class AppliedSplitInfoJsonFileGetter {

    Project project

    String splitDetailsFilePrefix

    AppliedSplitInfoJsonFileGetter(Project project, String splitDetailsFilePrefix) {
        this.project = project
        this.splitDetailsFilePrefix = splitDetailsFilePrefix
    }

    File getSplitInfoJsonFileFromQigsawOldApk() {
        String oldApkPath = project.extensions.qigsawSplit.oldApk
        if (oldApkPath == null) return null
        File oldApk = new File(oldApkPath)
        if (oldApk.exists() && oldApk.length() > 0) {
            return extractSplitInfoJsonFileInOldApk(oldApk)
        }
        return null
    }

    File getSplitInfoJsonFileFromTinkerOldApk() {
        String oldApkPath = null
        try {
            oldApkPath = project.extensions.tinkerPatch.oldApk
        } catch (Exception ignored) {

        }
        if (oldApkPath == null) return null
        File oldApk = new File(oldApkPath)
        if (oldApk.exists() && oldApk.length() > 0) {
            return extractSplitInfoJsonFileInOldApk(oldApk)
        }
        return null
    }

    private File extractSplitInfoJsonFileInOldApk(File oldApk) {
        ZipFile sourceZip = new ZipFile(oldApk)
        Enumeration e = sourceZip.entries()
        File file = null
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement()
            String entryName = entry.getName()
            if (entryName.charAt(0) < 'a') {
                continue
            }
            if (entryName.charAt(0) > 'a') {
                break
            }
            if (!entryName.startsWith("assets/")) {
                continue
            }
            if (!entryName.contains(splitDetailsFilePrefix) || !entryName.endsWith(".json")) {
                continue
            }
            String splitInfoJsonName = entryName.split("assets/")[1]
            file = new File(oldApk.getParent(), splitInfoJsonName)
            InputStream inputStream = sourceZip.getInputStream(entry)
            FileUtils.copyFile(inputStream, new FileOutputStream(file))
        }
        FileUtils.closeQuietly(sourceZip)
        return file
    }

}
