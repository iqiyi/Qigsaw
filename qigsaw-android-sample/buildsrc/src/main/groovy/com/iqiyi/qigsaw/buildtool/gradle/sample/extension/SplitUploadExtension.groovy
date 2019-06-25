package com.iqiyi.qigsaw.buildtool.gradle.sample.extension

import com.google.common.collect.Lists

class SplitUploadExtension {

    /**
     * Whether upload apk to test env
     */
    boolean useTestEnv = true

    /**
     * You can decide which split apks should be upload to test env.
     */
    List<String> testOnly = Lists.newArrayList()

    void setTestOnly(List<String> values) {
        testOnly.addAll(values)
    }
}
