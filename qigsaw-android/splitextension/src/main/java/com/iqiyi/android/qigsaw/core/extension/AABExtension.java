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
import android.content.pm.ProviderInfo;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArraySet;

import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

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
 * All {@link android.content.ContentProvider} are created after {@link Application#attachBaseContext(Context)},
 * so we need remove all unloaded splits' providers to avoid {@link ClassNotFoundException} crash.
 */
@RestrictTo(LIBRARY_GROUP)
public class AABExtension {

    private static final String TAG = "Split:AABExtension";

    private static final AtomicReference<AABExtension> sAABCompatReference = new AtomicReference<>(null);

    private final Map<String, List<ProviderInfo>> splitProviders = new HashMap<>();

    private final List<Application> splitApplications = new ArrayList<>();

    private final AABExtensionManager extensionManager;

    private final Set<String> splitNames;

    private AABExtension(Context context) {
        this.splitNames = getSplitNames();
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
     * @param loadedSplits list of loaded split name.
     */
    public void onBaseContextAttached(@NonNull Set<String> loadedSplits) {
        //remove unload split providers
        Set<String> unloadSplits = getUnloadSplits(loadedSplits);
        try {
            Map<String, List<ProviderInfo>> providers = extensionManager.removeSplitProviders(unloadSplits);
            if (providers != null && !providers.isEmpty()) {
                splitProviders.putAll(providers);
            }
        } catch (Exception e) {
            SplitLog.printErrStackTrace(TAG, e, "Failed to remove providers");
        }
        if (!loadedSplits.isEmpty()) {
            for (String splitName : loadedSplits) {
                try {
                    Application app = createApplication(splitName);
                    if (app != null) {
                        splitApplications.add(app);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

    public boolean isSplitActivities(String name) {
        return extensionManager.isSplitActivities(name);
    }

    public boolean isSplitServices(String name) {
        return extensionManager.isSplitServices(name);
    }

    public boolean isSplitReceivers(String name) {
        return extensionManager.isSplitReceivers(name);
    }

    public boolean isSplitComponents(String name) {
        if (extensionManager.isSplitActivities(name)) {
            return true;
        }
        if (extensionManager.isSplitServices(name)) {
            return true;
        }
        return extensionManager.isSplitReceivers(name);
    }

    /**
     * Install split providers.
     *
     * @param moduleName name of split.
     */
    public void installSplitProviders(String moduleName) throws AABExtensionException {
        List<ProviderInfo> providers = splitProviders.get(moduleName);
        extensionManager.installSplitProviders(providers);
        splitProviders.remove(moduleName);
    }

    private Set<String> getSplitNames() {
        String[] dynamicFeatures = SplitBaseInfoProvider.getDynamicFeatures();
        Set<String> modules = new HashSet<>();
        if (dynamicFeatures != null && dynamicFeatures.length > 0) {
            modules.addAll(Arrays.asList(dynamicFeatures));
        }
        return modules;
    }

    private Set<String> getUnloadSplits(Set<String> loadedSplits) {
        Set<String> unloadSplits = new ArraySet<>();
        if (!splitNames.isEmpty()) {
            for (String name : splitNames) {
                if (!loadedSplits.contains(name)) {
                    unloadSplits.add(name);
                }
            }
        }
        return unloadSplits;
    }


}
