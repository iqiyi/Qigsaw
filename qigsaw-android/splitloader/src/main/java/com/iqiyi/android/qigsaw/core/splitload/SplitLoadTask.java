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

package com.iqiyi.android.qigsaw.core.splitload;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;

import java.util.List;

abstract class SplitLoadTask implements SplitLoaderWrapper, Runnable, SplitLoadHandler.OnSplitLoadFinishListener {

    private static final String TAG = "SplitLoadTask";

    private final SplitLoadHandler loadHandler;

    private final OnSplitLoadListener loadListener;

    private SplitLoader splitLoader;

    SplitLoadTask(@NonNull SplitLoadManager loadManager,
                  @NonNull List<Intent> splitFileIntents,
                  @Nullable OnSplitLoadListener loadListener) {
        this.loadHandler = new SplitLoadHandler(this, loadManager, splitFileIntents);
        this.loadListener = loadListener;
    }

    Context getContext() {
        return loadHandler.getContext();
    }

    SplitLoader getSplitLoader() {
        if (splitLoader == null) {
            splitLoader = createSplitLoader();
        }
        return splitLoader;
    }

    @Override
    public void loadResources(String splitApkPath) throws SplitLoadException {
        getSplitLoader().loadResources(splitApkPath);
    }

    @Override
    public final void run() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            loadHandler.loadSplitsSync(this);
        } else {
            synchronized (this) {
                loadHandler.getMainHandler().post(new Runnable() {

                    @Override
                    public void run() {
                        synchronized (SplitLoadTask.this) {
                            loadHandler.loadSplitsSync(SplitLoadTask.this);
                            SplitLoadTask.this.notifyAll();
                        }
                    }
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    SplitLog.w(TAG, "Failed to block thread " + Thread.currentThread().getName(), e);
                    if (loadListener != null) {
                        loadListener.onFailed(SplitLoadError.INTERRUPTED_ERROR);
                    }
                }
            }
        }
    }

    @Override
    public void onLoadFinish(List<SplitBriefInfo> loadOKSplits, List<SplitLoadError> loadErrorSplits, String process, long totalTimeCost) {
        SplitLoadReporter loadReporter = SplitLoadReporterManager.getLoadReporter();
        if (!loadErrorSplits.isEmpty()) {
            if (loadListener != null) {
                int lastErrorCode = loadErrorSplits.get(loadErrorSplits.size() - 1).errorCode;
                loadListener.onFailed(lastErrorCode);
            }
            if (loadReporter != null) {
                loadReporter.onLoadFailed(process, loadOKSplits, loadErrorSplits, totalTimeCost);
            }
        } else {
            if (loadListener != null) {
                loadListener.onCompleted();
            }
            if (loadReporter != null) {
                loadReporter.onLoadOK(process, loadOKSplits, totalTimeCost);
            }
        }
    }
}
