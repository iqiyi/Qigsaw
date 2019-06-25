package com.google.android.play.core.tasks;

import java.util.concurrent.Executor;

final class InvokeSuccessListener<TResult> implements InvocationListener<TResult> {

    private final Executor mExecutor;

    final Object lock = new Object();

    final OnSuccessListener<? super TResult> mListener;

    InvokeSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener) {
        this.mExecutor = executor;
        this.mListener = listener;
    }

    @Override
    public void invoke(Task<TResult> task) {
        if (task.isSuccessful()) {
            synchronized (lock) {
                if (mListener == null) {
                    return;
                }
            }
            mExecutor.execute(new TaskSuccessRunnable(this, task));
        }
    }
}
