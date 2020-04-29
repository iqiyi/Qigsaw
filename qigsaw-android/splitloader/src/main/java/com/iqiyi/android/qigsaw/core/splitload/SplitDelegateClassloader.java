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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;

final class SplitDelegateClassloader extends PathClassLoader {

    private static final String TAG = "SplitDelegateClassloader";

    private static BaseDexClassLoader originClassLoader;

    private int splitLoadMode;

    SplitDelegateClassloader(ClassLoader parent) {
        super("", parent);
        originClassLoader = (BaseDexClassLoader) parent;
    }

    private static void reflectPackageInfoClassloader(Context baseContext, ClassLoader reflectClassLoader) throws Exception {
        Object packageInfo = HiddenApiReflection.findField(baseContext, "mPackageInfo").get(baseContext);
        if (packageInfo != null) {
            HiddenApiReflection.findField(packageInfo, "mClassLoader").set(packageInfo, reflectClassLoader);
        }
    }

    static void inject(ClassLoader originalClassloader, Context baseContext) throws Exception {
        SplitDelegateClassloader classloader = new SplitDelegateClassloader(originalClassloader);
        reflectPackageInfoClassloader(baseContext, classloader);
    }

    void setSplitLoadMode(int splitLoadMode) {
        this.splitLoadMode = splitLoadMode;
        SplitLog.i(TAG, "Split load mode is : " + splitLoadMode);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return originClassLoader.loadClass(name);
        } catch (ClassNotFoundException error) {
            if (SplitLoadManagerService.hasInstance()) {
                if (splitLoadMode == SplitLoad.MULTIPLE_CLASSLOADER) {
                    Class<?> result = onClassNotFound(name);
                    if (result != null) {
                        return result;
                    }
                } else if (splitLoadMode == SplitLoad.SINGLE_CLASSLOADER) {
                    Class<?> result = onClassNotFound2(name);
                    if (result != null) {
                        return result;
                    }
                }
            }
            throw error;
        }
    }

    private Class<?> onClassNotFound(String name) {
        Class<?> ret = findClassInSplits(name);
        if (ret != null) {
            return ret;
        }
        Class<?> fakeComponent = AABExtension.getInstance().getFakeComponent(name);
        if (fakeComponent != null) {
            SplitLoadManagerService.getInstance().loadInstalledSplits();
            ret = findClassInSplits(name);
            if (ret != null) {
                SplitLog.i(TAG, "Class %s is found in Splits after loading all installed splits.", name);
                return ret;
            }
            SplitLog.w(TAG, "Split component %s is still not found after installing all installed splits, return a %s to avoid crash", name, fakeComponent.getSimpleName());
            return fakeComponent;
        }
        return null;
    }

    private Class<?> onClassNotFound2(String name) {
        Class<?> fakeComponent = AABExtension.getInstance().getFakeComponent(name);
        if (fakeComponent != null) {
            SplitLoadManagerService.getInstance().loadInstalledSplits();
            try {
                return originClassLoader.loadClass(name);
            } catch (ClassNotFoundException e) {
                SplitLog.w(TAG, "Split component %s is still not found after installing all installed splits,return a %s to avoid crash", name, fakeComponent.getSimpleName());
                return fakeComponent;
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return originClassLoader.getResources(name);
    }

    @Override
    public URL getResource(String name) {
        return originClassLoader.getResource(name);
    }

    @Override
    protected URL findResource(String name) {
        URL resource = super.findResource(name);
        if (resource == null) {
            Set<SplitDexClassLoader> splitDexClassLoaders = SplitApplicationLoaders.getInstance().getClassLoaders();
            for (SplitDexClassLoader loader : splitDexClassLoaders) {
                resource = loader.findResourceItself(name);
                if (resource != null) {
                    break;
                }
            }
        }
        return resource;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        Enumeration<URL> resources = super.findResources(name);
        if (resources == null) {
            Set<SplitDexClassLoader> splitDexClassLoaders = SplitApplicationLoaders.getInstance().getClassLoaders();
            for (SplitDexClassLoader loader : splitDexClassLoaders) {
                resources = loader.findResourcesItself(name);
                if (resources != null) {
                    break;
                }
            }
        }
        return resources;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    public String findLibrary(String name) {
        String libName = originClassLoader.findLibrary(name);
        if (libName == null) {
            Set<SplitDexClassLoader> splitDexClassLoaders = SplitApplicationLoaders.getInstance().getClassLoaders();
            for (SplitDexClassLoader classLoader : splitDexClassLoaders) {
                libName = classLoader.findLibraryItself(name);
                if (libName != null) {
                    break;
                }
            }
        }
        return libName;
    }
}
