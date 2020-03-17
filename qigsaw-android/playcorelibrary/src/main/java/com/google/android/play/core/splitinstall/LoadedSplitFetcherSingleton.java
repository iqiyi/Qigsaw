package com.google.android.play.core.splitinstall;

import androidx.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicReference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class LoadedSplitFetcherSingleton {

    private static final AtomicReference<LoadedSplitFetcher> sInstalledSplitsFetcherRef = new AtomicReference<>(null);

    static LoadedSplitFetcher get() {
        return sInstalledSplitsFetcherRef.get();
    }

    public static void set(LoadedSplitFetcher fetcher) {
        sInstalledSplitsFetcherRef.compareAndSet(null, fetcher);
    }
}
