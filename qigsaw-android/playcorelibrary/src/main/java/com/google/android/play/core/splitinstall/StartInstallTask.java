package com.google.android.play.core.splitinstall;

import android.os.RemoteException;

import com.google.android.play.core.remote.RemoteTask;
import com.google.android.play.core.tasks.TaskWrapper;

import java.util.List;

final class StartInstallTask extends RemoteTask {

    private final SplitInstallService mSplitInstallService;

    private final TaskWrapper<Integer> mTask;

    private final List<String> moduleNames;

    StartInstallTask(SplitInstallService installService, TaskWrapper task, List<String> moduleNames, TaskWrapper<Integer> taskSame) {
        super(task);
        this.mSplitInstallService = installService;
        this.moduleNames = moduleNames;
        this.mTask = taskSame;
    }

    @Override
    protected void execute() {
        try {
            mSplitInstallService.mSplitRemoteManager.getIInterface().startInstall(
                    this.mSplitInstallService.mPackageName,
                    SplitInstallService.wrapModuleNames(moduleNames),
                    SplitInstallService.wrapVersionCode(),
                    new StartInstallCallback(mSplitInstallService, mTask)
            );
        } catch (RemoteException e) {
            SplitInstallService.playCore.error(e, "startInstall(%s)", this.moduleNames);
            this.mTask.setException(new RuntimeException(e));
        }
    }
}
