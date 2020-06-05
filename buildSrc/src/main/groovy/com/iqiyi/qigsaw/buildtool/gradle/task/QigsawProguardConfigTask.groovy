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

import com.android.annotations.Nullable
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class QigsawProguardConfigTask extends DefaultTask {

    static final String PROGUARD_CONFIG_SETTINGS =
            "-keep class com.google.android.play.core.**{\n *;\n }\n" +
                    "-keep class com.split.signature.**{\n *;\n }\n" +
                    "-keep class com.iqiyi.android.qigsaw.core.extension.ComponentInfo{\n *;\n }\n" +
                    "-keep class com.iqiyi.android.qigsaw.core.splitlib.**{\n *;\n }\n"

    @OutputFile
    File outputFile

    @InputFile
    @Optional
    @Nullable
    File applyMappingFile = QigsawSplitExtensionHelper.getApplyMappingFile(project)

    @TaskAction
    void updateQigsawProguardConfig() {
        if (outputFile.exists()) {
            FileUtils.deleteDir(outputFile)
        }
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        SplitLogger.w("try update qigsaw proguard file with ${outputFile}")
        // Write our recommended proguard settings to this file
        FileWriter fw = new FileWriter(outputFile.path)
        if (applyMappingFile != null) {
            if (applyMappingFile.exists() && applyMappingFile.isFile() && applyMappingFile.length() > 0) {
                SplitLogger.w("try to add applymapping ${applyMappingFile.path} to build the package")
                fw.write("-applymapping " + applyMappingFile.absolutePath)
                fw.write("\n")
            } else {
                SplitLogger.e("applymapping file ${applyMappingFile.absolutePath} is not valid, just ignore!")
            }
        } else {
            SplitLogger.e("applymapping file is null, just ignore!")
        }
        fw.write(PROGUARD_CONFIG_SETTINGS)
        fw.close()
    }
}

