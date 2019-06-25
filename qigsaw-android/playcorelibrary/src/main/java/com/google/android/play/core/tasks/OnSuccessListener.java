package com.google.android.play.core.tasks;

public interface OnSuccessListener<TResult> {

    /**
     * Called when the Task completes successfully.
     */
    void onSuccess(TResult result);

}
