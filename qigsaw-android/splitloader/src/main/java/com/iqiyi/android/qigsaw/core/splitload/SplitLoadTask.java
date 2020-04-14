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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class SplitLoadTask implements Runnable {

    private static final String TAG = "SplitLoadTask";

    final Context appContext;

    private final Handler mainHandler;

    private final SplitActivator activator;

    private final SplitLoadManager loadManager;

    private final SplitInfoManager infoManager;

    private final List<Intent> splitFileIntents;

    private final OnSplitLoadListener loadListener;

    private final Object mLock = new Object();

    SplitLoadTask(@NonNull SplitLoadManager loadManager,
                  @NonNull List<Intent> splitFileIntents,
                  @Nullable OnSplitLoadListener loadListener) {
        this.loadManager = loadManager;
        this.splitFileIntents = splitFileIntents;
        this.loadListener = loadListener;
        this.appContext = loadManager.getContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.infoManager = SplitInfoManagerService.getInstance();
        this.activator = new SplitActivator(loadManager.getContext());
    }

    abstract SplitLoader createSplitLoader();

    abstract ClassLoader loadCode(SplitLoader loader,
                                  String splitName,
                                  List<String> addedDexPaths,
                                  File optimizedDirectory,
                                  File librarySearchPath,
                                  List<String> dependencies) throws SplitLoadException;

    abstract void onSplitActivateFailed(ClassLoader classLoader);

    @Override
    public final void run() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            loadSplitInternal();
        } else {
            synchronized (mLock) {
                mainHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        synchronized (mLock) {
                            loadSplitInternal();
                            mLock.notifyAll();
                        }
                    }
                });
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    String splitName = splitFileIntents.get(0).getStringExtra(SplitConstants.KET_NAME);
                    SplitInfo info = infoManager.getSplitInfo(appContext, splitName);
                    if (info != null) {
                        SplitBriefInfo splitBriefInfo = new SplitBriefInfo(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn());
                        reportLoadResult(Collections.<SplitBriefInfo>emptyList(), Collections.singletonList(new SplitLoadError(splitBriefInfo, SplitLoadError.INTERRUPTED_ERROR, e)), 0);
                    }
                }
            }
        }
    }

    private void loadSplitInternal() {
        long time = System.currentTimeMillis();
        SplitLoader loader = createSplitLoader();
        Set<Split> splits = new HashSet<>();
        List<SplitLoadError> loadErrors = new ArrayList<>(0);
        List<SplitBriefInfo> splitBriefInfoList = new ArrayList<>(splitFileIntents.size());
        for (Intent splitFileIntent : splitFileIntents) {
            String splitName = splitFileIntent.getStringExtra(SplitConstants.KET_NAME);
            SplitInfo info = infoManager.getSplitInfo(appContext, splitName);
            if (info == null) {
                SplitLog.w(TAG, "Unable to get info of %s, just skip!", splitName == null ? "null" : splitName);
                continue;
            }
            SplitBriefInfo splitBriefInfo = new SplitBriefInfo(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn());
            //if if split has been loaded, just skip.
            if (checkSplitLoaded(splitName)) {
                SplitLog.i(TAG, "Split %s has been loaded!", splitName);
                continue;
            }
            String splitApkPath = splitFileIntent.getStringExtra(SplitConstants.KEY_APK);
            try {
                //load split's resources.
                loader.loadResources(splitApkPath);
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to load split %s resources!", splitName);
                loadErrors.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                continue;
            }
            List<String> addedDexPaths = splitFileIntent.getStringArrayListExtra(SplitConstants.KEY_ADDED_DEX);
            File optimizedDirectory = SplitPathManager.require().getSplitOptDir(info);
            File librarySearchPath = null;
            if (info.hasLibs()) {
                librarySearchPath = SplitPathManager.require().getSplitLibDir(info);
            }
            File splitDir = SplitPathManager.require().getSplitDir(info);
            ClassLoader classLoader;
            try {
                classLoader = loadCode(loader, splitName, addedDexPaths, optimizedDirectory, librarySearchPath, info.getDependencies());
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to load split %s code!", splitName);
                loadErrors.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                continue;
            }
            //activate split, include application and provider.
            try {
                activator.activate(classLoader, splitName);
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to activate " + splitName);
                loadErrors.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                onSplitActivateFailed(classLoader);
                continue;
            }
            if (!splitDir.setLastModified(System.currentTimeMillis())) {
                SplitLog.w(TAG, "Failed to set last modified time for " + splitName);
            }
            splitBriefInfoList.add(splitBriefInfo);
            splits.add(new Split(splitName, splitApkPath));
        }
        loadManager.putSplits(splits);
        reportLoadResult(splitBriefInfoList, loadErrors, System.currentTimeMillis() - time);
    }

    private void reportLoadResult(List<SplitBriefInfo> splitBriefInfoList, List<SplitLoadError> errors, long cost) {
        SplitLoadReporter loadReporter = SplitLoadReporterManager.getLoadReporter();
        if (!errors.isEmpty()) {
            if (loadListener != null) {
                int lastErrorCode = errors.get(errors.size() - 1).errorCode;
                loadListener.onFailed(lastErrorCode);
            }
            if (loadReporter != null) {
                loadReporter.onLoadFailed(loadManager.currentProcessName, splitBriefInfoList, errors, cost);
            }
        } else {
            if (loadListener != null) {
                loadListener.onCompleted();
            }
            if (loadReporter != null) {
                loadReporter.onLoadOK(loadManager.currentProcessName, splitBriefInfoList, cost);
            }
        }
    }

    private boolean checkSplitLoaded(String splitName) {
        for (Split split : loadManager.getLoadedSplits()) {
            if (split.splitName.equals(splitName)) {
                return true;
            }
        }
        return false;
    }


}
