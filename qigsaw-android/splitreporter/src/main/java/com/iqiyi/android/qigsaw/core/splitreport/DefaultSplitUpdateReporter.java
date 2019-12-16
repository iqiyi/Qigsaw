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

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.util.List;

public class DefaultSplitUpdateReporter implements SplitUpdateReporter {

    private static final String TAG = "SplitUpdateReporter";

    protected final Context context;

    public DefaultSplitUpdateReporter(Context context) {
        this.context = context;
    }

    @Override
    public void onUpdateOK(String oldSplitInfoVersion, String newSplitInfoVersion, List<String> updateSplits) {
        SplitLog.i(TAG, "Success to update version from %s to %s, update splits: %s.", oldSplitInfoVersion, newSplitInfoVersion, updateSplits.toString());
    }

    @Override
    public void onUpdateFailed(String oldSplitInfoVersion, String newSplitInfoVersion, int errorCode) {
        SplitLog.i(TAG, "Failed to update version from %s to %s, errorCode %d.", oldSplitInfoVersion, newSplitInfoVersion, errorCode);
    }

    @Override
    public void onNewSplitInfoVersionLoaded(String newSplitInfoVersion) {
        SplitLog.i(TAG, "Success to load new split info version ", newSplitInfoVersion);
    }
}
