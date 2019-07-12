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

package com.iqiyi.qigsaw.buildtool.gradle.internal.splits;

class SplitInfo {


    SplitInfo(String splitName,
              File splitApk,
              String md5,
              int minSdkVersion,
              String version) {
        this.splitName = splitName
        this.splitApk = splitApk
        this.md5 = md5
        this.size = splitApk.length()
        this.minSdkVersion = minSdkVersion
        this.version = version
    }

    final transient File splitApk

    /**
     * The name of split apk
     */
    final String splitName

    /**
     * Download link of split apk
     */
    String url

    /**
     * Whether put split apk in base.apk file
     */
    boolean builtIn

    /**
     * size of split apk file
     */
    final long size

    /**
     * application name in split AndroidManifest.xml
     */
    String applicationName

    /**
     * version of split apk
     */
    String version

    /**
     * md5 of split apk file
     */
    String md5

    List<String> workProcesses

    /**
     * The min Android sdk version that split apk does work.
     */
    int minSdkVersion

    /**
     * The number of split apk dex.
     */
    int dexNumber

    /**
     * info of split so files
     */
    LibInfo libInfo

    List<String> dependencies

    static class LibInfo {

        String abi

        List<Lib> libs

        static class Lib {
            String name
            String md5
            long size
        }
    }

    @Override
    String toString() {
        """| \nsplitName = ${splitName}
           | url = ${url}
           | size = ${size}
           | md5 = ${md5}
           | builtIn = ${builtIn}
           | applicationName = ${applicationName}
           | minSdkVersion = ${minSdkVersion}
           | dexNumber = ${dexNumber}
           | version = ${version}
           | workProcesses = ${workProcesses}
        """.stripMargin()
    }
}
