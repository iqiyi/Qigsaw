package com.iqiyi.android.qigsaw.core.common;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.InputStream;

@Keep
public interface ICompatBundle {
    /**
     * for 'parseSplitContentsForDefaultVersion'
     */
    @Nullable
    String readDefaultSplitVersionContent(@NonNull Context context, @NonNull String fileName);

    @NonNull
    String getMD5(@NonNull File file);

    @NonNull
    String getMD5(@NonNull InputStream inputStream);

    boolean injectActivityResource();

    boolean disableComponentInfoManager();

    Class<?> qigsawConfigClass();
}
