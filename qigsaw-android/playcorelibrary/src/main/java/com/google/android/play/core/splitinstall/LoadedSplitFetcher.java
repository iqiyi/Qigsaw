package com.google.android.play.core.splitinstall;

import androidx.annotation.RestrictTo;

import java.util.Set;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface LoadedSplitFetcher {

    Set<String> loadedSplits();

}
