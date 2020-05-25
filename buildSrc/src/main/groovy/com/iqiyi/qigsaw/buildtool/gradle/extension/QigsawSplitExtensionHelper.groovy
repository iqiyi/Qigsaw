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

package com.iqiyi.qigsaw.buildtool.gradle.extension

import org.gradle.api.Project

class QigsawSplitExtensionHelper {

    static String getSplitInfoVersion(Project project) {
        try {
            return project.extensions.qigsawSplit.splitInfoVersion
        } catch (Throwable ignored) {
            return QigsawSplitExtension.DEFAULT_SPLIT_INFO_VERSION
        }
    }

    static File getOldApkFile(Project project) {
        try {
            String oldApk = project.extensions.qigsawSplit.oldApk
            if (oldApk != null) {
                File oldApkFile = new File(oldApk)
                if (oldApkFile.exists()) {
                    return oldApkFile
                }
            }
        } catch (Throwable ignored) {

        }
        return null
    }

    static boolean isReleaseSplitApk(Project project) {
        try {
            return project.extensions.qigsawSplit.releaseSplitApk
        } catch (Throwable ignored) {
            return false
        }
    }

    static boolean isMultipleApkForABIs(Project project) {
        try {
            return project.extensions.qigsawSplit.multipleApkForABIs
        } catch (Throwable ignored) {
            return false
        }
    }

    static List<String> getRestrictWorkProcessesForSplits(Project project) {
        try {
            List<String> value = project.extensions.qigsawSplit.restrictWorkProcessesForSplits
            if (value != null && !value.isEmpty()) {
                return value
            }
        } catch (Throwable ignored) {
        }
        return null
    }

    static File getApplyMappingFile(Project project) {
        try {
            String mapping = project.extensions.qigsawSplit.applyMapping
            if (mapping != null) {
                File mappingFile = new File(mapping)
                if (mappingFile.exists()) {
                    return mappingFile
                }
            }
        } catch (Throwable ignored) {

        }
        return null
    }

    static boolean isUse7z(Project project) {
        try {
            return project.extensions.qigsawSplit.use7z
        } catch (Throwable ignored) {
            return false
        }
    }

    static Set<String> getSplitEntryFragments(Project project) {
        try {
            List<String> value = project.extensions.qigsawSplit.splitEntryFragments
            if (value != null && !value.isEmpty()) {
                return value
            }
        } catch (Throwable ignored) {

        }
        return null
    }

    static Set<String> getBaseContainerActivities(Project project) {
        try {
            List<String> value = project.extensions.qigsawSplit.baseContainerActivities
            if (value != null && !value.isEmpty()) {
                return value
            }
        } catch (Throwable ignored) {

        }
        return null
    }
}
