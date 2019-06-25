package com.google.android.play.core.remote;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

final class ServiceConnectionImpl implements ServiceConnection {

    final RemoteManager mRemoteManager;

    ServiceConnectionImpl(RemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mRemoteManager.mPlayCore.info("ServiceConnectionImpl.onServiceConnected(%s)", name);
        mRemoteManager.post(new ServiceConnectedTask(this, service));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mRemoteManager.mPlayCore.info("ServiceConnectionImpl.onServiceDisconnected(%s)", name);
        mRemoteManager.post(new ServiceDisconnectedTask(this));
    }
}
