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
import android.os.Build;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.OEMCompat;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dalvik.system.DexClassLoader;

final class SplitInstallerImpl extends SplitInstaller {

    private static final boolean IS_VM_MULTIDEX_CAPABLE = isVMMultiDexCapable(System.getProperty("java.vm.version"));

    private static final String TAG = "SplitInstallerImpl";

    private final Context appContext;

    private final boolean verifySignature;

    SplitInstallerImpl(Context context, boolean verifySignature) {
        this.appContext = context;
        this.verifySignature = verifySignature;
    }

    @Override
    public InstallResult install(boolean startInstall, SplitInfo info) throws InstallException {
        File splitDir = SplitPathManager.require().getSplitDir(info);
        File sourceApk;
        if (info.isBuiltIn() && info.getUrl().startsWith(SplitConstants.URL_NATIVE)) {
            sourceApk = new File(appContext.getApplicationInfo().nativeLibraryDir, System.mapLibraryName(SplitConstants.SPLIT_PREFIX + info.getSplitName()));
        } else {
            sourceApk = new File(splitDir, info.getSplitName() + SplitConstants.DOT_APK);
        }
        if (!FileUtil.isLegalFile(sourceApk)) {
            throw new InstallException(
                    SplitInstallError.APK_FILE_ILLEGAL,
                    new FileNotFoundException("Split apk " + sourceApk.getAbsolutePath() + " is illegal!")
            );
        }
        if (verifySignature) {
            SplitLog.d(TAG, "Need to verify split %s signature!", sourceApk.getAbsolutePath());
            verifySignature(sourceApk);
        }
        checkSplitMD5(sourceApk, info.getMd5());
        File splitLibDir = null;
        if (isLibExtractNeeded(info)) {
            extractLib(info, sourceApk);
            splitLibDir = SplitPathManager.require().getSplitLibDir(info);
        }
        List<String> addedDexPaths = null;
        if (info.hasDex()) {
            addedDexPaths = new ArrayList<>();
            addedDexPaths.add(sourceApk.getAbsolutePath());
            if (!isVMMultiDexCapable()) {
                if (isMultiDexExtractNeeded(info)) {
                    addedDexPaths.addAll(extractMultiDex(info, sourceApk));
                }
            }
        }
        File markFile = SplitPathManager.require().getSplitMarkFile(info);
        if (addedDexPaths != null) {
            String dexPath = TextUtils.join(File.pathSeparator, addedDexPaths);
            File optimizedDirectory = SplitPathManager.require().getSplitOptDir(info);
            String librarySearchPath = splitLibDir == null ? null : splitLibDir.getAbsolutePath();
            //trigger oat if need
            if (!markFile.exists()) {
                try {
                    new DexClassLoader(dexPath, optimizedDirectory.getAbsolutePath(), librarySearchPath, SplitInstallerImpl.class.getClassLoader());
                } catch (Throwable error) {
                    throw new InstallException(
                            SplitInstallError.CLASSLOADER_CREATE_FAILED,
                            error);
                }
            }
            //check oat file. We found many native crash in libart.so, especially vivo & oppo.
            if (OEMCompat.shouldCheckOatFileInCurrentSys()) {
                SplitLog.v(TAG, "Start to check oat file, current api level is " + Build.VERSION.SDK_INT);
                boolean specialManufacturer = OEMCompat.isSpecialManufacturer();
                File oatFile = OEMCompat.getOatFilePath(sourceApk, optimizedDirectory);
                if (FileUtil.isLegalFile(oatFile)) {
                    boolean checkResult = OEMCompat.checkOatFile(oatFile);
                    SplitLog.v(TAG, "Result of oat file %s is " + checkResult, oatFile.getAbsoluteFile());
                    if (!checkResult) {
                        SplitLog.w(TAG, "Failed to check oat file " + oatFile.getAbsolutePath());
                        if (specialManufacturer) {
                            File lockFile = SplitPathManager.require().getSplitSpecialLockFile(info);
                            try {
                                FileUtil.deleteFileSafelyLock(oatFile, lockFile);
                            } catch (IOException error) {
                                SplitLog.w(TAG, "Failed to delete corrupted oat file " + oatFile.exists());
                            }
                        } else {
                            FileUtil.deleteFileSafely(oatFile);
                        }
                        throw new InstallException(
                                SplitInstallError.DEX_OAT_FAILED,
                                new FileNotFoundException("System generate split " + info.getSplitName() + " oat file failed!")
                        );
                    }
                } else {
                    if (specialManufacturer) {
                        SplitLog.v(TAG, "Oat file %s is not exist in vivo & oppo, system would use interpreter mode.", oatFile.getAbsoluteFile());
                        File specialMarkFile = SplitPathManager.require().getSplitSpecialMarkFile(info);
                        if (!markFile.exists() && !specialMarkFile.exists()) {
                            File lockFile = SplitPathManager.require().getSplitSpecialLockFile(info);
                            boolean firstInstalled = createInstalledMarkLock(specialMarkFile, lockFile);
                            return new InstallResult(info.getSplitName(), sourceApk, addedDexPaths, firstInstalled);
                        }
                    }
                }
            }
        }
        boolean firstInstalled = createInstalledMark(markFile);
        return new InstallResult(info.getSplitName(), sourceApk, addedDexPaths, firstInstalled);
    }

