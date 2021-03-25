package com.iqiyi.android.qigsaw.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.play.core.splitinstall.SplitInstallHelper;
import com.iqiyi.android.qigsaw.core.common.CompatBundle;

class InjectActivityResource implements Application.ActivityLifecycleCallbacks {
    private static InjectActivityResource cb;

    public static void inject(Application application) {
        if (CompatBundle.instance != null && CompatBundle.instance.injectActivityResource() && cb == null) {
            cb = new InjectActivityResource();
            application.registerActivityLifecycleCallbacks(cb);
        }
    }

    @Override
    public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        SplitInstallHelper.loadResources(activity, activity.getResources());
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
