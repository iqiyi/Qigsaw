package com.google.android.play.core.splitinstall.protocol;

import android.os.Bundle;
import android.os.IInterface;
import androidx.annotation.RestrictTo;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface ISplitInstallServiceCallbackProxy extends IInterface {

    void onStartInstall(int sessionId, Bundle data);

    void onCompleteInstall(int sessionId);

    void onCancelInstall(int sessionId, Bundle data);

    void onGetSession(int sessionId, Bundle data);

    void onDeferredUninstall(Bundle data);

    void onDeferredInstall(Bundle data);

    void onGetSessionStates(List<Bundle> data);

    void onError(Bundle data);

}
