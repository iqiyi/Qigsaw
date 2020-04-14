/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.android.qigsaw.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.util.LruCache;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;

public abstract class SplitActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "SplitActivityLifecycleCallbacks";

    private static final String SPLIT_NAME_BASE = "base";

    private final LruCache<String, String> splitActivityNameCache = new LruCache<>(20);

    private final LruCache<String, SplitBriefInfo> splitBriefInfoCache = new LruCache<>(10);

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityCreated(briefInfo, activity, savedInstanceState);
            SplitLog.i(TAG, "Activity %s of split %s is created.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityStarted(@NonNull Activity activity) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityStarted(briefInfo, activity);
            SplitLog.i(TAG, "Activity %s of split %s is started.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityResumed(@NonNull Activity activity) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityResumed(briefInfo, activity);
            SplitLog.i(TAG, "Activity %s of split %s is resumed.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityPaused(@NonNull Activity activity) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityPaused(briefInfo, activity);
            SplitLog.i(TAG, "Activity %s of split %s is paused.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityStopped(@NonNull Activity activity) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityStopped(briefInfo, activity);
            SplitLog.i(TAG, "Activity %s of split %s is stopped.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivitySaveInstanceState(briefInfo, activity, outState);
            SplitLog.i(TAG, "Activity %s of split %s is saving state.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void onActivityDestroyed(@NonNull Activity activity) {
        SplitBriefInfo briefInfo = getSplitBriefInfoForActivity(activity);
        if (briefInfo != null) {
            onSplitActivityDestroyed(briefInfo, activity);
            SplitLog.i(TAG, "Activity %s of split %s is destroyed.", activity.getClass().getName(), briefInfo.toString());
        }
    }

    @Nullable
    private SplitBriefInfo getSplitBriefInfoForActivity(Activity activity) {
        String splitName = getSplitNameForActivityName(activity);
        if (!SPLIT_NAME_BASE.equals(splitName)) {
            SplitBriefInfo briefInfo = splitBriefInfoCache.get(splitName);
            if (briefInfo == null) {
                SplitInfoManager infoManager = SplitInfoManagerService.getInstance();
                if (infoManager != null) {
                    SplitInfo splitInfo = infoManager.getSplitInfo(activity, splitName);
                    if (splitInfo != null) {
                        briefInfo = new SplitBriefInfo(splitInfo.getSplitName(), splitInfo.getSplitVersion(), splitInfo.isBuiltIn());
                        splitBriefInfoCache.put(splitName, briefInfo);
                    }
                }
            }
            return briefInfo;
        }
        return null;
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
    public abstract void onSplitActivityCreated(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity, @Nullable Bundle savedInstanceState);

    /**
     * Called when split Activity calls {@link Activity#onStart super.onStart()}.
     */
    public abstract void onSplitActivityStarted(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onResume super.onResume()}.
     */
    public abstract void onSplitActivityResumed(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onPause super.onPause()}.
     */
    public abstract void onSplitActivityPaused(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity);

    /**
     * Called when split Activity calls {@link Activity#onStop super.onStop()}.
     */
    public abstract void onSplitActivityStopped(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity);

    /**
     * Called when split Activity calls
     * {@link Activity#onSaveInstanceState super.onSaveInstanceState()}.
     */
    public abstract void onSplitActivitySaveInstanceState(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity, @NonNull Bundle outState);

    /**
     * Called when split Activity calls {@link Activity#onDestroy super.onDestroy()}.
     */
    public abstract void onSplitActivityDestroyed(@NonNull SplitBriefInfo briefInfo, @NonNull Activity activity);

}
