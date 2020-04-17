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

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.common.SplitAABInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.fakecomponents.FakeActivity;
import com.iqiyi.android.qigsaw.core.extension.fakecomponents.FakeReceiver;
import com.iqiyi.android.qigsaw.core.extension.fakecomponents.FakeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Extension of Android App Bundles function.
 * AAB don't support {@link Application} and {@link android.content.ContentProvider} for dynamic feature,
 * but sometimes before an activity or service of dynamic feature is launched, we need initialize some SDKs.
 * AABExtension provides interfaces to create its application.
 */
@RestrictTo(LIBRARY_GROUP)
public class AABExtension {

    private static final String TAG = "Split:AABExtension";

    private static final AtomicReference<AABExtension> sAABCompatReference = new AtomicReference<>(null);

    private final Map<String, List<ContentProviderProxy>> sSplitContentProviderMap = new HashMap<>();

    private final AABExtensionManager extensionManager;

    private final List<Application> aabApplications = new ArrayList<>();

    private AABExtension() {
        Set<String> splitNames = getSplitNames();
        this.extensionManager = new AABExtensionManagerImpl(new SplitComponentInfoProvider(splitNames));
    }

    public static AABExtension getInstance() {
        if (sAABCompatReference.get() == null) {
            sAABCompatReference.set(new AABExtension());
        }
        return sAABCompatReference.get();
    }

    public void createAndActiveSplitApplication(Context appContext, boolean qigsawMode) {
        if (qigsawMode) {
            return;
        }
        final Set<String> aabLoadedSplits = new SplitAABInfoProvider(appContext).getInstalledSplitsForAAB();
        if (!aabLoadedSplits.isEmpty()) {
            for (String splitName : aabLoadedSplits) {
                try {
                    Application app = createApplication(AABExtension.class.getClassLoader(), splitName);
                    if (app != null) {
                        activeApplication(app, appContext);
                        aabApplications.add(app);
                    }
                } catch (AABExtensionException e) {
                    SplitLog.w(TAG, "Failed to create " + splitName + " application", e);
                }
            }
        }
    }

    public void onApplicationCreate() {
        if (!aabApplications.isEmpty()) {
            for (Application application : aabApplications) {
                application.onCreate();
            }
        }
    }

    /**
     * Create and active split application.
     *
     * @param classLoader used to load class.
     * @param splitName   name of split.
     */
    public Application createApplication(ClassLoader classLoader, String splitName) throws AABExtensionException {
        return extensionManager.createApplication(classLoader, splitName);
    }

    public void activeApplication(Application splitApplication, Context appContext) throws AABExtensionException {
        extensionManager.activeApplication(splitApplication, appContext);
    }

    void put(String splitName, ContentProviderProxy providerProxy) {
        List<ContentProviderProxy> providerProxies = sSplitContentProviderMap.get(splitName);
        if (providerProxies == null) {
            providerProxies = new ArrayList<>();
            sSplitContentProviderMap.put(splitName, providerProxies);
        }
        providerProxies.add(providerProxy);
    }

    public void activateSplitProviders(ClassLoader classLoader, String splitName) throws AABExtensionException {
        List<ContentProviderProxy> providerProxies = sSplitContentProviderMap.get(splitName);
        if (providerProxies != null) {
            for (ContentProviderProxy providerProxy : providerProxies) {
                providerProxy.activateRealContentProvider(classLoader);
            }
        }
    }

    public Class<?> getFakeComponent(String className) {
        if (extensionManager.isSplitActivity(className)) {
            return FakeActivity.class;
        }
        if (extensionManager.isSplitService(className)) {
            return FakeService.class;
        }
        if (extensionManager.isSplitReceiver(className)) {
            return FakeReceiver.class;
        }
        return null;
    }

    public String getSplitNameForActivityName(@NonNull String activityClassName) {
        String ret = null;
        Map<String, List<String>> splitActivitiesMap = extensionManager.getSplitActivitiesMap();
        for (Map.Entry<String, List<String>> entry : splitActivitiesMap.entrySet()) {
            String splitName = entry.getKey();
            List<String> activities = entry.getValue();
            if (activities != null && activities.contains(activityClassName)) {
                ret = splitName;
                break;
            }
        }
        return ret;
    }

    private Set<String> getSplitNames() {
        String[] dynamicFeatures = SplitBaseInfoProvider.getDynamicFeatures();
        Set<String> modules = new HashSet<>();
        if (dynamicFeatures != null && dynamicFeatures.length > 0) {
            modules.addAll(Arrays.asList(dynamicFeatures));
        }
        return modules;
    }

}
