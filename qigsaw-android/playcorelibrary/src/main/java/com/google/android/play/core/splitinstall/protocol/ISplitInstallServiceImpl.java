package com.google.android.play.core.splitinstall.protocol;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.RestrictTo;

import com.google.android.binder.IInterfaceProxy;
import com.google.android.binder.ParcelHelper;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class ISplitInstallServiceImpl extends IInterfaceProxy implements ISplitInstallServiceProxy {

    ISplitInstallServiceImpl(IBinder remote) {
        super(remote, "com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallService");
    }

    @Override
    public final void startInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        data.writeTypedList(moduleNames);
        ParcelHelper.writeToParcel(data, versionCode);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(1, data);
    }

    @Override
    public void cancelInstall(String packageName, int sessionId, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        data.writeInt(sessionId);
        ParcelHelper.writeToParcel(data, versionCode);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(2, data);
    }

    @Override
    public void getSessionState(String packageName, int sessionId, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        data.writeInt(sessionId);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(3, data);
    }

    @Override
    public void getSessionStates(String packageName, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(4, data);
    }

    @Override
    public void deferredInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        data.writeTypedList(moduleNames);
        ParcelHelper.writeToParcel(data, versionCode);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(5, data);
    }

    @Override
    public void deferredUninstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException {
        Parcel data;
        (data = this.obtainData()).writeString(packageName);
        data.writeTypedList(moduleNames);
        ParcelHelper.writeToParcel(data, versionCode);
        ParcelHelper.writeStrongBinder(data, callback);
        this.transact(6, data);
    }
}
