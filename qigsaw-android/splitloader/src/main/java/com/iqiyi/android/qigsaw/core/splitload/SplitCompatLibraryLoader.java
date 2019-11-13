package com.iqiyi.android.qigsaw.core.splitload;

import android.os.Build;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Source code from Tinker
 */
final class SplitCompatLibraryLoader {

    private static final String TAG = "SplitCompatLibraryLoader";

    /**
     * All versions of load logic follow these rules:
     * 1. If path of {@code folder} is not injected into the classloader, inject it to the
     * beginning of pathList in the classloader.
     * <p>
     * 2. Otherwise remove path of {@code folder} first, then re-inject it to the
     * beginning of pathList in the classloader.
     */
    static void load(ClassLoader classLoader, File folder)
            throws Throwable {
        if (folder == null || !folder.exists()) {
            SplitLog.e(TAG, "load, folder %s is illegal", folder);
            return;
        }
        // android o sdk_int 26
        // for android o preview sdk_int 25
        if ((Build.VERSION.SDK_INT == 25 && Build.VERSION.PREVIEW_SDK_INT != 0)
                || Build.VERSION.SDK_INT > 25) {
            try {
                V25.load(classLoader, folder);
            } catch (Throwable throwable) {
                // load fail, try to treat it as v23
                // some preview N version may go here
                SplitLog.e(TAG, "load, v25 fail, sdk: %d, error: %s, try to fallback to V23",
                        Build.VERSION.SDK_INT, throwable.getMessage());
                V23.load(classLoader, folder);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            try {
                V23.load(classLoader, folder);
            } catch (Throwable throwable) {
                // load fail, try to treat it as v14
                SplitLog.e(TAG, "load, v23 fail, sdk: %d, error: %s, try to fallback to V14",
                        Build.VERSION.SDK_INT, throwable.getMessage());

                V14.load(classLoader, folder);
            }
        } else if (Build.VERSION.SDK_INT >= 14) {
            V14.load(classLoader, folder);
        } else {
            throw new UnsupportedOperationException("don't support under SDK version 14!");
        }
    }

    private static final class V14 {
        private static void load(ClassLoader classLoader, File folder) throws Throwable {
            final Field pathListField = HiddenApiReflection.findField(classLoader, "pathList");
            final Object dexPathList = pathListField.get(classLoader);

            final Field nativeLibDirField = HiddenApiReflection.findField(dexPathList, "nativeLibraryDirectories");
            final File[] origNativeLibDirs = (File[]) nativeLibDirField.get(dexPathList);

            final List<File> newNativeLibDirList = new ArrayList<>(origNativeLibDirs.length + 1);
            newNativeLibDirList.add(folder);
            for (File origNativeLibDir : origNativeLibDirs) {
                if (!folder.equals(origNativeLibDir)) {
                    newNativeLibDirList.add(origNativeLibDir);
                }
            }
            nativeLibDirField.set(dexPathList, newNativeLibDirList.toArray(new File[0]));
        }
    }

    private static final class V23 {
        private static void load(ClassLoader classLoader, File folder) throws Throwable {
            final Field pathListField = HiddenApiReflection.findField(classLoader, "pathList");
            final Object dexPathList = pathListField.get(classLoader);

            final Field nativeLibraryDirectories = HiddenApiReflection.findField(dexPathList, "nativeLibraryDirectories");

            List<File> origLibDirs = (List<File>) nativeLibraryDirectories.get(dexPathList);
            if (origLibDirs == null) {
                origLibDirs = new ArrayList<>(2);
            }
            final Iterator<File> libDirIt = origLibDirs.iterator();
            while (libDirIt.hasNext()) {
                final File libDir = libDirIt.next();
                if (folder.equals(libDir)) {
                    libDirIt.remove();
                    break;
                }
            }
            origLibDirs.add(0, folder);

            final Field systemNativeLibraryDirectories = HiddenApiReflection.findField(dexPathList, "systemNativeLibraryDirectories");
            List<File> origSystemLibDirs = (List<File>) systemNativeLibraryDirectories.get(dexPathList);
            if (origSystemLibDirs == null) {
                origSystemLibDirs = new ArrayList<>(2);
            }

            final List<File> newLibDirs = new ArrayList<>(origLibDirs.size() + origSystemLibDirs.size() + 1);
            newLibDirs.addAll(origLibDirs);
            newLibDirs.addAll(origSystemLibDirs);

            final Method makeElements = HiddenApiReflection.findMethod(dexPathList,
                    "makePathElements", List.class, File.class, List.class);
            final ArrayList<IOException> suppressedExceptions = new ArrayList<>();

            final Object[] elements = (Object[]) makeElements.invoke(dexPathList, newLibDirs, null, suppressedExceptions);

            final Field nativeLibraryPathElements = HiddenApiReflection.findField(dexPathList, "nativeLibraryPathElements");
            nativeLibraryPathElements.set(dexPathList, elements);
        }
    }

    private static final class V25 {
        private static void load(ClassLoader classLoader, File folder) throws Throwable {
            final Field pathListField = HiddenApiReflection.findField(classLoader, "pathList");
            final Object dexPathList = pathListField.get(classLoader);

            final Field nativeLibraryDirectories = HiddenApiReflection.findField(dexPathList, "nativeLibraryDirectories");

            List<File> origLibDirs = (List<File>) nativeLibraryDirectories.get(dexPathList);
            if (origLibDirs == null) {
                origLibDirs = new ArrayList<>(2);
            }
            final Iterator<File> libDirIt = origLibDirs.iterator();
            while (libDirIt.hasNext()) {
                final File libDir = libDirIt.next();
                if (folder.equals(libDir)) {
                    libDirIt.remove();
                    break;
                }
            }
            origLibDirs.add(0, folder);
            final Field systemNativeLibraryDirectories = HiddenApiReflection.findField(dexPathList, "systemNativeLibraryDirectories");
            List<File> origSystemLibDirs = (List<File>) systemNativeLibraryDirectories.get(dexPathList);
            if (origSystemLibDirs == null) {
                origSystemLibDirs = new ArrayList<>(2);
            }

            final List<File> newLibDirs = new ArrayList<>(origLibDirs.size() + origSystemLibDirs.size() + 1);
            newLibDirs.addAll(origLibDirs);
            newLibDirs.addAll(origSystemLibDirs);

            final Method makeElements = HiddenApiReflection.findMethod(dexPathList, "makePathElements", List.class);

            final Object[] elements = (Object[]) makeElements.invoke(dexPathList, newLibDirs);

            final Field nativeLibraryPathElements = HiddenApiReflection.findField(dexPathList, "nativeLibraryPathElements");
            nativeLibraryPathElements.set(dexPathList, elements);
        }
    }
}
