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

import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitApkInstaller;
import com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallServiceCallback;

import java.util.List;

abstract class DefaultTask implements Runnable, SplitInstallSupervisor.Callback {

    private static final String TAG = "Split:DefaultTask";

    final ISplitInstallServiceCallback mCallback;

    private final SplitInstallSupervisor installSupervisor;

    DefaultTask(ISplitInstallServiceCallback callback) {
        this.mCallback = callback;
        installSupervisor = SplitApkInstaller.getSplitInstallSupervisor();
    }

    @Override
    public void run() {
        if (installSupervisor != null) {
            try {
                execute(installSupervisor);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mCallback.onError(SplitInstallSupervisor.bundleErrorCode(-101));
                SplitLog.w(TAG, "Have you call Qigsaw#onApplicationCreated method?");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    abstract void execute(@NonNull SplitInstallSupervisor supervisor) throws RemoteException;

    @Override
    public void onStartInstall(int sessionId, Bundle data) {

    }

    @Override
    public void onDeferredInstall(Bundle data) {

    }

    @Override
    public void onCancelInstall(int sessionId, Bundle data) {

    }

    @Override
    public void onDeferredUninstall(Bundle data) {

    }

    @Override
    public void onError(Bundle data) {
        try {
            mCallback.onError(data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGetSession(int sessionId, Bundle data) {

    }

    @Override
    public void onGetSessionStates(List<Bundle> data) {

    }
}

