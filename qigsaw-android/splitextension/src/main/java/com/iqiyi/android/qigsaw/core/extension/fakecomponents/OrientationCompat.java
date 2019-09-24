package com.iqiyi.android.qigsaw.core.extension.fakecomponents;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class OrientationCompat {

    static int releaseFixedOrientation(Activity activity) {
        int orientation = -1;
        if (Build.VERSION.SDK_INT == 26 && activity.getApplicationInfo().targetSdkVersion > 26
                && isTranslucentOrFloating(activity) && isFixedOrientation(activity)) {
            try {
                Field activityInfoField = Activity.class.getDeclaredField("mActivityInfo");
                activityInfoField.setAccessible(true);
                Object obj = activityInfoField.get(activity);
                Field screenOrientationFiled = ActivityInfo.class.getDeclaredField("screenOrientation");
                screenOrientationFiled.setAccessible(true);
                orientation = screenOrientationFiled.getInt(obj);
                if (orientation != -1) {
                    screenOrientationFiled.setInt(obj, -1);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                //
            }
        }
        return orientation;
    }

    static void fixedOrientation(Activity activity, int orientation) {
        if (orientation != -1) {
            if (Build.VERSION.SDK_INT == 26 && activity.getApplicationInfo().targetSdkVersion > 26
                    && isTranslucentOrFloating(activity) && !isFixedOrientation(activity)) {
                try {
                    Field activityInfoField = Activity.class.getDeclaredField("mActivityInfo");
                    activityInfoField.setAccessible(true);
                    Object obj = activityInfoField.get(activity);
                    Field screenOrientationFile = ActivityInfo.class.getDeclaredField("screenOrientation");
                    screenOrientationFile.setAccessible(true);
                    int screenOrientation = screenOrientationFile.getInt(obj);
                    if (screenOrientation == -1) {
                        screenOrientationFile.setInt(obj, orientation);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    //
                }
            }
        }
    }

    @SuppressLint("PrivateApi")
    private static boolean isTranslucentOrFloating(Activity activity) {
        try {
            Class clazz = Class.forName("com.android.internal.R$styleable");
            Field window = clazz.getDeclaredField("Window");
            window.setAccessible(true);
            TypedArray attributes = activity.obtainStyledAttributes((int[]) window.get(null));
            Field Window_windowIsTranslucent = clazz.getDeclaredField("Window_windowIsTranslucent");
            Window_windowIsTranslucent.setAccessible(true);
            Field Window_windowSwipeToDismiss = clazz.getDeclaredField("Window_windowSwipeToDismiss");
            Window_windowSwipeToDismiss.setAccessible(true);
            Field Window_windowIsFloating = clazz.getDeclaredField("Window_windowIsFloating");
            Window_windowIsFloating.setAccessible(true);
            boolean isTranslucent
                    = attributes.getBoolean((Integer) Window_windowIsTranslucent.get(null), false);
            boolean isSwipeToDismiss = !attributes.hasValue((Integer) Window_windowIsTranslucent.get(null))
                    && attributes.getBoolean((Integer) Window_windowSwipeToDismiss.get(null), false);
            boolean isFloating
                    = attributes.getBoolean((Integer) Window_windowIsFloating.get(null), false);
            return isFloating || isTranslucent || isSwipeToDismiss;

        } catch (ClassNotFoundException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (NoSuchFieldException e) {
            //
        }
        return false;
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private static boolean isFixedOrientation(Activity activity) {
        boolean isFixedOrientation = false;
        try {
            Field activityInfoField = Activity.class.getDeclaredField("mActivityInfo");
            activityInfoField.setAccessible(true);
            Object obj = activityInfoField.get(activity);
            Method isFixedOrientationMethod = ActivityInfo.class.getDeclaredMethod("isFixedOrientation");
            isFixedOrientationMethod.setAccessible(true);
            isFixedOrientation = (Boolean) isFixedOrientationMethod.invoke(obj);
        } catch (NoSuchMethodException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (InvocationTargetException e) {
            //
        } catch (NoSuchFieldException e) {
            //
        }

        return isFixedOrientation;
    }

}
