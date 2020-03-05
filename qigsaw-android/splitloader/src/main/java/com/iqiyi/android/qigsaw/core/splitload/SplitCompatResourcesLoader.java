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

package com.iqiyi.android.qigsaw.core.splitload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Developer is forbidden to use this Class.
 */
@RestrictTo(LIBRARY_GROUP)
public class SplitCompatResourcesLoader {

    private static final String TAG = "SplitCompatResourcesLoader";

    private static final Object sLock = new Object();

    /**
     * Check if split res dir has been added into {@link Resources}, this method should be invoked in {@link Activity#getResources()}.
     * After Android 7.0, WebView.apk resources is added dynamically.
     */
    public static void loadResources(Context context, Resources resources) throws Throwable {
        checkOrUpdateResources(context, resources);
    }

    static void loadResources(Context context, Resources preResources, String splitApkPath) throws Throwable {
        List<String> loadedResDirs = getLoadedResourcesDirs(preResources.getAssets());
        if (!loadedResDirs.contains(splitApkPath)) {
            installSplitResDirs(context, preResources, Collections.singletonList(splitApkPath));
            SplitLog.d(TAG, "Install split %s resources for application.", splitApkPath);
        }
    }

    private static void checkOrUpdateResources(Context context, Resources resources) throws SplitCompatResourcesException {
        List<String> loadedResDirsInAsset;
        try {
            loadedResDirsInAsset = getLoadedResourcesDirs(resources.getAssets());
        } catch (Throwable e) {
            throw new SplitCompatResourcesException("Failed to get all loaded split resources for " + context.getClass().getName(), e);
        }
        Collection<String> loadedSplitPaths = getLoadedSplitPaths();
        if (loadedSplitPaths != null && !loadedSplitPaths.isEmpty()) {
            if (!loadedResDirsInAsset.containsAll(loadedSplitPaths)) {
                List<String> unloadedSplitPaths = new ArrayList<>();
                for (String splitPath : loadedSplitPaths) {
                    if (!loadedResDirsInAsset.contains(splitPath)) {
                        unloadedSplitPaths.add(splitPath);
                    }
                }
                try {
                    installSplitResDirs(context, resources, unloadedSplitPaths);
                } catch (Throwable e) {
                    throw new SplitCompatResourcesException("Failed to install resources " + unloadedSplitPaths.toString() + " for " + context.getClass().getName(), e);
                }
            }
        }
    }

    private static Collection<String> getLoadedSplitPaths() {
        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
        if (loadManager != null) {
            return loadManager.getLoadedSplitApkPaths();
        }
        return null;
    }

