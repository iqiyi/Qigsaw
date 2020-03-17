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

package com.iqiyi.android.qigsaw.core.splitreport;

import android.content.Context;
import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.util.List;

public class DefaultSplitInstallReporter implements SplitInstallReporter {

    private static final String TAG = "SplitInstallReporter";

    protected final Context context;

    public DefaultSplitInstallReporter(Context context) {
        this.context = context;
    }

    @Override
    public void onStartInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost) {
        SplitLog.i(TAG, "Start install %s OK, cost time %d ms.", installedSplits.toString(), cost);
    }

    @Override
    public void onStartInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull SplitInstallError error, long cost) {
        SplitLog.printErrStackTrace(TAG, error.cause, "Start to install split %s failed, cost time %d ms.", error.splitName, cost);
    }

    @Override
    public void onDeferredInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost) {
        SplitLog.i(TAG, "Deferred install %s OK, cost time %d ms.", installedSplits.toString(), cost);
    }

    @Override
    public void onDeferredInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull List<SplitInstallError> errors, long cost) {
        for (SplitInstallError installError : errors) {
            SplitLog.printErrStackTrace(TAG, installError.cause,
                    "Defer to install split %s failed with error code %d, cost time %d ms.",
                    installError.splitName, installError.errorCode, cost);
        }
    }
}
