package com.google.android.play.core.remote;

final class BindServiceTask extends RemoteTask {

    private final RemoteManager remoteManager;

    private final RemoteTask task;

    BindServiceTask(RemoteManager remoteManager, RemoteTask task) {
        this.remoteManager = remoteManager;
        this.task = task;
    }

    @Override
    protected void execute() {
        remoteManager.bindServiceInternal(task);
    }
}
