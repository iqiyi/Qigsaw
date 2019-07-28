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
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.ProcessUtil;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import dalvik.system.PathClassLoader;

final class SplitLoadManagerImpl extends SplitLoadManager {

    private static final String TAG = "SplitLoadManagerImpl";

    private PathClassLoader mClassloader;

    private final boolean loadInstalledSplitsOnApplicationCreate;

    private final boolean isAAB;

    SplitLoadManagerImpl(Context context,
                         String[] processes,
                         boolean loadInstalledSplitsOnApplicationCreate,
                         boolean isAAB) {
        super(context, processes);
        this.loadInstalledSplitsOnApplicationCreate = loadInstalledSplitsOnApplicationCreate;
        this.isAAB = isAAB;
        SplitInfoManagerService.install(context);
        SplitPathManager.install(context);
    }

    @Override
    public void injectPathClassloaderIfNeed(boolean needHookClassLoader) {
        if (hasWorkProcess()) {
            for (String process : getWorkProcesses()) {
                if (getCompleteProcessName(process).equals(getCurrentProcessName())) {
                    hookPathClassLoaderIfNeed(needHookClassLoader);
                }
            }
        } else {
            hookPathClassLoaderIfNeed(needHookClassLoader);
        }
    }

    @Override
    public void onCreate() {
        if (isAAB || !loadInstalledSplitsOnApplicationCreate) {
            return;
        }
        if (hasWorkProcess()) {
            for (String process : getWorkProcesses()) {
                if (getCompleteProcessName(process).equals(getCurrentProcessName())) {
                    loadInstalledSplits();
                }
            }
        } else {
            loadInstalledSplits();
        }
    }

