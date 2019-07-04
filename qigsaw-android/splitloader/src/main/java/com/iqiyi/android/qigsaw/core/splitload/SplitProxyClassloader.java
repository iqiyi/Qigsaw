
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

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeActivity;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeReceiver;
import com.iqiyi.android.qigsaw.core.splitload.fakecomponents.FakeService;

import dalvik.system.PathClassLoader;

final class SplitProxyClassloader extends PathClassLoader {

    private static final String TAG = "SplitDexClassloader";

    private PathClassLoader originClassLoader;

    private String lastClassNotFound = null;

    private SplitProxyClassloader(String dexPath, ClassLoader parent) {
        super(dexPath, parent);
        this.originClassLoader = (PathClassLoader) parent;
    }

    private static void reflectPackageInfoClassloader(Context appContext, ClassLoader reflectClassLoader) throws Exception {
        Context baseContext = (Context) HiddenApiReflection.findField(appContext, "mBase").get(appContext);
        Object basePackageInfo = HiddenApiReflection.findField(baseContext, "mPackageInfo").get(baseContext);
        HiddenApiReflection.findField(basePackageInfo, "mClassLoader").set(basePackageInfo, reflectClassLoader);
        Thread.currentThread().setContextClassLoader(reflectClassLoader);
    }

    static PathClassLoader inject(ClassLoader originalClassloader, Context appContext) throws Exception {
        SplitProxyClassloader classLoader = new SplitProxyClassloader("", originalClassloader);
        reflectPackageInfoClassloader(appContext, classLoader);
        return classLoader;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return originClassLoader.loadClass(name);
        } catch (ClassNotFoundException error) {
            if (lastClassNotFound == null) {
                if (AABExtension.getInstance().isSplitComponents(name)) {
                    if (SplitLoadManagerService.hasInstance()) {
                        lastClassNotFound = name;
                        SplitLog.w(TAG, "class %s is not found", name);
                        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                        loadManager.loadInstalledSplits(false);
                        return findClass(name);
                    } else {
                        SplitLog.e(TAG, "SplitLoadManagerService has not been created!", name);
                    }
                }
            } else {
                SplitLog.w(TAG, "class %s is still not found!", name);
                if (AABExtension.getInstance().isSplitActivities(name)) {
                    SplitLog.w(TAG, "Split activity %s not found, return a fake activity to avoid crash", name);
                    return FakeActivity.class;
                }
                if (AABExtension.getInstance().isSplitServices(name)) {
                    SplitLog.w(TAG, "Split service %s not found, return a fake service to avoid crash", name);
                    return FakeService.class;
                }
                if (AABExtension.getInstance().isSplitReceivers(name)) {
                    SplitLog.w(TAG, "Split receiver %s not found, return a fake receiver to avoid crash", name);
                    return FakeReceiver.class;
                }
                lastClassNotFound = null;
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
