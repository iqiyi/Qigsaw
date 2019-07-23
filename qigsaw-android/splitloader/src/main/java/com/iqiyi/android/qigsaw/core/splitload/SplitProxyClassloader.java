
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
import android.util.Pair;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;

import dalvik.system.PathClassLoader;

final class SplitProxyClassloader extends PathClassLoader {

    private static final String TAG = "SplitDexClassloader";

    private PathClassLoader originClassLoader;

    private SplitProxyClassloader(String dexPath, ClassLoader parent) {
        super(dexPath, parent);
        this.originClassLoader = (PathClassLoader) parent;
    }

    private static void reflectPackageInfoClassloader(Context baseContext, ClassLoader reflectClassLoader) throws Exception {
        Object basePackageInfo = HiddenApiReflection.findField(baseContext, "mPackageInfo").get(baseContext);
        HiddenApiReflection.findField(basePackageInfo, "mClassLoader").set(basePackageInfo, reflectClassLoader);
        Thread.currentThread().setContextClassLoader(reflectClassLoader);
    }

    static PathClassLoader inject(ClassLoader originalClassloader, Context baseContext) throws Exception {
        SplitProxyClassloader classLoader = new SplitProxyClassloader("", originalClassloader);
        reflectPackageInfoClassloader(baseContext, classLoader);
        return classLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return originClassLoader.loadClass(name);
        } catch (ClassNotFoundException error) {
            Pair<String, Class<?>> result = AABExtension.getInstance().getSplitNameForComponent(name);
            if (result != null) {
                if (SplitLoadManagerService.hasInstance()) {
                    SplitLog.w(TAG, "class %s is not found", name);
                    SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                    loadManager.loadInstalledSplits();
                    if (loadManager.getLoadedSplitNames().contains(result.first)) {
                        try {
                            return originClassLoader.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", name, result.second.getSimpleName());
                            return result.second;
                        }
                    } else {
                        SplitLog.w(TAG, "Split component %s not found, return a %s to avoid crash", name, result.second.getSimpleName());
                        return result.second;
                    }
                } else {
                    SplitLog.e(TAG, "SplitLoadManagerService has not been created!");
                }
            }
            throw error;
        }
    }

    @Override
    public String findLibrary(String name) {
        String mapLib = originClassLoader.findLibrary(name);
        if (mapLib == null || mapLib.length() == 0) {
            return super.findLibrary(name);
        }
        return mapLib;
    }
}