    @Override
    public void getResources(Resources resources) {
        Set<Split> loadedSplits = getLoadedSplits();
        if (!loadedSplits.isEmpty()) {
            for (Split split : loadedSplits) {
                try {
                    SplitCompatResourcesLoader.loadResources(getContext(), resources, split.splitApkPath);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    @Override
    public Runnable createSplitLoadTask(List<Intent> splitFileIntents, @Nullable OnSplitLoadListener loadListener) {
        return new SplitLoadTask(this, splitFileIntents, loadListener);
    }

    private List<Intent> createInstalledSplitFileIntents(@NonNull Collection<SplitInfo> splitInfoList) {
        List<Intent> splitFileIntents = new ArrayList<>();
        for (SplitInfo splitInfo : splitInfoList) {
            if (canBeWorkedInThisProcessForSplit(splitInfo)) {
                if (getLoadedSplitNames().contains(splitInfo.getSplitName())) {
                    SplitLog.i(TAG, "Split %s has been load! 'createInstalledSplitFileIntents'", splitInfo.getSplitName());
                    continue;
                }
                SplitLog.i(TAG, "Split %s will work in this process", splitInfo.getSplitName());
                Intent splitFileIntent = createLastInstalledSplitFileIntent(splitInfo);
                if (splitFileIntent != null) {
                    SplitLog.i(TAG, "Split %s has been installed! 'createInstalledSplitFileIntents'", splitInfo.getSplitName());
                    splitFileIntents.add(splitFileIntent);
                }
            } else {
                SplitLog.i(TAG, "Split %s do not need work in this process", splitInfo.getSplitName());
            }
        }
        return splitFileIntents;
    }

    private boolean canBeWorkedInThisProcessForSplit(SplitInfo splitInfo) {
        List<String> workProcesses = splitInfo.getWorkProcesses();
        if (workProcesses != null && !workProcesses.isEmpty()) {
            String currentProcessName = ProcessUtil.getProcessName(getContext());
            String packageName = getContext().getPackageName();
            String simpleProcessName = currentProcessName.replace(packageName, "");
            SplitLog.i(TAG, "Current process simple name: " + (TextUtils.isEmpty(simpleProcessName) ? "null" : simpleProcessName));
            return workProcesses.contains(simpleProcessName);
        }
        return true;
    }

    @Override
    PathClassLoader getInjectedClassloader() {
        return (PathClassLoader) (mClassloader == null ? getContext().getClassLoader() : mClassloader.getParent());
    }

    @Override
    public void loadInstalledSplits() {
        SplitInfoManager manager = SplitInfoManagerService.getInstance();
        if (manager != null) {
            Collection<SplitInfo> splitInfoList = manager.getAllSplitInfo(getContext());
            if (splitInfoList != null) {
                List<Intent> splitFileIntents = createInstalledSplitFileIntents(splitInfoList);
                if (!splitFileIntents.isEmpty()) {
                    createSplitLoadTask(splitFileIntents, null).run();
                } else {
                    SplitLog.w(TAG, "There are no installed splits!");
                }
            } else {
                SplitLog.w(TAG, "Failed to get Split-Info list!");
            }
        } else {
            SplitLog.w(TAG, "Failed to get SplitInfoManager instance!");
        }
    }

    private void hookPathClassLoaderIfNeed(boolean needHookClassLoader) {
        if (needHookClassLoader) {
            SplitLog.i(TAG, "AppComponentFactory is not declared in app Manifest, so we need hook PathClassLoader!");
            injectClassLoader(getContext().getClassLoader());
        } else {
            SplitLog.i(TAG, "AppComponentFactory is  declared in app Manifest!");
        }
    }

    private String getCompleteProcessName(@Nullable String process) {
        String packageName = getContext().getPackageName();
        if (TextUtils.isEmpty(process)) {
            return packageName;
        }
        if (process.startsWith(packageName)) {
            return process;
        }
        return packageName + process;
    }

    /**
     * fast check operation
     */
    private Intent createLastInstalledSplitFileIntent(SplitInfo splitInfo) {
        String splitName = splitInfo.getSplitName();
        File splitDir = SplitPathManager.require().getSplitDir(splitInfo);
        File markFile = new File(splitDir, splitInfo.getMd5());
        File splitApk = new File(splitDir, splitName + SplitConstants.DOT_APK);
        if (markFile.exists()) {
            SplitLog.i(TAG, "Split %s mark file is existed!", splitName);
            List<String> dependencies = splitInfo.getDependencies();
            if (dependencies != null) {
                SplitLog.i(TAG, "Split %s has dependencies %s !", splitName, dependencies);
                for (String dependency : dependencies) {
                    SplitInfo dependencySplitInfo = SplitInfoManagerService.getInstance().getSplitInfo(getContext(), dependency);
                    File dependencySplitDir = SplitPathManager.require().getSplitDir(dependencySplitInfo);
                    File dependencyMarkFile = new File(dependencySplitDir, dependencySplitInfo.getMd5());
                    if (!dependencyMarkFile.exists()) {
                        SplitLog.i(TAG, "Dependency %s mark file is not existed!", dependency);
                        return null;
                    }
                }
            }
            File libDir = null;
            if (splitInfo.hasLibs()) {
                libDir = SplitPathManager.require().getSplitLibDir(splitInfo);
            }
            ArrayList<String> multiDexFiles = null;
            File optDir = null;
            if (splitInfo.hasDex()) {
                optDir = SplitPathManager.require().getSplitOptDir(splitInfo);
                File[] results = SplitPathManager.require().getSplitCodeCacheDir(splitInfo).listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(SplitConstants.DOT_ZIP);
                    }
                });
                if (results != null && results.length > 0) {
                    multiDexFiles = new ArrayList<>(results.length);
                    for (File result : results) {
                        multiDexFiles.add(result.getAbsolutePath());
                    }
                }
            }
            Intent splitFileIntent = new Intent();
            splitFileIntent.putExtra(SplitConstants.KET_NAME, splitName);
            splitFileIntent.putExtra(SplitConstants.KEY_APK, splitApk.getAbsolutePath());
            splitFileIntent.putExtra(SplitConstants.KET_SPLIT_DIR, splitDir.getAbsolutePath());
            if (optDir != null) {
                splitFileIntent.putExtra(SplitConstants.KEY_OPTIMIZED_DIRECTORY, optDir.getAbsolutePath());
            }
            if (libDir != null) {
                splitFileIntent.putExtra(SplitConstants.KEY_NATIVE_LIBRARIES, libDir.getAbsolutePath());
            }
            if (multiDexFiles != null) {
                splitFileIntent.putStringArrayListExtra(SplitConstants.KEY_MULTI_DEX, multiDexFiles);
            }
            SplitLog.i(TAG, "Split %s has been installed, so we can load it!", splitName);
            return splitFileIntent;
        }
        SplitLog.i(TAG, "Split %s mark file is not existed!", splitName);
        SplitLog.i(TAG, "Split %s apk file is existed? " + splitApk.exists(), splitName);
        return null;
    }

    private void injectClassLoader(ClassLoader originalClassloader) {
        try {
            mClassloader = SplitProxyClassloader.inject(originalClassloader, getContext());
        } catch (Exception e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to hook PathClassloader");
        }
    }
}
