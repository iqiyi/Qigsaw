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

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallService;
import com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallServiceCallback;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitInstallService extends Service {

    private static final Map<String, Handler> sHandlerMap = Collections.synchronizedMap(new HashMap<String, Handler>());

    ISplitInstallService.Stub mBinder = new ISplitInstallService.Stub() {

        @Override
        public void startInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnStartInstallTask(callback, moduleNames));
        }

        @Override
        public void cancelInstall(String packageName, int sessionId, Bundle versionCode, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnCancelInstallTask(callback, sessionId));
        }

        @Override
        public void getSessionState(String packageName, int sessionId, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnGetSessionStateTask(callback, sessionId));
        }

        @Override
        public void getSessionStates(String packageName, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnGetSessionStatesTask(callback));
        }

        @Override
        public void deferredInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnDeferredInstallTask(callback, moduleNames));
        }

        @Override
        public void deferredUninstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallback callback) {
            getHandler(packageName).post(new OnDeferredUninstallTask(callback, moduleNames));
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private Handler getHandler(String packageName) {
        Handler handler;
        synchronized (sHandlerMap) {
            if (!sHandlerMap.containsKey(packageName)) {
                HandlerThread handlerThread = new HandlerThread("split_remote_" + packageName, THREAD_PRIORITY_BACKGROUND);
                handlerThread.start();
                sHandlerMap.put(packageName, new Handler(handlerThread.getLooper()));
            }
            handler = sHandlerMap.get(packageName);
        }
        return handler;
    }

}
