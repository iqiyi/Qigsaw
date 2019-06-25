package com.google.android.play.core.tasks;

import java.util.ArrayDeque;
import java.util.Queue;

final class InvocationListenerManager<TResult> {

    private final Object lock = new Object();

    private Queue<InvocationListener<TResult>> mInvocationListenerQueue;

    private boolean invoked;

    void addInvocationListener(InvocationListener<TResult> invocationListener) {
        synchronized (lock) {
            if (mInvocationListenerQueue == null) {
                mInvocationListenerQueue = new ArrayDeque<>();
            }
            mInvocationListenerQueue.add(invocationListener);
        }
    }

    void invokeListener(Task<TResult> task) {
        synchronized (lock) {
            if (mInvocationListenerQueue == null || invoked) {
                return;
            }
            invoked = true;
        }
        while (true) {
            InvocationListener invocationListener;
            synchronized (lock) {
                invocationListener = mInvocationListenerQueue.poll();
                if (invocationListener == null) {
                    invoked = false;
                    return;
                }
            }
            invocationListener.invoke(task);
        }
    }

}
