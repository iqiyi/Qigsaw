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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

final class SplitInstallSessionManagerImpl implements SplitInstallSessionManager {

    private final SparseArray<SplitInstallInternalSessionState> mActiveSessionStates = new SparseArray<>();

    private final Context mContext;

    private final String mPackageName;

    private final Object mLock = new Object();

    SplitInstallSessionManagerImpl(Context context) {
        this.mContext = context;
        this.mPackageName = context.getPackageName();
    }

    @Override
    public void setSessionState(int sessionId, SplitInstallInternalSessionState sessionState) {
        synchronized (mLock) {
            if (sessionId != 0) {
                if (mActiveSessionStates.get(sessionId) == null) {
                    mActiveSessionStates.put(sessionId, sessionState);
                }
            }
        }
    }

    @Override
    public void changeSessionState(int sessionId, int status) {
        synchronized (mLock) {
            SplitInstallInternalSessionState sessionState = mActiveSessionStates.get(sessionId);
            if (sessionState != null) {
                sessionState.setStatus(status);
                if (status == SplitInstallInternalSessionStatus.CANCELED
                        || status == SplitInstallInternalSessionStatus.FAILED
                        || status == SplitInstallInternalSessionStatus.POST_INSTALLED) {
                    removeSessionState(sessionId);
                }
            }
        }
    }

    @Override
    public void removeSessionState(int sessionId) {
        synchronized (mLock) {
            if (sessionId != 0) {
                mActiveSessionStates.remove(sessionId);
            }
        }
    }

    @Override
    public boolean isActiveSessionsLimitExceeded() {
        synchronized (mLock) {
            for (int i = 0; i < mActiveSessionStates.size(); i++) {
                SplitInstallInternalSessionState state = mActiveSessionStates.valueAt(i);
                if (state.status() == SplitInstallInternalSessionStatus.DOWNLOADING) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public SplitInstallInternalSessionState getSessionState(int sessionId) {
        synchronized (mLock) {
            return mActiveSessionStates.get(sessionId);
        }
    }

    @Override
    public List<SplitInstallInternalSessionState> getSessionStates() {
        synchronized (mLock) {
            return asList(mActiveSessionStates);
        }
    }

    @Override
    public boolean isIncompatibleWithExistingSession(List<String> moduleNames) {
        synchronized (mLock) {
            boolean incompatibleWithExistingSession = false;
            List<SplitInstallInternalSessionState> sessionStates = getSessionStates();
            for (int i = 0; i < sessionStates.size(); i++) {
                SplitInstallInternalSessionState sessionState = sessionStates.get(i);
                for (String moduleName : moduleNames) {
                    if (sessionState.moduleNames().contains(moduleName)) {
                        incompatibleWithExistingSession = true;
                        break;
                    }
                    if (incompatibleWithExistingSession) {
                        break;
                    }
                }
            }
            return incompatibleWithExistingSession;
        }
    }

    @Override
    public void emitSessionState(SplitInstallInternalSessionState sessionState) {
        Bundle bundle = SplitInstallInternalSessionState.transform2Bundle(sessionState);
        Intent intent = new Intent();
        intent.putExtra("session_state", bundle);
        intent.setPackage(mPackageName);
        intent.setAction("com.iqiyi.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService");
        mContext.sendBroadcast(intent);
    }

    private static <C> List<C> asList(SparseArray<C> sparseArray) {
        List<C> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++) {
            arrayList.add(sparseArray.valueAt(i));
        }
        return arrayList;
    }
}
