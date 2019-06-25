package com.iqiyi.qigsaw.sample.downloader;

public interface GroupTaskDownloadCallBack {

    void onProgress(long currentOffset);

    void onStarted();

    void onCompleted();

    void onCanceled();

    void onError(int errorCode);

}
