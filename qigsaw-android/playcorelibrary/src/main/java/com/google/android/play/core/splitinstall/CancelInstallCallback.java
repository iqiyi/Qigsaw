package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.tasks.TaskWrapper;

final class CancelInstallCallback extends SplitInstallServiceCallbackImpl<Void> {

    CancelInstallCallback(SplitInstallService splitInstallService, TaskWrapper<Void> task) {
        super(splitInstallService, task);
    }

    @Override
    public void onCancelInstall(int sessionId, Bundle data) {
        super.onCancelInstall(sessionId, data);
        mTask.setResult(null);
    }
}
