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

public class SplitLoadError {

    /**
     * Loading split res failed.
     */
    public static final int LOAD_RES_FAILED = -21;

    /**
     * Loading split lib dir failed.
     */
    public static final int LOAD_LIB_FAILED = -22;

    /**
     * Loading split dex file failed.
     */
    public static final int LOAD_DEX_FAILED = -23;

    /**
     * Activating split application failed.
     */
    public static final int ACTIVATE_APPLICATION_FAILED = -24;

    /**
     * Activating split providers failed.
     */
    public static final int ACTIVATE_PROVIDERS_FAILED = -25;

    public static final int INTERRUPTED_ERROR = -26;

    private final String moduleName;

    private final int errorCode;

    private final Throwable cause;

    public SplitLoadError(String moduleName,
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
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "SplitLoadError{"
                + " moduleName=" + moduleName
                + " errorCode=" + errorCode
                + " message=" + cause.toString()
                + "}";
    }

}
