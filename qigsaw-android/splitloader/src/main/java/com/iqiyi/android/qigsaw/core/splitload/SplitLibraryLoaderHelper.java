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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import androidx.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitLibraryLoaderHelper {

    @SuppressLint("UnsafeDynamicallyLoadedCode")
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
                for (SplitInfo.LibInfo.Lib lib : libs) {
                    if (lib.getName().equals(System.mapLibraryName(libraryName))) {
                        if (context instanceof Application) {
                            String libPath = SplitPathManager.require().getSplitLibDir(info).getAbsolutePath() + File.separator + lib.getName();
                            try {
                                System.load(libPath);
                                return true;
                            } catch (UnsatisfiedLinkError error) {
                                return false;
                            }
                        } else {
                            SplitDexClassLoader classLoader = SplitApplicationLoaders.getInstance().getClassLoader(info.getSplitName());
                            if (classLoader != null) {
                                return loadSplitLibrary0(classLoader, info.getSplitName(), libraryName);
                            }
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }

    private static boolean loadSplitLibrary0(ClassLoader classLoader, String splitName, String name) {
        try {
            Class<?> splitLoaderCl = classLoader.loadClass("com.iqiyi.android.qigsaw.core.splitlib." + splitName + "SplitLibraryLoader");
            Object splitLoader = splitLoaderCl.newInstance();
            Method method = HiddenApiReflection.findMethod(splitLoaderCl, "loadSplitLibrary", String.class);
            method.invoke(splitLoader, name);
            return true;
        } catch (Throwable ignored) {

        }
        return false;
    }

}
