package com.google.android.play.core.tasks;

final class TaskSuccessRunnable implements Runnable {

    private final InvokeSuccessListener mSuccessExecutor;

    private final Task mTask;

    TaskSuccessRunnable(InvokeSuccessListener executor, Task task) {
        this.mSuccessExecutor = executor;
        this.mTask = task;
    }

    @Override
    public void run() {
        synchronized (mSuccessExecutor.lock) {
            if (mSuccessExecutor.mListener != null) {
                mSuccessExecutor.mListener.onSuccess(mTask.getResult());
            }
        }
    }
}
