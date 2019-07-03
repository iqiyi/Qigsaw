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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;


import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

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
    @SuppressLint("PrivateApi")
    public static void loadResources(Activity activity, Resources preResources) throws Throwable {
        checkOrUpdateResources(activity, preResources);
    }

    public static void loadResources(Service service) throws Throwable {
        checkOrUpdateResources(service, service.getBaseContext().getResources());
    }

    //Todo:: remove receiver param
    public static void loadResources(BroadcastReceiver receiver, Context context, Resources resources) throws Throwable {
        checkOrUpdateResources(context, resources);
    }

    static void loadResources(Context context, Resources preResources, String splitApkPath) throws Throwable {
        List<String> loadedResDirs = getLoadedResourcesDirs(preResources.getAssets());
        if (!loadedResDirs.contains(splitApkPath)) {
            installSplitResDirs(context, preResources, splitApkPath);
            SplitLog.d(TAG, "Install split %s resources for application.", splitApkPath);
        }
    }

    private static void checkOrUpdateResources(Context context, Resources resources) throws Throwable {
        List<String> loadedResDirsInAsset = getLoadedResourcesDirs(resources.getAssets());
        Collection<String> loadedSplitPath = getLoadedSplitPaths();
        if (loadedSplitPath != null) {
            if (!loadedResDirsInAsset.containsAll(loadedSplitPath)) {
                for (String splitPath : loadedSplitPath) {
                    if (!loadedResDirsInAsset.contains(splitPath)) {
                        installSplitResDirs(context, resources, splitPath);
                    }
                }
            }
        }
    }

    private static Collection<String> getLoadedSplitPaths() {
        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
        if (loadManager != null) {
            Set<Split> splits = loadManager.getLoadedSplits();
            if (!splits.isEmpty()) {
                Collection<String> loadedSplits = new ArrayList<>();
                for (Split split : splits) {
                    loadedSplits.add(split.splitApkPath);
                }
                return loadedSplits;
            }
        }
        return null;
    }

    private static void installSplitResDirs(final Context context, final Resources resources, final String splitFileDir) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            V21.installSplitResDirs(resources, splitFileDir);
        } else {
            //run on UI Thread
            //some rom like zte 4.2.2, fetching @ActivityThread instance in work thread will return null.
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                SplitLog.i(TAG, "Install res on main thread");
                V14.installSplitResDirs(context, resources, splitFileDir);
            } else {
                synchronized (sLock) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (sLock) {
                                try {
                                    V14.installSplitResDirs(context, resources, splitFileDir);
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
            Object[] appStringBlocks = (Object[]) VersionCompat.getmStringBlocksField().get(asset);
            int totalResCount = appStringBlocks.length;
            for (int appResIndex = 1; appResIndex <= totalResCount; ++appResIndex) {
                String inApp = (String) VersionCompat.getGetCookieNameMethod().invoke(asset, appResIndex);
                existedAppResDirList.add(inApp);
            }
        }
        return existedAppResDirList;
    }

    private static class V21 extends VersionCompat {

        private static void installSplitResDirs(Resources preResources, String splitFileDir) throws Throwable {
            Method method = VersionCompat.getAddAssetPathMethod();
            method.invoke(preResources.getAssets(), splitFileDir);
        }
    }


    private static class V14 extends VersionCompat {

        private static void installSplitResDirs(Context context, Resources preResources, String splitApkPath) throws Throwable {
            String appResDir = context.getPackageResourcePath();
            AssetManager oldAsset = preResources.getAssets();
            List<String> resDirs = getAppResDirs(appResDir, oldAsset);
            resDirs.add(splitApkPath);
            AssetManager newAsset = createAssetManager();
            for (String recent : resDirs) {
                int ret = (int) getAddAssetPathMethod().invoke(newAsset, recent);
                if (ret == 0) {
                    throw new RuntimeException("invoke addAssetPath failure! apk format maybe incorrect");
                }
            }
            getmStringBlocksField().set(newAsset, null);
            getEnsureStringBlocksMethod().invoke(newAsset);
            getmAssetsField().set(preResources, newAsset);
            clearPreloadTypedArrayIssue(preResources);
            preResources.updateConfiguration(preResources.getConfiguration(), preResources.getDisplayMetrics());
        }

        private static List<String> getAppResDirs(String appResDir, AssetManager asset) throws NoSuchFieldException,
                IllegalAccessException, NoSuchMethodException, InvocationTargetException {
            List<String> existedAppResDirList;
            AssetManager sysAsset = Resources.getSystem().getAssets();
            Object[] sysStringBlocks = (Object[]) getmStringBlocksField().get(sysAsset);
            Object[] appStringBlocks = (Object[]) getmStringBlocksField().get(asset);
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

        /**
         * Why must I do these?
         * Resource has mTypedArrayPool field, which just like Message Poll to reduce gc
         * MiuiResource change TypedArray to MiuiTypedArray, but it getInstance string block from offset instead of assetManager
         */
        private static void clearPreloadTypedArrayIssue(Resources resources) {
            // Perform this trick not only in Miui system since we can't predict if any other
            // manufacturer would do the same modification to Android.
            // if (!isMiuiSystem) {
            //     return;
            // }
            SplitLog.w(TAG, "try to clear typedArray cache!");
            // Clear typedArray cache.
            try {
                final Field typedArrayPoolField = getmTypedArrayPoolField();
                final Object origTypedArrayPool = typedArrayPoolField.get(resources);
                final Method acquireMethod = HiddenApiReflection.findMethod(origTypedArrayPool, "acquire");
                while (true) {
                    if (acquireMethod.invoke(origTypedArrayPool) == null) {
                        break;
                    }
                }
            } catch (Throwable e) {
                SplitLog.e(TAG, "clearPreloadTypedArrayIssue failed, ignore error: " + e);
            }
        }

        private static AssetManager createAssetManager() throws IllegalAccessException, InstantiationException {
            return AssetManager.class.newInstance();
        }
    }

    private static abstract class VersionCompat {

        private static Field mStringBlocksField;

        private static Field mAssetsField;

        private static Field mTypedArrayPoolField;

        private static Method addAssetPathMethod;

        private static Method ensureStringBlocksMethod;

        private static Method getCookieNameMethod;

        private static Method getAssetPathMethod;

        private static Method getApkAssetsMethod;

        static Field getmStringBlocksField() throws NoSuchFieldException {
            if (mStringBlocksField == null) {
                mStringBlocksField = HiddenApiReflection.findField(AssetManager.class, "mStringBlocks");
            }
            return mStringBlocksField;
        }

        static Field getmAssetsField() throws NoSuchFieldException {
            if (mAssetsField == null) {
                mAssetsField = HiddenApiReflection.findField(Resources.class, "mAssets");
            }
            return mAssetsField;
        }

        static Field getmTypedArrayPoolField() throws NoSuchFieldException {
            if (mTypedArrayPoolField == null) {
                mTypedArrayPoolField = HiddenApiReflection.findField(Resources.class, "mTypedArrayPool");
            }
            return mTypedArrayPoolField;
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

        static Method getEnsureStringBlocksMethod() throws NoSuchMethodException {
            if (ensureStringBlocksMethod == null) {
                ensureStringBlocksMethod = HiddenApiReflection.findMethod(AssetManager.class, "ensureStringBlocks");
            }
            return ensureStringBlocksMethod;
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
