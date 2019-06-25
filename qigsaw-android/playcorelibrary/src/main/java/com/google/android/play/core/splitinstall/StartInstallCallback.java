package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.tasks.TaskWrapper;

final class StartInstallCallback extends SplitInstallServiceCallbackImpl<Integer> {

    StartInstallCallback(SplitInstallService splitInstallService, TaskWrapper<Integer> task) {
        super(splitInstallService, task);
    }

    @Override
    public void onStartInstall(int sessionId, Bundle sessionStateBundle) {
        super.onStartInstall(sessionId, sessionStateBundle);
        mTask.setResult(sessionId);
    }
}
