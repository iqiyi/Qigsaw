package com.google.android.play.core.splitinstall;

import android.os.RemoteException;

import com.google.android.play.core.remote.RemoteTask;
import com.google.android.play.core.tasks.TaskWrapper;

import java.util.List;

final class GetSessionStatesTask extends RemoteTask {

    private final SplitInstallService mSplitInstallService;

    private final TaskWrapper<List<SplitInstallSessionState>> mTask;

    GetSessionStatesTask(SplitInstallService installService, TaskWrapper task, TaskWrapper<List<SplitInstallSessionState>> taskSame) {
        super(task);
        this.mSplitInstallService = installService;
        this.mTask = taskSame;
    }

    @Override
    protected void execute() {
        try {
            mSplitInstallService.mSplitRemoteManager.getIInterface().getSessionStates(
                    this.mSplitInstallService.mPackageName,
                    new GetSessionStatesCallback(mSplitInstallService, mTask)
            );
        } catch (RemoteException e) {
            SplitInstallService.playCore.error(e, "getSessionStates");
            this.mTask.setException(new RuntimeException(e));
        }
    }
}
