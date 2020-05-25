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

package com.iqiyi.qigsaw.buildtool.gradle.internal.entity

class SplitInfo implements Cloneable {

    /**
     * The name of split
     */
    String splitName

    /**
     * Whether put split apk into base.apk
     */
    boolean builtIn

    /**
     * whether install split apk on demand.
     */
    boolean onDemand

    /**
     * application name in split AndroidManifest.xml
     */
    String applicationName

    /**
     * version of split apk
     */
    String version

    /**
     * The min Android sdk version that split apk does work.
     */
    int minSdkVersion

    /**
     * The number of split apk dex.
     */
    int dexNumber

    /**
     * dependencies of the split
     */
    Set<String> dependencies

    /**
     * A list of processes which can be worked.
     */
    Set<String> workProcesses

    /**
     * a list of split apk data
     */
    List<SplitApkData> apkData

    /**
     * a list of split so data
     */
    List<SplitLibData> libData

    static class SplitApkData implements Cloneable {

        String abi

        /**
         * Download link of split apk
         */
        String url

        /**
         * md5 of split apk file
         */
        String md5

        /**
         * size of split apk file
         */
        long size
    }

    static class SplitLibData implements Cloneable {

        String abi

        List<Lib> jniLibs

        static class Lib {

            String name

            String md5

            long size
        }
    }
}
