package com.google.android.play.core.splitinstall;

import android.os.RemoteException;

import com.google.android.play.core.remote.RemoteTask;
import com.google.android.play.core.tasks.TaskWrapper;

final class GetSessionStateTask extends RemoteTask {

    private final SplitInstallService mSplitInstallService;

    private final TaskWrapper<SplitInstallSessionState> mTask;

    private final int sessionId;

    GetSessionStateTask(SplitInstallService installService, TaskWrapper task, int sessionId, TaskWrapper<SplitInstallSessionState> taskSame) {
        super(task);
        this.mSplitInstallService = installService;
        this.sessionId = sessionId;
        this.mTask = taskSame;
    }

    @Override
    protected void execute() {
        try {
            mSplitInstallService.mSplitRemoteManager.getIInterface().getSessionState(
                    this.mSplitInstallService.mPackageName,
                    sessionId,
                    new GetSessionStateCallback(mSplitInstallService, mTask)
            );
        } catch (RemoteException e) {
            SplitInstallService.playCore.error(e, "getSessionState(%d)", this.sessionId);
            this.mTask.setException(new RuntimeException(e));
        }
    }
}
