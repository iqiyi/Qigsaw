package com.google.android.play.core.tasks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Tasks {

    public static <TResult> Task<TResult> createTaskAndSetResult(TResult result) {
        TaskImpl<TResult> task;
        (task = new TaskImpl<>()).setResult(result);
        return task;
    }


    private Tasks() {

    }

    private static <TResult> TResult getResult(Task<TResult> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        throw new ExecutionException(task.getException());
    }

    private static void addTaskListener(Task<?> task, AwaitTaskListener listener) {
        task.addOnSuccessListener(TaskExecutors.sExecutor, listener);
        task.addOnFailureListener(TaskExecutors.sExecutor, listener);
    }

    /**
     * Blocks until the specified Task is complete.
     */
    public static <TResult> TResult await(Task<TResult> task) throws ExecutionException, InterruptedException {
        if (task == null) {
            throw new NullPointerException("Task must not be null");
        }
        if (task.isComplete()) {
            return getResult(task);
        }
        AwaitTaskListener listener = new AwaitTaskListener();
        addTaskListener(task, listener);
        listener.await();
        return getResult(task);
    }

    /**
     * Blocks until the specified Task is complete.
     */
    public static <TResult> TResult await(Task<TResult> task, long j, TimeUnit timeUnit) throws TimeoutException, InterruptedException, ExecutionException {
        if (task == null) {
            throw new NullPointerException("Task must not be null");
        }
        if (timeUnit == null) {
            throw new NullPointerException("TimeUnit must not be null");
        }
        if (task.isComplete()) {
            return getResult(task);
        }
        AwaitTaskListener listener = new AwaitTaskListener();
        addTaskListener(task, listener);
        if (listener.awaitTimeout(j, timeUnit)) {
            return getResult(task);
        }
        throw new TimeoutException("Timed out waiting for Task");
    }


    private static class AwaitTaskListener<TResult> implements OnFailureListener, OnSuccessListener<TResult> {

        private final CountDownLatch countDownLatch;

        private AwaitTaskListener() {
            this.countDownLatch = new CountDownLatch(1);
        }

        void await() throws InterruptedException {
            this.countDownLatch.await();
        }

        boolean awaitTimeout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return this.countDownLatch.await(timeout, timeUnit);
        }

        public void onFailure(Exception exception) {
            this.countDownLatch.countDown();
        }

        public void onSuccess(TResult result) {
            this.countDownLatch.countDown();
        }
    }

}
