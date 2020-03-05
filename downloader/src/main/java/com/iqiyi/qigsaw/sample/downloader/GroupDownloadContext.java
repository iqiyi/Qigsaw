package com.iqiyi.qigsaw.sample.downloader;


import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.listener.DownloadListener2;
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupDownloadContext {

    private static final String TAG = "GroupDownloadContext";

    private static final Executor SERIAL_EXECUTOR = new ThreadPoolExecutor(0,
            Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            Util.threadFactory("OkDownload Serial", false));

    private static final int MaxSerialDownload = 3;

    private static Map<Integer, List<DownloadTask>> tasksMap = new HashMap<>();

    public Map<Integer, List<DownloadTask>> getTasksMapDeepClone() {
        Map<Integer, List<DownloadTask>> tasksMapDeepClone = new HashMap<>(tasksMap);
        return tasksMapDeepClone;
    }

    private DownloadTask[] downloadTasks;

    private volatile static ConcurrentLinkedQueue<DownloadTask> scheduleTaskLinkedQueue = new ConcurrentLinkedQueue<>();

    /**
     * control the progress of download queue
     */
    private volatile boolean isDownloadStarted = false;

    @Nullable
    private final GroupDownloadTaskQueueListener downloadTaskQueueListener;

    private final QueueSet queueSet;

    private Handler uiHandler;

    GroupDownloadContext(@NonNull DownloadTask[] tasks,
                         @Nullable GroupDownloadTaskQueueListener downloadTaskQueueListener,
                         @NonNull GroupDownloadContext.QueueSet set,
                         @NonNull Handler uiHandler) {
        this(tasks, downloadTaskQueueListener, set);
        this.uiHandler = uiHandler;
    }

    GroupDownloadContext(@NonNull DownloadTask[] tasks,
                         @Nullable GroupDownloadTaskQueueListener downloadTaskQueueListener,
                         @NonNull GroupDownloadContext.QueueSet set) {
        this.downloadTasks = tasks;
        this.downloadTaskQueueListener = downloadTaskQueueListener;
        this.queueSet = set;
    }


    public boolean isStarted() {
        return isDownloadStarted;
    }

    public DownloadTask[] getTasks() {
        return downloadTasks;
    }

    /**
     * Start downloading.
     *
     * @param listener the listener for each task, if you have already provided
     *                 {@link #downloadTaskQueueListener}, it's accept {@code null} for each task's listener.
     * @param isSerial whether download queue serial or parallel.
     */
    private void start(@Nullable final DownloadListener listener, boolean isSerial, final DownloadTask[] tasks) {

        final long startTime = SystemClock.uptimeMillis();
        Util.d(TAG, "start " + isSerial);
        isDownloadStarted = true;
        final DownloadListener targetListener;
        if (downloadTaskQueueListener != null) {
            targetListener = new DownloadListenerBunch.Builder()
                    .append(listener)//set several listeners for each task
                    .append(new QueueAttachListener(this, downloadTaskQueueListener, tasks.length))
                    .build();
        } else {
            targetListener = listener;
        }
        if (isSerial) {
            Collections.addAll(scheduleTaskLinkedQueue, tasks); //the priority of tasks can be reset by implementing Comparable
            executeOnSerialExecutor(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Thread name  = " + Thread.currentThread().getName());
                    for (DownloadTask task : scheduleTaskLinkedQueue) {
                        List<DownloadTask> tasks1 = new ArrayList<>(Arrays.asList(tasks));
                        if (!scheduleTaskLinkedQueue.containsAll(tasks1)) {
                            break;
                        }
                        if (!isStarted()) {
                            callbackQueueEndOnSerialLoop(task.isAutoCallbackToUIThread());
                            break;
                        }
                        Log.d(TAG, "run: " + task.getFilename());

                        task.execute(targetListener);
                    }
                }
            });
        } else {
            DownloadTask.enqueue(tasks, targetListener);
        }
        Util.d(TAG,
                "start finish " + isSerial + " " + (SystemClock.uptimeMillis() - startTime) + "ms");
    }


    public AlterContext alter() {
        return new AlterContext(this);
    }


    /**
     * build the serial queue download by below params
     * There have been an error exits there. Don't use this method
     *
     * @param sessionId of the serial queue download
     * @param priority  of the serial queue download
     */
    public void startSerialQueueDownload(@NonNull Integer sessionId, String[] parentPath, String[] urls, @NonNull String[] fileName, int priority, DownloadListener downloadListener) {
        int i = 0;

        List<DownloadTask> tasksList = new ArrayList<>();
        for (String url : urls) {
            DownloadTask task = new DownloadTask.Builder(url, parentPath[i], fileName[i]).setPriority(priority).build(); //if filename[i] is null,the name of downloaded file will be decided by url
            Log.d(TAG, "startQueueDownload: tempPriority:" + priority + " i:" + i);
            tasksList.add(task);
            i++;
        }
        DownloadTask[] tasks = new DownloadTask[tasksList.size()];
        tasksList.toArray(tasks);

        tasksMap.put(sessionId, tasksList);

        //serial download
        start(downloadListener, true, tasks);

    }

    /**
     * build the parallel queue download by below params
     *
     * @param sessionId of the parallel queue download
     * @param priority  of the parallel parallel download
     */
    public void startParallelQueueDownload(int sessionId, String[] parentPath, String[] urls, String[] fileName, int priority, DownloadListener downloadListener) {
        Log.d(TAG, "startParallelQueueDownload: " + runningParallelCount());
        List<DownloadTask> tasksList = new ArrayList<>();
        int i = 0;
        for (String url : urls) {
            DownloadTask task = new DownloadTask.Builder(url, parentPath[i], fileName[i]).setPriority(priority).build(); //if filename[i] is null,the name of downloaded file will be decided by url
            Log.d(TAG, "startQueueDownload: tempPriority:" + priority + " i:" + i);
            tasksList.add(task);
            i++;
        }
        DownloadTask[] tasks = new DownloadTask[tasksList.size()];
        tasksList.toArray(tasks);
        tasksMap.put(sessionId, tasksList);
        //parallel download
        start(downloadListener, false, tasks);
        Log.d(TAG, "startParallelQueueDownload: " + runningParallelCount());
    }

    /**
     * @return runningParallelCount or error(-1)
     */
    public int runningParallelCount() {
        try {
            DownloadDispatcher downloadDispatcher = OkDownload.with().downloadDispatcher();
            Method method = downloadDispatcher.getClass().getDeclaredMethod("runningAsyncSize");
            method.setAccessible(true);
            return (Integer) method.invoke(downloadDispatcher);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * stop all tasks in this queue
     */
    public void stop() {
        if (isDownloadStarted) OkDownload.with().downloadDispatcher().cancel(downloadTasks);
        isDownloadStarted = false;
    }

    /**
     * suspend the tasks with sessionId of tasks array
     * if you want to suspend all tasks ,please choose DownloadTaskQueue.stop() instead.
     */

    public void suspendQueueDownload(Integer sessionId, OnBunchCancelListener onBunchCancelListener) {

        if (tasksMap.get(sessionId) != null) {
            DownloadTask[] tasks = new DownloadTask[tasksMap.get(sessionId).size()];
            tasksMap.get(sessionId).toArray(tasks);
            if (tasksMap.get(sessionId).size() != 0) {
                scheduleTaskLinkedQueue.removeAll(tasksMap.get(sessionId));
                DownloadTask.cancel(tasks);
                onBunchCancelListener.onSuccess();
            } else {
                onBunchCancelListener.onFailure();
            }
        } else {
            onBunchCancelListener.onFailure();
        }


    }

    /**
     * delete the download task whether it is completed or not
     */
    public void deleteQueueDownload(int sessionId, OnBunchCancelListener onBunchCancelListener) {
        if (tasksMap.get(sessionId) != null) {
            //suspend group task if it exits
            suspendQueueDownload(sessionId, onBunchCancelListener);
            for (DownloadTask task : tasksMap.get(sessionId)) {
                new DeleteDownloadedFilesTask(task.getParentFile().getPath(), false, task.getFilename()).run();
            }
            tasksMap.remove(sessionId);
            onBunchCancelListener.onSuccess();
        } else {
            onBunchCancelListener.onFailure();
        }
    }

    private void callbackQueueEndOnSerialLoop(boolean isAutoCallbackToUIThread) {
        if (downloadTaskQueueListener == null) return;

        if (isAutoCallbackToUIThread) {
            if (uiHandler == null) uiHandler = new Handler(Looper.getMainLooper());
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadTaskQueueListener.queueEnd(GroupDownloadContext.this);
                }
            });
        } else {
            downloadTaskQueueListener.queueEnd(this);
        }
    }

    void executeOnSerialExecutor(Runnable runnable) {
        SERIAL_EXECUTOR.execute(runnable);
    }

    public static int getMaxSerialDownload() {
        return MaxSerialDownload;
    }

    public Builder toBuilder() {
        return new Builder(queueSet, new ArrayList<>(Arrays.asList(downloadTasks)))
                .setListener(this.downloadTaskQueueListener);
    }

    public static class Builder {

        final ArrayList<DownloadTask> boundTaskList;
        private final QueueSet set;

        private GroupDownloadTaskQueueListener listener;

        public Builder() {
            this(new QueueSet());
        }

        public Builder(QueueSet set) {
            this(set, new ArrayList<DownloadTask>());
        }

        public Builder(QueueSet set, ArrayList<DownloadTask> taskArrayList) {
            this.set = set;
            this.boundTaskList = taskArrayList;
        }

        public Builder setListener(GroupDownloadTaskQueueListener listener) {
            if (listener == null) {
                return this;
            }
            this.listener = listener;
            return this;
        }


        public Builder bindSetTask(@NonNull DownloadTask task) {
            final int index = boundTaskList.indexOf(task);
            if (index >= 0) {
                // replace
                boundTaskList.set(index, task);
            } else {
                boundTaskList.add(task);
            }

            return this;
        }

        public DownloadTask bind(@NonNull String url) {
            if (set.uri == null) {
                throw new IllegalArgumentException("If you want to bind only with url, you have to"
                        + " provide parentPath on QueueSet!");
            }

            return bind(new DownloadTask.Builder(url, set.uri).setFilenameFromResponse(true));
        }

        public DownloadTask bind(@NonNull DownloadTask.Builder taskBuilder) {
            if (set.headerMapFields != null) taskBuilder.setHeaderMapFields(set.headerMapFields);
            if (set.readBufferSize != null) taskBuilder.setReadBufferSize(set.readBufferSize);
            if (set.flushBufferSize != null) taskBuilder.setFlushBufferSize(set.flushBufferSize);
            if (set.syncBufferSize != null) taskBuilder.setSyncBufferSize(set.syncBufferSize);
            if (set.wifiRequired != null) taskBuilder.setWifiRequired(set.wifiRequired);
            if (set.syncBufferIntervalMillis != null) {
                taskBuilder.setSyncBufferIntervalMillis(set.syncBufferIntervalMillis);
            }
            if (set.autoCallbackToUIThread != null) {
                taskBuilder.setAutoCallbackToUIThread(set.autoCallbackToUIThread);
            }
            if (set.minIntervalMillisCallbackProcess != null) {
                taskBuilder
                        .setMinIntervalMillisCallbackProcess(set.minIntervalMillisCallbackProcess);
            }

            if (set.passIfAlreadyCompleted != null) {
                taskBuilder.setPassIfAlreadyCompleted(set.passIfAlreadyCompleted);
            }

            final DownloadTask task = taskBuilder.build();
            if (set.tag != null) task.setTag(set.tag);

            boundTaskList.add(task);
            return task;
        }

        public void unbind(@NonNull DownloadTask task) {
            boundTaskList.remove(task);
        }

        public void unbind(int id) {
            List<DownloadTask> list = (List<DownloadTask>) boundTaskList.clone();
            for (DownloadTask task : list) {
                if (task.getId() == id) boundTaskList.remove(task);
            }
        }

        public GroupDownloadContext build() {
            DownloadTask[] tasks = new DownloadTask[boundTaskList.size()];
            return new GroupDownloadContext(boundTaskList.toArray(tasks), listener, set);
        }

    }

    public static class QueueSet {
        private Map<String, List<String>> headerMapFields;

        private Uri uri;
        private Integer readBufferSize;
        private Integer flushBufferSize;
        private Integer syncBufferSize;
        private Integer syncBufferIntervalMillis;
        private Boolean autoCallbackToUIThread;

        private Integer minIntervalMillisCallbackProcess;
        private Boolean passIfAlreadyCompleted;

        private Boolean wifiRequired;
        private Object tag;

        public Map<String, List<String>> getHeaderMapFields() {
            return headerMapFields;
        }

        public void setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
        }

        public Uri getDirUri() {
            return uri;
        }

        public QueueSet setParentPathUri(@NonNull Uri uri) {
            this.uri = uri;
            return this;
        }

        public QueueSet setParentPathFile(@NonNull File parentPathFile) {
            if (parentPathFile.isFile()) {
                throw new IllegalArgumentException("parent path only accept directory path");
            }

            this.uri = Uri.fromFile(parentPathFile);
            return this;
        }

        public QueueSet setParentPath(@NonNull String parentPath) {
            return setParentPathFile(new File(parentPath));
        }

        public int getReadBufferSize() {
            return readBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_READ_BUFFER_SIZE : readBufferSize;
        }

        public QueueSet setReadBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        public QueueSet setWifiRequired(Boolean wifiRequired) {
            this.wifiRequired = wifiRequired;
            return this;
        }

        public boolean isWifiRequired() {
            return wifiRequired == null
                    ? DownloadTask.Builder.DEFAULT_IS_WIFI_REQUIRED : wifiRequired;
        }

        public int getFlushBufferSize() {
            return flushBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_FLUSH_BUFFER_SIZE : flushBufferSize;
        }

        public QueueSet setFlushBufferSize(int flushBufferSize) {
            this.flushBufferSize = flushBufferSize;
            return this;
        }

        public int getSyncBufferSize() {
            return syncBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_SIZE : syncBufferSize;
        }

        public QueueSet setSyncBufferSize(int syncBufferSize) {
            this.syncBufferSize = syncBufferSize;
            return this;
        }

        public int getSyncBufferIntervalMillis() {
            return syncBufferIntervalMillis == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS
                    : syncBufferIntervalMillis;
        }

        public QueueSet setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        public boolean isAutoCallbackToUIThread() {
            return autoCallbackToUIThread == null
                    ? DownloadTask.Builder.DEFAULT_AUTO_CALLBACK_TO_UI_THREAD
                    : autoCallbackToUIThread;
        }

        public QueueSet setAutoCallbackToUIThread(Boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        public int getMinIntervalMillisCallbackProcess() {
            return minIntervalMillisCallbackProcess == null
                    ? DownloadTask.Builder.DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS
                    : minIntervalMillisCallbackProcess;
        }

        public QueueSet setMinIntervalMillisCallbackProcess(
                Integer minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Object getTag() {
            return tag;
        }

        public QueueSet setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public boolean isPassIfAlreadyCompleted() {
            return passIfAlreadyCompleted == null
                    ? DownloadTask.Builder.DEFAULT_PASS_IF_ALREADY_COMPLETED
                    : passIfAlreadyCompleted;
        }

        public QueueSet setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        public Builder commit() {
            return new Builder(this);
        }

    }

    static class QueueAttachListener extends DownloadListener2 {

        private final AtomicInteger remainCount;
        @NonNull
        private final GroupDownloadTaskQueueListener downloadTaskQueueListener;
        @NonNull
        private final GroupDownloadContext hostContext;

        QueueAttachListener(@NonNull GroupDownloadContext context,
                            @NonNull GroupDownloadTaskQueueListener downloadTaskQueueListener, int taskCount) {
            remainCount = new AtomicInteger(taskCount);
            this.downloadTaskQueueListener = downloadTaskQueueListener;
            this.hostContext = context;
        }

        @Override
        public void taskStart(@NonNull DownloadTask task) {
        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause,
                            @Nullable Exception realCause) {
            final int remainCount = this.remainCount.decrementAndGet();
            downloadTaskQueueListener.taskEnd(hostContext, task, cause, realCause, remainCount);
            if (remainCount <= 0) {
                downloadTaskQueueListener.queueEnd(hostContext);
                // only log the last one
                Util.d(TAG, "taskEnd and remainCount " + remainCount);
            }
        }

    }

    /**
     * The Alter helper for the {@link GroupDownloadContext}.
     */
    public static class AlterContext {

        private final GroupDownloadContext context;

        AlterContext(GroupDownloadContext context) {
            this.context = context;
        }

        /**
         * Replace the {@code oldTask} to the {@code newTask}
         *
         * @param oldTask the old task which has been added to the context.
         * @param newTask the new task which will be replace the {@code oldTask} on the
         *                {@code context}.
         */
        public AlterContext replaceTask(DownloadTask oldTask, DownloadTask newTask) {
            final DownloadTask[] tasks = context.downloadTasks;
            for (int i = 0; i < tasks.length; i++) {
                final DownloadTask task = tasks[i];
                if (task == oldTask) {
                    tasks[i] = newTask;
                }
            }

            return this;
        }

    }

}