    @Override
    protected void verifySignature(File splitApk) throws InstallException {
        if (!SignatureValidator.validateSplit(appContext, splitApk)) {
            deleteCorruptedFiles(Collections.singletonList(splitApk));
            throw new InstallException(
                    SplitInstallError.SIGNATURE_MISMATCH,
                    new SignatureException("Failed to check split apk " + splitApk.getAbsolutePath() + " signature!")
            );
        }
    }

    @Override
    protected void checkSplitMD5(File splitApk, String splitApkMd5) throws InstallException {
        String curMd5 = FileUtil.getMD5(splitApk);
        if (!splitApkMd5.equals(curMd5)) {
            deleteCorruptedFiles(Collections.singletonList(splitApk));
            throw new InstallException(SplitInstallError.MD5_ERROR, new IOException("Failed to check split apk md5, expect " + splitApkMd5 + " but " + curMd5));
        }
    }

    @Override
    protected List<String> extractMultiDex(SplitInfo info, File splitApk) throws InstallException {
        SplitLog.w(TAG,
                "VM do not support multi-dex, but split %s has multi dex files, so we need install other dex files manually",
                splitApk.getName());
        File codeCacheDir = SplitPathManager.require().getSplitCodeCacheDir(info);
        String prefsKeyPrefix = info.getSplitName() + "@" + SplitBaseInfoProvider.getVersionName() + "@" + info.getSplitVersion();
        try {
            SplitMultiDexExtractor extractor = new SplitMultiDexExtractor(splitApk, codeCacheDir);
            try {
                List<? extends File> dexFiles = extractor.load(appContext, prefsKeyPrefix, false);
                List<String> dexPaths = new ArrayList<>(dexFiles.size());
                for (File dexFile : dexFiles) {
                    dexPaths.add(dexFile.getAbsolutePath());
                }
                SplitLog.w(TAG, "Succeed to load or extract dex files", dexFiles.toString());
                return dexPaths;
            } catch (IOException e) {
                SplitLog.w(TAG, "Failed to load or extract dex files", e);
                throw new InstallException(SplitInstallError.DEX_EXTRACT_FAILED, e);
            } finally {
                FileUtil.closeQuietly(extractor);
            }
        } catch (IOException ioError) {
            throw new InstallException(SplitInstallError.DEX_EXTRACT_FAILED, ioError);
        }
    }

    @Override
    protected void extractLib(SplitInfo info, File sourceApk) throws InstallException {
        try {
            File splitLibDir = SplitPathManager.require().getSplitLibDir(info);
            SplitLibExtractor extractor = new SplitLibExtractor(sourceApk, splitLibDir);
            try {
                List<File> libFiles = extractor.load(info, false);
                SplitLog.i(TAG, "Succeed to extract libs:  %s", libFiles.toString());
            } catch (IOException e) {
                SplitLog.w(TAG, "Failed to load or extract lib files", e);
                throw new InstallException(SplitInstallError.LIB_EXTRACT_FAILED, e);
            } finally {
                FileUtil.closeQuietly(extractor);
            }
        } catch (IOException ioError) {
            throw new InstallException(SplitInstallError.LIB_EXTRACT_FAILED, ioError);
        }
    }

    @Override
    protected boolean createInstalledMark(File markFile) throws InstallException {
        if (!markFile.exists()) {
            try {
                FileUtil.createFileSafely(markFile);
                return true;
            } catch (IOException e) {
                throw new InstallException(SplitInstallError.MARK_CREATE_FAILED, e);
            }
        }
        return false;
    }

    @Override
    protected boolean createInstalledMarkLock(File markFile, File lockFile) throws InstallException {
        if (!markFile.exists()) {
            try {
                FileUtil.createFileSafelyLock(markFile, lockFile);
                return true;
            } catch (IOException e) {
                throw new InstallException(SplitInstallError.MARK_CREATE_FAILED, e);
            }
        }
        return false;
    }

    /**
     * Estimate whether current platform supports multi dex.
     *
     * @return {@code true} if supports multi dex, otherwise {@code false}
     */
    private boolean isVMMultiDexCapable() {
        return IS_VM_MULTIDEX_CAPABLE;
    }

    /**
     * check whether split apk has multi dexes.
     *
     * @param info {@link SplitInfo}
     */
    private boolean isMultiDexExtractNeeded(SplitInfo info) {
        return info.isMultiDex();
    }

    /**
     * check whether split apk has native libraries.
     *
     * @param info {@link SplitInfo}
     */
    private boolean isLibExtractNeeded(SplitInfo info) {
        return info.hasLibs();
    }

    /**
     * Delete corrupted files if split apk installing failed.
     *
     * @param files list of corrupted files
     */
    private void deleteCorruptedFiles(List<File> files) {
        for (File file : files) {
            FileUtil.deleteFileSafely(file);
        }
    }

    private static boolean isVMMultiDexCapable(String versionString) {
        boolean isMultiDexCapable = false;
        if (versionString != null) {
            Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(versionString);
            if (matcher.matches()) {
                try {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = Integer.parseInt(matcher.group(2));
                    isMultiDexCapable = major > 2 || major == 2 && minor >= 1;
                } catch (NumberFormatException var5) {
                    //ignored
                }
            }
        }
        SplitLog.i("Split:MultiDex", "VM with version " + versionString + (isMultiDexCapable ? " has multidex support" : " does not have multidex support"));
        return isMultiDexCapable;
    }
}
