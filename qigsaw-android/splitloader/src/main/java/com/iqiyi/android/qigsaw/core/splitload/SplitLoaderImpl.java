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
import android.content.Intent;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class SplitLoaderImpl extends SplitLoader {

    SplitLoaderImpl(Context context) {
        super(context);
    }

    @Override
    public void load(ClassLoader classLoader, Intent splitFileIntent) throws SplitLoadException {
        String splitApk = splitFileIntent.getStringExtra(SplitConstants.KEY_APK);
        String splitOptDir = splitFileIntent.getStringExtra(SplitConstants.KEY_OPTIMIZED_DIRECTORY);
        String splitLibPath = splitFileIntent.getStringExtra(SplitConstants.KEY_NATIVE_LIBRARIES);
        List<String> splitMultiDex = splitFileIntent.getStringArrayListExtra(SplitConstants.KEY_MULTI_DEX);
        List<String> dexPaths = new ArrayList<>();
        dexPaths.add(splitApk);
        if (splitMultiDex != null) {
            dexPaths.addAll(splitMultiDex);
        }
        loadResources(splitApk);
        loadNativePath(classLoader, splitLibPath);
        loadDex(classLoader, splitOptDir, dexPaths);
    }

    @Override
    protected void loadResources(String splitResDir) throws SplitLoadException {
        try {
            SplitCompatResourcesLoader.loadResources(context, context.getResources(), splitResDir);
            Context base = getBaseContext();
            try {
                installSplitResourceDir(base, splitResDir);
            } catch (Throwable ignored) {

            }
        } catch (Throwable throwable) {
            throw new SplitLoadException(SplitLoadError.LOAD_RES_FAILED, throwable);
        }
    }

    @Override
    protected void loadNativePath(ClassLoader classLoader, String libPath) throws SplitLoadException {
        if (!TextUtils.isEmpty(libPath)) {
            try {
                SplitCompatLibraryLoader.load(classLoader, new File(libPath));
            } catch (Throwable throwable) {
                throw new SplitLoadException(SplitLoadError.LOAD_LIB_FAILED, throwable);
            }
        }
    }

    @Override
    protected void loadDex(ClassLoader classLoader, String optDir, List<String> dexPaths) throws SplitLoadException {
        if (!TextUtils.isEmpty(optDir)) {
            List<File> dexFiles = new ArrayList<>(dexPaths.size());
            for (String dexPath : dexPaths) {
                dexFiles.add(new File(dexPath));
            }
            try {
                SplitCompatDexLoader.load(classLoader, new File(optDir), dexFiles);
            } catch (Throwable throwable) {
                throw new SplitLoadException(SplitLoadError.LOAD_DEX_FAILED, throwable);
            }
        } else {
            SplitLog.i(TAG, "No dexes are needed to load!");
        }
    }
}
