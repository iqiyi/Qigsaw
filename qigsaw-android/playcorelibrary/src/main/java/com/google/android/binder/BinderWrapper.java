package com.google.android.binder;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class BinderWrapper extends Binder implements IInterface {

    private static Empty empty = null;

    protected BinderWrapper(String descriptor) {
        this.attachInterface(this, descriptor);
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        boolean ret;
        if (code > LAST_CALL_TRANSACTION) {
            ret = super.onTransact(code, data, reply, flags);
        } else {
            data.enforceInterface(this.getInterfaceDescriptor());
            ret = false;
        }
        return ret || this.dispatchTransact(code, data);
    }

    protected boolean dispatchTransact(int code, Parcel data) {
        return false;
    }
}
