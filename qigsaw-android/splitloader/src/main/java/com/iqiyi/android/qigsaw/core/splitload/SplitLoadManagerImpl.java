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
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
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

final class SplitLoadManagerImpl extends SplitLoadManager {

    private static final String TAG = "SplitLoadManagerImpl";

    SplitLoadManagerImpl(Context context,
                         String currentProcessName,
                         String[] processes) {
        super(context, currentProcessName, processes);
        SplitInfoManagerService.install(context, currentProcessName);
        SplitPathManager.install(context);
    }

    @Override
    public void injectPathClassloader() {
        if (isInjectPathClassloaderNeeded()) {
            if (isProcessAllowedToWork()) {
                injectClassLoader(getContext().getClassLoader());
            }
        }
    }

    @Override
    public void loadInstalledSplitsInitially(boolean aabMode) {
        if (aabMode) {
            return;
        }
        if (isProcessAllowedToWork()) {
            deferredLoadInstalledSplits();
        }
    }

    @Override
    public void getResources(Resources resources) {
        try {
            SplitCompatResourcesLoader.loadResources(getContext(), resources);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public Runnable createSplitLoadTask(List<Intent> splitFileIntents, @Nullable OnSplitLoadListener loadListener) {
        return new SplitLoadTask(this, splitFileIntents, loadListener);
    }

    private boolean isInjectPathClassloaderNeeded() {
        boolean qigsawAssembleMode = SplitBaseInfoProvider.isQigsawAssembleMode();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return qigsawAssembleMode;
        } else {
            boolean exist = (getContext().getClassLoader() instanceof SplitDelegateClassloader);
            return !exist && qigsawAssembleMode;
        }
    }

    private List<Intent> createInstalledSplitFileIntents(@NonNull Collection<SplitInfo> splitInfoList) {
        List<Intent> splitFileIntents = new ArrayList<>();
        for (SplitInfo splitInfo : splitInfoList) {
            if (canBeWorkedInThisProcessForSplit(splitInfo)) {
                if (getLoadedSplitNames().contains(splitInfo.getSplitName())) {
                    SplitLog.i(TAG, "Split %s has been load, ignore it!", splitInfo.getSplitName());
                    continue;
                }
                SplitLog.i(TAG, "Split %s will work in this process!", splitInfo.getSplitName());
                Intent splitFileIntent = createLastInstalledSplitFileIntent(splitInfo);
                if (splitFileIntent != null) {
                    SplitLog.i(TAG, "Split %s has been installed, pack it!", splitInfo.getSplitName());
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
            String packageName = getContext().getPackageName();
            String simpleProcessName = currentProcessName.replace(packageName, "");
            SplitLog.i(TAG, "Current process simple name: " + (TextUtils.isEmpty(simpleProcessName) ? "null" : simpleProcessName));
            return workProcesses.contains(simpleProcessName);
        }
        return true;
    }

    private void deferredLoadInstalledSplits() {
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                loadInstalledSplits();
                return false;
            }
        });
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
            ArrayList<String> addedDexPaths = null;
            if (splitInfo.hasDex()) {
                addedDexPaths = new ArrayList<>();
                addedDexPaths.add(splitApk.getAbsolutePath());
                File[] results = SplitPathManager.require().getSplitCodeCacheDir(splitInfo).listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(SplitConstants.DOT_ZIP);
                    }
                });
                if (results != null && results.length > 0) {
                    for (File result : results) {
                        addedDexPaths.add(result.getAbsolutePath());
                    }
                }
            }
            Intent splitFileIntent = new Intent();
            splitFileIntent.putExtra(SplitConstants.KET_NAME, splitName);
            splitFileIntent.putExtra(SplitConstants.KEY_APK, splitApk.getAbsolutePath());
            if (addedDexPaths != null) {
                splitFileIntent.putStringArrayListExtra(SplitConstants.KEY_ADDED_DEX, addedDexPaths);
            }
            SplitLog.i(TAG, "Split %s has been installed, we can load it!", splitName);
            return splitFileIntent;
        }
        SplitLog.i(TAG, "Split %s mark file is not existed!", splitName);
        return null;
    }

    private void injectClassLoader(ClassLoader originalClassloader) {
        try {
            SplitDelegateClassloader.inject(originalClassloader, getBaseContext());
        } catch (Exception e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to hook PathClassloader");
        }
    }

}
