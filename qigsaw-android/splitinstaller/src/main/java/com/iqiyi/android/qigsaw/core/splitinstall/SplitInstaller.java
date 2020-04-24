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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.io.File;
import java.util.List;

abstract class SplitInstaller {

    /**
     * Install downloaded split apk.
     *
     * @param startInstall whether install splits immediately.
     * @param splitInfo    {@link SplitInfo}
     */
    public abstract InstallResult install(boolean startInstall, SplitInfo splitInfo) throws InstallException;

    /**
     * Check whether the signature of split apk is consistent with base app.
     *
     * @param splitApk file of split apk.
     */
    protected abstract void verifySignature(File splitApk) throws InstallException;

    /**
     * check md5 for split apk.
     *
     * @param splitApk    file of split apk.
     * @param splitApkMd5 md5 value of split apk.
     */
    protected abstract void checkSplitMD5(File splitApk, String splitApkMd5) throws InstallException;

    /**
     * Extract split apk dex files if current platform does't support multi dex.
     *
     * @param splitInfo {@link SplitInfo}
     * @param splitApk  file of split apk.
     * @return a list of extracted dex file path.
     */
    protected abstract List<String> extractMultiDex(SplitInfo splitInfo, File splitApk) throws InstallException;

    /**
     * Extract split native library files.
     *
     * @param info     {@link   SplitInfo}
     * @param splitApk file of split apk.
     */
    protected abstract void extractLib(SplitInfo info, File splitApk) throws InstallException;

    /**
     * create a mark file to record that this split has been installed successfully.
     *
     * @return if {@code true} means the split is first installed, otherwise {@code false} means the split has been install.
     */
    protected abstract boolean createInstalledMark(File markFile) throws InstallException;

    protected abstract boolean createInstalledMarkLock(File markFile, File lockFile) throws InstallException;

    static class InstallResult {

        final String splitName;

        final File apkFile;

        final List<String> addedDexPaths;

        final boolean firstInstalled;

        InstallResult(@NonNull String splitName,
                      @NonNull File apkFile,
                      @Nullable List<String> addedDexPaths,
                      boolean firstInstalled) {
            this.splitName = splitName;
            this.apkFile = apkFile;
            this.addedDexPaths = addedDexPaths;
            this.firstInstalled = firstInstalled;
        }
    }

    static final class InstallException extends Exception {

        private final int errorCode;

        InstallException(int errorCode, Throwable e) {
            super((new StringBuilder(32)).append("Split Install Error: ").append(errorCode).toString(), e);
            this.errorCode = errorCode;
        }

        int getErrorCode() {
            return this.errorCode;
        }
    }

}
