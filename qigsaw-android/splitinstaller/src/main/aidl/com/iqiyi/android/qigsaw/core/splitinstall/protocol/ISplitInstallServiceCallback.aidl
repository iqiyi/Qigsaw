// ISplitInstallServiceCallback.aidl
package com.iqiyi.android.qigsaw.core.splitinstall.protocol;

// Declare any non-default types here with import statements

interface ISplitInstallServiceCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onStartInstall(int sessionId, in Bundle data);

    void onCompleteInstall(int sessionId);

    void onCancelInstall(int sessionId, in Bundle data);

    void onGetSession(int sessionId, in Bundle data);

    void onDeferredUninstall(in Bundle data);

    void onDeferredInstall(in Bundle data);

    void onGetSessionStates(in List<Bundle> data);

    void onError(in Bundle data);
}
