package com.google.android.play.core.listener;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.RestrictTo;

import com.google.android.play.core.splitcompat.util.PlayCore;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public abstract class StateUpdateListenerRegister<StateT> {

    protected final PlayCore playCore;

    private final Set<StateUpdatedListener<StateT>> mStateUpdatedListeners = Collections.newSetFromMap(new ConcurrentHashMap());

    private final Context context;

    private final IntentFilter intentFilter;

    private final StateUpdatedReceiver receiver = new StateUpdatedReceiver(this);

    protected StateUpdateListenerRegister(PlayCore playCore, IntentFilter intentFilter, Context context) {
        this.playCore = playCore;
        this.intentFilter = intentFilter;
        this.context = context;
    }

    public final synchronized void registerListener(StateUpdatedListener<StateT> listener) {
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

    public final synchronized void unregisterListener(StateUpdatedListener<StateT> listener) {
        this.playCore.debug("unregisterListener");
        mStateUpdatedListeners.remove(listener);
        if (mStateUpdatedListeners.isEmpty()) {
            context.unregisterReceiver(receiver);
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
