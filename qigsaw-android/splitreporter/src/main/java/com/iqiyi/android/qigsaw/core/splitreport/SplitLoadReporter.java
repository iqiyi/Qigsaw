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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.List;

/**
 * report load status of split APKs.
 */
public interface SplitLoadReporter {

    /**
     * When all modules are loaded successfully, this method would be invoked.
     *
     * @param processName  current process name.
     * @param loadedSplits splits which have been loaded successfully.
     * @param cost         time in ms.
     */
    @MainThread
    void onLoadOK(String processName, @NonNull List<SplitBriefInfo> loadedSplits, long cost);

    /**
     * When all modules are loaded completely, and at least one module failed, this method will be invoked.
     *
     * @param processName  current process name.
     * @param loadedSplits splits which have been loaded successfully, maybe empty.
     * @param errors       splits which have been loaded unsuccessfully.
     * @param cost         time in ms.
     */
    @MainThread
    void onLoadFailed(String processName, @NonNull List<SplitBriefInfo> loadedSplits, @NonNull List<SplitLoadError> errors, long cost);

}
