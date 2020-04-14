package com.google.android.play.core.splitinstall.protocol;


import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;
import androidx.annotation.RestrictTo;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface ISplitInstallServiceProxy extends IInterface {

    void startInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

    void cancelInstall(String packageName, int sessionId, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

    void getSessionState(String packageName, int sessionId, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

    void getSessionStates(String packageName, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

    void deferredInstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

    void deferredUninstall(String packageName, List<Bundle> moduleNames, Bundle versionCode, ISplitInstallServiceCallbackProxy callback) throws RemoteException;

}
