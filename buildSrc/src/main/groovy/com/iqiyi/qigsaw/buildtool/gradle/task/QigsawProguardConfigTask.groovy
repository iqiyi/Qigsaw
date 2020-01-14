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
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class QigsawProguardConfigTask extends DefaultTask {

    static final String PROGUARD_CONFIG_NAME = "qigsaw_proguard.pro"

    static final String PROGUARD_CONFIG_SETTINGS = "-keep class com.google.android.play.core.splitcompat.SplitCompat{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.listener.StateUpdatedListener{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.splitinstall.SplitInstallManager{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallManagerFactory{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallRequest{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallException{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallRequest\$Builder{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallHelper{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.splitinstall.model.SplitInstallErrorCode{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus{\n *;\n }\n" +
            "-keep class com.google.android.play.core.splitinstall.SplitInstallSessionState{\n *;\n }\n" +
            "-keep class com.google.android.play.core.tasks.Task{\n *;\n }\n" +
            "-keep class com.google.android.play.core.tasks.TaskExecutors{\n *;\n }\n" +
            "-keep class com.google.android.play.core.tasks.Tasks{\n *;\n }\n" +
            "-keep class com.google.android.play.core.tasks.RuntimeExecutionException{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.tasks.OnSuccessListener{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.tasks.OnFailureListener{\n *;\n }\n" +
            "-keep interface com.google.android.play.core.tasks.OnCompleteListener{\n *;\n }\n" +
            "-keep class com.split.signature.**{\n *;\n }\n" +
            "-keep class com.iqiyi.android.qigsaw.core.extension.AABExtension{\n public <methods>;\n }\n" +
            "-keep class com.iqiyi.android.qigsaw.core.splitdownload.Downloader{\n *;\n }\n" +
            "-keep class * implements com.iqiyi.android.qigsaw.core.splitdownload.Downloader{\n *;\n }\n" +
            "-keep class com.iqiyi.android.qigsaw.core.Qigsaw{\n public <methods>;\n }\n" +
            "-keep class com.iqiyi.android.qigsaw.core.extension.ComponentInfo{\n *;\n }\n" +
            "-keep class com.iqiyi.android.qigsaw.core.splitlib.**{\n *;\n }\n"

    @OutputDirectory
    File outputDir

    @InputFile
    @Optional
    @Nullable
    File applyMappingFile

    @Input
    String applicationId

    void initArgs(String applicationId) {
        this.applicationId = applicationId
        String applyMappingPath = QigsawSplitExtensionHelper.getApplyMapping(project)
        this.applyMappingFile = (applyMappingPath == null ? null : new File(applyMappingPath))
    }

    File getOutputProguardFile() {
        return new File(outputDir, PROGUARD_CONFIG_NAME)
    }

    @TaskAction
    void updateQigsawProguardConfig() {
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        outputDir.mkdirs()
        File file = new File(outputDir, PROGUARD_CONFIG_NAME)
        QigsawLogger.w("try update qigsaw proguard file with ${file}")
        // Write our recommended proguard settings to this file
        FileWriter fw = new FileWriter(file.path)
        if (applyMappingFile != null) {
            if (applyMappingFile.exists() && applyMappingFile.isFile() && applyMappingFile.length() > 0) {
                QigsawLogger.w("try to add applymapping ${applyMappingFile.path} to build the package")
                fw.write("-applymapping " + applyMappingFile.absolutePath)
                fw.write("\n")
            } else {
                QigsawLogger.e("applymapping file ${applyMappingFile.absolutePath} is not valid, just ignore!")
            }
        } else {
            QigsawLogger.e("applymapping file is null, just ignore!")
        }
        fw.write(PROGUARD_CONFIG_SETTINGS + "-keep class ${applicationId}.QigsawConfig{\n *;\n }\n")
        fw.close()
    }
}

