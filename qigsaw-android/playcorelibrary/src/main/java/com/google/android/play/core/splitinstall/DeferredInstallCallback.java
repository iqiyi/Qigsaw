package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.tasks.TaskWrapper;

final class DeferredInstallCallback extends SplitInstallServiceCallbackImpl<Void> {

    DeferredInstallCallback(SplitInstallService splitInstallService, TaskWrapper<Void> task) {
        super(splitInstallService, task);
    }

    @Override
    public void onDeferredInstall(Bundle data) {
        super.onDeferredInstall(data);
        mTask.setResult(null);
    }
}
