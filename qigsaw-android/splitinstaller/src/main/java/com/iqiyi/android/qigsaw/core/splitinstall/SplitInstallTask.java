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

import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class SplitInstallTask implements Runnable {

    private final SplitInstaller installer;

    private final Collection<SplitInfo> needUpdateSplits;

    SplitInstallTask(SplitInstaller installer,
                     Collection<SplitInfo> needUpdateSplits) {
        this.installer = installer;
        this.needUpdateSplits = needUpdateSplits;
    }

    abstract boolean isStartInstallOperation();

    @Override
    public final void run() {
        onPreInstall();
        long currentTime = System.currentTimeMillis();
        boolean installCompleted = true;
        boolean isStartInstall = isStartInstallOperation();
        List<SplitInstaller.InstallResult> installResults = new ArrayList<>();
        List<SplitBriefInfo> installedSplits = new ArrayList<>(needUpdateSplits.size());
        List<SplitInstallError> installErrors = new ArrayList<>();
        for (SplitInfo info : needUpdateSplits) {
            SplitBriefInfo briefInfo = new SplitBriefInfo(info.getSplitName(), info.getSplitVersion(), info.isBuiltIn());
            try {
                SplitInstaller.InstallResult installResult = installer.install(isStartInstall, info);
                briefInfo.setInstallFlag(installResult.firstInstalled ? SplitBriefInfo.FIRST_INSTALLED : SplitBriefInfo.ALREADY_INSTALLED);
                installedSplits.add(briefInfo);
                installResults.add(installResult);
            } catch (SplitInstaller.InstallException error) {
                installCompleted = false;
                installErrors.add(new SplitInstallError(briefInfo, error.getErrorCode(), error.getCause()));
                if (isStartInstall) {
                    break;
                }
            }
        }
        SplitInstallReporter installReporter = SplitInstallReporterManager.getInstallReporter();
        if (installCompleted) {
            onInstallCompleted(installResults);
            if (installReporter != null) {
                if (isStartInstall) {
                    installReporter.onStartInstallOK(installedSplits, System.currentTimeMillis() - currentTime);
                } else {
                    installReporter.onDeferredInstallOK(installedSplits, System.currentTimeMillis() - currentTime);
                }
            }
        } else {
            onInstallFailed(installErrors);
            if (installReporter != null) {
                if (isStartInstall) {
                    installReporter.onStartInstallFailed(installedSplits, installErrors.get(0), System.currentTimeMillis() - currentTime);
                } else {
                    installReporter.onDeferredInstallFailed(installedSplits, installErrors, System.currentTimeMillis() - currentTime);
                }
            }
        }
    }

    protected void onPreInstall() {

    }

    void onInstallCompleted(List<SplitInstaller.InstallResult> installResults) {

    }

    void onInstallFailed(List<SplitInstallError> errors) {

    }

}
