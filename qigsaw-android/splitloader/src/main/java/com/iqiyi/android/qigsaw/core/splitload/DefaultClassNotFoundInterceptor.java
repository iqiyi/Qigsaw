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
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;

import java.util.List;
import java.util.Set;

final class DefaultClassNotFoundInterceptor implements ClassNotFoundInterceptor {

    private static final String TAG = "Split:ClassNotFound";

    private final Context context;

    private final ClassLoader originClassLoader;

    private final int splitLoadMode;

    DefaultClassNotFoundInterceptor(Context appContext, ClassLoader originClassLoader, int splitLoadMode) {
        this.context = appContext;
        this.originClassLoader = originClassLoader;
        this.splitLoadMode = splitLoadMode;
    }

    @Override
    public Class<?> findClass(String name) {
        if (SplitLoadManagerService.hasInstance()) {
            if (splitLoadMode == SplitLoad.MULTIPLE_CLASSLOADER) {
                return onClassNotFound(name);
            } else if (splitLoadMode == SplitLoad.SINGLE_CLASSLOADER) {
                return onClassNotFound2(name);
            }
        }
        return null;
    }

    private Class<?> onClassNotFound(String name) {
        Class<?> ret = findClassInSplits(name);
        if (ret != null) {
            return ret;
        }
        Class<?> fakeComponent = AABExtension.getInstance().getFakeComponent(name);
        if (fakeComponent != null || isSplitEntryFragments(name)) {
            SplitLoadManagerService.getInstance().loadInstalledSplits();
            ret = findClassInSplits(name);
            if (ret != null) {
                SplitLog.i(TAG, "Class %s is found in Splits after loading all installed splits.", name);
                return ret;
            }
            if (fakeComponent != null) {
                SplitLog.w(TAG, "Split component %s is still not found after installing all installed splits, return a %s to avoid crash", name, fakeComponent.getSimpleName());
                return fakeComponent;
            }
        }
        return null;
    }

    private boolean isSplitEntryFragments(String name) {
        SplitInfoManager infoManager = SplitInfoManagerService.getInstance();
        if (infoManager != null) {
            List<String> splitEntryFragments = infoManager.getSplitEntryFragments(context);
            if (splitEntryFragments != null && !splitEntryFragments.isEmpty()) {
                return splitEntryFragments.contains(name);
            }
        }
        return false;
    }

    private Class<?> onClassNotFound2(String name) {
        Class<?> fakeComponent = AABExtension.getInstance().getFakeComponent(name);
        if (fakeComponent != null || isSplitEntryFragments(name)) {
            SplitLoadManagerService.getInstance().loadInstalledSplits();
            try {
                return originClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                if (fakeComponent != null) {
                    SplitLog.w(TAG, "Split component %s is still not found after installing all installed splits,return a %s to avoid crash", name, fakeComponent.getSimpleName());
                    return fakeComponent;
                }
            }
        }
        return null;
    }

    private Class<?> findClassInSplits(String name) {
        Set<SplitDexClassLoader> splitDexClassLoaders = SplitApplicationLoaders.getInstance().getClassLoaders();
        for (SplitDexClassLoader classLoader : splitDexClassLoaders) {
            try {
                Class<?> clazz = classLoader.loadClassItself(name);
                SplitLog.i(TAG, "Class %s is found in %s ClassLoader", name, classLoader.moduleName());
                return clazz;
            } catch (ClassNotFoundException e) {
                SplitLog.w(TAG, "Class %s is not found in %s ClassLoader", name, classLoader.moduleName());
            }
        }
        return null;
    }
}
