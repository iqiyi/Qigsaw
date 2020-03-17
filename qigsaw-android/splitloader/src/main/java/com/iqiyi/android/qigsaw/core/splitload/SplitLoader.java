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
import java.util.List;

/**
 * Interface definition for loading installed splits.
 */
abstract class SplitLoader {

    final Context context;

    SplitLoader(Context context) {
        this.context = context;
    }

    /**
     * Load split' code for multiple class loader mode.
     */
    SplitDexClassLoader loadCode(String moduleNames,
                                 @Nullable List<String> dexPaths,
                                 File optimizedDirectory,
                                 @Nullable File librarySearchPath,
                                 @Nullable List<String> dependencies) throws SplitLoadException {
        return null;
    }

    /**
     * Load split' code for single class loader mode.
     */
    void loadCode2(@Nullable List<String> dexPaths,
                   File optimizedDirectory,
                   @Nullable File librarySearchPath) throws SplitLoadException {

    }

    /**
     * load resources of installed split.
     *
     * @param splitResDir local file path of split apk.
     */
    final void loadResources(String splitResDir) throws SplitLoadException {
        try {
            SplitCompatResourcesLoader.loadResources(context, context.getResources(), splitResDir);
        } catch (Throwable throwable) {
            throw new SplitLoadException(SplitLoadError.LOAD_RES_FAILED, throwable);
        }
    }
}