    private static void installSplitResDirs(final Context context, final Resources resources, final List<String> splitResPaths) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            V21.installSplitResDirs(resources, splitResPaths);
        } else {
            //run on UI Thread
            //some rom like zte 4.2.2, fetching @ActivityThread instance in work thread will return null.
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                SplitLog.i(TAG, "Install res on main thread");
                V14.installSplitResDirs(context, resources, splitResPaths);
            } else {
                synchronized (sLock) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (sLock) {
                                try {
                                    V14.installSplitResDirs(context, resources, splitResPaths);
                                } catch (Throwable throwable) {
                                    throw new RuntimeException(throwable);
                                }
                                sLock.notify();
                            }
                        }
                    });
                    sLock.wait();
                }
            }
        }
    }

    private static List<String> getLoadedResourcesDirs(AssetManager asset) throws NoSuchFieldException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        List<String> existedAppResDirList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 28) {
            Object[] apkAssets = (Object[]) VersionCompat.getGetApkAssetsMethod().invoke(asset);
            for (Object apkAsset : apkAssets) {
                String path = (String) VersionCompat.getGetAssetPathMethod().invoke(apkAsset);
                existedAppResDirList.add(path);
            }
        } else {
            Object[] appStringBlocks = (Object[]) VersionCompat.mStringBlocksInAssetManager().get(asset);
            int totalResCount = appStringBlocks.length;
            for (int appResIndex = 1; appResIndex <= totalResCount; ++appResIndex) {
                String inApp = (String) VersionCompat.getGetCookieNameMethod().invoke(asset, appResIndex);
                existedAppResDirList.add(inApp);
            }
        }
        return existedAppResDirList;
    }

    private static class V21 extends VersionCompat {

        private static void installSplitResDirs(Resources preResources, List<String> splitResPaths) throws Throwable {
            Method method = VersionCompat.getAddAssetPathMethod();
            for (String splitResPath : splitResPaths) {
                method.invoke(preResources.getAssets(), splitResPath);
            }
        }
    }

    private static class V14 extends VersionCompat {

        private static Context getBaseContext(Context context) {
            Context ctx = context;
            while (ctx instanceof ContextWrapper) {
                ctx = ((ContextWrapper) ctx).getBaseContext();
            }
            return ctx;
        }

        private static void checkOrUpdateResourcesForContext(Context context, Resources preResources, Resources newResources)
                throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
            //if context is a ContextThemeWrapper.
            if (context instanceof ContextThemeWrapper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Resources themeWrapperResources = (Resources) mResourcesInContextThemeWrapper().get(context);
                if (themeWrapperResources == preResources) {
                    SplitLog.i(TAG, "context %s type is @ContextThemeWrapper, and it has its own resources instance!", context.getClass().getSimpleName());
                    mResourcesInContextThemeWrapper().set(context, newResources);
                    mThemeInContextThemeWrapper().set(context, null);
                }
            }
            //find base context
            Context baseContext = getBaseContext(context);
            if (baseContext.getClass().getName().equals("android.app.ContextImpl")) {
                Resources baseContextRes = (Resources) mResourcesInContextImpl().get(baseContext);
                if (baseContextRes == preResources) {
                    mResourcesInContextImpl().set(baseContext, newResources);
                    mThemeInContentImpl().set(baseContext, null);
                }
            } else {
                //some rom customize ContextImpl for base context of Application
                try {
                    Resources baseContextRes = (Resources) HiddenApiReflection.findField(baseContext, "mResources").get(baseContext);
                    if (baseContextRes == preResources) {
                        HiddenApiReflection.findField(baseContext, "mResources").set(baseContext, newResources);
                        HiddenApiReflection.findField(baseContext, "mTheme").set(baseContext, null);
                    }
                } catch (NoSuchFieldException e) {
                    SplitLog.w(TAG, "Can not find mResources in " + baseContext.getClass().getName(), e);
                }
                Resources baseContextRes = (Resources) mResourcesInContextImpl().get(baseContext);
                if (baseContextRes == preResources) {
                    mResourcesInContextImpl().set(baseContext, newResources);
                    mThemeInContentImpl().set(baseContext, null);
                }
            }
        }

        @SuppressLint("PrivateApi")
        private static void installSplitResDirs(Context context, Resources preResources, List<String> splitResPaths) throws Throwable {
            //create a new Resources.
            Resources newResources = createResources(context, preResources, splitResPaths);
            checkOrUpdateResourcesForContext(context, preResources, newResources);
            Object activityThread = getActivityThread();
            Map<IBinder, Object> activities = (Map<IBinder, Object>) mActivitiesInActivityThread().get(activityThread);
            for (Map.Entry<IBinder, Object> entry : activities.entrySet()) {
                Object activityClientRecord = entry.getValue();
                Activity activity = (Activity) HiddenApiReflection.findField(activityClientRecord, "activity").get(activityClientRecord);
                if (context != activity) {
                    SplitLog.i(TAG, "pre-resources found in @mActivities");
                    checkOrUpdateResourcesForContext(activity, preResources, newResources);
                }
            }

            Map<Object, WeakReference<Resources>> activeResources;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activeResources = (Map<Object, WeakReference<Resources>>) mActiveResourcesInActivityThread().get(activityThread);
            } else {
                Object resourcesManager = getResourcesManager();
                activeResources = (Map<Object, WeakReference<Resources>>) mActiveResourcesInResourcesManager().get(resourcesManager);
            }
            for (Map.Entry<Object, WeakReference<Resources>> entry : activeResources.entrySet()) {
                Resources res = entry.getValue().get();
                if (res == null) {
                    continue;
                }
                if (res == preResources) {
                    activeResources.put(entry.getKey(), new WeakReference<>(newResources));
                    SplitLog.i(TAG, "pre-resources found in @mActiveResources");
                    break;
                }
            }

            Map<String, WeakReference<Object>> instance_mPackages =
                    (Map<String, WeakReference<Object>>) mPackagesInActivityThread().get(activityThread);
            for (Map.Entry<String, WeakReference<Object>> entry : instance_mPackages.entrySet()) {
                Object packageInfo = entry.getValue().get();
                if (packageInfo == null) {
                    continue;
                }
                Resources resources = (Resources) mResourcesInLoadedApk().get(packageInfo);
                if (resources == preResources) {
                    SplitLog.i(TAG, "pre-resources found in @mPackages");
                    mResourcesInLoadedApk().set(packageInfo, newResources);
                }
            }

            Map<String, WeakReference<Object>> instance_mResourcePackages =
                    (Map<String, WeakReference<Object>>) mResourcePackagesInActivityThread().get(activityThread);
            for (Map.Entry<String, WeakReference<Object>> entry : instance_mResourcePackages.entrySet()) {
                Object packageInfo = entry.getValue().get();
                if (packageInfo == null) {
                    continue;
                }
                Resources resources = (Resources) mResourcesInLoadedApk().get(packageInfo);
                if (resources == preResources) {
                    SplitLog.i(TAG, "pre-resources found in @mResourcePackages");
                    mResourcesInLoadedApk().set(packageInfo, newResources);
                }
            }
        }

        private static List<String> getAppResDirs(String appResDir, AssetManager asset) throws NoSuchFieldException,
                IllegalAccessException, NoSuchMethodException, InvocationTargetException {
            List<String> existedAppResDirList;
            AssetManager sysAsset = Resources.getSystem().getAssets();
            Object[] sysStringBlocks = (Object[]) mStringBlocksInAssetManager().get(sysAsset);
            Object[] appStringBlocks = (Object[]) mStringBlocksInAssetManager().get(asset);
            int totalResCount = appStringBlocks.length;
            int sysResCount = sysStringBlocks.length;
            existedAppResDirList = new ArrayList<>(totalResCount - sysResCount);
            for (int appResIndex = sysResCount + 1; appResIndex <= totalResCount; ++appResIndex) {
                String inApp = (String) getGetCookieNameMethod().invoke(asset, appResIndex);
                existedAppResDirList.add(inApp);
            }
            if (!existedAppResDirList.contains(appResDir)) {
                boolean inSystem = false;
                for (int i = 1; i <= sysResCount; i++) {
                    final String cookieNameSys = (String) getGetCookieNameMethod().invoke(sysAsset, i);
                    if (appResDir.equals(cookieNameSys)) {
                        inSystem = true;
                        break;
                    }
                }
                if (!inSystem) {
                    existedAppResDirList.add(0, appResDir);
                }
            }
            return existedAppResDirList;
        }

        private static Resources createResources(Context context, Resources oldRes, List<String> splitResPaths) throws NoSuchFieldException,
                IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
            String appResDir = context.getPackageResourcePath();
            AssetManager oldAsset = oldRes.getAssets();
            List<String> resDirs = getAppResDirs(appResDir, oldAsset);
            resDirs.addAll(0, splitResPaths);
            AssetManager newAsset = createAssetManager();
            for (String recent : resDirs) {
                int ret = (int) getAddAssetPathMethod().invoke(newAsset, recent);
                if (ret == 0) {
                    SplitLog.e(TAG, "Split Apk res path : " + recent);
                    throw new RuntimeException("invoke addAssetPath failure! apk format maybe incorrect");
                }
            }
            return newResources(oldRes, newAsset);
        }

        private static Resources newResources(Resources originRes, AssetManager asset)
                throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            return (Resources) HiddenApiReflection.findConstructor(originRes, AssetManager.class, DisplayMetrics.class, Configuration.class)
                    .newInstance(asset, originRes.getDisplayMetrics(), originRes.getConfiguration());
        }

        private static AssetManager createAssetManager() throws IllegalAccessException, InstantiationException {
            return AssetManager.class.newInstance();
        }
    }

    private static abstract class VersionCompat {

        private static Field mStringBlocksField;

        private static Method addAssetPathMethod;

        private static Method getCookieNameMethod;

        private static Method getAssetPathMethod;

        private static Method getApkAssetsMethod;

        private static Field mActivitiesInActivityThread;

        private static Object activityThread;

        private static Class<?> activityThreadClass;

        private static Class<?> contextImplClass;

        private static Field mResourcesInContextImpl;

        private static Field mThemeInContentImpl;

        private static Field mPackagesInActivityThread;

        private static Field mResourcePackagesInActivityThread;

        private static Field mActiveResourcesInActivityThread;

        private static Field mActiveResourcesInResourcesManager;

        private static Class<?> resourcesManagerClass;

        private static Object resourcesManager;

        private static Field mResourcesInContextThemeWrapper;

        private static Field mThemeInContextThemeWrapper;

        private static Class<?> loadedApkClass;

        private static Field mResourcesInLoadedApk;

        @SuppressLint("PrivateApi")
        static Object getActivityThread() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            if (activityThread == null) {
                activityThread = HiddenApiReflection.findMethod(getActivityThreadClass(), "currentActivityThread").invoke(null);
            }
            return activityThread;
        }

        @SuppressLint("PrivateApi")
        static Object getResourcesManager() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            if (resourcesManager == null) {
                resourcesManager = HiddenApiReflection.findMethod(getResourcesManagerClass(), "getInstance").invoke(null);
            }
            return resourcesManager;
        }

        @SuppressLint("PrivateApi")
        static Class<?> getActivityThreadClass() throws ClassNotFoundException {
            if (activityThreadClass == null) {
                activityThreadClass = Class.forName("android.app.ActivityThread");
            }
            return activityThreadClass;
        }

        @SuppressLint("PrivateApi")
        static Class<?> getResourcesManagerClass() throws ClassNotFoundException {
            if (resourcesManagerClass == null) {
                resourcesManagerClass = Class.forName("android.app.ResourcesManager");
            }
            return resourcesManagerClass;
        }

        @SuppressLint("PrivateApi")
        static Class<?> getLoadedApkClass() throws ClassNotFoundException {
            if (loadedApkClass == null) {
                loadedApkClass = Class.forName("android.app.LoadedApk");
            }
            return loadedApkClass;
        }

        @SuppressLint("PrivateApi")
        static Class<?> getContextImplClass() throws ClassNotFoundException {
            if (contextImplClass == null) {
                contextImplClass = Class.forName("android.app.ContextImpl");
            }
            return contextImplClass;
        }

        static Field mResourcesInLoadedApk() throws ClassNotFoundException, NoSuchFieldException {
            if (mResourcesInLoadedApk == null) {
                mResourcesInLoadedApk = HiddenApiReflection.findField(getLoadedApkClass(), "mResources");
            }
            return mResourcesInLoadedApk;
        }

        static Field mResourcesInContextImpl() throws ClassNotFoundException, NoSuchFieldException {
            if (mResourcesInContextImpl == null) {
                mResourcesInContextImpl = HiddenApiReflection.findField(getContextImplClass(), "mResources");
            }
            return mResourcesInContextImpl;
        }

        static Field mResourcesInContextThemeWrapper() throws NoSuchFieldException {
            if (mResourcesInContextThemeWrapper == null) {
                mResourcesInContextThemeWrapper = HiddenApiReflection.findField(ContextThemeWrapper.class, "mResources");
            }
            return mResourcesInContextThemeWrapper;
        }

        static Field mThemeInContextThemeWrapper() throws NoSuchFieldException {
            if (mThemeInContextThemeWrapper == null) {
                mThemeInContextThemeWrapper = HiddenApiReflection.findField(ContextThemeWrapper.class, "mTheme");
            }
            return mThemeInContextThemeWrapper;
        }

        static Field mThemeInContentImpl() throws ClassNotFoundException, NoSuchFieldException {
            if (mThemeInContentImpl == null) {
                mThemeInContentImpl = HiddenApiReflection.findField(getContextImplClass(), "mTheme");
            }
            return mThemeInContentImpl;
        }

        static Field mPackagesInActivityThread() throws ClassNotFoundException, NoSuchFieldException {
            if (mPackagesInActivityThread == null) {
                mPackagesInActivityThread = HiddenApiReflection.findField(getActivityThreadClass(), "mPackages");
            }
            return mPackagesInActivityThread;
        }

        static Field mActiveResourcesInActivityThread() throws ClassNotFoundException, NoSuchFieldException {
            if (mActiveResourcesInActivityThread == null) {
                mActiveResourcesInActivityThread = HiddenApiReflection.findField(getActivityThreadClass(), "mActiveResources");
            }
            return mActiveResourcesInActivityThread;
        }

        static Field mActiveResourcesInResourcesManager() throws ClassNotFoundException, NoSuchFieldException {
            if (mActiveResourcesInResourcesManager == null) {
                mActiveResourcesInResourcesManager = HiddenApiReflection.findField(getResourcesManagerClass(), "mActiveResources");
            }
            return mActiveResourcesInResourcesManager;
        }

        static Field mResourcePackagesInActivityThread() throws ClassNotFoundException, NoSuchFieldException {
            if (mResourcePackagesInActivityThread == null) {
                mResourcePackagesInActivityThread = HiddenApiReflection.findField(getActivityThreadClass(), "mResourcePackages");
            }
            return mResourcePackagesInActivityThread;
        }

        static Field mActivitiesInActivityThread() throws NoSuchFieldException, ClassNotFoundException {
            if (mActivitiesInActivityThread == null) {
                mActivitiesInActivityThread = HiddenApiReflection.findField(getActivityThreadClass(), "mActivities");
            }
            return mActivitiesInActivityThread;
        }

        static Field mStringBlocksInAssetManager() throws NoSuchFieldException {
            if (mStringBlocksField == null) {
                mStringBlocksField = HiddenApiReflection.findField(AssetManager.class, "mStringBlocks");
            }
            return mStringBlocksField;
        }

        static Method getAddAssetPathMethod() throws NoSuchMethodException {
            if (addAssetPathMethod == null) {
                addAssetPathMethod = HiddenApiReflection.findMethod(AssetManager.class, "addAssetPath", String.class);
            }
            return addAssetPathMethod;
        }

        static Method getGetCookieNameMethod() throws NoSuchMethodException {
            if (getCookieNameMethod == null) {
                getCookieNameMethod = HiddenApiReflection.findMethod(AssetManager.class, "getCookieName", int.class);
            }
            return getCookieNameMethod;
        }

        @SuppressLint("PrivateApi")
        @RequiresApi(Build.VERSION_CODES.P)
        static Method getGetAssetPathMethod() throws ClassNotFoundException, NoSuchMethodException {
            if (getAssetPathMethod == null) {
                Class clazz = Class.forName("android.content.res.ApkAssets");
                getAssetPathMethod = HiddenApiReflection.findMethod(clazz, "getAssetPath");
            }
            return getAssetPathMethod;
        }

        @RequiresApi(Build.VERSION_CODES.P)
        static Method getGetApkAssetsMethod() throws NoSuchMethodException {
            if (getApkAssetsMethod == null) {
                getApkAssetsMethod = HiddenApiReflection.findMethod(AssetManager.class, "getApkAssets");
            }
            return getApkAssetsMethod;
        }
    }
}
