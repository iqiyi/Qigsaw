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

class QigsawSplitExtension {

    final static String DEFAULT_SPLIT_INFO_VERSION = "1.0.0"

    final static String EMPTY = ""

    /**
     * Specifies the last old apk's mapping file for proguard to applymapping
     */
    String applyMapping = EMPTY

    /**
     * the old apk path
     */
    String oldApk = EMPTY

    /**
     * Specifies the version of json file of split-info, default value is 1.0.0
     */
    String splitInfoVersion = DEFAULT_SPLIT_INFO_VERSION

    /**
     * Whether release split apk to server if it need dynamic delivery
     */
    boolean releaseSplitApk = false

    /**
     * Whether repack base apk with 7z format when you use qigsawUploadSplit${VariantName} task to upload split apk files.
     * default value is {@code false}
     */
    boolean use7z = false

    /**
     * Restrict splits working process, if you do not assign split name, this split will work on
     * all processes, otherwise only work processes declared in its manifest.
     */
    List<String> restrictWorkProcessesForSplits = Collections.emptyList()

    @Override
    String toString() {
        """| applyMapping = ${applyMapping}
           | splitInfoVersion = ${splitInfoVersion}
           | oldApk = ${oldApk}
           | releaseSplitApk = ${releaseSplitApk}
           | restrictWorkProcessesForSplits = ${restrictWorkProcessesForSplits}
        """.stripMargin()
    }
}
