package com.iqiyi.android.qigsaw.core.splitload;

import android.os.Build;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

final class SplitUnKnownFileTypeDexLoader {

    private static final String TAG = "SplitUnKnownFileTypeDexLoader";

    static void loadDex(ClassLoader classLoader, List<String> dexPaths, File optimizedDirectory) throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (dexPaths != null) {
                List<File> unsupportedZips = new ArrayList<>();
                for (String path : dexPaths) {
                    if (path.endsWith(SplitConstants.DOT_SO)) {
                        unsupportedZips.add(new File(path));
                    }
                }
                if (!unsupportedZips.isEmpty()) {
                    Field field_dexPathList = HiddenApiReflection.findField(classLoader, "pathList");
                    Object pathList = field_dexPathList.get(classLoader);
                    Class class_DexPathList = Class.forName("dalvik.system.DexPathList");
                    Method method_loadDexFile = HiddenApiReflection.findMethod(class_DexPathList, "loadDexFile", File.class, File.class);
                    List<Object> elements = new ArrayList<>(unsupportedZips.size());
                    for (File file : unsupportedZips) {
                        DexFile dex;
                        try {
                            dex = (DexFile) method_loadDexFile.invoke(null, file, optimizedDirectory);
                        } catch (Throwable e) {
                            if (!(e instanceof IOException)) {
                                throw e;
                            }
                            continue;
                        }
                        Class class_Element = Class.forName("dalvik.system.DexPathList$Element");
                        Object element;
                        try {
                            Constructor<?> constructor_Init = HiddenApiReflection.findConstructor(class_Element, File.class, boolean.class, File.class, DexFile.class);
                            element = constructor_Init.newInstance(file, false, file, dex);
                        } catch (NoSuchMethodException ex) {
                            try {
                                Constructor<?> constructor_Init = HiddenApiReflection.findConstructor(class_Element, File.class, File.class, DexFile.class);
                                element = constructor_Init.newInstance(file, file, dex);
                            } catch (NoSuchMethodException ex2) {
                                Constructor<?> constructor_Init = HiddenApiReflection.findConstructor(class_Element, File.class, ZipFile.class, DexFile.class);
                                try {
                                    ZipFile zipFile = new ZipFile(file);
                                    element = constructor_Init.newInstance(file, zipFile, dex);
                                } catch (IOException ioEx) {
                                    SplitLog.printErrStackTrace(TAG, ioEx, "Unable to open zip file: " + file.getAbsolutePath());
                                    continue;
                                }
                            }
                        }
                        elements.add(element);
                    }
                    if (!elements.isEmpty()) {
                        HiddenApiReflection.expandFieldArray(pathList, "dexElements", elements.toArray());
                    }
                }
            }
        }
    }
}
