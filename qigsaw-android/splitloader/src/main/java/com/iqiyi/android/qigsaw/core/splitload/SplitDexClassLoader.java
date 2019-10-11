package com.iqiyi.android.qigsaw.core.splitload;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import dalvik.system.BaseDexClassLoader;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitDexClassLoader extends BaseDexClassLoader {

    private final Context appContext;

    private final String moduleName;

    private SplitDexClassLoader(Context context,
                                String moduleName,
                                String dexPath,
                                File optimizedDirectory,
                                String librarySearchPath,
                                ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.moduleName = moduleName;
        this.appContext = context;
    }

    public static SplitDexClassLoader create(Context context,
                                             String moduleName,
                                             List<String> dexPaths,
                                             File optimizedDirectory,
                                             File librarySearchFile) {
        String dexPath = (dexPaths == null) ? "" : TextUtils.join(File.pathSeparator, dexPaths);
        return new SplitDexClassLoader(
                context,
                moduleName,
                dexPath,
                optimizedDirectory,
                librarySearchFile == null ? null : librarySearchFile.getAbsolutePath(),
                SplitDexClassLoader.class.getClassLoader()
        );
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException error) {
            return appContext.getClassLoader().loadClass(name);
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
