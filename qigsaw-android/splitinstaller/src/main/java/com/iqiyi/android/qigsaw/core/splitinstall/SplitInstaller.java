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

package com.iqiyi.android.qigsaw.core.splitinstall;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import dalvik.system.DexFile;

abstract class SplitInstaller {

    static final int WAIT_ASYNC_OAT_TIME = 10 * 1000;

    static final int MAX_WAIT_COUNT = 120;

    /**
     * Install downloaded split apk.
     *
     * @param splitInfo {@link SplitInfo}
     */
    public abstract InstallResult install(SplitInfo splitInfo) throws InstallException;

    /**
     * Check whether the signature of split apk is consistent with base app.
     *
     * @param splitApk file of split apk.
     */
    protected abstract void validateSignature(File splitApk) throws InstallException;

    /**
     * Extract split apk dex files if current platform does't support multi dex.
     *
     * @param splitInfo {@link SplitInfo}
     * @param splitApk  file of split apk.
     * @return a list of extracted dex files
     */
    protected abstract List<File> extractMultiDex(SplitInfo splitInfo, File splitApk) throws InstallException;

    /**
     * Extract split native library files.
     *
     * @param info     {@link   SplitInfo}
     * @param splitApk file of split apk.
     */
    protected abstract File extractLib(SplitInfo info, File splitApk) throws InstallException;

    /**
     * Optimize dex files of split.
     *
     * @param dexFiles     a list of dex files.
     * @param optimizedDir a dir for store optimized dex files.
     * @return a list of {@link DexFile}
     */
    protected abstract List<DexFile> optimizeDex(List<File> dexFiles, File optimizedDir) throws InstallException;

    /**
     * Check whether optimized dex files are stale.
     */
    protected abstract void checkOptimizedDexFiles(List<File> optFiles, List<DexFile> dexFiles) throws InstallException;

    /**
     * create a mark file to record that this split has been installed successfully.
     */
    protected abstract void createInstalledMark(SplitInfo info) throws InstallException;

    protected abstract Context getApplicationContext();

    class InstallResult {

        final String splitName;

        final File splitDir;

        final File apkFile;

        final File libFile;

        final File optDir;

        final List<File> multiDexFiles;

        final boolean dependenciesInstalled;

        InstallResult(@NonNull String splitName,
                      @NonNull File splitDir,
                      @NonNull File apkFile,
                      @Nullable File optDir,
                      @Nullable File libFile,
                      @Nullable List<File> multiDexFiles,
                      boolean dependenciesInstalled) {
            this.splitName = splitName;
            this.splitDir = splitDir;
            this.apkFile = apkFile;
            this.optDir = optDir;
            this.libFile = libFile;
            this.multiDexFiles = multiDexFiles;
            this.dependenciesInstalled = dependenciesInstalled;
        }
    }

    static final class DexOptimizer {

        private final File dexFile;

        private final File optimizedDir;

        DexOptimizer(File dexFile, File optimizedDir) {
            this.dexFile = dexFile;
            this.optimizedDir = optimizedDir;
        }

        DexFile optimize() throws IOException {
            String optimizedPath = SplitInstallerInternals.optimizedPathFor(this.dexFile, this.optimizedDir);
            return DexFile.loadDex(dexFile.getAbsolutePath(), optimizedPath, 0);
        }
    }

    static final class InstallException extends Exception {

        private final int errorCode;

        InstallException(int errorCode, Exception e) {
            super((new StringBuilder(32)).append("Split Install Error: ").append(errorCode).toString(), e);
            this.errorCode = errorCode;
        }

        int getErrorCode() {
            return this.errorCode;
        }
    }

}
