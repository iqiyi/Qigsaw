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

package com.iqiyi.android.qigsaw.core.extension;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AABExtensionManagerImpl implements AABExtensionManager {

    private static final String TAG = "Split:AABExtensionManagerImpl";

    private final Context context;

    private SplitComponentInfoProvider infoProvider;

    @Nullable
    private Object activityThread;

    private List<String> splitActivities;

    private List<String> splitServices;

    private List<String> splitReceivers;

    AABExtensionManagerImpl(Context context, SplitComponentInfoProvider infoProvider) {
        this.context = context;
        this.activityThread = getActivityThread();
        this.infoProvider = infoProvider;
    }

    @Override
    public Map<String, List<ProviderInfo>> removeSplitProviders(Set<String> unLoadSplits) throws AABExtensionException {
        Throwable error;
        try {
            return removeSplitProvidersInternal(unLoadSplits);
        } catch (IllegalAccessException e) {
            error = e;
        } catch (NoSuchFieldException e) {
            error = e;
        }
        throw new AABExtensionException(error);
    }

    @Override
    public void installSplitProviders(List<ProviderInfo> providers) throws AABExtensionException {
        Throwable error = null;
        try {
            installSplitProvidersInternal(providers);
        } catch (NoSuchFieldException e) {
            error = e;
        } catch (IllegalAccessException e) {
            error = e;
        } catch (NoSuchMethodException e) {
            error = e;
        } catch (InvocationTargetException e) {
            error = e;
        }
        if (error != null) {
            throw new AABExtensionException(error);
        }
    }

    @Override
    @SuppressLint("PrivateApi")
    public Application createApplication(String splitName) throws AABExtensionException {
        Exception error = null;
        String applicationName = infoProvider.getSplitApplicationName(splitName);
        if (!TextUtils.isEmpty(applicationName)) {
            try {
                Class<?> appClass = context.getClassLoader().loadClass(applicationName);
                return (Application) appClass.newInstance();
            } catch (ClassNotFoundException e) {
                error = e;
            } catch (InstantiationException e) {
                error = e;
            } catch (IllegalAccessException e) {
                error = e;
            }
        }
        if (error != null) {
            throw new AABExtensionException(error);
        }
        return null;
    }

    @Override
    @SuppressLint("PrivateApi")
    public void activeApplication(Application app) throws AABExtensionException {
        if (app != null) {
            Exception error = null;
            try {
                Method method = Application.class.getDeclaredMethod("attach", Context.class);
                method.setAccessible(true);
                method.invoke(app, context);
            } catch (NoSuchMethodException e) {
                error = e;
            } catch (IllegalAccessException e) {
                error = e;
            } catch (InvocationTargetException e) {
                error = e;
            }
            if (error != null) {
                throw new AABExtensionException(error);
            }
        }
    }

    @Override
    public boolean isSplitActivities(String name) {
        if (getSplitActivities() != null) {
            return getSplitActivities().contains(name);
        }
        return false;
    }

    @Override
    public boolean isSplitServices(String name) {
        if (getSplitServices() != null) {
            return getSplitServices().contains(name);
        }
        return false;
    }

    @Override
    public boolean isSplitReceivers(String name) {
        if (getSplitReceivers() != null) {
            return getSplitReceivers().contains(name);
        }
        return false;
    }

    @SuppressLint("PrivateApi")
    private Object getActivityThread() {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method method = atClass.getDeclaredMethod("currentActivityThread");
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Exception e) {
            //ignored
        }
        return null;
    }

    private Map<String, List<ProviderInfo>> removeSplitProvidersInternal(Set<String> unLoadSplits) throws
            NoSuchFieldException, IllegalAccessException {
        if (!unLoadSplits.isEmpty()) {
            Map<String, List<ProviderInfo>> unloadSplitProvidersMap = new HashMap<>();
            Map<String, List<String>> providerMap = infoProvider.getSplitProviders();
            for (String name : unLoadSplits) {
                List<String> splitProviders = providerMap.get(name);
                if (splitProviders != null && !splitProviders.isEmpty()) {
                    List<ProviderInfo> providerInfos = removeSplitProvidersForApp(splitProviders);
                    unloadSplitProvidersMap.put(name, providerInfos);
                    SplitLog.i(TAG, "Success to remove providers for %s", name);
                }
            }
            return unloadSplitProvidersMap;
        }
        return null;
    }

    private void installSplitProvidersInternal(List<ProviderInfo> providers) throws NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (providers != null && !providers.isEmpty()) {
            List<ProviderInfo> appProviders = getAppProviders();
            if (activityThread != null && appProviders != null) {
                Method method = activityThread.getClass().getDeclaredMethod("installContentProviders",
                        Context.class, List.class);
                method.setAccessible(true);
                method.invoke(activityThread, context, providers);
                appProviders.addAll(providers);
            }
        }
    }

    private List<ProviderInfo> removeSplitProvidersForApp(List<String> splitProviderNames) throws NoSuchFieldException, IllegalAccessException {
        List<ProviderInfo> appProviders = getAppProviders();
        if (appProviders != null && !appProviders.isEmpty()) {
            List<ProviderInfo> splitProviders = new ArrayList<>();
            for (String providerName : splitProviderNames) {
                for (ProviderInfo info : appProviders) {
                    if (providerName.equals(info.name)) {
                        splitProviders.add(info);
                        appProviders.remove(info);
                        SplitLog.i(TAG, "Provider %s removed successfully", info.name);
                        break;
                    }
                }
            }
            return splitProviders;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ProviderInfo> getAppProviders() throws NoSuchFieldException, IllegalAccessException {
        if (activityThread != null) {
            Field boundApplicationField = activityThread.getClass().getDeclaredField("mBoundApplication");
            boundApplicationField.setAccessible(true);
            Object boundApplication = boundApplicationField.get(activityThread);
            Field providersField = boundApplication.getClass().getDeclaredField("providers");
            providersField.setAccessible(true);
            return (List<ProviderInfo>) providersField.get(boundApplication);
        }
        SplitLog.w(TAG, "Failed to get ActivityThread instance!");
        return null;
    }

    private List<String> getSplitActivities() {
        if (splitActivities == null) {
            splitActivities = infoProvider.getSplitActivities();
        }
        return splitActivities;
    }

    private List<String> getSplitServices() {
        if (splitServices == null) {
            splitServices = infoProvider.getSplitServices();
        }
        return splitServices;
    }

    private List<String> getSplitReceivers() {
        if (splitReceivers == null) {
            splitReceivers = infoProvider.getSplitReceivers();
        }
        return splitReceivers;
    }
}
