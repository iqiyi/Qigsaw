package com.google.android.play.core.splitinstall;

import android.os.IBinder;
import android.support.annotation.RestrictTo;

import com.google.android.play.core.remote.IRemote;
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceHolder;
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceProxy;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitRemoteImpl implements IRemote<ISplitInstallServiceProxy> {

    static final IRemote sInstance = new SplitRemoteImpl();

    @Override
    public ISplitInstallServiceProxy asInterface(IBinder remote) {
        return ISplitInstallServiceHolder.queryLocalInterface(remote);
    }
}
