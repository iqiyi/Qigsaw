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

import com.iqiyi.qigsaw.buildtool.gradle.internal.model.BaseAppInfoGetter
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import org.gradle.api.Project

class BaseAppInfoGetterImpl implements BaseAppInfoGetter {

    Project project

    ManifestReader manifestReader

    BaseAppInfoGetterImpl(Project project,
                          File baseAppManifest) {
        this.project = project
        if (baseAppManifest != null && baseAppManifest.exists()) {
            manifestReader = new ManifestReaderImpl(baseAppManifest)
        }
    }

    @Override
    String getPackageName() {
        if (manifestReader != null) {
            return manifestReader.readPackageName()
        }
        return null
    }

    @Override
    String getVersionName() {
        String baseAppVersionName = project.extensions.android.defaultConfig.versionName
        if (baseAppVersionName == null) {
            if (manifestReader != null) {
                return manifestReader.readVersionName()
            }
        }
        return baseAppVersionName
    }
}
