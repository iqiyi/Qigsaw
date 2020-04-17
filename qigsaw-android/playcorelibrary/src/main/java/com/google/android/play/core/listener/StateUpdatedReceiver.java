package com.google.android.play.core.listener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class StateUpdatedReceiver extends BroadcastReceiver {

    private final StateUpdateListenerRegister mRegister;

    StateUpdatedReceiver(StateUpdateListenerRegister listenerRegister) {
        this.mRegister = listenerRegister;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mRegister.onReceived(intent);
    }

}
