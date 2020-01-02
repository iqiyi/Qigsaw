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

class SplitDetails {

    transient boolean updateMode

    transient boolean updateModeButNoVersionChanged

    String qigsawId

    String appVersionName

    String builtInUrlPrefix

    List<SplitInfo> splits

    List<String> updateSplits

    Set<String> abiFilters

    static Builder newBuilder() {
        return new Builder()
    }

    SplitDetails(Builder builder) {
        this.qigsawId = builder.qigsawId
        this.appVersionName = builder.appVersionName
        this.abiFilters = builder.abiFilters
        this.builtInUrlPrefix = builder.builtInUrlPrefix
    }

    @Override
    String toString() {
        """| qigsawId = ${qigsawId}
           | appVersionName = ${appVersionName}
           | builtInUrlPrefix = ${builtInUrlPrefix}
           | abiFilters = ${abiFilters}
           | updateSplits = ${updateSplits}
           | splits = \n${splits}
        """.stripMargin()
    }


    static class Builder {

        private String qigsawId

        private String appVersionName

        private Set<String> abiFilters

        private String builtInUrlPrefix

        Builder qigsawId(String qigsawId) {
            this.qigsawId = qigsawId
            return this
        }

        Builder builtInUrlPrefix(String builtInUrlPrefix) {
            this.builtInUrlPrefix = builtInUrlPrefix
            return this
        }

        Builder appVersionName(String appVersionName) {
            this.appVersionName = appVersionName
            return this
        }

        Builder abiFilters(Set<String> abiFilters) {
            this.abiFilters = (abiFilters == null || abiFilters.isEmpty() ? null : abiFilters)
            return this
        }

        SplitDetails build() {
            return new SplitDetails(this)
        }
    }
}
