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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArraySet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitApplicationLoaders {

    private final Set<SplitDexClassLoader> splitDexClassLoaders = new ArraySet<>();

    private static final AtomicReference<SplitApplicationLoaders> sInstance = new AtomicReference<>();

    private static final Object sLock = new Object();

    public static SplitApplicationLoaders getInstance() {
        if (sInstance.get() == null) {
            sInstance.set(new SplitApplicationLoaders());
        }
        return sInstance.get();
    }

    public void addClassLoader(SplitDexClassLoader classLoader) {
        synchronized (sLock) {
            splitDexClassLoaders.add(classLoader);
        }
    }

    Set<SplitDexClassLoader> getClassLoaders() {
        synchronized (sLock) {
            return splitDexClassLoaders;
        }
    }

    boolean removeClassLoader(SplitDexClassLoader classLoader) {
        synchronized (sLock) {
            return splitDexClassLoaders.remove(classLoader);
        }
    }

    @Nullable
    SplitDexClassLoader getClassLoader(String moduleName) {
        synchronized (sLock) {
            for (SplitDexClassLoader classLoader : splitDexClassLoaders) {
                if (classLoader.moduleName().equals(moduleName)) {
                    return classLoader;
                }
            }
            return null;
        }
    }

}
