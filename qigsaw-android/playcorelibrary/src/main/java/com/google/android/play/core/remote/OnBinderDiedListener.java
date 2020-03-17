package com.google.android.play.core.remote;

import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface OnBinderDiedListener {

    void onBinderDied();

}
