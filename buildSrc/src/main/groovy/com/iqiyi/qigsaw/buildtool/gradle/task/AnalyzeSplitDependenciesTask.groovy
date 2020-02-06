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
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class AnalyzeSplitDependenciesTask extends DefaultTask {

    @Input
    List<String> dfClassPaths

    @Input
    List<String> splitAllDependencies

    @OutputDirectory
    File outputDir

    void initArgs(List<String> splitAllDependencies, List<String> dfClassPaths) {
        this.splitAllDependencies = splitAllDependencies
        this.dfClassPaths = dfClassPaths
    }

    @TaskAction
    analyzeDependencies() {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        File splitDependenciesFile = new File(outputDir, project.name + SdkConstants.DOT_JSON)
        if (splitDependenciesFile.exists()) {
            splitDependenciesFile.delete()
        }

        List<String> dfDependencies = new ArrayList<>()
        splitAllDependencies.each { String name ->
            if (dfClassPaths.contains(name)) {
                dfDependencies.add(name.split(":")[1])
            }
        }
        if (!dfDependencies.empty) {
            FileUtils.createFileForTypeClass(dfDependencies, splitDependenciesFile)
        }
        QigsawLogger.e(">Task :AnalyzeSplitDependenciesTask ${project.name} has dependencies ${dfDependencies}")
    }
}
