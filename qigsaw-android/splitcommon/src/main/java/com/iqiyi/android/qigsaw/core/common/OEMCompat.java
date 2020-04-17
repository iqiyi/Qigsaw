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

package com.iqiyi.android.qigsaw.core.common;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.File;
import java.io.IOException;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class OEMCompat {

    private static final String TAG = "Split.OEMCompat";

    public static boolean isSpecialManufacturer() {
        return "vivo".equalsIgnoreCase(Build.MANUFACTURER) || "oppo".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean shouldCheckOatFileInCurrentSys() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O;
    }

    /**
     * Only work from OS.Version L to N
     */
    public static File getOatFilePath(@NonNull File sourceApk, @NonNull File optimizedDirectory) {
        String fileName = sourceApk.getName();
        if (!fileName.endsWith(SplitConstants.DOT_DEX)) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                fileName += SplitConstants.DOT_DEX;
            } else {
                fileName = fileName.substring(0, lastDot) + SplitConstants.DOT_DEX;
            }
        }
        return new File(optimizedDirectory, fileName);
    }

    /**
     * Check oat file whether is ELF format or not.
     *
     * @param oatFile oat file of split apk
     * @return {@code true} is a valid oat file, otherwise {@code false}.
     */
    public static boolean checkOatFile(@NonNull File oatFile) {
        int returnType;
        try {
            returnType = SplitElfFile.getFileTypeByMagic(oatFile);
        } catch (IOException e) {
            // read error just continue
            return true;
        }
        if (returnType == SplitElfFile.FILE_TYPE_ELF) {
            SplitElfFile elfFile = null;
            try {
                elfFile = new SplitElfFile(oatFile);
                return true;
            } catch (Throwable e) {
                SplitLog.e(TAG, "final parallel dex optimizer file %s is not elf format, return false", oatFile.getName());
            } finally {
                FileUtil.closeQuietly(elfFile);
            }
        }
        return false;
    }
}
