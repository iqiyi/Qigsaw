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

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.scope.VariantScope
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger

class FixedMainDexList {

    final String[] FILTERRULES = [
            "class * implements com.tencent.tinker.entry.ApplicationLifeCycle",
            "class * extends android.app.Application"
    ]
    final String ADDRULES = "\n# Adjust by qigsaw"

    final VariantScope variantScope

    FixedMainDexList(ApplicationVariant variant) {
        this.variantScope = variant.variantData.scope
    }

    void execute() {
        File multiDexKeepProguard = getDexKeepProguardFile()
        if (multiDexKeepProguard == null) {
            return
        }
        QigsawLogger.w(">Task FixedMainDex -> " + multiDexKeepProguard.absolutePath)
        println(multiDexKeepProguard.path)
        boolean needUpdate = false
        boolean ignoreToCloseParenthesis = false
        StringBuilder sb = new StringBuilder()
        multiDexKeepProguard.eachLine {
            if (it.trim().startsWith("-keep")) {
                ignoreToCloseParenthesis = shouldRemove(it)
            }
            if (!ignoreToCloseParenthesis) {
                sb.append(it).append("\n")
            } else {
                needUpdate = true
            }
        }
        if (needUpdate) {
            FileWriter multiDexWriter = new FileWriter(multiDexKeepProguard)
            try {
                multiDexWriter.write(sb.toString())
                multiDexWriter.write(ADDRULES)
            } finally {
                multiDexWriter.close()
            }
        }
    }

    boolean shouldRemove(String line) {
        boolean match = false
        FILTERRULES.each { rule ->
            if (line.contains(rule)) {
                match = true
            }
        }
        println(match)
        return match
    }

    File getDexKeepProguardFile() {
        File multiDexKeepProguard = null
        try {
            multiDexKeepProguard = variantScope.getManifestKeepListProguardFile()
        } catch (Throwable ignore) {
            try {
                def buildableArtifact = this.variantScope.getArtifacts().getFinalArtifactFiles(
                        Class.forName("com.android.build.gradle.internal.scope.InternalArtifactType")
                                .getDeclaredField("LEGACY_MULTIDEX_AAPT_DERIVED_PROGUARD_RULES")
                                .get(null)
                )

                //noinspection GroovyUnch eckedAssignmentOfMemberOfRawType,UnnecessaryQualifiedReference
                multiDexKeepProguard = com.google.common.collect.Iterators.getOnlyElement(buildableArtifact.iterator())
            } catch (Throwable e) {

            }
            if (multiDexKeepProguard == null) {
                try {
                    multiDexKeepProguard = variantScope.getManifestKeepListFile()
                } catch (Throwable e) {
                    QigsawLogger.e("can't find getManifestKeepListFile method, exception:${e}")
                }
            }
        }
        if (multiDexKeepProguard == null) {
            QigsawLogger.e("can't get multiDexKeepProguard file")
        }
        return multiDexKeepProguard
    }
}
