package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.tasks.TaskWrapper;

final class GetSessionStateCallback extends SplitInstallServiceCallbackImpl<SplitInstallSessionState> {

    GetSessionStateCallback(SplitInstallService splitInstallService, TaskWrapper<SplitInstallSessionState> task) {
        super(splitInstallService, task);
    }

    @Override
    public void onGetSession(int sessionId, Bundle data) {
        super.onGetSession(sessionId, data);
        mTask.setResult(SplitInstallSessionState.createFrom(data));
    }
}
