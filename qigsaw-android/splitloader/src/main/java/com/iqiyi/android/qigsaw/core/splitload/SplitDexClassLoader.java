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

import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import dalvik.system.BaseDexClassLoader;

final class SplitDexClassLoader extends BaseDexClassLoader {

    private static final String TAG = "SplitDexClassLoader";

    private final String moduleName;

    private Set<SplitDexClassLoader> dependenciesLoaders;

    private SplitDexClassLoader(String moduleName,
                                List<String> dexPaths,
                                File optimizedDirectory,
                                String librarySearchPath,
                                List<String> dependencies,
                                ClassLoader parent) throws Throwable {
        super((dexPaths == null) ? "" : TextUtils.join(File.pathSeparator, dexPaths), optimizedDirectory, librarySearchPath, parent);
        this.moduleName = moduleName;
        this.dependenciesLoaders = SplitApplicationLoaders.getInstance().getClassLoaders(dependencies);
        SplitUnKnownFileTypeDexLoader.loadDex(this, dexPaths, optimizedDirectory);
    }

    static SplitDexClassLoader create(String moduleName,
                                      List<String> dexPaths,
                                      File optimizedDirectory,
                                      File librarySearchFile,
                                      List<String> dependencies) throws Throwable {
        long time = System.currentTimeMillis();
        SplitDexClassLoader cl = new SplitDexClassLoader(
                moduleName,
                dexPaths,
                optimizedDirectory,
                librarySearchFile == null ? null : librarySearchFile.getAbsolutePath(),
                dependencies,
                SplitDexClassLoader.class.getClassLoader()
        );
        SplitLog.d(TAG, "Cost %d ms to load %s code", System.currentTimeMillis() - time, moduleName);
        return cl;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e1) {
            if (dependenciesLoaders != null) {
                for (SplitDexClassLoader loader : dependenciesLoaders) {
                    try {
                        return loader.loadClassItself(name);
                    } catch (ClassNotFoundException e2) {
                        SplitLog.w(TAG, "SplitDexClassLoader: Class %s is not found in %s ClassLoader", name, loader.moduleName());
                    }
                }
            }
            throw e1;
        }
    }

    String moduleName() {
        return moduleName;
    }

    @Override
    public String findLibrary(String name) {
        String libName = super.findLibrary(name);
        if (libName == null) {
            if (dependenciesLoaders != null) {
                for (SplitDexClassLoader loader : dependenciesLoaders) {
                    libName = loader.findLibrary(name);
                    if (libName != null) {
                        break;
                    }
                }
            }
        }
        if (libName == null) {
            if (getParent() instanceof BaseDexClassLoader) {
                libName = ((BaseDexClassLoader) getParent()).findLibrary(name);
            }
        }
        return libName;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        Enumeration<URL> resources = super.findResources(name);
        if (resources == null) {
            if (dependenciesLoaders != null) {
                for (SplitDexClassLoader loader : dependenciesLoaders) {
                    resources = loader.findResourcesItself(name);
                    if (resources != null) {
                        break;
                    }
                }
            }
        }
        return resources;
    }

    @Override
    protected URL findResource(String name) {
        URL resource = super.findResource(name);
        if (resource == null) {
            if (dependenciesLoaders != null) {
                for (SplitDexClassLoader loader : dependenciesLoaders) {
                    resource = loader.findResourceItself(name);
                    if (resource != null) {
                        break;
                    }
                }
            }
        }
        return resource;
    }

    URL findResourceItself(String name) {
        return super.findResource(name);
    }

    Enumeration<URL> findResourcesItself(String name) {
        return super.findResources(name);
    }

    String findLibraryItself(String name) {
        return super.findLibrary(name);
    }

    Class<?> loadClassItself(String name) throws ClassNotFoundException {
        Class<?> cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }
        return super.findClass(name);
    }

}
