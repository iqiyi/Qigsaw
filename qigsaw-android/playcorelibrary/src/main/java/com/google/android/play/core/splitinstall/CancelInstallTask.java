package com.google.android.play.core.splitinstall;

import android.os.RemoteException;

import com.google.android.play.core.remote.RemoteTask;
import com.google.android.play.core.tasks.TaskWrapper;

final class CancelInstallTask extends RemoteTask {

    private final SplitInstallService mSplitInstallService;

    private final TaskWrapper<Void> mTask;

    private final int sessionId;

    CancelInstallTask(SplitInstallService installService, TaskWrapper<Void> task, int sessionId, TaskWrapper<Void> taskSame) {
        super(taskSame);
        this.mSplitInstallService = installService;
        this.mTask = task;
        this.sessionId = sessionId;
    }

    @Override
    protected void execute() {
        try {
            mSplitInstallService.mSplitRemoteManager.getIInterface().cancelInstall(
                    this.mSplitInstallService.mPackageName,
                    sessionId,
                    SplitInstallService.wrapVersionCode(),
                    new CancelInstallCallback(mSplitInstallService, mTask)
            );
        } catch (RemoteException e) {
            SplitInstallService.playCore.error(e, "cancelInstall(%d)", this.sessionId);
            this.mTask.setException(new RuntimeException(e));
        }
    }
}
