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

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dalvik.system.DexFile;

final class SplitInstallerImpl extends SplitInstaller {

    private static final boolean IS_VM_MULTIDEX_CAPABLE = SplitInstallerInternals.isVMMultiDexCapable(System.getProperty("java.vm.version"));

    private static final String TAG = "Split:SplitInstallerImpl";

    private final Context mContext;

    SplitInstallerImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public InstallResult install(SplitInfo info) throws InstallException {
        File splitDir = SplitPathManager.require().getSplitDir(info);
        File sourceApk = new File(splitDir, info.getSplitName() + SplitConstants.DOT_APK);
        validateSignature(sourceApk);
        File libDir = null;
        if (isLibExtractNeeded(info)) {
            libDir = extractLib(info, sourceApk);
        }
        List<File> multiDexFiles = null;
        File optimizedDir = null;
        if (info.hasDex()) {
            if (!isVMMultiDexCapable()) {
                if (isMultiDexExtractNeeded(info)) {
                    multiDexFiles = extractMultiDex(info, sourceApk);
                }
            }
            List<File> dexFiles;
            if (multiDexFiles == null || multiDexFiles.isEmpty()) {
                dexFiles = Collections.singletonList(sourceApk);
            } else {
                dexFiles = new ArrayList<>(multiDexFiles);
                dexFiles.add(sourceApk);
            }
            optimizedDir = SplitPathManager.require().getSplitOptDir(info);
            //get all optimized dex files
            List<File> optDexFiles = getOptimizedDexFiles(dexFiles, optimizedDir);
            //check if need optimize dex files
            if (isOptimizeDexNeeded(optDexFiles)) {
                //delete corrupted optimized dex files if necessary
                deleteCorruptedFiles(optDexFiles);
                //optimize dex files
                List<DexFile> dexes = optimizeDex(dexFiles, optimizedDir);
                checkOptimizedDexFiles(optDexFiles, dexes);
            }
        }
        createInstalledMark(info);
        return new InstallResult(info.getSplitName(), splitDir, sourceApk, optimizedDir, libDir, multiDexFiles, checkDependenciesInstalledStatus(info));
    }

