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

import com.iqiyi.android.qigsaw.core.splitdownload.DownloadCallback;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.util.List;

final class DeferredDownloadCallback implements DownloadCallback {

    private final SplitInstaller splitInstaller;

    private final List<SplitInfo> splitInfoList;

    DeferredDownloadCallback(SplitInstaller splitInstaller,
                             List<SplitInfo> splitInfoList) {
        this.splitInfoList = splitInfoList;
        this.splitInstaller = splitInstaller;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onCanceled() {

    }

    @Override
    public void onCanceling() {

    }

    @Override
    public void onProgress(long currentBytes) {

    }

    @Override
    public void onCompleted() {
        SplitBackgroundExecutor.getExecutor().execute(new SplitDeferredInstallTask(splitInstaller, splitInfoList));
    }

    @Override
    public void onError(int errorCode) {

    }
}
