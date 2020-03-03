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

package com.iqiyi.qigsaw.buildtool.gradle.internal.entity;

class SplitInfo {

    transient File splitApk

    transient boolean versionChanged = false

    /**
     * The name of split apk
     */
    String splitName

    /**
     * Download link of split apk
     */
    String url

    /**
     * Whether put split apk into base.apk
     */
    boolean builtIn

    /**
     * whether install split apk on demand.
     */
    boolean onDemand

    /**
     * size of split apk file
     */
    long size

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
    List<LibInfo> nativeLibraries

    /**
     * dependencies of the split
     */
    List<String> dependencies

    SplitInfo(Builder builder) {
        this.splitApk = builder.splitApkFile
        this.splitName = builder.splitName
        this.version = builder.version
        this.minSdkVersion = builder.minSdkVersion
        this.dependencies = builder.dependencies
    }

    void copySplitInfo(SplitInfo origin) {
        this.splitName = origin.splitName
        this.url = origin.url
        this.builtIn = origin.builtIn
        this.onDemand = origin.onDemand
        this.size = origin.size
        this.applicationName = origin.applicationName
        this.version = origin.version
        this.md5 = origin.md5
        this.workProcesses = origin.workProcesses
        this.minSdkVersion = origin.minSdkVersion
        this.dexNumber = origin.dexNumber
        this.nativeLibraries = origin.nativeLibraries
        this.dependencies = origin.dependencies
    }

    static Builder newBuilder() {
        return new Builder()
    }

    static class LibInfo {

        String abi

        List<Lib> jniLibs

        @Override
        String toString() {
            """|\nabi = ${abi}
               | jniLibs = ${jniLibs}

            """.stripMargin()
        }

        static class Lib {

            String name

            String md5

            long size

            @Override
            String toString() {
                """|\nname = ${name}
                   | md5 = ${md5}
                   | size = ${size}

                """.stripMargin()
            }
        }
    }

    @Override
    String toString() {
        """|\nsplitName = ${splitName}
           | url = ${url}
           | size = ${size}
           | md5 = ${md5}
           | builtIn = ${builtIn}
           | onDemand = ${onDemand}
           | applicationName = ${applicationName}
           | minSdkVersion = ${minSdkVersion}
           | dexNumber = ${dexNumber}
           | version = ${version}
           | workProcesses = ${workProcesses}
           | dependencies = ${dependencies}
           | nativeLibraries = ${nativeLibraries}

        """.stripMargin()
    }

    static class Builder {

        private File splitApkFile

        private String splitName

        private String version

        private int minSdkVersion

        List<String> dependencies

        Builder splitApkFile(File splitApkFile) {
            this.splitApkFile = splitApkFile
            return this
        }

        Builder splitName(String splitName) {
            this.splitName = splitName
            return this
        }

        Builder version(String versionName, int versionCode) {
            this.version = "${versionName}@${versionCode}"
            return this
        }

        Builder minSdkVersion(int minSdkVersion) {
            this.minSdkVersion = minSdkVersion
            return this
        }

        Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies.empty ? null : dependencies
            return this
        }

        SplitInfo build() {
            return new SplitInfo(this)
        }
    }
}
