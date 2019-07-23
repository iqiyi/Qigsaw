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

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class SplitLoadTask implements Runnable {

    private static final String TAG = "SplitLoadTask";

    private final List<Intent> splitFileIntents;

    private final OnSplitLoadListener loadListener;

    private final SplitLoadManager loadManager;

    private final SplitActivator splitActivator;

    private final Object mLock = new Object();

    private final List<String> moduleNames;

    SplitLoadTask(SplitLoadManager loadManager,
                  @NonNull List<Intent> splitFileIntents,
                  @Nullable OnSplitLoadListener loadListener) {
        this.loadManager = loadManager;
        this.splitActivator = new SplitActivator(AABExtension.getInstance());
        this.splitFileIntents = splitFileIntents;
        this.loadListener = loadListener;
        this.moduleNames = getRequestModuleNames();
    }

    @Override
    public void run() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            loadSplits();
        } else {
            synchronized (mLock) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            loadSplits();
                            mLock.notifyAll();
                        }
                    }
                });
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    List<SplitLoadError> errors = Collections.singletonList(new SplitLoadError(moduleNames.get(0), SplitLoadError.INTERRUPTED_ERROR, e));
                    reportLoadResult(errors, 0);
                }
            }
        }
    }

    private void loadSplits() {
        long lastTimeMillis = System.currentTimeMillis();
        SplitLoader loader = new SplitLoaderImpl(loadManager.getContext());
        Set<Split> splits = new ArraySet<>(splitFileIntents.size());
        ClassLoader classLoader = loadManager.getInjectedClassloader();
        List<SplitLoadError> errors = new ArrayList<>(0);
        for (Intent splitFileIntent : splitFileIntents) {
            String splitName = splitFileIntent.getStringExtra(SplitConstants.KET_NAME);
            String splitApkPath = splitFileIntent.getStringExtra(SplitConstants.KEY_APK);
            //if if split has been loaded, just skip.
            if (isSplitLoaded(splitName)) {
                SplitLog.i(TAG, "Split %s has been loaded!", splitName);
                continue;
            }
            try {
                loader.load(classLoader, splitFileIntent);
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to load split %s, error code: %d", splitName, e.getErrorCode());
                errors.add(new SplitLoadError(splitName, e.getErrorCode(), e.getCause()));
                continue;
            }
            //activate application
            try {
                splitActivator.activate(splitName);
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to activate %s", splitName);
                errors.add(new SplitLoadError(splitName, e.getErrorCode(), e.getCause()));
                try {
                    SplitCompatDexLoader.unLoad(classLoader);
                } catch (Throwable throwable1) {
                    //ignored
                }
                continue;
            }

            splits.add(new Split(splitName, splitApkPath));
            File splitDir = new File(splitFileIntent.getStringExtra(SplitConstants.KET_SPLIT_DIR));
            if (!splitDir.setLastModified(System.currentTimeMillis())) {
                SplitLog.w(TAG, "Failed to set last modified time for " + splitName);
            }
        }
        loadManager.putSplits(splits);
        reportLoadResult(errors, System.currentTimeMillis() - lastTimeMillis);
    }

    private void reportLoadResult(List<SplitLoadError> errors, long cost) {
        SplitLoadReporter loadReporter = SplitLoadReporterManager.getLoadReporter();
        if (!errors.isEmpty()) {
            if (loadListener != null) {
                int lastErrorCode = errors.get(errors.size() - 1).getErrorCode();
                loadListener.onFailed(lastErrorCode);
            }
            if (loadReporter != null) {
                loadReporter.onLoadFailed(moduleNames, loadManager.getCurrentProcessName(), errors, cost);
            }

        } else {
            if (loadListener != null) {
                loadListener.onCompleted();
            }
            if (loadReporter != null) {
                loadReporter.onLoadOK(moduleNames, loadManager.getCurrentProcessName(), cost);
            }
        }
    }

    private List<String> getRequestModuleNames() {
        List<String> requestModuleNames = new ArrayList<>(splitFileIntents.size());
        for (Intent intent : splitFileIntents) {
            requestModuleNames.add(intent.getStringExtra(SplitConstants.KET_NAME));
        }
        return requestModuleNames;
    }

    private boolean isSplitLoaded(String splitName) {
        for (Split split : loadManager.getLoadedSplits()) {
            if (split.splitName.equals(splitName)) {
                return true;
            }
        }
        return false;
    }
}
