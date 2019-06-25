package com.google.android.play.core.tasks;

import java.util.concurrent.Executor;

class InvokeCompleteListener<TResult> implements InvocationListener<TResult> {

    private final Executor mExecutor;

    final Object lock = new Object();

    final OnCompleteListener<? super TResult> mListener;

    InvokeCompleteListener(Executor executor, OnCompleteListener<? super TResult> listener) {
        this.mExecutor = executor;
        this.mListener = listener;
    }

    @Override
    public void invoke(Task<TResult> task) {
        synchronized (lock) {
            if (mListener == null) {
                return;
            }
        }
        mExecutor.execute(new TaskCompleteRunnable(this, task));
    }
}
