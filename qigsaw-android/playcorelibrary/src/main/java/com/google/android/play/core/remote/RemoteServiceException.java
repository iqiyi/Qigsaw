package com.google.android.play.core.remote;

import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException() {
        super("Failed to bind to the service.");
    }

    public RemoteServiceException(String str) {
        super(str);
    }

    public RemoteServiceException(String str, Throwable th) {
        super(str, th);
    }
}
