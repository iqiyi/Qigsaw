package com.google.android.play.core.splitinstall;

import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitSessionStatusChanger {

    final SplitInstallListenerRegistry mRegistry;

    final SplitInstallSessionState sessionState;

    SplitSessionStatusChanger(SplitInstallListenerRegistry registry, SplitInstallSessionState sessionState) {
        this.mRegistry = registry;
        this.sessionState = sessionState;
    }

    public void changeStatus(int status) {
        mRegistry.mMainHandler.post(new ChangeSessionStatusWorker(this, status));
    }

    public void changeStatus(int status, int errorCode) {
        mRegistry.mMainHandler.post(new ChangeSessionStatusWorker(this, status, errorCode));
    }
}
