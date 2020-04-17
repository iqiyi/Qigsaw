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

package com.iqiyi.android.qigsaw.core.common;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitConstants {

    public static final String QIGSAW = "qigsaw";

    public static final int MAX_RETRY_ATTEMPTS = 3;

    public static final String QIGSAW_PREFIX = "qigsaw_";

    public static final String DOT_APK = ".apk";

    public static final String DOT_DEX = ".dex";

    public static final String DOT_ZIP = ".zip";

    public static final String DOT_SO = ".so";

    public static final String DOT_JSON = ".json";

    public static final String NEW_SPLIT_INFO_PATH = "new_split_info_path";

    public static final String NEW_SPLIT_INFO_VERSION = "new_split_info_version";

    public static final String KET_NAME = "splitName";

    public static final String KEY_APK = "apk";

    public static final String KEY_ADDED_DEX = "added-dex";

    public static final String URL_ASSETS = "assets://";

    public static final String URL_NATIVE = "native://";

    public static final String SPLIT_PREFIX = "split_";


}
