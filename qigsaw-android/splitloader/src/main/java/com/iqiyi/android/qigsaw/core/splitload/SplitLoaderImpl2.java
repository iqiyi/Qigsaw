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
import androidx.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class SplitLoaderImpl2 extends SplitLoader {

    SplitLoaderImpl2(Context context) {
        super(context);
    }

    @Override
    void loadCode2(@Nullable List<String> dexPaths,
                   File optimizedDirectory,
                   @Nullable File librarySearchPath) throws SplitLoadException {
        ClassLoader curCl = SplitLoader.class.getClassLoader();
        loadLibrary(curCl, librarySearchPath);
        loadDex(curCl, dexPaths, optimizedDirectory);
    }

    private void loadLibrary(ClassLoader classLoader, File librarySearchPath) throws SplitLoadException {
        if (librarySearchPath != null) {
            try {
                SplitCompatLibraryLoader.load(classLoader, librarySearchPath);
            } catch (Throwable cause) {
                throw new SplitLoadException(SplitLoadError.LOAD_LIB_FAILED, cause);
            }
        }
    }

    private void loadDex(ClassLoader classLoader, List<String> dexPaths, File optimizedDirectory) throws SplitLoadException {
        if (dexPaths != null) {
            List<File> dexFiles = new ArrayList<>(dexPaths.size());
            for (String dexPath : dexPaths) {
                dexFiles.add(new File(dexPath));
            }
            try {
                SplitCompatDexLoader.load(classLoader, optimizedDirectory, dexFiles);
                SplitUnKnownFileTypeDexLoader.loadDex(classLoader, dexPaths, optimizedDirectory);
            } catch (Throwable cause) {
                throw new SplitLoadException(SplitLoadError.LOAD_DEX_FAILED, cause);
            }
        }
    }

}
