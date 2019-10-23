package com.iqiyi.android.qigsaw.core.splitload;

import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.util.List;

import dalvik.system.BaseDexClassLoader;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitDexClassLoader extends BaseDexClassLoader {

    private static final String TAG = "SplitDexClassLoader";

    private final String moduleName;

    private SplitDexClassLoader(String moduleName,
                                String dexPath,
                                File optimizedDirectory,
                                String librarySearchPath,
                                ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.moduleName = moduleName;
    }

    public static SplitDexClassLoader create(String moduleName,
                                             List<String> dexPaths,
                                             File optimizedDirectory,
                                             File librarySearchFile) {
        String dexPath = (dexPaths == null) ? "" : TextUtils.join(File.pathSeparator, dexPaths);
        long time = System.currentTimeMillis();
        SplitDexClassLoader cl = new SplitDexClassLoader(
                moduleName,
                dexPath,
                optimizedDirectory,
                librarySearchFile == null ? null : librarySearchFile.getAbsolutePath(),
                SplitDexClassLoader.class.getClassLoader()
        );
        SplitLog.d(TAG, "Cost %d ms to load %s code", System.currentTimeMillis() - time, moduleName);
        return cl;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException error) {
            if (SplitDelegateClassloader.sInstance != null) {
                return SplitDelegateClassloader.sInstance.findClassInSplits(name, this);
            }
            throw error;
        }
    }

    String moduleName() {
        return moduleName;
    }

    Class<?> loadClassItself(String name) throws ClassNotFoundException {
        Class<?> cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }
        return super.findClass(name);
    }

}
