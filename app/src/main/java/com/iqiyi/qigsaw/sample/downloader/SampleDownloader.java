package com.iqiyi.qigsaw.sample.downloader;

import android.util.Log;

import com.iqiyi.android.qigsaw.core.splitdownload.DownloadCallback;
import com.iqiyi.android.qigsaw.core.splitdownload.DownloadRequest;
import com.iqiyi.android.qigsaw.core.splitdownload.Downloader;

import java.util.List;

public class SampleDownloader implements Downloader {

    private static final String TAG = "Split:SampleDownloader";

    private static final int HIGH_PRIORITY = 10;

    private static final int LOW_PRIORITY = 0;

    private GroupTaskDownloader groupTaskDownloader = new GroupTaskDownloader();

    @Override
    public void startDownload(int sessionId, List<DownloadRequest> requests, final DownloadCallback callback) {
        String[] urls = new String[requests.size()];
        String[] parentPath = new String[requests.size()];
        String[] fileName = new String[requests.size()];
        int i = 0;
        groupTaskDownloader.setExternalListenerCallBack(new GroupTaskDownloadCallBack() {
            @Override
            public void onProgress(long currentOffset) {
                callback.onProgress(currentOffset);
                Log.d(TAG, "onProgress: ---------------" + currentOffset + "B");
            }

            @Override
            public void onStarted() {
                callback.onStart();
                Log.d(TAG, "onStarted: ---------------");
            }

            @Override
            public void onCompleted() {
                callback.onCompleted();
                Log.d(TAG, "onCompleted: ---------------");
            }

            @Override
            public void onCanceled() {
                callback.onCanceled();
                Log.d(TAG, "onCanceled: ---------------");
            }

            @Override
            public void onError(int errorCode) {
                callback.onError(errorCode);
                Log.d(TAG, "onError: ---------------" + errorCode);
            }
        });


        for (DownloadRequest request : requests) {
            if (i == requests.size()) {
                break;
            }
            if (!request.getUrl().startsWith("assets")) {
                parentPath[i] = request.getFileDir();
                fileName[i] = request.getFileName();
                urls[i] = request.getUrl();
                i++;
            }
        }

        if (urls[0] == null) {
            callback.onCompleted();
            return;
        }

        groupTaskDownloader.startParallelDownload(sessionId, parentPath, urls, fileName, HIGH_PRIORITY);
        Log.d(TAG, "startDownload:......");
    }

    @Override
    public void deferredDownload(int sessionId, List<DownloadRequest> requests, final DownloadCallback callback, boolean usingMobileDataPermitted) {

        groupTaskDownloader.setExternalListenerCallBack(new GroupTaskDownloadCallBack() {
            @Override
            public void onProgress(long currentOffset) {
                callback.onProgress(currentOffset);
            }

            @Override
            public void onStarted() {
                callback.onStart();
            }

            @Override
            public void onCompleted() {
                callback.onCompleted();
                Log.d(TAG, "onCompleted: ");
            }

            @Override
            public void onCanceled() {
                callback.onCanceled();
                Log.d(TAG, "onCanceled: ");
            }

            @Override
            public void onError(int errorCode) {
                callback.onError(errorCode);
            }
        });
        String[] urls = new String[requests.size()];
        String[] parentPath = new String[requests.size()];
        String[] fileName = new String[requests.size()];
        int i = 0;
        for (DownloadRequest request
                : requests) {

            if (i == requests.size()) {
                break;
            }
            if (!request.getUrl().startsWith("assets")) {
                parentPath[i] = request.getFileDir();
                fileName[i] = request.getFileName();
                urls[i] = request.getUrl();
                i++;
            }
        }

        if (urls[0] == null) {
            callback.onCompleted();
            return;
        }

        groupTaskDownloader.startParallelDownload(sessionId, parentPath, urls, fileName, LOW_PRIORITY);
        Log.d(TAG, "startDownload:......");
    }

    @Override
    public boolean cancelDownloadSync(int sessionId) {
        groupTaskDownloader.suspendQueueDownload(sessionId);
        groupTaskDownloader.deleteQueueDownloadFile(sessionId);
        if (groupTaskDownloader.runningParallelCount() > 0) {
            Log.e(TAG, "cancelDownloadSync: cancel failed....");
            return false;
        } else {
            return true;
        }

    }

    @Override
    public long getDownloadSizeThresholdWhenUsingMobileData() {
        return 10 * 1024 * 1024;
    }

    @Override
    public boolean isDeferredDownloadOnlyWhenUsingWifiData() {
        return true;
    }

}
