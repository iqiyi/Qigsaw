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

import com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallServiceCallback;

class OnGetSessionStateTask extends DefaultTask {

    private final int mSessionId;

    OnGetSessionStateTask(ISplitInstallServiceCallback callback, int sessionId) {
        super(callback);
        this.mSessionId = sessionId;
    }

    @Override
    void execute(@NonNull SplitInstallSupervisor supervisor) throws RemoteException {
        supervisor.getSessionState(mSessionId, this);
    }

    @Override
    public void onGetSession(int sessionId, Bundle data) {
        super.onGetSession(sessionId, data);
        try {
            mCallback.onGetSession(sessionId, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
