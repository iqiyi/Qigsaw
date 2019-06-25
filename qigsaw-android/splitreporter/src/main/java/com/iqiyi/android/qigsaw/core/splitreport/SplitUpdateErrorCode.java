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

public @interface SplitUpdateErrorCode {

    /**
     * New version of split info file is null or empty.
     */
    int ERROR_SPLIT_INFO_VERSION_NULL = -31;

    /**
     * Path of new split info file is null or empty.
     */
    int ERROR_SPLIT_INFO_PATH_NULL = -32;

    /**
     * File of new split info does not exist, or can't be written.
     */
    int ERROR_SPLIT_INFO_FILE_INVALID = -33;

    /**
     * New version of split info file existed in current app.
     */
    int ERROR_SPLIT_INFO_VERSION_EXISTED = -34;

    /**
     * Failed to parse file of new split info, or content is not correct.
     */
    int ERROR_SPLIT_INFO_INVALID = -35;

    /**
     * No splits need to be updated.
     */
    int ERROR_SPLIT_INFO_NOT_CHANGED = -36;

    /**
     * Qigsaw id does not match base app.
     */
    int ERROR_QIGSAW_ID_MISMATCH = -37;

    /**
     * Internal error, may be io exception.
     */
    int INTERNAL_ERROR = -38;
}
