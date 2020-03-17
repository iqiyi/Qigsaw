package com.google.android.play.core.tasks;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

class TaskImpl<TResult> extends Task<TResult> {

    private final Object lock = new Object();

    private InvocationListenerManager<TResult> mListenerManager = new InvocationListenerManager<>();

    private Exception mException;

    private TResult mResult;

    private boolean isComplete;

    private void assertComplete() {
        if (!isComplete) {
            throw new RuntimeException("Task is not yet complete");
        }
    }

    private void invokeListeners() {
        synchronized (lock) {
            if (isComplete) {
                mListenerManager.invokeListener(this);
            }
        }
    }

    public final void setResultCheck(TResult result) {
        synchronized (lock) {
            if (isComplete) {
                throw new RuntimeException("Task is already complete");
            }
            isComplete = true;
            mResult = result;
        }
        mListenerManager.invokeListener(this);
    }

    @Override
    public TResult getResult() {
        TResult result;
        synchronized (lock) {
            assertComplete();
            if (mException == null) {
                result = mResult;
            } else {
                throw new RuntimeExecutionException(mException);
            }
        }
        return result;
    }

    @Override
    public boolean isSuccessful() {
        boolean success;
        synchronized (lock) {
            success = isComplete && mException == null;
        }
        return success;
    }

    @Override
    public boolean isComplete() {
        boolean complete;
        synchronized (lock) {
            complete = isComplete;
        }
        return complete;
    }

    @Override
    public Exception getException() {
        Exception exception;
        synchronized (lock) {
            exception = mException;
        }
        return exception;
    }

    @Override
    public <X extends Throwable> TResult getResult(Class<X> aClass) throws X {
        return null;
    }

    @Override
    public Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener) {
        mListenerManager.addInvocationListener(new InvokeSuccessListener<>(executor, listener));
        invokeListeners();
        return this;
    }

    @Override
    public Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> listener) {
        return addOnSuccessListener(TaskExecutors.MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener) {
        mListenerManager.addInvocationListener(new InvokeFailureListener<TResult>(executor, listener));
        invokeListeners();
        return this;
    }

    @Override
    public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener) {
        return addOnCompleteListener(TaskExecutors.MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
        mListenerManager.addInvocationListener(new InvokeCompleteListener<>(executor, listener));
        invokeListeners();
        return this;
    }

    @Override
    public Task<TResult> addOnFailureListener(OnFailureListener listener) {
        return addOnFailureListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public boolean setException(@NonNull Exception exception) {
        synchronized (lock) {
            if (isComplete) {
                return false;
            }
            isComplete = true;
            mException = exception;
            mListenerManager.invokeListener(this);
            return true;
        }
    }

    public boolean setResult(TResult result) {
        synchronized (lock) {
            if (isComplete) {
                return false;
            }
            isComplete = true;
            mResult = result;
            mListenerManager.invokeListener(this);
            return true;
        }
    }
}
