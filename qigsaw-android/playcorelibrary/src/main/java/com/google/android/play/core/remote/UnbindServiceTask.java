package com.google.android.play.core.remote;

final class UnbindServiceTask extends RemoteTask {

    private final RemoteManager mRemoteManager;

    UnbindServiceTask(RemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    @Override
    protected void execute() {
        if (this.mRemoteManager.mIInterface != null) {
            this.mRemoteManager.mContext.unbindService(this.mRemoteManager.mServiceConnection);
            this.mRemoteManager.mBindingService = false;
            this.mRemoteManager.mIInterface = null;
            this.mRemoteManager.mServiceConnection = null;
        }
    }
}
