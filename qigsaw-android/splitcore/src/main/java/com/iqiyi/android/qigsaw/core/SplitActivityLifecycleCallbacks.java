package com.iqiyi.android.qigsaw.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.LruCache;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;

public abstract class SplitActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "SplitActivityLifecycleCallbacks";

    private static final String SPLIT_NAME_BASE = "base";

    private final LruCache<String, String> splitActivityNameCache = new LruCache<>(20);

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityCreated(splitName, activity, savedInstanceState);
            SplitLog.i(TAG, "Activity %s of split %s is created.", activity.getClass().getName(), splitName);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityStarted(@NonNull Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityStarted(splitName, activity);
            SplitLog.i(TAG, "Activity %s of split %s is started.", activity.getClass().getName(), splitName);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityResumed(@NonNull Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityResumed(splitName, activity);
            SplitLog.i(TAG, "Activity %s of split %s is started.", activity.getClass().getName(), splitName);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityPaused(@NonNull Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityPaused(splitName, activity);
            SplitLog.i(TAG, "Activity %s of split %s is paused.", activity.getClass().getName(), splitName);

        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityStopped(@NonNull Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityStopped(splitName, activity);
            SplitLog.i(TAG, "Activity %s of split %s is stopped.", activity.getClass().getName(), splitName);

        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivitySaveInstanceState(splitName, activity, outState);
            SplitLog.i(TAG, "Activity %s of split %s is saving state.", activity.getClass().getName(), splitName);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityDestroyed(@NonNull Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            onSplitActivityDestroyed(splitName, activity);
            SplitLog.i(TAG, "Activity %s of split %s is destroyed.", activity.getClass().getName(), splitName);
        }
    }

    private String getSplitNameForActivityName(Activity activity) {
        String activityClassName = activity.getClass().getName();
        String splitName = splitActivityNameCache.get(activityClassName);
        if (splitName == null) {
            splitName = AABExtension.getInstance().getSplitNameForActivityName(activityClassName);
            splitName = (splitName == null ? SPLIT_NAME_BASE : splitName);
            splitActivityNameCache.put(activityClassName, splitName);
        }
        return splitName;
    }

    /**
     * Called when split Activity calls {@link Activity#onCreate super.onCreate()}.
     */
    public abstract void onSplitActivityCreated(@NonNull String splitName, @NonNull Activity activity, @Nullable Bundle savedInstanceState);

    /**
     * Called when split Activity calls {@link Activity#onStart super.onStart()}.
     */
    public abstract void onSplitActivityStarted(@NonNull String splitName, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onResume super.onResume()}.
     */
    public abstract void onSplitActivityResumed(@NonNull String splitName, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onPause super.onPause()}.
     */
    public abstract void onSplitActivityPaused(@NonNull String splitName, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onStop super.onStop()}.
     */
    public abstract void onSplitActivityStopped(@NonNull String splitName, @NonNull Activity activity);

    /**
     * Called when split Activity calls
     * {@link Activity#onSaveInstanceState super.onSaveInstanceState()}.
     */
    public abstract void onSplitActivitySaveInstanceState(@NonNull String splitName, @NonNull Activity activity, @NonNull Bundle outState);

    /**
     * Called when split Activity calls {@link Activity#onDestroy super.onDestroy()}.
     */
    public abstract void onSplitActivityDestroyed(@NonNull String splitName, @NonNull Activity activity);

}
