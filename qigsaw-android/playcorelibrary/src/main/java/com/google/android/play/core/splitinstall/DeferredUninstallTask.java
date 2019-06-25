package com.google.android.play.core.splitinstall;

import android.os.RemoteException;

import com.google.android.play.core.remote.RemoteTask;
import com.google.android.play.core.tasks.TaskWrapper;

import java.util.List;

final class DeferredUninstallTask extends RemoteTask {

    private final SplitInstallService mSplitInstallService;

    private final TaskWrapper<Void> mTask;

    private final List<String> moduleNames;

    DeferredUninstallTask(SplitInstallService installService, TaskWrapper task, List<String> moduleNames, TaskWrapper<Void> taskSame) {
        super(task);
        this.mSplitInstallService = installService;
        this.moduleNames = moduleNames;
        this.mTask = taskSame;
    }

    @Override
    protected void execute() {
        try {
            mSplitInstallService.mSplitRemoteManager.getIInterface().deferredUninstall(
                    this.mSplitInstallService.mPackageName,
                    SplitInstallService.wrapModuleNames(moduleNames),
                    SplitInstallService.wrapVersionCode(),
                    new DeferredUninstallCallback(mSplitInstallService, mTask)
            );
        } catch (RemoteException e) {
            SplitInstallService.playCore.error(e, "deferredUninstall(%s)", this.moduleNames);
            this.mTask.setException(new RuntimeException(e));
        }
    }
}
