package com.google.android.play.core.remote;

final class ServiceDisconnectedTask extends RemoteTask {

    private final ServiceConnectionImpl mServiceConnection;

    ServiceDisconnectedTask(ServiceConnectionImpl serviceConnection) {
        this.mServiceConnection = serviceConnection;
    }

    @Override
    protected void execute() {
        mServiceConnection.mRemoteManager.unlinkToDeath();
        mServiceConnection.mRemoteManager.mIInterface = null;
        mServiceConnection.mRemoteManager.mBindingService = false;
    }
}
