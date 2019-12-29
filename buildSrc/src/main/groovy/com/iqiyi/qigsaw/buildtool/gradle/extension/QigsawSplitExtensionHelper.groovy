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

    static String getOldApk(Project project) {
        try {
            String oldApk = project.extensions.qigsawSplit.oldApk
            if (oldApk != null && new File(oldApk).exists()) {
                return oldApk
            }
        } catch (Throwable ignored) {

        }
        return QigsawSplitExtension.EMPTY
    }

    static boolean getReleaseSplitApk(Project project) {
        try {
            return project.extensions.qigsawSplit.releaseSplitApk
        } catch (Throwable ignored) {
            return false
        }
    }

    static List<String> getRestrictWorkProcessesForSplits(Project project) {
        try {
            return project.extensions.qigsawSplit.restrictWorkProcessesForSplits
        } catch (Throwable ignored) {
            return Collections.emptyList()
        }
    }

    static String getApplyMapping(Project project) {
        try {
            String mapping = project.extensions.qigsawSplit.applyMapping
            if (mapping != null && new File(mapping).exists()) {
                return mapping
            }
        } catch (Throwable ignored) {

        }
        return QigsawSplitExtension.EMPTY
    }
}
