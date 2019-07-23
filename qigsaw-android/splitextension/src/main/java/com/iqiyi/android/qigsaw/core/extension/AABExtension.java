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
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Pair;

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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

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

    private final List<Application> splitApplications = new ArrayList<>();

    private final Map<String, List<ContentProviderProxy>> sSplitContentProviderMap = new HashMap<>();

    private final AABExtensionManager extensionManager;

    private AABExtension(Context context) {
        Set<String> splitNames = getSplitNames();
        this.extensionManager = new AABExtensionManagerImpl(context, new SplitComponentInfoProvider(splitNames));
    }

    public static void install(Context context) {
        sAABCompatReference.compareAndSet(null, new AABExtension(context));
    }

    public static AABExtension getInstance() {
        if (sAABCompatReference.get() == null) {
            throw new RuntimeException("Have you invoke AABCompat#install(Context) method?");
        }
        return sAABCompatReference.get();
    }

    /**
     * Called when base app {@link Application#attachBaseContext(Context)} is invoked.
     *
     * @param aabLoadedSplits list of loaded split name for AAB.
     */
    public void onBaseContextAttached(@NonNull Set<String> aabLoadedSplits) {
        //remove unload split providers
        if (!aabLoadedSplits.isEmpty()) {
            for (String splitName : aabLoadedSplits) {
                try {
                    Application app = createApplication(splitName);
                    if (app != null) {
                        splitApplications.add(app);
                    }
                } catch (Exception e) {
                    SplitLog.w(TAG, "Failed to create " + splitName + " application", e);
                }
            }
        }
    }

    /**
     * Called when base app {@link Application#onCreate()} method is invoked!
     */
    public void onCreate() {
        for (Application application : splitApplications) {
            application.onCreate();
        }
    }

    /**
     * Create and active split application.
     *
     * @param splitName name of split.
     */
    public Application createApplication(String splitName) throws AABExtensionException {
        Application app = extensionManager.createApplication(splitName);
        extensionManager.activeApplication(app);
        return app;
    }

    void put(String splitName, ContentProviderProxy providerProxy) {
        List<ContentProviderProxy> providerProxies = sSplitContentProviderMap.get(splitName);
        if (providerProxies == null) {
            providerProxies = new ArrayList<>();
            sSplitContentProviderMap.put(splitName, providerProxies);
        }
        providerProxies.add(providerProxy);
    }

    public void activateSplitProviders(String splitName) throws AABExtensionException {
        List<ContentProviderProxy> providerProxies = sSplitContentProviderMap.get(splitName);
        if (providerProxies != null) {
            for (ContentProviderProxy providerProxy : providerProxies) {
                providerProxy.activateRealContentProvider();
            }
        }
    }

    public Pair<String, Class<?>> getSplitNameForComponent(String name) {
        String targetSplitName = extensionManager.getSplitNameForActivity(name);
        if (!TextUtils.isEmpty(targetSplitName)) {
            return new Pair<String, Class<?>>(targetSplitName, FakeActivity.class);
        }
        targetSplitName = extensionManager.getSplitNameForService(name);
        if (!TextUtils.isEmpty(targetSplitName)) {
            return new Pair<String, Class<?>>(targetSplitName, FakeService.class);
        }
        targetSplitName = extensionManager.getSplitNameForReceiver(name);
        if (!TextUtils.isEmpty(targetSplitName)) {
            return new Pair<String, Class<?>>(targetSplitName, FakeReceiver.class);
        }
        return null;
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
