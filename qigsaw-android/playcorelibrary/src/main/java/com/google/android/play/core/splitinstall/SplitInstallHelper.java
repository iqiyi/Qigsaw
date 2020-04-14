package com.google.android.play.core.splitinstall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import androidx.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.splitload.SplitCompatResourcesLoader;
import com.iqiyi.android.qigsaw.core.splitload.SplitLibraryLoaderHelper;

import java.io.File;

public class SplitInstallHelper {

    private SplitInstallHelper() {

    }

    /**
     * Load all installed splits resources for split's activity.
     * you should not call this method manually. Qigsaw-Gradle-Plugin will inject this
     * method in {@link Activity#getResources()}.
     *
     * @param activity  split's activity instance.
     * @param resources activity resources.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void loadResources(Activity activity, Resources resources) {
        try {
            SplitCompatResourcesLoader.loadResources(activity, resources);
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load activity resources", throwable);
        }
    }

    /**
     * Load all installed splits resources for split's service.
     * you should not call this method manually. Qigsaw-Gradle-Plugin will inject this
     * method in {@link Service#getResources()}.
     *
     * @param service split's service instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void loadResources(Service service) {
        try {
            SplitCompatResourcesLoader.loadResources(service, service.getBaseContext().getResources());
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to load service resources", throwable);
        }
    }

    /**
     * Load all installed splits resources for split's receiver.
     * you should not call this method manually. Qigsaw-Gradle-Plugin will inject this
     * method in {@link BroadcastReceiver#onReceive(Context, Intent)}.
     *
     * @param receiver split's receiver instance.
     * @param context  context pass by {@link BroadcastReceiver#onReceive(Context, Intent)}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void loadResources(BroadcastReceiver receiver, Context context) {
        if (context.getClass().getSimpleName().equals("ReceiverRestrictedContext")) {
            try {
                Context base = ((ContextWrapper) context).getBaseContext();
                SplitCompatResourcesLoader.loadResources(base, context.getResources());
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to load receiver resources", throwable);
            }
        }
    }

    /**
     * Updates application info based on currently installed splits.
     */
    public static void updateAppInfo(Context context) {
        //do nothing
    }

    /**
     * Loads native library using classloader or full path if library is not available in the class path.
     *
     * @param context Under {@link com.iqiyi.android.qigsaw.core.splitload.SplitLoad#MULTIPLE_CLASSLOADER} mode,
     *                if context is {@link Application}, load split's library for base apk, otherwise for split apk.
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void loadLibrary(Context context, String libraryName) {
        if (SplitLibraryLoaderHelper.loadSplitLibrary(context, libraryName)) {
            return;
        }
        try {
            System.loadLibrary(libraryName);
        } catch (UnsatisfiedLinkError error) {
            boolean loadLibOK = false;
            try {
                String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
                String libName = System.mapLibraryName(libraryName);
                String targetLibFile = (new StringBuilder(1 + String.valueOf(nativeLibraryDir).length() + String.valueOf(libName).length())).append(nativeLibraryDir).append("/").append(libName).toString();
                if ((new File(targetLibFile)).exists()) {
                    System.load(targetLibFile);
                    loadLibOK = true;
                }
            } catch (UnsatisfiedLinkError e) {
                throw e;
            }
            if (!loadLibOK) {
                throw error;
            }
        }
    }

}
