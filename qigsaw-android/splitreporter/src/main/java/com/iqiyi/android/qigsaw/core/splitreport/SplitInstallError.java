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

import android.support.annotation.NonNull;

public class SplitInstallError {

    /**
     * Split apk file does not exist.
     */
    public static final int APK_FILE_ILLEGAL = -11;

    /**
     * Split signature does not match that of base app.
     */
    public static final int SIGNATURE_MISMATCH = -12;

    /**
     * Split dex files failed to be extracted.
     */
    public static final int DEX_EXTRACT_FAILED = -13;

    /**
     * Split lib files failed to be extracted.
     */
    public static final int LIB_EXTRACT_FAILED = -14;

    /**
     * Split dex files failed to be optimized.
     */
    public static final int DEX_OPT_FAILED = -15;

    /**
     * Split opt files are stale.
     */
    public static final int OPT_CHECK_FAILED = -16;

    /**
     * Failed to mark a file that indicates split is installed.
     */
    public static final int MARK_CREATE_FAILED = -17;

    /**
     * split's dependencies are not installed.
     */
    public static final int DEPENDENCIES_NOT_INSTALLED = -18;

    private final String moduleName;

    private final int errorCode;

    private final Throwable cause;

    public SplitInstallError(String moduleName,
                             int errorCode,
                             Throwable cause) {
        this.moduleName = moduleName;
        this.errorCode = errorCode;
        this.cause = cause;
    }

    /**
     * module in which load exception occurs.
     *
     * @return name of module.
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * @return error code of this exception.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @return cause of exception.
     */
    @NonNull
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "SplitInstallError{"
                + " moduleName=" + moduleName
                + " errorCode=" + errorCode
                + " message=" + cause.getMessage()
                + "}";
    }
}
