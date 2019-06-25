package com.google.android.play.core.splitinstall;

import android.support.annotation.RestrictTo;

import java.util.Set;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface LoadedSplitFetcher {

    Set<String> loadedSplits();

}
