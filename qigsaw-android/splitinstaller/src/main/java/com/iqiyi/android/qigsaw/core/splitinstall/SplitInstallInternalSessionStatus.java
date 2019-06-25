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

package com.iqiyi.android.qigsaw.core.splitinstall;

@interface SplitInstallInternalSessionStatus {

    int UNKNOWN = 0;
    /**
     * The download is pending and will be processed soon.
     */
    int PENDING = 1;
    /**
     * The download requires user confirmation.
     */
    int REQUIRES_USER_CONFIRMATION = 8;
    /**
     * The split download is in progress.
     */
    int DOWNLOADING = 2;
    /**
     * The split is downloaded but not yet installed.
     */
    int DOWNLOADED = 3;
    /**
     * The splits are being installed.
     */
    int INSTALLING = 4;
    /**
     * Installation is complete; the splits are available to the client app.
     */
    int INSTALLED = 5;
    /**
     * Split download or installation has failed.
     */
    int FAILED = 6;
    /**
     * The split download is being cancelled.
     */
    int CANCELING = 9;
    /**
     * The split download has been cancelled.
     */
    int CANCELED = 7;

    /**
     * Split has been downloaded, extracted and optimized, but not loaded.
     */
    int POST_INSTALLED = 10;
}
