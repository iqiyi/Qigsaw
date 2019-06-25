package com.google.android.play.core.tasks;

import java.util.concurrent.Executor;

public abstract class Task<Result> {

    /**
     * Gets the result of the Task, if it has already completed.
     */
    public abstract Result getResult();

    /**
     * Gets the result of the Task, if it has already completed.
     */
    public abstract <X extends Throwable> Result getResult(Class<X> aClass) throws X;

    /**
     * Returns true if the Task has completed successfully; false otherwise.
     */
    public abstract boolean isSuccessful();

    /**
     * Returns true if the Task is complete; false otherwise.
     */
    public abstract boolean isComplete();

    /**
     * Returns the exception that caused the Task to fail.
     */
    public abstract Exception getException();

    /**
     * Adds a listener that is called if the Task completes successfully.
     */
    public abstract Task<Result> addOnSuccessListener(Executor executor, OnSuccessListener<? super Result> listener);

    /**
     * Adds a listener that is called if the Task completes successfully.
     */
    public abstract Task<Result> addOnSuccessListener(OnSuccessListener<? super Result> listener);

    /**
     * Adds a listener that is called when the Task completes.
     */
    public abstract Task<Result> addOnCompleteListener(OnCompleteListener<Result> listener);

    /**
     * Adds a listener that is called when the Task completes.
     */
    public abstract Task<Result> addOnCompleteListener(Executor executor, OnCompleteListener<Result> listener);

    /**
     * Adds a listener that is called if the Task fails.
     */
    public abstract Task<Result> addOnFailureListener(Executor executor, OnFailureListener listener);

    /**
     * Adds a listener that is called if the Task fails.
     */
    public abstract Task<Result> addOnFailureListener(OnFailureListener listener);

}
