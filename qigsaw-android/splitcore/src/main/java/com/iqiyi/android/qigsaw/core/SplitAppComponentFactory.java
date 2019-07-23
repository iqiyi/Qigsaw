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
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;

@RequiresApi(api = Build.VERSION_CODES.P)
public class SplitAppComponentFactory extends AppComponentFactory {

    private static final String TAG = "SplitAppComponentFactory";

    @NonNull
    @CallSuper
    @Override
    public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.instantiateActivity(cl, className, intent);
        } catch (ClassNotFoundException error) {
            Pair<String, Class<?>> result = AABExtension.getInstance().getSplitNameForComponent(className);
            if (result != null) {
                if (SplitLoadManagerService.hasInstance()) {
                    SplitLog.w(TAG, "class %s is not found", className);
                    SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                    loadManager.loadInstalledSplits();
                    if (loadManager.getLoadedSplitNames().contains(result.first)) {
                        try {
                            return super.instantiateActivity(cl, className, intent);
                        } catch (ClassNotFoundException e) {
                            SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                            return super.instantiateActivity(cl, result.second.getName(), intent);
                        }
                    } else {
                        SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                        return super.instantiateActivity(cl, result.second.getName(), intent);
                    }
                } else {
                    SplitLog.e(TAG, "SplitLoadManagerService has not been created!");
                }
            }
            throw error;
        }
    }

    @NonNull
    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.instantiateApplication(cl, className);
    }

    @NonNull
    @CallSuper
    @Override
    public BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.instantiateReceiver(cl, className, intent);
        } catch (ClassNotFoundException error) {
            Pair<String, Class<?>> result = AABExtension.getInstance().getSplitNameForComponent(className);
            if (result != null) {
                if (SplitLoadManagerService.hasInstance()) {
                    SplitLog.w(TAG, "class %s is not found", className);
                    SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                    loadManager.loadInstalledSplits();
                    if (loadManager.getLoadedSplitNames().contains(result.first)) {
                        try {
                            return super.instantiateReceiver(cl, className, intent);
                        } catch (ClassNotFoundException e) {
                            SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                            return super.instantiateReceiver(cl, result.second.getName(), intent);
                        }
                    } else {
                        SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                        return super.instantiateReceiver(cl, result.second.getName(), intent);
                    }
                } else {
                    SplitLog.e(TAG, "SplitLoadManagerService has not been created!");
                }
            }
            throw error;
        }
    }

    @NonNull
    @Override
    public ContentProvider instantiateProvider(@NonNull ClassLoader cl, @NonNull String className)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.instantiateProvider(cl, className);
    }

    @NonNull
    @CallSuper
    @Override
    public Service instantiateService(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.instantiateService(cl, className, intent);
        } catch (ClassNotFoundException error) {
            Pair<String, Class<?>> result = AABExtension.getInstance().getSplitNameForComponent(className);
            if (result != null) {
                if (SplitLoadManagerService.hasInstance()) {
                    SplitLog.w(TAG, "class %s is not found", className);
                    SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                    loadManager.loadInstalledSplits();
                    if (loadManager.getLoadedSplitNames().contains(result.first)) {
                        try {
                            return super.instantiateService(cl, className, intent);
                        } catch (ClassNotFoundException e) {
                            SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                            return super.instantiateService(cl, result.second.getName(), intent);
                        }
                    } else {
                        SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", className, result.second.getSimpleName());
                        return super.instantiateService(cl, result.second.getName(), intent);
                    }
                } else {
                    SplitLog.e(TAG, "SplitLoadManagerService has not been created!");
                }
            }
            throw error;
        }
    }

}
