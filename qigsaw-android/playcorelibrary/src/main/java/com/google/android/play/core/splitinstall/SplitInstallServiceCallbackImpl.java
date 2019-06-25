package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.splitinstall.protocol.SplitInstallServiceCallback;
import com.google.android.play.core.tasks.TaskWrapper;

import java.util.List;

class SplitInstallServiceCallbackImpl<T> extends SplitInstallServiceCallback {

    private final SplitInstallService mSplitInstallService;

    final TaskWrapper<T> mTask;

    SplitInstallServiceCallbackImpl(SplitInstallService splitInstallService, TaskWrapper<T> task) {
        this.mSplitInstallService = splitInstallService;
        this.mTask = task;
    }

    @Override
    public void onStartInstall(int sessionId, Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onStartInstall(%d)", sessionId);
    }

    @Override
    public void onCompleteInstall(int sessionId) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onCompleteInstall(%d)", sessionId);
    }

    @Override
    public void onCancelInstall(int sessionId, Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onCancelInstall(%d)", sessionId);
    }

    @Override
    public void onGetSession(int sessionId, Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onGetSession(%d)", sessionId);
    }

    @Override
    public void onDeferredUninstall(Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onDeferredUninstall");
    }

    @Override
    public void onDeferredInstall(Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onDeferredInstall");
    }

    @Override
    public void onGetSessionStates(List<Bundle> data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        SplitInstallService.playCore.info("onGetSessionStates");
    }

    @Override
    public final void onError(Bundle data) {
        mSplitInstallService.mSplitRemoteManager.unbindService();
        int errorCode = data.getInt("error_code");
        SplitInstallService.playCore.info("onError(%d)", errorCode);
        mTask.setException(new SplitInstallException(errorCode));
    }
}