    private boolean checkDependenciesInstalledStatus(SplitInfo info) {
        List<String> dependencies = info.getDependencies();
        if (dependencies != null) {
            for (String dependency : dependencies) {
                SplitInfo dependencySplitInfo = SplitInfoManagerService.getInstance().getSplitInfo(getApplicationContext(), dependency);
                File dependencySplitDir = SplitPathManager.require().getSplitDir(dependencySplitInfo);
                File dependencyMarkFile = new File(dependencySplitDir, dependencySplitInfo.getMd5());
                if (!dependencyMarkFile.exists()) {
                    SplitLog.i(TAG, "Dependency %s mark file is not existed!", dependency);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void validateSignature(File splitApk) throws InstallException {
        if (!FileUtil.isLegalFile(splitApk)) {
            throw new InstallException(
                    SplitInstallError.APK_FILE_ILLEGAL,
                    new FileNotFoundException("Split apk " + splitApk.getAbsolutePath() + " is illegal!")
            );
        }
        if (!SignatureValidator.validateSplit(mContext, splitApk)) {
            deleteCorruptedFiles(Collections.singletonList(splitApk));
            throw new InstallException(
                    SplitInstallError.SIGNATURE_MISMATCH,
                    new SignatureException("Failed to check split apk " + splitApk.getAbsolutePath() + " signature!")
            );
        }
    }

    @Override
    protected List<File> extractMultiDex(SplitInfo info, File splitApk) throws InstallException {
        SplitLog.w(TAG,
                "VM do not support multi-dex, but split %s has multi dex files, so we need creteSplitInstallService other dex files manually",
                splitApk.getName());
        File codeCacheDir = SplitPathManager.require().getSplitCodeCacheDir(info);
        String prefsKeyPrefix = info.getSplitName() + "@" + SplitBaseInfoProvider.getVersionName() + "@" + info.getSplitVersion();
        try {
            SplitMultiDexExtractor extractor = new SplitMultiDexExtractor(splitApk, codeCacheDir);
            try {
                List dexFiles = extractor.load(mContext, prefsKeyPrefix, false);
                SplitLog.w(TAG, "Succeed to load or extract dex files", dexFiles.toString());
                return dexFiles;
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
    protected File extractLib(SplitInfo info, File sourceApk) throws InstallException {
        try {
            File splitLibDir = SplitPathManager.require().getSplitLibDir(info);
            SplitLibExtractor extractor = new SplitLibExtractor(sourceApk, splitLibDir);
            try {
                List<File> libFiles = extractor.load(info, false);
                SplitLog.i(TAG, "Succeed to extract libs:  %s", libFiles.toString());
                return splitLibDir;
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
    protected List<DexFile> optimizeDex(List<File> dexFiles, File optimizedDir) throws InstallException {
        List<DexFile> dexes = new ArrayList<>(dexFiles.size());
        for (File dexFile : dexFiles) {
            DexOptimizer optimizer = new DexOptimizer(dexFile, optimizedDir);
            try {
                DexFile dex = optimizer.optimize();
                dexes.add(dex);
            } catch (IOException e) {
                throw new InstallException(SplitInstallError.DEX_OPT_FAILED, e);
            }
        }
        return dexes;
    }

    @Override
    protected void checkOptimizedDexFiles(List<File> optFiles, List<DexFile> dexFiles) throws InstallException {
        if (!waitForDexOptimize(optFiles)) {
            deleteCorruptedFiles(optFiles);
            closeDexFileQuietly(dexFiles);
            throw new InstallException(
                    SplitInstallError.OPT_CHECK_FAILED,
                    new IOException("Failed to check opt files " + dexFiles.toString())
            );
        }
    }

    @Override
    protected void createInstalledMark(SplitInfo info) throws InstallException {
        File splitDir = SplitPathManager.require().getSplitDir(info);
        File markFile = new File(splitDir, info.getMd5());
        if (!markFile.exists()) {
            boolean isCreationSuccessful = false;
            int numAttempts = 0;
            Exception cause = null;
            while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isCreationSuccessful) {
                numAttempts++;
                try {
                    if (!markFile.createNewFile()) {
                        SplitLog.w(TAG, "Split %s mark file %s already exists", info.getSplitName(), markFile.getAbsolutePath());
                    }
                    isCreationSuccessful = true;
                } catch (Exception e) {
                    isCreationSuccessful = false;
                    cause = e;
                }
            }
            if (!isCreationSuccessful) {
                throw new InstallException(SplitInstallError.MARK_CREATE_FAILED, cause);
            }

        }
    }

    @Override
    protected Context getApplicationContext() {
        return mContext;
    }

    /**
     * Some phones like vivo and oppo will optimize dex files asynchronously, so we need to wait until optimization is finished.
     *
     * @param optFiles a list of optimized dex files
     * @return {@code true} if succeed to optimize dex files, otherwise {@code false}
     */
    private boolean waitForDexOptimize(List<File> optFiles) {
        int size = optFiles.size() * 30;
        if (size > MAX_WAIT_COUNT) {
            size = MAX_WAIT_COUNT;
        }
        for (int i = 0; i < size; i++) {
            if (!checkAllDexOptFileExisting(optFiles, i)) {
                try {
                    Thread.sleep(WAIT_ASYNC_OAT_TIME);
                } catch (InterruptedException e) {
                    SplitLog.e(TAG, "thread sleep InterruptedException", e);
                }
            }
        }
        return checkOptimizedDexFilesValid(optFiles);
    }

    private boolean checkOptimizedDexFilesValid(List<File> optFiles) {
        List<File> failedOptFiles = new ArrayList<>();
        //check legality
        for (File file : optFiles) {
            if (!FileUtil.isLegalFile(file)
                    && !SplitInstallerInternals.shouldAcceptEvenIfOptFileIllegal(file)) {
                SplitLog.e(TAG, "final parallel dex optimizer file %s is not exist, return false", file.getName());
                failedOptFiles.add(file);
            }
        }
        if (!failedOptFiles.isEmpty()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            Throwable lastThrowable = null;
            for (File file : optFiles) {
                if (SplitInstallerInternals.shouldAcceptEvenIfOptFileIllegal(file)) {
                    continue;
                }
                int retType;
                try {
                    retType = SplitElfFile.getFileTypeByMagic(file);
                } catch (IOException e) {
                    continue;
                }
                if (retType == SplitElfFile.FILE_TYPE_ELF) {
                    SplitElfFile elfFile = null;
                    try {
                        elfFile = new SplitElfFile(file);
                        SplitLog.i(TAG, "Succeed to check oat file format!");
                    } catch (Throwable e) {
                        SplitLog.e(TAG, "final parallel dex optimizer file %s is not elf format", file.getName());
                        failedOptFiles.add(file);
                        lastThrowable = e;
                    } finally {
                        FileUtil.closeQuietly(elfFile);
                    }
                }
            }
            if (!failedOptFiles.isEmpty()) {
                SplitLog.e(TAG, "Failed to opt dex file " + optFiles.toString(), lastThrowable);
                deleteCorruptedFiles(failedOptFiles);
                return false;
            }
        }
        return true;
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
     * Gets files of optimized Dex.
     *
     * @param dexFiles     a list of dex files.
     * @param optimizedDir a dir to store optimized dex files.
     */
    private List<File> getOptimizedDexFiles(List<File> dexFiles, File optimizedDir) {
        List<File> optFiles = new ArrayList<>();
        for (File dexFile : dexFiles) {
            String optimizedPath = SplitInstallerInternals.optimizedPathFor(dexFile, optimizedDir);
            optFiles.add(new File(optimizedPath));
        }
        return optFiles;
    }

    /**
     * Check if existing optimized dex files are all legal.
     *
     * @param optFiles a list of existed optimized dex files
     */
    private boolean isOptimizeDexNeeded(List<File> optFiles) {
        for (File optFile : optFiles) {
            if (!optFile.exists()) {
                return true;
            }
        }
        return !checkOptimizedDexFilesValid(optFiles);
    }

    /**
     * Delete corrupted files if split apk installing failed.
     *
     * @param files list of corrupted files
     */
    private void deleteCorruptedFiles(List<File> files) {
        for (File file : files) {
            FileUtil.safeDeleteFile(file);
        }
    }

    private boolean checkAllDexOptFileExisting(List<File> files, int count) {
        for (File file : files) {
            if (!FileUtil.isLegalFile(file)) {
                if (SplitInstallerInternals.shouldAcceptEvenIfOptFileIllegal(file)) {
                    continue;
                }
                SplitLog.e(TAG, "parallel dex optimizer file %s is not exist, just wait %d times", file.getName(), count);
                return false;
            }
        }
        return true;
    }

    private void closeDexFileQuietly(List<DexFile> dexFiles) {
        for (DexFile dex : dexFiles) {
            if (dex != null) {
                try {
                    dex.close();
                } catch (Exception e) {
                    //
                }
            }
        }
    }
}
