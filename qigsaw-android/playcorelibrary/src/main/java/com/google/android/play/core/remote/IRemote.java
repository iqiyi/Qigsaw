package com.google.android.play.core.remote;

import android.os.IBinder;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface IRemote<T> {

    T asInterface(IBinder remote);

}
