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

package com.iqiyi.android.qigsaw.core.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitBaseInfoProvider {

    private static final String TAG = "SplitBaseInfoProvider";

    private static final String CLASS_QigsawConfig = ".QigsawConfig";

    private static final String VERSION_NAME = "VERSION_NAME";

    private static final String DYNAMIC_FEATURES = "DYNAMIC_FEATURES";

    private static final String DEFAULT_SPLIT_INFO_VERSION = "DEFAULT_SPLIT_INFO_VERSION";

    private static final String QIGSAW_ID = "QIGSAW_ID";

    private static final String QIGSAW_MODE = "QIGSAW_MODE";

    private static final String DEFAULT_VALUE = "unknown";

    private static final String DEFAULT_SPLIT_INFO_VERSION_VALUE = "unknown_1.0.0";

    private static String sPackageName;

    public static void setPackageName(String packageName) {
        sPackageName = packageName;
    }

    private static Class getQigsawConfigClass() throws ClassNotFoundException {
        try {
            return Class.forName(sPackageName + CLASS_QigsawConfig);
        } catch (ClassNotFoundException e) {
            SplitLog.w(TAG, "Qigsaw Warning: Can't find class " + sPackageName + ".QigsawConfig.class!");
            throw e;
        }
    }

    public static boolean isQigsawMode() {
        try {
            Field field = getQigsawConfigClass().getField(QIGSAW_MODE);
            field.setAccessible(true);
            return (boolean) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            //
        }
        return false;
    }

    @NonNull
    public static String getVersionName() {
        try {
            Field field = getQigsawConfigClass().getField(VERSION_NAME);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            //
        }
        return DEFAULT_VALUE;
    }

    @NonNull
    public static String getQigsawId() {
        try {
            Field field = getQigsawConfigClass().getField(QIGSAW_ID);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            //
        }
        return DEFAULT_VALUE;
    }

    @NonNull
    public static String getDefaultSplitInfoVersion() {
        try {
            Field field = getQigsawConfigClass().getField(DEFAULT_SPLIT_INFO_VERSION);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            //
        }
        return DEFAULT_SPLIT_INFO_VERSION_VALUE;
    }

    @Nullable
    public static String[] getDynamicFeatures() {
        try {
            Field field = getQigsawConfigClass().getField(DYNAMIC_FEATURES);
            field.setAccessible(true);
            return (String[]) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            //
        }
        return null;
    }


}
