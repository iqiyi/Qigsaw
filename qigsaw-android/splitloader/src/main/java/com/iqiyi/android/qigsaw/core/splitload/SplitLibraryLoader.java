package com.iqiyi.android.qigsaw.core.splitload;

import android.content.Context;
import android.support.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitLibraryLoader {

    public static boolean loadSplitLibrary(Context context, String libraryName) {
        if (!SplitLoadManagerService.hasInstance()) {
            return false;
        }
        if (SplitLoadManagerService.getInstance().splitLoadMode() != SplitLoad.MULTIPLE_CLASSLOADER) {
            return false;
        }
        SplitInfoManager manager = SplitInfoManagerService.getInstance();
        assert manager != null;
        Collection<SplitInfo> splits = manager.getAllSplitInfo(context);
        if (splits == null) {
            return false;
        }
        for (SplitInfo info : splits) {
            if (info.hasLibs()) {
                List<SplitInfo.LibInfo.Lib> libs = info.getLibInfo().getLibs();
                boolean hitLib = false;
                for (SplitInfo.LibInfo.Lib lib : libs) {
                    if (lib.getName().equals(System.mapLibraryName(libraryName))) {
                        hitLib = true;
                        break;
                    }
                }
                if (hitLib) {
                    SplitDexClassLoader classLoader = SplitApplicationLoaders.getInstance().getClassLoader(info.getSplitName());
                    if (classLoader != null) {
                        return loadSplitLibrary0(classLoader, libraryName);
                    }
                }
            }
        }
        return false;
    }

    private static boolean loadSplitLibrary0(ClassLoader classLoader, String name) {
        try {
            Method method = HiddenApiReflection.findMethod(Runtime.class, "loadLibrary0", ClassLoader.class, String.class);
            method.invoke(Runtime.getRuntime(), classLoader, name);
            return true;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

}
