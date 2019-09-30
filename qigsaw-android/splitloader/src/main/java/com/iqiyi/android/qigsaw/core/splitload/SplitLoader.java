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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Interface definition for loading installed splits.
 */
abstract class SplitLoader {

    private static final String TAG = "SplitLoader";

    /**
     * private field in {@link android.app.LoadedApk}
     */
    private Field mSplitResDirsField;

    private Object mPackageInfo;

    final Context context;

    SplitLoader(Context context) {
        this.context = context;
    }

    Context getBaseContext() {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return ctx;
    }

    /**
     * load installed split's code.
     */
    abstract SplitDexClassLoader loadCode(String moduleName,
                                          String splitApk,
                                          List<String> dexPaths,
                                          File optimizedDirectory,
                                          File librarySearchPath);

    /**
     * load resources of installed split.
     *
     * @param splitResDir local file path of split apk.
     */
    protected abstract void loadResources(String splitResDir) throws SplitLoadException;

    private Field getFieldSplitResDirsInPackageInfo(Object packageInfo) {
        try {
            mSplitResDirsField = HiddenApiReflection.findField(packageInfo, "mSplitResDirs");
        } catch (NoSuchFieldException e) {
            SplitLog.w(TAG, "Failed to reflect 'mSplitResDirs' field!");
        }
        return mSplitResDirsField;
    }

    void installSplitResourceDir(Context baseContext, String splitResDir) throws Throwable {
        Object packageInfo = getPackageInfo(baseContext);
        if (packageInfo != null) {
            Field mSplitResDirsField = getFieldSplitResDirsInPackageInfo(packageInfo);
            if (mSplitResDirsField != null) {
                String[] splitResDirs = (String[]) mSplitResDirsField.get(packageInfo);
                if (splitResDirs == null) {
                    mSplitResDirsField.set(packageInfo, new String[]{splitResDir});
                } else {
                    int splitSourceDirsLength = splitResDirs.length;
                    String[] combined = new String[splitSourceDirsLength + 1];
                    String[] expanded = new String[]{splitResDir};
                    System.arraycopy(splitResDirs, 0, combined, 0, splitResDirs.length);
                    System.arraycopy(expanded, 0, combined, splitResDirs.length, expanded.length);
                    mSplitResDirsField.set(packageInfo, combined);
                }
            }
        }
    }

    private Object getPackageInfo(Context baseContext) {
        if (mPackageInfo == null) {
            try {
                mPackageInfo = HiddenApiReflection.findField(baseContext, "mPackageInfo").get(baseContext);
            } catch (Throwable e) {
                SplitLog.w(TAG, "Failed to reflect 'mPackageInfo' instance!", e);
            }
        }
        return mPackageInfo;
    }

}
