package com.google.android.play.core.remote;

import android.os.IBinder;
import android.os.IInterface;

import java.util.List;

final class ServiceConnectedTask extends RemoteTask {

    private final ServiceConnectionImpl mServiceConnection;

    private final IBinder mService;

    ServiceConnectedTask(ServiceConnectionImpl serviceConnection, IBinder service) {
        this.mServiceConnection = serviceConnection;
        this.mService = service;
    }

    @Override
    protected void execute() {
        this.mServiceConnection.mRemoteManager.mIInterface = (IInterface) mServiceConnection.mRemoteManager.mRemote.asInterface(mService);
        this.mServiceConnection.mRemoteManager.linkToDeath();
        this.mServiceConnection.mRemoteManager.mBindingService = false;
        List<Runnable> remoteTasks = mServiceConnection.mRemoteManager.mPendingTasks;
        for (Runnable run : remoteTasks) {
            run.run();
        }
        mServiceConnection.mRemoteManager.mPendingTasks.clear();
    }
}
