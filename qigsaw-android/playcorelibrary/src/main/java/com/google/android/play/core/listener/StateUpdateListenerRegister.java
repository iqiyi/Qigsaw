package com.google.android.play.core.listener;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.RestrictTo;

import com.google.android.play.core.splitcompat.util.PlayCore;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class StateUpdateListenerRegister<StateT> {

    protected final PlayCore playCore;

    private final Set<StateUpdatedListener<StateT>> mStateUpdatedListeners = Collections.newSetFromMap(new ConcurrentHashMap());

    private final Context context;

    private final IntentFilter intentFilter;

    private final StateUpdatedReceiver receiver = new StateUpdatedReceiver(this);

    private final Object mLock = new Object();

    protected StateUpdateListenerRegister(PlayCore playCore, IntentFilter intentFilter, Context context) {
        this.playCore = playCore;
        this.intentFilter = intentFilter;
        this.context = context;
    }

    public final void registerListener(StateUpdatedListener<StateT> listener) {
        synchronized (mLock) {
            this.playCore.debug("registerListener");
            if (mStateUpdatedListeners.contains(listener)) {
                this.playCore.debug("listener has been registered!");
                return;
            }
            mStateUpdatedListeners.add(listener);
            if (mStateUpdatedListeners.size() == 1) {
                context.registerReceiver(receiver, intentFilter);
            }
        }
    }

    public final void unregisterListener(StateUpdatedListener<StateT> listener) {
        synchronized (mLock) {
            this.playCore.debug("unregisterListener");
            boolean contained = mStateUpdatedListeners.remove(listener);
            if (mStateUpdatedListeners.isEmpty() && contained) {
                try {
                    context.unregisterReceiver(receiver);
                } catch (IllegalArgumentException e) {
                    playCore.error(e, "Receiver not registered: " + intentFilter.getAction(0));
                }
            }
        }
    }

    protected abstract void onReceived(Intent intent);

    public final void notifyListeners(StateT result) {
        Iterator iterator = mStateUpdatedListeners.iterator();
        while (iterator.hasNext()) {
            StateUpdatedListener<StateT> stateUpdatedListener = (StateUpdatedListener<StateT>) iterator.next();
            stateUpdatedListener.onStateUpdate(result);
        }
    }


}
