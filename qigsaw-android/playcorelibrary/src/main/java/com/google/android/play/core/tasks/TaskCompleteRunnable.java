package com.google.android.play.core.tasks;

final class TaskCompleteRunnable implements Runnable {

    private final InvokeCompleteListener mCompleteExecutor;

    private final Task mTask;

    TaskCompleteRunnable(InvokeCompleteListener executor, Task task) {
        this.mCompleteExecutor = executor;
        this.mTask = task;
    }

    @Override
    public void run() {
        synchronized (mCompleteExecutor.lock) {
            if (mCompleteExecutor.mListener != null) {
                mCompleteExecutor.mListener.onComplete(mTask);
            }
        }
    }
}
