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

import android.content.Intent;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class SplitStartInstallTask extends SplitInstallTask {

    private final SplitInstallSessionManager mSessionManager;

    private final SplitInstallInternalSessionState mSessionState;

    SplitStartInstallTask(int sessionId,
                          SplitInstaller installer,
                          SplitInstallSessionManager sessionManager,
                          List<String> moduleNames,
                          List<SplitInfo> splitInfoList) {
        super(installer, moduleNames, splitInfoList);
        this.mSessionState = sessionManager.getSessionState(sessionId);
        this.mSessionManager = sessionManager;
    }

    @Override
    boolean continueInstallIgnoreError() {
        return false;
    }

    @Override
    protected void onPreInstall() {
        super.onPreInstall();
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.INSTALLING);
        emitSessionStatus();
    }

    @Override
    protected void onInstallCompleted(List<SplitInstaller.InstallResult> installResults, long cost) {
        List<SplitInstallError> installErrors = new ArrayList<>(0);
        for (SplitInstaller.InstallResult installResult : installResults) {
            if (!installResult.dependenciesInstalled) {
                Throwable exception = new Exception("Split " + installResult.splitName + "' dependencies are not installed!");
                installErrors.add(new SplitInstallError(installResult.splitName, SplitInstallError.DEPENDENCIES_NOT_INSTALLED, exception));
            }
        }
        if (!installErrors.isEmpty()) {
            onInstallFailed(installErrors, cost);
            return;
        }
        List<Intent> splitFileIntents = new ArrayList<>(installResults.size());
        for (SplitInstaller.InstallResult installResult : installResults) {
            Intent splitFileIntent = new Intent();
            if (installResult.libFile != null) {
                splitFileIntent.putExtra(SplitConstants.KEY_NATIVE_LIBRARIES, installResult.libFile.getAbsolutePath());
            }
            if (installResult.multiDexFiles != null) {
                splitFileIntent.putStringArrayListExtra(SplitConstants.KEY_MULTI_DEX, getDexFilePaths(installResult.multiDexFiles));
            }
            if (installResult.optDir != null) {
                splitFileIntent.putExtra(SplitConstants.KEY_OPTIMIZED_DIRECTORY, installResult.optDir.getAbsolutePath());
            }
            splitFileIntent.putExtra(SplitConstants.KET_SPLIT_DIR, installResult.splitDir.getAbsolutePath());
            splitFileIntent.putExtra(SplitConstants.KEY_APK, installResult.apkFile.getAbsolutePath());
            splitFileIntent.putExtra(SplitConstants.KET_NAME, installResult.splitName);
            splitFileIntents.add(splitFileIntent);
        }
        mSessionState.setSplitFileIntents(splitFileIntents);
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.POST_INSTALLED);
        emitSessionStatus();
        if (SplitInstallReporterManager.getInstallReporter() != null) {
            SplitInstallReporterManager.getInstallReporter().onStartInstallOK(moduleNames, cost);
        }
    }

    @Override
    protected void onInstallFailed(List<SplitInstallError> errors, long cost) {
        mSessionState.setErrorCode(errors.get(0).getErrorCode());
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.FAILED);
        emitSessionStatus();
        if (SplitInstallReporterManager.getInstallReporter() != null) {
            SplitInstallReporterManager.getInstallReporter().onStartInstallFailed(moduleNames, errors.get(0), cost);
        }
    }

    private void emitSessionStatus() {
        mSessionManager.emitSessionState(mSessionState);
    }

    private ArrayList<String> getDexFilePaths(List<File> dexFiles) {
        ArrayList<String> dexFilePaths = new ArrayList<>(dexFiles.size());
        for (File dexFile : dexFiles) {
            dexFilePaths.add(dexFile.getAbsolutePath());
        }
        return dexFilePaths;
    }
}
