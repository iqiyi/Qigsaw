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

import java.util.ArrayList;
import java.util.List;

final class SplitStartInstallTask extends SplitInstallTask {

    private final SplitInstallSessionManager mSessionManager;

    private final SplitInstallInternalSessionState mSessionState;

    SplitStartInstallTask(int sessionId,
                          SplitInstaller installer,
                          SplitInstallSessionManager sessionManager,
                          List<SplitInfo> splitInfoList) {
        super(installer, splitInfoList);
        this.mSessionState = sessionManager.getSessionState(sessionId);
        this.mSessionManager = sessionManager;
    }

    @Override
    boolean isStartInstallOperation() {
        return true;
    }

    @Override
    protected void onPreInstall() {
        super.onPreInstall();
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.INSTALLING);
        emitSessionStatus();
    }

    @Override
    void onInstallCompleted(List<SplitInstaller.InstallResult> installResults) {
        super.onInstallCompleted(installResults);
        List<Intent> splitFileIntents = new ArrayList<>(installResults.size());
        for (SplitInstaller.InstallResult installResult : installResults) {
            Intent splitFileIntent = new Intent();
            if (installResult.addedDexPaths != null) {
                splitFileIntent.putStringArrayListExtra(SplitConstants.KEY_ADDED_DEX, (ArrayList<String>) installResult.addedDexPaths);
            }
            splitFileIntent.putExtra(SplitConstants.KEY_APK, installResult.apkFile.getAbsolutePath());
            splitFileIntent.putExtra(SplitConstants.KET_NAME, installResult.splitName);
            splitFileIntents.add(splitFileIntent);
        }
        mSessionState.setSplitFileIntents(splitFileIntents);
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.POST_INSTALLED);
        emitSessionStatus();

    }

    @Override
    void onInstallFailed(List<SplitInstallError> errors) {
        super.onInstallFailed(errors);
        mSessionState.setErrorCode(errors.get(0).errorCode);
        mSessionManager.changeSessionState(mSessionState.sessionId(), SplitInstallInternalSessionStatus.FAILED);
        emitSessionStatus();
    }

    private void emitSessionStatus() {
        mSessionManager.emitSessionState(mSessionState);
    }

}
