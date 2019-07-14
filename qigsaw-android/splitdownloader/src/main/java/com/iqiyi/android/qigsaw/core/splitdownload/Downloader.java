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

package com.iqiyi.android.qigsaw.core.splitdownload;

import java.util.List;

/**
 * Interface used to download splits, you are expected to implement it with your own downloader lib.
 */
public interface Downloader {

    /**
     * Start to download splits immediately if allowed.
     *
     * @param sessionId represents a single download task.
     * @param requests  contains a group of download requests.
     * @param callback  receives the downloading status of splits.
     */
    void startDownload(int sessionId, List<DownloadRequest> requests, DownloadCallback callback);

    /**
     * Defer to download splits. Deferred tasks will be processed when all started tasks are finished.
     *
     * @param sessionId                represents a single download task.
     * @param requests                 contains a group of download requests.
     * @param callback                 receives the downloading status of splits.
     * @param usingMobileDataPermitted whether permitted to use mobile date to download splits.
     */
    void deferredDownload(int sessionId, List<DownloadRequest> requests, DownloadCallback callback, boolean usingMobileDataPermitted);

    /**
     * Cancel the download task.
     *
     * @param sessionId represents a single download task.
     * @return whether succeeds to cancel, {@code true} if succeeds, otherwise {@code false}
     */
    boolean cancelDownloadSync(int sessionId);

    /**
     * Get the threshold of size allowed to download splits while using mobile data.
     *
     * @return if value < 0,  splits will be download ignore size in mobile data.
     */
    long getDownloadSizeThresholdWhenUsingMobileData();

    /**
     * @return {@code true} if splits could only be deferred-downloaded while using wifi, otherwise {@code false}
     */
    boolean isDeferredDownloadOnlyWhenUsingWifiData();

}
