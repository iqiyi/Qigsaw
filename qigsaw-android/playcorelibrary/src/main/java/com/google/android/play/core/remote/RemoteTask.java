package com.google.android.play.core.remote;

import androidx.annotation.RestrictTo;

import com.google.android.play.core.tasks.TaskWrapper;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class RemoteTask implements Runnable {

    private final TaskWrapper<?> task;

    RemoteTask() {
        this.task = null;
    }

    public RemoteTask(TaskWrapper<?> task) {
        this.task = task;
    }

    @Override
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            e.printStackTrace();
            if (this.task != null) {
                this.task.setException(e);
            }
        }
    }

    final TaskWrapper getTask() {
        return this.task;
    }

    protected abstract void execute();

}
