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

@interface SplitInstallInternalErrorCode {

    int NO_ERROR = 0;

    /**
     * Too many sessions are running for current app, existing sessions must be resolved first.
     */
    int ACTIVE_SESSIONS_LIMIT_EXCEEDED = -1;

    /**
     * A requested module is not available (to this user/device, for the installed apk).
     */
    int MODULE_UNAVAILABLE = -2;

    /**
     * Request is invalid.
     */
    int INVALID_REQUEST = -3;

    /**
     * Requested session is not found.
     */
    int SESSION_NOT_FOUND = -4;

    /**
     * Split Install API is not available.
     */
    int API_NOT_AVAILABLE = -5;

    /**
     * Network error: unable to obtain split details
     */
    int NETWORK_ERROR = -6;

    /**
     * Download not permitted under current device circumstances (e.g. in background)
     */
    int ACCESS_DENIED = -7;

    /**
     * The request contains one or more modules that have already been requested but have not yet been installed.
     */
    int INCOMPATIBLE_WITH_EXISTING_SESSION = -8;

    /**
     * Split download has failed.
     */
    int DOWNLOAD_FAILED = -10;

    /**
     * Can't uninstall splits under AAB mode.
     */
    int UNINSTALLATION_UNSUPPORTED = -98;

    /**
     * Copying builtin split apk failed
     */
    int BUILTIN_SPLIT_APK_COPIED_FAILED = -99;

    /**
     * Unknown error processing split install.
     */
    int INTERNAL_ERROR = -100;


}
