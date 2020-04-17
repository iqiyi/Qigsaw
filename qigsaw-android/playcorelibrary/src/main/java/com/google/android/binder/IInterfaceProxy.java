package com.google.android.binder;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.RestrictTo;

import static android.os.IBinder.FLAG_ONEWAY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class IInterfaceProxy implements IInterface {

    private final IBinder mRemote;

    private final String mDescriptor;

    protected IInterfaceProxy(IBinder remote, String descriptor) {
        this.mRemote = remote;
        this.mDescriptor = descriptor;
    }

    @Override
    public IBinder asBinder() {
        return mRemote;
    }

    protected final Parcel obtainData() {
        Parcel data;
        (data = Parcel.obtain()).writeInterfaceToken(this.mDescriptor);
        return data;
    }

    protected final void transact(int code, Parcel data) throws RemoteException {
        Parcel reply = Parcel.obtain();
        try {
            this.mRemote.transact(code, data, reply, FLAG_ONEWAY);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }
}
