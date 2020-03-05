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

package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.ProcessUtil;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;

import java.io.File;
import java.io.IOException;

final class SplitInfoVersionManagerImpl implements SplitInfoVersionManager {

    private final static String TAG = "SplitInfoVersionManager";

    private String defaultVersion;

    private File rootDir;

    private String currentVersion;

    private boolean isMainProcess;

    static SplitInfoVersionManager createSplitInfoVersionManager(Context context, boolean isMainProcess) {
        String defaultVersion = SplitBaseInfoProvider.getDefaultSplitInfoVersion();
        String qigsawId = SplitBaseInfoProvider.getQigsawId();
        return new SplitInfoVersionManagerImpl(context, isMainProcess, defaultVersion, qigsawId);
    }

    private SplitInfoVersionManagerImpl(
            Context context,
            boolean isMainProcess,
            String defaultVersion,
            String qigsawId) {
        this.defaultVersion = defaultVersion;
        this.isMainProcess = isMainProcess;
        File baseRootDir = new File(context.getDir(SplitConstants.QIGSAW, Context.MODE_PRIVATE), qigsawId);
        this.rootDir = new File(baseRootDir, SPLIT_ROOT_DIR_NAME);
        processVersionData(context);
        reportNewSplitInfoVersionLoaded();
    }

    private void reportNewSplitInfoVersionLoaded() {
        if (isMainProcess) {
            if (!TextUtils.equals(currentVersion, defaultVersion)) {
                SplitUpdateReporter updateReporter = SplitUpdateReporterManager.getUpdateReporter();
                if (updateReporter != null) {
                    updateReporter.onNewSplitInfoVersionLoaded(currentVersion);
                }
            }
        }
    }

    private void processVersionData(Context context) {
        SplitInfoVersionData versionData = readVersionData();
        if (versionData == null) {
            SplitLog.i(TAG, "No new split info version, just use default version.");
            currentVersion = defaultVersion;
        } else {
            String oldVersion = versionData.oldVersion;
            String newVersion = versionData.newVersion;
            if (oldVersion.equals(newVersion)) {
                SplitLog.i(TAG, "Splits have been updated, so we use new split info version %s.", newVersion);
                currentVersion = newVersion;
            } else {
                if (isMainProcess) {
                    if (updateVersionData(new SplitInfoVersionData(newVersion, newVersion))) {
                        currentVersion = newVersion;
                        ProcessUtil.killAllOtherProcess(context);
                        SplitLog.i(TAG, "Splits have been updated, start to kill other processes!");
                    } else {
                        currentVersion = oldVersion;
                        SplitLog.w(TAG, "Failed to update new split info version: " + newVersion);
                    }
                } else {
                    currentVersion = oldVersion;
                }
            }
        }
    }

    private boolean updateVersionData(SplitInfoVersionData versionData) {
        try {
            SplitInfoVersionDataStorage versionDataStorage = new SplitInfoVersionDataStorageImpl(rootDir);
            boolean result = versionDataStorage.updateVersionData(versionData);
            FileUtil.closeQuietly(versionDataStorage);
            return result;
        } catch (IOException e) {
            //
        }
        return false;
    }

    private SplitInfoVersionData readVersionData() {
        try {
            SplitInfoVersionDataStorage versionDataStorage = new SplitInfoVersionDataStorageImpl(rootDir);
            SplitInfoVersionData versionData = versionDataStorage.readVersionData();
            FileUtil.closeQuietly(versionDataStorage);
            return versionData;
        } catch (IOException e) {
            //
        }
        return null;
    }

    @Override
    public boolean updateVersion(Context context, String newSplitInfoVersion, File newSplitInfoFile) {
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                SplitLog.w(TAG, "Failed to make dir for split info file!");
                return false;
            }
        }
        boolean result = false;
        String fileName = SplitConstants.QIGSAW_PREFIX + newSplitInfoVersion + SplitConstants.DOT_JSON;
        File dest = new File(rootDir, fileName);
        try {
            FileUtil.copyFile(newSplitInfoFile, dest);
            SplitInfoVersionData versionData = new SplitInfoVersionData(currentVersion, newSplitInfoVersion);
            if (updateVersionData(versionData)) {
                SplitLog.i(TAG, "Success to update split info version, current version %s, new version %s", currentVersion, newSplitInfoVersion);
                result = true;
            }
            if (newSplitInfoFile.exists()) {
                if (!newSplitInfoFile.delete()) {
                    SplitLog.w(TAG, "Failed to delete temp split info file: " + newSplitInfoFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to rename file : " + newSplitInfoFile.getAbsolutePath());
        }
        return result;
    }

    @Override
    @NonNull
    public String getDefaultVersion() {
        return defaultVersion;
    }

    @Override
    @NonNull
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public File getRootDir() {
        return rootDir;
    }
}
