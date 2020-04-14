package com.iqiyi.qigsaw.sample.downloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.listener.DownloadListener3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class GroupTaskDownloader {

    private static final String TAG = "GroupTaskDownloader";

    private Map<Integer, List<DownloadTask>> tasksMapsDeepClone = new ConcurrentHashMap<>();

    private GroupDownloadContext downloadTaskQueue;

    private int maxParallelRunningCount = 5;

    private Map<Integer, Map<Integer, Long>> sessionWithCurrentOffsetMap = new ConcurrentHashMap<>();

    private Map<Integer, Long> currentOffsetMap = new ConcurrentHashMap<>();

    private CallbackProgress callbackProgress;

    private volatile boolean isCompleted = true;

    private GroupTaskDownloadCallBack groupTaskDownloadCallBack;

    private Map<Integer, DownloadListener> sessionWithDownloadListenerMap = new ConcurrentHashMap<>();

    private Map<Integer, OnBunchCancelListener> sessionWithOnBunchCancelListenerMap = new ConcurrentHashMap<>();

    public void setCallbackProgress(CallbackProgress callbackProgress) {
        this.callbackProgress = callbackProgress;
    }

    /**
     * build the serial task queue (group of tasks) and start the download.
     * the priority of the group can be set
     */
    public void startSerialDownload(int sessionId, String[] parentPath, String[] urls, String[] fileName, int priority) {
        downloadTaskQueue.startSerialQueueDownload(sessionId, parentPath, urls, fileName, priority, getDownloadListenerBySessionId(sessionId));
    }

    /**
     * build the parallel task queue (group of tasks) and start the download.
     * the priority of the group can be set
     */
    public void startParallelDownload(int sessionId, String[] parentPath, String[] urls, String[] fileName, int priority) {
        downloadTaskQueue.startParallelQueueDownload(sessionId, parentPath, urls, fileName, priority, getDownloadListenerBySessionId(sessionId));
    }


    /**
     * for suspending single group of tasks.
     * GroupDownloadContext.stop() will be used to suspend all groups.
     *
     * @param sessionId
     */
    public void suspendQueueDownload(int sessionId) {
        downloadTaskQueue.suspendQueueDownload(sessionId, getOnBunchCancelListenerBySessionId(sessionId));
    }

    /**
     * delete downloaded or downloading group files.
     */
    public void deleteQueueDownloadFile(int sessionId) {
        downloadTaskQueue.deleteQueueDownload(sessionId, getOnBunchCancelListenerBySessionId(sessionId));
    }

    private OnBunchCancelListener getOnBunchCancelListenerBySessionId(int sessionId) {
        sessionWithOnBunchCancelListenerMap.put(sessionId, new OnBunchCancelListener() {
            @Override
            public void onSuccess() {
                groupTaskDownloadCallBack.onCanceled();
            }

            @Override
            public void onFailure() {
                groupTaskDownloadCallBack.onCanceled();
            }
        });
        return sessionWithOnBunchCancelListenerMap.get(sessionId);
    }

    private DownloadListener getDownloadListenerBySessionId(final int sessionId) {

        sessionWithDownloadListenerMap.put(sessionId, new DownloadListener3() {
            @Override
            public void retry(@NonNull DownloadTask task, @NonNull ResumeFailedCause cause) {
                Log.d(TAG, "retry: ");

            }

            @Override
            public void connected(@NonNull DownloadTask task, int blockCount, long currentOffset, long totalLength) {
                Log.d(TAG, "connected: " + task.getFilename());
            }

            @Override
            public void progress(@NonNull DownloadTask task, long currentOffset, long totalLength) {
                if (totalLength == 0) {
                    return;
                }
                Log.d(TAG, task.getFilename() + "  progress: " + currentOffset * 100 / totalLength + "%");
                if (callbackProgress != null) {
                    callbackProgress.onProgress(currentOffset, totalLength);
                }
                currentOffsetMap.put(task.getId(), currentOffset);
                sessionWithCurrentOffsetMap.put(sessionId, currentOffsetMap);
                Long totalCurrentOffset = 0L;
                if (sessionWithCurrentOffsetMap.get(sessionId) != null) {
                    for (Long aLong
                            : sessionWithCurrentOffsetMap.get(sessionId).values()) {
                        totalCurrentOffset += aLong;
                    }
                }
                groupTaskDownloadCallBack.onProgress(totalCurrentOffset);
            }

            @Override
            protected void started(@NonNull DownloadTask task) {
                Log.d(TAG, "started: ");
                groupTaskDownloadCallBack.onStarted();
                isCompleted = true;
            }

            @Override
            protected void completed(@NonNull DownloadTask task) {
                Log.d(TAG, "completed: ");
                if (tasksMapsDeepClone.get(sessionId) == null) {
                    tasksMapsDeepClone.put(sessionId, downloadTaskQueue.getTasksMapDeepClone().get(sessionId));
                }
                if (tasksMapsDeepClone.get(sessionId).contains(task) && tasksMapsDeepClone.get(sessionId) != null) {
                    tasksMapsDeepClone.get(sessionId).remove(task);
                }
                if (tasksMapsDeepClone.get(sessionId) == null) {
                    groupTaskDownloadCallBack.onCompleted();
                    Log.d(TAG, "onCompleted:: ----------");
                    sessionWithDownloadListenerMap.remove(sessionId);
                    sessionWithOnBunchCancelListenerMap.remove(sessionId);
                }
            }

            @Override
            protected void canceled(@NonNull DownloadTask task) {
                Log.d(TAG, "canceled: ");
                isCompleted = false;
            }

            @Override
            protected void error(@NonNull DownloadTask task, @NonNull Exception e) {
                Log.d(TAG, "error: " + e.getMessage());
                isCompleted = false;
                groupTaskDownloadCallBack.onError(e.hashCode());
            }

            @Override
            protected void warn(@NonNull DownloadTask task) {
                Log.d(TAG, "warn: ");
            }

        });

        return sessionWithDownloadListenerMap.get(sessionId);
    }

    public GroupTaskDownloader() {
        GroupDownloadTaskQueueListener downloadTaskQueueListener = new GroupDownloadTaskQueueListener() {
            @Override
            public void taskEnd(@NonNull GroupDownloadContext context, @NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, int remainCount) {
                Log.d(TAG, "DownloadTaskQueueListener :taskEnd.....");
            }

            @Override
            public void queueEnd(@NonNull GroupDownloadContext context) {
                Log.d(TAG, "queueEnd: ....");
                if (isCompleted) {
                    groupTaskDownloadCallBack.onCompleted();
                    Log.d(TAG, "onCompleted:: ----------");
                }
            }
        };

        GroupDownloadContext.Builder builder = new GroupDownloadContext.QueueSet()
                .setMinIntervalMillisCallbackProcess(150)
                .commit();
        downloadTaskQueue = builder.setListener(downloadTaskQueueListener).build();

    }

    /**
     * set listener callback from external
     */
    public void setExternalListenerCallBack(GroupTaskDownloadCallBack groupTaskDownloadCallBack) {
        this.groupTaskDownloadCallBack = groupTaskDownloadCallBack;
    }

    /**
     * return the count of running parallel groups
     *
     * @return runningParallelCount
     */
    public int runningParallelCount() {
        return downloadTaskQueue.runningParallelCount();
    }

    /**
     * set max parallel running count.
     * the default value of it in one downloader is 5
     */
    public void setMaxParallelRunningCount(int maxParallelRunningCount) {
        this.maxParallelRunningCount = maxParallelRunningCount;
        DownloadDispatcher.setMaxParallelRunningCount(maxParallelRunningCount);
    }

    /**
     * @return maxParallelRunningCount
     */
    public int getMaxParallelRunningCount() {
        return this.maxParallelRunningCount;
    }

    public interface CallbackProgress {
        void onProgress(long currentOffset, long totalLength);
    }


}
