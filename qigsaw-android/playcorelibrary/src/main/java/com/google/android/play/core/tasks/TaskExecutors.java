package com.google.android.play.core.tasks;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class TaskExecutors {

    /**
     * An Executor that uses the main application thread.
     */
    public static final Executor MAIN_THREAD = new TaskExecutors.MainThreadExecutor();

    static final Executor sExecutor = new TaskExecutor();

    static final class MainThreadExecutor implements Executor {

        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }

}
