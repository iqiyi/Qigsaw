package com.google.android.play.core.splitinstall.protocol;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.RestrictTo;

import com.google.android.binder.BinderWrapper;
import com.google.android.binder.ParcelHelper;

import java.util.ArrayList;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class SplitInstallServiceCallback extends BinderWrapper implements ISplitInstallServiceCallbackProxy {

    protected SplitInstallServiceCallback() {
        super("com.iqiyi.android.qigsaw.core.splitinstall.protocol.ISplitInstallServiceCallback");
    }

    @Override
    protected final boolean dispatchTransact(int code, Parcel data) {
        Bundle var3;
        Bundle var4;
        int sessionId;
        switch (code) {
            //onStartInstall
            case 1:
                sessionId = data.readInt();
                var4 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onStartInstall(sessionId, var4);
                break;
            //onCompleteInstall
            case 2:
                sessionId = data.readInt();
                ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onCompleteInstall(sessionId);
                break;
            //onCancelInstall
            case 3:
                sessionId = data.readInt();
                var4 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onCancelInstall(sessionId, var4);
                break;
            //onGetSession
            case 4:
                sessionId = data.readInt();
                var4 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onGetSession(sessionId, var4);
                break;
            //onDeferredUninstall
            case 5:
                var3 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onDeferredUninstall(var3);
                break;
            //onDeferredInstall
            case 6:
                var3 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onDeferredInstall(var3);
                break;
            //onGetSessionStates
            case 7:
                ArrayList<Bundle> sessionStates = data.createTypedArrayList(Bundle.CREATOR);
                this.onGetSessionStates(sessionStates);
                break;
            //onError
            case 8:
                var3 = ParcelHelper.createFromParcel(data, Bundle.CREATOR);
                this.onError(var3);
                break;
            default:
                return false;
        }
        return true;
    }
}
