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
import androidx.annotation.RestrictTo;

import java.util.concurrent.atomic.AtomicReference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Create and manage {@link SplitLoadManager} instance.
 */
@RestrictTo(LIBRARY_GROUP)
public class SplitLoadManagerService {

    private static final AtomicReference<SplitLoadManager> sReference = new AtomicReference<>();

    public static void install(Context context,
                               int splitLoadMode,
                               boolean qigsawMode,
                               boolean isMainProcess,
                               String currentProcessName,
                               String[] workProcesses,
                               String[] forbiddenWorkProcesses) {
        sReference.set(create(context, splitLoadMode, qigsawMode, isMainProcess, currentProcessName, workProcesses, forbiddenWorkProcesses));
    }

    public static boolean hasInstance() {
        return sReference.get() != null;
    }

    public static SplitLoadManager getInstance() {
        if (sReference.get() == null) {
            throw new RuntimeException("Have you invoke SplitLoadManagerService#install(Context) method?");
        }
        return sReference.get();
    }

    private static SplitLoadManager create(Context context,
                                           int splitLoadMode,
                                           boolean qigsawMode,
                                           boolean isMainProcess,
                                           String currentProcessName,
                                           String[] workProcesses,
                                           String[] forbiddenWorkProcesses) {
        return new SplitLoadManagerImpl(context, splitLoadMode, qigsawMode, isMainProcess, currentProcessName, workProcesses, forbiddenWorkProcesses);
    }
}
