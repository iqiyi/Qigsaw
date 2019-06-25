package com.google.android.play.core.tasks;

final class TaskFailureRunnable implements Runnable {

    private final InvokeFailureListener mFailureExecutor;

    private final Task mTask;

    TaskFailureRunnable(InvokeFailureListener executor, Task task) {
        this.mFailureExecutor = executor;
        this.mTask = task;
    }

    @Override
    public void run() {
        synchronized (mFailureExecutor.lock) {
            if (mFailureExecutor.mListener != null) {
                mFailureExecutor.mListener.onFailure(mTask.getException());
            }
        }
    }
}
