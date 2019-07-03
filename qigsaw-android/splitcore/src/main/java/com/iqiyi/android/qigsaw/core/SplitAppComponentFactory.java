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

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeActivity;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeReceiver;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeService;

@RequiresApi(api = Build.VERSION_CODES.P)
public class SplitAppComponentFactory extends AppComponentFactory {

    private static final String TAG = "SplitAppComponentFactory";

    private String lastActivityClassNotFound = null;

    private String lastServiceClassNotFound = null;

    private String lastReceiverClassNotFound = null;

    @NonNull
    @CallSuper
    @Override
    public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, @Nullable Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.instantiateActivity(cl, className, intent);
        } catch (ClassNotFoundException error) {
            if (lastActivityClassNotFound == null) {
                if (AABExtension.getInstance().isSplitActivities(className)) {
                    if (SplitLoadManagerService.hasInstance()) {
                        lastActivityClassNotFound = className;
                        SplitLog.w(TAG, "Activity class %s is not found", className);
                        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                        loadManager.loadInstalledSplits(true);
                        return instantiateActivity(cl, className, intent);
                    } else {
                        SplitLog.e(TAG, "SplitLoadManagerService has not been created!", className);
                    }
                }
            } else {
                SplitLog.w(TAG, "Activity class %s is still not found!", className);
                if (AABExtension.getInstance().isSplitActivities(className)) {
                    SplitLog.w(TAG, "Split activity %s not found, return a fake activity to avoid crash", className);
                    return instantiateActivity(cl, FakeActivity.class.getName(), intent);
                }
                lastActivityClassNotFound = null;
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
            if (lastReceiverClassNotFound == null) {
                if (AABExtension.getInstance().isSplitReceivers(className)) {
                    if (SplitLoadManagerService.hasInstance()) {
                        lastReceiverClassNotFound = className;
                        SplitLog.w(TAG, "Receiver class %s is not found", className);
                        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                        loadManager.loadInstalledSplits(true);
                        return instantiateReceiver(cl, className, intent);
                    } else {
                        SplitLog.e(TAG, "SplitLoadManagerService has not been created!", className);
                    }
                }
            } else {
                SplitLog.w(TAG, "Receiver class %s is still not found!", className);
                if (AABExtension.getInstance().isSplitReceivers(className)) {
                    SplitLog.w(TAG, "Split receiver %s not found, return a fake receiver to avoid crash", className);
                    return instantiateReceiver(cl, FakeReceiver.class.getName(), intent);
                }
                lastReceiverClassNotFound = null;
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
            if (lastServiceClassNotFound == null) {
                if (AABExtension.getInstance().isSplitServices(className)) {
                    if (SplitLoadManagerService.hasInstance()) {
                        lastServiceClassNotFound = className;
                        SplitLog.w(TAG, "Service class %s is not found", className);
                        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                        loadManager.loadInstalledSplits(true);
                        return instantiateService(cl, className, intent);
                    } else {
                        SplitLog.e(TAG, "SplitLoadManagerService has not been created!", className);
                    }
                }
            } else {
                SplitLog.w(TAG, "Service class %s is still not found!", className);
                if (AABExtension.getInstance().isSplitServices(className)) {
                    SplitLog.w(TAG, "Split service %s not found, return a fake service to avoid crash", className);
                    return instantiateService(cl, FakeService.class.getName(), intent);
                }
                lastServiceClassNotFound = null;
            }
            throw error;
        }
    }

}
