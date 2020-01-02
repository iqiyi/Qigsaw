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

package com.iqiyi.qigsaw.buildtool.gradle.compiling

import com.android.SdkConstants
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitJsonFileCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.task.QigsawProcessOldApkTask
import org.gradle.api.GradleException

class SplitJsonFileCreatorImpl implements SplitJsonFileCreator {

    final File outputDir

    final File oldApkOutputDir

    SplitJsonFileCreatorImpl(File outputDir, File oldApkOutputDir) {
        this.outputDir = outputDir
        this.oldApkOutputDir = oldApkOutputDir
    }

    @Override
    File createSplitJsonFile(SplitDetails splitDetails, String splitInfoVersion) {
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        outputDir.mkdirs()
        File dest = new File(outputDir, "qigsaw_${splitInfoVersion}${SdkConstants.DOT_JSON}")
        if (splitDetails.updateMode && splitDetails.updateModeButNoVersionChanged) {
            FileUtils.copyFile(new File(oldApkOutputDir, QigsawProcessOldApkTask.OUTPUT_NAME), dest)
            return dest
        }
        if (!FileUtils.createFileForTypeClass(splitDetails, dest)) {
            throw new GradleException("Failed to create split json file ${dest.absolutePath}")
        }
        return dest
    }
}
