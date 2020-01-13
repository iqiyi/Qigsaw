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

package com.iqiyi.android.qigsaw.core.splitinstall.remote;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitPendingUninstallManager;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitUninstallReporterManager;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUninstallReporter;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class SplitStartUninstallTask implements Runnable {

    private static final String TAG = "SplitStartUninstallTask";

    private final List<SplitInfo> uninstallSplits;

    SplitStartUninstallTask(List<SplitInfo> uninstallSplits) {
        this.uninstallSplits = uninstallSplits;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        List<String> realUninstallSplits = new ArrayList<>(uninstallSplits.size());
        for (SplitInfo info : uninstallSplits) {
            SplitLog.d(TAG, "split %s need to be uninstalled, try to delete its files", info.getSplitName());
            File splitRootDir = SplitPathManager.require().getSplitRootDir(info);
            FileUtil.deleteDir(splitRootDir);
            realUninstallSplits.add(info.getSplitName());
        }
        SplitUninstallReporter uninstallReporter = SplitUninstallReporterManager.getUninstallReporter();
        if (uninstallReporter != null) {
            uninstallReporter.onSplitUninstallOK(realUninstallSplits, System.currentTimeMillis() - time);
        }
        boolean result = new SplitPendingUninstallManager().deletePendingUninstallSplitsRecord();
        SplitLog.d(TAG, "%s to delete record file of pending uninstall splits!", result ? "Succeed" : "Failed");
    }
}
