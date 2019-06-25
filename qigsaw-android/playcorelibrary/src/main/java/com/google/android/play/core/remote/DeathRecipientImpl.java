package com.google.android.play.core.remote;

import android.os.IBinder;

final class DeathRecipientImpl implements IBinder.DeathRecipient {

    private final RemoteManager mRemoteManager;

    DeathRecipientImpl(RemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    public void binderDied() {
        this.mRemoteManager.reportBinderDeath();
    }
}
