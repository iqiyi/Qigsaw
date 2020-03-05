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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class SplitApplicationLoaders {

    private final Set<SplitDexClassLoader> splitDexClassLoaders = Collections.newSetFromMap(new ConcurrentHashMap<SplitDexClassLoader, Boolean>());

    private static final AtomicReference<SplitApplicationLoaders> sInstance = new AtomicReference<>();

    public static SplitApplicationLoaders getInstance() {
        if (sInstance.get() == null) {
            sInstance.set(new SplitApplicationLoaders());
        }
        return sInstance.get();
    }

    void addClassLoader(@NonNull SplitDexClassLoader classLoader) {
        splitDexClassLoaders.add(classLoader);
    }

    Set<SplitDexClassLoader> getClassLoaders() {
        return splitDexClassLoaders;
    }

    boolean removeClassLoader(@NonNull SplitDexClassLoader classLoader) {
        return splitDexClassLoaders.remove(classLoader);
    }

    @Nullable
    Set<SplitDexClassLoader> getClassLoaders(@Nullable List<String> moduleNames) {
        if (moduleNames == null) {
            return null;
        }
        Set<SplitDexClassLoader> loaders = new HashSet<>(moduleNames.size());
        for (SplitDexClassLoader classLoader : splitDexClassLoaders) {
            if (moduleNames.contains(classLoader.moduleName())) {
                loaders.add(classLoader);
            }
        }
        return loaders;
    }

    @Nullable
    SplitDexClassLoader getClassLoader(String moduleName) {
        for (SplitDexClassLoader classLoader : splitDexClassLoaders) {
            if (classLoader.moduleName().equals(moduleName)) {
                return classLoader;
            }
        }
        return null;
    }

}
