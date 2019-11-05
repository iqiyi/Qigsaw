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

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class SplitInstallTask implements Runnable {

    private static final String TAG = "SplitInstallTask";

    private final SplitInstaller installer;

    private final Collection<SplitInfo> needUpdateSplits;

    final List<SplitBriefInfo> splitBriefInfoList;

    SplitInstallTask(SplitInstaller installer,
                     Collection<SplitInfo> needUpdateSplits) {
        this.installer = installer;
        this.needUpdateSplits = needUpdateSplits;
        splitBriefInfoList = createSplitBriefInfoList(needUpdateSplits);
    }

    private List<SplitBriefInfo> createSplitBriefInfoList(Collection<SplitInfo> needUpdateSplits) {
        List<SplitBriefInfo> splitBriefInfoList = new ArrayList<>(needUpdateSplits.size());
        for (SplitInfo info : needUpdateSplits) {
            splitBriefInfoList.add(new SplitBriefInfo(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn()));
        }
        return splitBriefInfoList;
    }

    abstract boolean isStartInstallOperation();

    @Override
    public final void run() {
        onPreInstall();
        long currentTime = System.currentTimeMillis();
        List<SplitInstaller.InstallResult> installResults = new ArrayList<>();
        List<SplitInstallError> installErrors = new ArrayList<>(0);
        boolean installCompleted = true;
        boolean isStartInstall = isStartInstallOperation();
        for (SplitInfo info : needUpdateSplits) {
            try {
                SplitInstaller.InstallResult installResult = installer.install(isStartInstall, info);
                installResults.add(installResult);
            } catch (SplitInstaller.InstallException error) {
                SplitLog.printErrStackTrace(TAG, error, "Failed to install split " + info.getSplitName());
                installCompleted = false;
                installErrors.add(new SplitInstallError(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn(), error.getErrorCode(), error.getCause()));
                if (isStartInstall) {
                    break;
                }
            }
        }
        if (installCompleted) {
            onInstallCompleted(installResults, System.currentTimeMillis() - currentTime);
        } else {
            onInstallFailed(installErrors, System.currentTimeMillis() - currentTime);
        }
    }

    protected void onPreInstall() {

    }

    abstract void onInstallCompleted(List<SplitInstaller.InstallResult> installResults, long cost);

    abstract void onInstallFailed(List<SplitInstallError> errors, long cost);

}
