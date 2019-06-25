// ISplitInstallService.aidl
package com.iqiyi.android.qigsaw.core.splitinstall.protocol;

import com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallServiceCallback;
// Declare any non-default types here with import statements

interface ISplitInstallService {

    void startInstall(String packageName, in List<Bundle> moduleNames, in Bundle versionCode,  ISplitInstallServiceCallback callback);

    void cancelInstall(String packageName, int sessionId, in Bundle versionCode, ISplitInstallServiceCallback callback);

    void getSessionState(String packageName, int sessionId, ISplitInstallServiceCallback callback);

    void getSessionStates(String packageName, ISplitInstallServiceCallback callback);

    void deferredInstall(String packageName, in List<Bundle> moduleNames, in Bundle versionCode, ISplitInstallServiceCallback callback);

    void deferredUninstall(String packageName, in List<Bundle> moduleNames, in Bundle versionCode, ISplitInstallServiceCallback callback);
}
