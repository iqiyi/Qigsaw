package com.google.android.play.core.splitinstall;

import android.support.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicReference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitSessionLoaderSingleton {

    private static final AtomicReference<SplitSessionLoader> sSplitLoaderHolder = new AtomicReference<>();

    static SplitSessionLoader get() {
        return sSplitLoaderHolder.get();
    }

    public static void set(SplitSessionLoader loader) {
        sSplitLoaderHolder.compareAndSet(null, loader);
    }
}
