package com.iqiyi.qigsaw.sample.downloader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;


public interface GroupDownloadTaskQueueListener {

    void taskEnd(@NonNull GroupDownloadContext context, @NonNull DownloadTask task,
                 @NonNull EndCause cause, @Nullable Exception realCause, int remainCount);

    void queueEnd(@NonNull GroupDownloadContext context);
}
