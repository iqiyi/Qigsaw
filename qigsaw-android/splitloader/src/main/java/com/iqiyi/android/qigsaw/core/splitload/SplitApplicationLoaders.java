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
