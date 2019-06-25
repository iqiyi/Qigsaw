package com.google.android.play.core.tasks;

public interface OnFailureListener {

    /**
     * Called when the Task fails with an exception.
     */
    void onFailure(Exception e);

}
