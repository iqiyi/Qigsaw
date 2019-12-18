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

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class SplitLibExtractor implements Closeable {

    private static final String TAG = "Split:LibExtractor";

    private final File sourceApk;

    private final File libDir;

    private static final String LOCK_FILENAME = "SplitLib.lock";

    private final RandomAccessFile lockRaf;

    private final FileChannel lockChannel;

    private final FileLock cacheLock;

    SplitLibExtractor(File sourceApk, File libDir) throws IOException {
        this.sourceApk = sourceApk;
        this.libDir = libDir;
        File lockFile = new File(libDir, LOCK_FILENAME);
        this.lockRaf = new RandomAccessFile(lockFile, "rw");
        try {
            this.lockChannel = this.lockRaf.getChannel();
            try {
                SplitLog.i(TAG, "Blocking on lock " + lockFile.getPath());
                this.cacheLock = this.lockChannel.lock();
            } catch (RuntimeException | Error | IOException var5) {
                FileUtil.closeQuietly(this.lockChannel);
                throw var5;
            }
            SplitLog.i(TAG, lockFile.getPath() + " locked");
        } catch (RuntimeException | Error | IOException var6) {
            FileUtil.closeQuietly(this.lockRaf);
            throw var6;
        }
    }

    List<File> load(SplitInfo info, boolean forceReload) throws IOException {
        if (!cacheLock.isValid()) {
            throw new IllegalStateException("SplitLibExtractor was closed");
        } else {
            List<File> files;
            if (!forceReload) {
                try {
                    files = loadExistingExtractions(info.getLibInfo().getLibs());
                } catch (IOException e) {
                    SplitLog.w(TAG, "Failed to reload existing extracted lib files, falling back to fresh extraction");
                    files = performExtractions(info);
                }
            } else {
                files = performExtractions(info);
            }
            SplitLog.i(TAG, "load found " + files.size() + " lib files");
            return files;
        }
    }

    private List<File> performExtractions(SplitInfo info) throws IOException {
        ZipFile sourceZip = new ZipFile(sourceApk);
        String libPrefix = String.format("lib/%s/", info.getLibInfo().getAbi());
        Enumeration e = sourceZip.entries();
        List<File> libFiles = new ArrayList<>();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();
            if (entryName.charAt(0) < 'l') {
                continue;
            }
            if (entryName.charAt(0) > 'l') {
                break;
            }
            if (!entryName.startsWith("lib/")) {
                continue;
            }
            if (!entryName.endsWith(SplitConstants.DOT_SO) || !entryName.startsWith(libPrefix)) {
                continue;
            }
            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            SplitInfo.LibInfo.Lib lib = findLib(libName, info.getLibInfo().getLibs());
            if (lib == null) {
                throw new IOException(String.format("Failed to find %s in split-info", libName));
            }
            File extractedLib = new File(libDir, libName);
            if (extractedLib.exists()) {
                if (lib.getMd5().equals(FileUtil.getMD5(extractedLib))) {
                    libFiles.add(extractedLib);
                    continue;
                } else {
                    FileUtil.deleteFileSafely(extractedLib);
                    if (extractedLib.exists()) {
                        SplitLog.w(TAG, "Failed to delete corrupted lib file '" + extractedLib.getPath() + "'");
                    }
                }
            }
            SplitLog.i(TAG, "Extraction is needed for lib: " + extractedLib.getAbsolutePath());
            int numAttempts = 0;
            boolean isExtractionSuccessful = false;
            File tempDir = SplitPathManager.require().getSplitTmpDir();
            File tmp = File.createTempFile("tmp-" + libName, "", tempDir);
            while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isExtractionSuccessful) {
                ++numAttempts;
                try {
                    FileOutputStream fos = new FileOutputStream(tmp);
                    FileUtil.copyFile(sourceZip.getInputStream(entry), fos);
                    if (!tmp.renameTo(extractedLib)) {
                        SplitLog.w(TAG, "Failed to rename \"" + tmp.getAbsolutePath() + "\" to \"" + extractedLib.getAbsolutePath() + "\"");
                    } else {
                        isExtractionSuccessful = true;
                    }
                } catch (IOException copyError) {
                    SplitLog.w(TAG, "Failed to extract so :" + libName + ", attempts times : " + numAttempts);
                }
                SplitLog.i(TAG, "Extraction " + (isExtractionSuccessful ? "succeeded" : "failed") + " '" + extractedLib.getAbsolutePath() + "': length " + extractedLib.length());
                //check md5
                String libFileMd5 = FileUtil.getMD5(extractedLib);
                if (!lib.getMd5().equals(libFileMd5)) {
                    SplitLog.w(TAG, "Failed to check %s md5, excepted %s but %s", libName, lib.getMd5(), libFileMd5);
                    isExtractionSuccessful = false;
                }
                if (!isExtractionSuccessful) {
                    FileUtil.deleteFileSafely(extractedLib);
                    if (extractedLib.exists()) {
                        SplitLog.w(TAG, "Failed to delete extracted lib that has been corrupted'" + extractedLib.getPath() + "'");
                    }
                } else {
                    libFiles.add(extractedLib);
                }
            }
            FileUtil.deleteFileSafely(tmp);
            if (!isExtractionSuccessful) {
                throw new IOException("Could not create lib file " + extractedLib.getAbsolutePath() + ")");
            }
        }
        FileUtil.closeQuietly(sourceZip);
        return libFiles;
    }


    private SplitInfo.LibInfo.Lib findLib(String libName, List<SplitInfo.LibInfo.Lib> libs) {
        for (SplitInfo.LibInfo.Lib lib : libs) {
            if (lib.getName().equals(libName)) {
                return lib;
            }
        }
        return null;
    }

    private List<File> loadExistingExtractions(List<SplitInfo.LibInfo.Lib> libs) throws IOException {
        SplitLog.i(TAG, "loading existing lib files");
        File[] files = libDir.listFiles();
        if (files == null || files.length <= 0) {
            throw new IOException("Missing extracted lib file '" + libDir.getPath() + "'");
        }
        List<File> libFiles = new ArrayList<>(files.length);
        for (SplitInfo.LibInfo.Lib lib : libs) {
            boolean hasSo = false;
            for (File file : files) {
                if (lib.getName().equals(file.getName())) {
                    hasSo = true;
                    if (!lib.getMd5().equals(FileUtil.getMD5(file))) {
                        throw new IOException("Invalid extracted lib : file md5 is unmatched!");
                    } else {
                        libFiles.add(file);
                    }
                }
            }
            if (!hasSo) {
                throw new IOException(String.format("Invalid extracted lib: file %s is not existing!", lib.getName()));
            }
        }
        SplitLog.i(TAG, "Existing lib files loaded");
        return libFiles;
    }

    @Override
    public void close() throws IOException {
        lockChannel.close();
        lockRaf.close();
        cacheLock.release();
    }
}
