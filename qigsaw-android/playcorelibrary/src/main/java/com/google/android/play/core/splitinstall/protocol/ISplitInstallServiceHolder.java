package com.google.android.play.core.splitinstall.protocol;

import android.os.IBinder;
import android.os.IInterface;
import androidx.annotation.RestrictTo;

import com.google.android.binder.BinderWrapper;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class ISplitInstallServiceHolder extends BinderWrapper implements ISplitInstallServiceProxy {

    protected ISplitInstallServiceHolder(String descriptor) {
        super(descriptor);
    }

    public static ISplitInstallServiceProxy queryLocalInterface(IBinder remote) {
        if (remote == null) {
            return null;
        } else {
            IInterface iin;
            return ((iin = remote.queryLocalInterface("com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallService")) instanceof ISplitInstallServiceProxy ? (ISplitInstallServiceProxy) iin : new ISplitInstallServiceImpl(remote));
        }
    }
}
