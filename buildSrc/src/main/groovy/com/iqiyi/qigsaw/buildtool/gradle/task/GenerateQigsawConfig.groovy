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

import com.android.utils.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.compiling.QigsawConfigGenerator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateQigsawConfig extends ProcessOldOutputsBaseTask {

    @Input
    String qigsawId

    @Input
    boolean qigsawMode

    @Input
    String applicationId

    @Input
    String versionName

    @Input
    String defaultSplitInfoVersion

    @Input
    Set<String> dynamicFeatureNames

    @OutputDirectory
    File outputDir

    @OutputDirectory
    File buildConfigDir

    @TaskAction
    void generate() throws IOException {
        QigsawConfigGenerator generator = new QigsawConfigGenerator(outputDir, applicationId)
        File qigsawConfigFile = generator.getQigsawConfigFile()
        if (qigsawConfigFile.exists()) {
            qigsawConfigFile.delete()
        }
        List<String> jointList = new ArrayList<>()
        for (String name : dynamicFeatureNames) {
            jointList.add("\"" + name + "\"")
        }
        if (targetFilesExtractedDir != null) {
            File oldSplitDetailsFile = getOldSplitDetailsFile()
            if (oldSplitDetailsFile != null && oldSplitDetailsFile.exists()) {
                qigsawId = TypeClassFileParser.parseFile(oldSplitDetailsFile, SplitDetails.class).qigsawId
                if (qigsawId == null) {
                    throw new GradleException("Qigsaw Error: Can't read qigsaw id from old apk!")
                }
                SplitLogger.w("Read qigsaw id ${qigsawId} from old apk!")
            }
        }
        generator
                .addField(
                        "boolean",
                        "QIGSAW_MODE",
                        qigsawMode ? "Boolean.parseBoolean(\"true\")" : "false")
                .addField("String", "QIGSAW_ID", '"' + qigsawId + '"')
                .addField("String", "VERSION_NAME", '"' + versionName + '"')
                .addField("String", "DEFAULT_SPLIT_INFO_VERSION", '"' + defaultSplitInfoVersion + '"')
                .addField("String[]", "DYNAMIC_FEATURES", "{" + jointList.join(",") + "}")
        generator.generate()
        File destDir = new File(buildConfigDir, applicationId.replace(".", File.separator))
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        File destFile = new File(destDir, QigsawConfigGenerator.QIGSAW_CONFIG_NAME)
        if (destFile.exists()) {
            destFile.delete()
        }
        FileUtils.copyFile(qigsawConfigFile, destFile)
    }
}
