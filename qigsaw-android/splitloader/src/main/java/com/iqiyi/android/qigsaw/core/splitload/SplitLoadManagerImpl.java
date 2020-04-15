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
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.OEMCompat;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SplitLoadManagerImpl extends SplitLoadManager {

    private final boolean qigsawMode;

    private final String[] forbiddenWorkProcesses;

    private final String[] workProcesses;

    SplitLoadManagerImpl(Context context,
                         int splitLoadMode,
                         boolean qigsawMode,
                         boolean isMainProcess,
                         String currentProcessName,
                         String[] workProcesses,
                         String[] forbiddenWorkProcesses) {
        super(context, currentProcessName, splitLoadMode);
        this.qigsawMode = qigsawMode;
        this.workProcesses = workProcesses;
        this.forbiddenWorkProcesses = forbiddenWorkProcesses;
        SplitInfoManagerService.install(context, isMainProcess);
        SplitPathManager.install(context);
    }

    @Override
    public void injectPathClassloader() {
        if (isInjectPathClassloaderNeeded()) {
            if (isProcessAllowedToWork()) {
                injectClassLoader(getContext().getClassLoader());
            }
        }
        ClassLoader curCl = getContext().getClassLoader();
        if (curCl instanceof SplitDelegateClassloader) {
            ((SplitDelegateClassloader) curCl).setSplitLoadMode(splitLoadMode);
        }
    }

    @Override
    public void loadInstalledSplitsWhenAppLaunches() {
        if (!qigsawMode) {
            return;
        }
        if (isProcessAllowedToWork()) {
            deferredLoadInstalledSplitsIfNeed();
        }
    }

    @Override
    public void getResources(Resources resources) {
        try {
            SplitCompatResourcesLoader.loadResources(getContext(), resources);
        } catch (Throwable error) {
            error.printStackTrace();
        }
    }

    @Override
    public Runnable createSplitLoadTask(List<Intent> splitFileIntents, @Nullable OnSplitLoadListener loadListener) {
        if (splitLoadMode == SplitLoad.MULTIPLE_CLASSLOADER) {
            return new SplitLoadTaskImpl(this, splitFileIntents, loadListener);
        } else {
            return new SplitLoadTaskImpl2(this, splitFileIntents, loadListener);
        }
    }

    @Override
    public void loadInstalledSplits() {
        SplitInfoManager manager = SplitInfoManagerService.getInstance();
        if (manager == null) {
            SplitLog.w(TAG, "Failed to get SplitInfoManager instance, have you invoke Qigsaw#install(...) method?");
            return;
        }
        Collection<SplitInfo> splitInfoList = manager.getAllSplitInfo(getContext());
        if (splitInfoList == null) {
            SplitLog.w(TAG, "Failed to get Split-Info list!");
            return;
        }
        //main process start to uninstall splits, other processes don't load pending uninstall splits.
        List<Intent> splitFileIntents = createInstalledSplitFileIntents(splitInfoList);
        if (splitFileIntents.isEmpty()) {
            SplitLog.w(TAG, "There are no installed splits!");
            return;
        }
        createSplitLoadTask(splitFileIntents, null).run();
    }

    private boolean isInjectPathClassloaderNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return qigsawMode;
        } else {
            boolean exist = (getContext().getClassLoader() instanceof SplitDelegateClassloader);
            return !exist && qigsawMode;
        }
    }

    private List<Intent> createInstalledSplitFileIntents(@NonNull Collection<SplitInfo> splitInfoList) {
        List<Intent> splitFileIntents = new ArrayList<>();
        for (SplitInfo splitInfo : splitInfoList) {
            if (canBeWorkedInThisProcessForSplit(splitInfo)) {
                if (getLoadedSplitNames().contains(splitInfo.getSplitName())) {
                    SplitLog.i(TAG, "Split %s has been loaded, ignore it!", splitInfo.getSplitName());
                    continue;
                }
                Intent splitFileIntent = createLastInstalledSplitFileIntent(splitInfo);
                if (splitFileIntent != null) {
                    splitFileIntents.add(splitFileIntent);
                }
                SplitLog.i(TAG, "Split %s will work in process %s, %s it is %s",
                        splitInfo.getSplitName(), currentProcessName,
                        splitFileIntent == null ? "but" : "and",
                        splitFileIntent == null ? "not installed" : "installed");
            } else {
                SplitLog.i(TAG, "Split %s do not need work in process %s", splitInfo.getSplitName(), currentProcessName);
            }
        }
        return splitFileIntents;
    }

    private boolean canBeWorkedInThisProcessForSplit(SplitInfo splitInfo) {
        List<String> workProcesses = splitInfo.getWorkProcesses();
        if (workProcesses != null && !workProcesses.isEmpty()) {
            String packageName = getContext().getPackageName();
            String simpleProcessName = currentProcessName.replace(packageName, "");
            return workProcesses.contains(simpleProcessName);
        }
        return true;
    }

    private void deferredLoadInstalledSplitsIfNeed() {
        if (splitLoadMode == SplitLoad.MULTIPLE_CLASSLOADER) {
            Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    loadInstalledSplits();
                    return false;
                }
            });
        } else {
            loadInstalledSplits();
        }
    }

    /**
     * fast check operation
     */
    private Intent createLastInstalledSplitFileIntent(SplitInfo splitInfo) {
        String splitName = splitInfo.getSplitName();
        File splitDir = SplitPathManager.require().getSplitDir(splitInfo);
        File markFile = SplitPathManager.require().getSplitMarkFile(splitInfo);
        File specialMarkFile = SplitPathManager.require().getSplitSpecialMarkFile(splitInfo);
        File splitApk;
        if (splitInfo.isBuiltIn() && splitInfo.getUrl().startsWith(SplitConstants.URL_NATIVE)) {
            splitApk = new File(getContext().getApplicationInfo().nativeLibraryDir, System.mapLibraryName(SplitConstants.SPLIT_PREFIX + splitInfo.getSplitName()));
        } else {
            splitApk = new File(splitDir, splitName + SplitConstants.DOT_APK);
        }
        //check oat file if special mark file is exist.
        if (specialMarkFile.exists() && !markFile.exists()) {
            SplitLog.v(TAG, "In vivo & oppo, we need to check oat file when split is going to be loaded.");
            File optimizedDirectory = SplitPathManager.require().getSplitOptDir(splitInfo);
            File oatFile = OEMCompat.getOatFilePath(splitApk, optimizedDirectory);
            if (FileUtil.isLegalFile(oatFile)) {
                boolean result = OEMCompat.checkOatFile(oatFile);
                SplitLog.v(TAG, "Check result of oat file %s is " + result, oatFile.getAbsoluteFile());
                File lockFile = SplitPathManager.require().getSplitSpecialLockFile(splitInfo);
                if (result) {
                    try {
                        FileUtil.createFileSafelyLock(markFile, lockFile);
                    } catch (IOException e) {
                        SplitLog.w(TAG, "Failed to create installed mark file " + oatFile.exists());
                    }
                } else {
                    try {
                        FileUtil.deleteFileSafelyLock(oatFile, lockFile);
                    } catch (IOException e) {
                        SplitLog.w(TAG, "Failed to delete corrupted oat file " + oatFile.exists());
                    }
                }
            } else {
                SplitLog.v(TAG, "Oat file %s is still not exist in vivo & oppo, system continue to use interpreter mode.", oatFile.getAbsoluteFile());
            }
        }
        if (markFile.exists() || specialMarkFile.exists()) {
            List<String> dependencies = splitInfo.getDependencies();
            if (dependencies != null) {
                SplitLog.i(TAG, "Split %s has dependencies %s !", splitName, dependencies);
                for (String dependency : dependencies) {
                    SplitInfo dependencySplitInfo = SplitInfoManagerService.getInstance().getSplitInfo(getContext(), dependency);
                    File dependencyMarkFile = SplitPathManager.require().getSplitMarkFile(dependencySplitInfo);
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
            return splitFileIntent;
        }
        return null;
    }

    private void injectClassLoader(ClassLoader originalClassloader) {
        try {
            SplitDelegateClassloader.inject(originalClassloader, getBaseContext());
        } catch (Exception e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to hook PathClassloader");
        }
    }

    private Context getBaseContext() {
        Context ctx = getContext();
        while (ctx instanceof ContextWrapper) {
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return ctx;
    }

    private boolean isProcessAllowedToWork() {
        if (workProcesses == null && forbiddenWorkProcesses == null) {
            return true;
        }
        if (getContext().getPackageName().equals(currentProcessName)) {
            return true;
        }
        if (forbiddenWorkProcesses != null) {
            for (String process : forbiddenWorkProcesses) {
                if (getCompleteProcessName(process).equals(currentProcessName)) {
                    return false;
                }
            }
        }
        if (workProcesses != null) {
            for (String process : workProcesses) {
                if (getCompleteProcessName(process).equals(currentProcessName)) {
                    return true;
                }
            }
        }
        return true;
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

}
