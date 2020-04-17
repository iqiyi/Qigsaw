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

package com.iqiyi.android.qigsaw.core.splitreport;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

public final class SplitInstallError extends SplitBriefInfo {

    /**
     * Split apk file does not exist.
     */
    public static final int APK_FILE_ILLEGAL = -11;

    /**
     * Split signature does not match with base app.
     */
    public static final int SIGNATURE_MISMATCH = -12;

    /**
     * Split MD5 is not correct.
     */
    public static final int MD5_ERROR = -13;

    /**
     * Split dex files failed to be extracted.
     */
    public static final int DEX_EXTRACT_FAILED = -14;

    /**
     * Split lib files failed to be extracted.
     */
    public static final int LIB_EXTRACT_FAILED = -15;

    /**
     * Failed to mark a file that indicates split is installed.
     */
    public static final int MARK_CREATE_FAILED = -16;

    /**
     * Failed to create {@link dalvik.system.DexClassLoader}
     */
    public static final int CLASSLOADER_CREATE_FAILED = -17;

    /**
     * System generate oat file failed.
     */
    public static final int DEX_OAT_FAILED = -18;

    /**
     * error code of this exception.
     */
    public final int errorCode;

    /**
     * cause of exception.
     */
    public final Throwable cause;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SplitInstallError(SplitBriefInfo briefInfo,
                             int errorCode,
                             Throwable cause) {
        super(briefInfo.splitName, briefInfo.version, briefInfo.builtIn);
        this.errorCode = errorCode;
        this.cause = cause;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"splitName\":"
                + "\"" + splitName + "\","
                + "\"version\":"
                + "\"" + version + "\","
                + "\"builtIn\":" + builtIn
                + "\",errorCode\":" + errorCode
                + "\",errorMsg\":"
                + "\"" + cause.getMessage() + "\"" +
                "}";
    }
}
