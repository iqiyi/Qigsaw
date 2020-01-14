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

import com.iqiyi.qigsaw.buildtool.gradle.compiling.QigsawConfigGenerator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateQigsawConfig extends DefaultTask {

    @Input
    String qigsawId

    @Input
    String versionName

    @Input
    boolean qigsawMode

    @Input
    String defaultSplitInfoVersion

    @Input
    List<String> dfNames

    @Input
    String applicationId

    File sourceOutputDir

    @InputDirectory
    File oldApkOutputDir

    @OutputDirectory
    File outputDir

    void initArgs(boolean qigsawMode,
                  String qigsawId,
                  String applicationId,
                  String versionName,
                  String defaultSplitInfoVersion,
                  List<String> dfNames) {
        this.qigsawMode = qigsawMode
        this.qigsawId = qigsawId
        this.applicationId = applicationId
        this.versionName = versionName
        this.defaultSplitInfoVersion = defaultSplitInfoVersion
        this.dfNames = dfNames
    }

    @TaskAction
    void generate() throws IOException {
        QigsawConfigGenerator generator = new QigsawConfigGenerator(outputDir, applicationId)
        File qigsawConfigFile = generator.getQigsawConfigFile()
        if (qigsawConfigFile.exists()) {
            qigsawConfigFile.delete()
        }

        List<String> dfNameJoinList = new ArrayList<>()
        for (String dfName : dfNames) {
            dfNameJoinList.add("\"" + dfName + "\"")
        }
        File oldSplitJsonFile = new File(oldApkOutputDir, QigsawProcessOldApkTask.OUTPUT_NAME)
        if (oldSplitJsonFile.exists()) {
            qigsawId = TypeClassFileParser.parseFile(oldSplitJsonFile, SplitDetails.class).qigsawId
            if (qigsawId == null) {
                throw new GradleException("Can't read qigsaw id from old apk!")
            }
            QigsawLogger.w("Read qigsaw id ${qigsawId} from old apk!")
        }
        generator
                .addField(
                        "boolean",
                        "QIGSAW_MODE",
                        qigsawMode ? "Boolean.parseBoolean(\"true\")" : "false")
                .addField("String", "QIGSAW_ID", '"' + qigsawId + '"')
                .addField("String", "VERSION_NAME", '"' + versionName + '"')
                .addField("String", "DEFAULT_SPLIT_INFO_VERSION", '"' + defaultSplitInfoVersion + '"')
                .addField("String[]", "DYNAMIC_FEATURES", "{" + dfNameJoinList.join(",") + "}")
        generator.generate()
        File destDir = new File(sourceOutputDir, applicationId.replace(".", File.separator))
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
