package com.google.android.play.core.tasks;

import java.util.concurrent.Executor;

class InvokeFailureListener<TResult> implements InvocationListener<TResult> {

    private final Executor mExecutor;

    final Object lock = new Object();

    final OnFailureListener mListener;

    InvokeFailureListener(Executor executor, OnFailureListener listener) {
        this.mExecutor = executor;
        this.mListener = listener;
    }

    @Override
    public void invoke(Task<TResult> task) {
        if (!task.isSuccessful()) {
            synchronized (lock) {
                if (mListener == null) {
                    return;
                }
            }
            mExecutor.execute(new TaskFailureRunnable(this, task));
        }
    }
}
