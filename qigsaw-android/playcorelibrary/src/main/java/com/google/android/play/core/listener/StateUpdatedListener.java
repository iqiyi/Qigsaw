package com.google.android.play.core.listener;

public interface StateUpdatedListener<State> {

    /**
     * Callback triggered whenever the state has changed.
     */
    void onStateUpdate(State state);
}
