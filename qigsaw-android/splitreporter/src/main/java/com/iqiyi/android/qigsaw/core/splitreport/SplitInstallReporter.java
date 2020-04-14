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

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

/**
 * report install status of split APKs, called in main process.
 */
public interface SplitInstallReporter {

    /**
     * Using {@code startInstall} to install modules.
     * When all modules are installed successfully,
     * this method will be invoked.
     *
     * @param installedSplits splits which have been installed successfully.
     * @param cost            time in ms.
     */
    @WorkerThread
    void onStartInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost);

    /**
     * Using {@code startInstall} to install modules,
     * When one module is installed failed,
     * installation would be cancelled and this method will be invoked.
     *
     * @param installedSplits splits which have been installed successfully, maybe empty.
     * @param error           split which has been installed unsuccessfully.
     * @param cost            time in ms.
     */
    @WorkerThread
    void onStartInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull SplitInstallError error, long cost);

    /**
     * Using {@code deferredInstall} to install modules.
     * When all modules are installed successfully,
     * this method will be invoked.
     *
     * @param installedSplits splits which have been installed successfully.
     * @param cost            time in ms.
     */
    @WorkerThread
    void onDeferredInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost);

    /**
     * Using {@code deferredInstall} to install modules.
     * When installations of all modules are done,
     * and at least one module failed,
     * this method will be invoked.
     *
     * @param installedSplits splits which have been installed successfully, maybe empty.
     * @param errors          splits which have been installed unsuccessfully.
     * @param cost            time in ms.
     */
    @WorkerThread
    void onDeferredInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull List<SplitInstallError> errors, long cost);

}
