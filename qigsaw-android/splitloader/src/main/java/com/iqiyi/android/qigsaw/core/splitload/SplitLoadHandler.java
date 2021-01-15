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

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.compat.NativePathMapper;
import com.iqiyi.android.qigsaw.core.splitload.compat.NativePathMapperImpl;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SplitLoadHandler {

    private static final String TAG = "SplitLoadHandler";

    private final Handler mainHandler;

    private final SplitLoadManager loadManager;

    private final SplitLoaderWrapper splitLoader;

    private final SplitInfoManager infoManager;

    private final List<Intent> splitFileIntents;

    private final SplitActivator activator;

    private final NativePathMapper mapper;

    SplitLoadHandler(@NonNull SplitLoaderWrapper splitLoader,
                     @NonNull SplitLoadManager loadManager,
                     @NonNull List<Intent> splitFileIntents) {
        this.splitLoader = splitLoader;
        this.loadManager = loadManager;
        this.splitFileIntents = splitFileIntents;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.infoManager = SplitInfoManagerService.getInstance();
        this.activator = new SplitActivator(loadManager.getContext());
        this.mapper = new NativePathMapperImpl(loadManager.getContext());
    }

    Handler getMainHandler() {
        return mainHandler;
    }

    @MainThread
    final void loadSplitsSync(final OnSplitLoadFinishListener loadFinishListener) {
        loadSplits(loadFinishListener);
    }

    private void loadSplits(final OnSplitLoadFinishListener loadFinishListener) {
        long totalLoadStart = System.currentTimeMillis();
        Set<Split> loadedSpits = new HashSet<>();
        List<SplitLoadError> loadErrorInfos = new ArrayList<>(0);
        List<SplitBriefInfo> loadOKInfos = new ArrayList<>(splitFileIntents.size());
        for (Intent splitFileIntent : splitFileIntents) {
            long loadStart = System.currentTimeMillis();
            final String splitName = splitFileIntent.getStringExtra(SplitConstants.KET_NAME);
            SplitInfo info = infoManager.getSplitInfo(getContext(), splitName);
            if (info == null) {
                SplitLog.w(TAG, "Unable to get info for %s, just skip!", splitName == null ? "null" : splitName);
                continue;
            }
            SplitBriefInfo splitBriefInfo = new SplitBriefInfo(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn());
            //if if split has been loaded, just skip.
            if (checkSplitLoaded(splitName)) {
                SplitLog.i(TAG, "Split %s has been loaded!", splitName);
                continue;
            }
            String splitApkPath = splitFileIntent.getStringExtra(SplitConstants.KEY_APK);
            if (splitApkPath == null) {
                SplitLog.w(TAG, "Failed to read split %s apk path", splitName);
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, SplitLoadError.INTERNAL_ERROR, new Exception("split apk path " + splitName + " is missing!")));
                continue;
            }
            String dexOptPath = splitFileIntent.getStringExtra(SplitConstants.KEY_DEX_OPT_DIR);
            //check opt-path for split.
            if (info.hasDex() && dexOptPath == null) {
                SplitLog.w(TAG, "Failed to %s get dex-opt-dir", splitName);
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, SplitLoadError.INTERNAL_ERROR, new Exception("dex-opt-dir of " + splitName + " is missing!")));
                continue;
            }
            //check native library path for split.
            String nativeLibPath = splitFileIntent.getStringExtra(SplitConstants.KEY_NATIVE_LIB_DIR);
            try {
                SplitInfo.LibData libData = info.getPrimaryLibData(getContext());
                if (libData != null && nativeLibPath == null) {
                    SplitLog.w(TAG, "Failed to get %s native-lib-dir", splitName);
                    loadErrorInfos.add(new SplitLoadError(splitBriefInfo, SplitLoadError.INTERNAL_ERROR, new Exception("native-lib-dir of " + splitName + " is missing!")));
                    continue;
                }
            } catch (IOException e) {
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, SplitLoadError.INTERNAL_ERROR, e));
                continue;
            }
            //load split's dex files
            List<String> addedDexPaths = splitFileIntent.getStringArrayListExtra(SplitConstants.KEY_ADDED_DEX);
            ClassLoader classLoader;

            // check if need compat native lib path on android 5.x
            SplitLog.d(TAG, "split name: %s, origin native path: %s", splitName, nativeLibPath);
            nativeLibPath = mapper.map(splitName, nativeLibPath);
            SplitLog.d(TAG, "split name: %s, mapped native path: %s", splitName, nativeLibPath);

            try {
                classLoader = splitLoader.loadCode(splitName,
                        addedDexPaths, dexOptPath == null ? null : new File(dexOptPath),
                        nativeLibPath == null ? null : new File(nativeLibPath),
                        info.getDependencies()
                );
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to load split %s code!", splitName);
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                continue;
            }
            //create split application instance.
            final Application application;
            try {
                application = activator.createSplitApplication(classLoader, splitName);
            } catch (SplitLoadException e) {
                SplitLog.printErrStackTrace(TAG, e, "Failed to create %s application ", splitName);
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                splitLoader.unloadCode(classLoader);
                continue;
            }
            try {
                activateSplit(splitName, splitApkPath, application, classLoader);
            } catch (SplitLoadException e) {
                loadErrorInfos.add(new SplitLoadError(splitBriefInfo, e.getErrorCode(), e.getCause()));
                splitLoader.unloadCode(classLoader);
                continue;
            }
            File splitDir = SplitPathManager.require().getSplitDir(info);
            if (!splitDir.setLastModified(System.currentTimeMillis())) {
                SplitLog.w(TAG, "Failed to set last modified time for " + splitName);
            }
            loadOKInfos.add(splitBriefInfo.setTimeCost(System.currentTimeMillis() - loadStart));
            loadedSpits.add(new Split(splitName, splitApkPath));
        }
        loadManager.putSplits(loadedSpits);
        if (loadFinishListener != null) {
            loadFinishListener.onLoadFinish(loadOKInfos, loadErrorInfos, loadManager.currentProcessName, System.currentTimeMillis() - totalLoadStart);
        }
    }

    private void activateSplit(String splitName, String splitApkPath, Application application, ClassLoader classLoader) throws SplitLoadException {
        try {
            splitLoader.loadResources(splitApkPath);
        } catch (SplitLoadException e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to load %s resources", splitApkPath);
        }
        //attach split application.
        try {
            activator.attachSplitApplication(application);
        } catch (SplitLoadException e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to attach %s application", splitName);
            throw e;
        }
        //create split content-provider instance.
        try {
            activator.createAndActivateSplitContentProviders(classLoader, splitName);
        } catch (SplitLoadException e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to create %s content-provider ", splitName);
            throw e;
        }
        //invoke onCreate for split application.
        try {
            activator.invokeOnCreateForSplitApplication(application);
        } catch (SplitLoadException e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to invoke onCreate for %s application", splitName);
            throw e;
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

    final Context getContext() {
        return loadManager.getContext();
    }

    interface OnSplitLoadFinishListener {

        void onLoadFinish(List<SplitBriefInfo> loadOKSplits, List<SplitLoadError> loadErrorSplits, String process, long totalTimeCost);
    }
}
