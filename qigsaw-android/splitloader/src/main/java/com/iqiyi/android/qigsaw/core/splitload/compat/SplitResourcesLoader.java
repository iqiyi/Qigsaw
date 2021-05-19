package com.iqiyi.android.qigsaw.core.splitload.compat;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

/**
 * fetch with ServiceLoader
 */
public interface SplitResourcesLoader {
    void loadResources(@NonNull Context context, @NonNull Resources resources) throws Throwable;

    void loadResources(@NonNull Context context, @NonNull Resources preResources, @NonNull String splitApkPath) throws Throwable;
}
