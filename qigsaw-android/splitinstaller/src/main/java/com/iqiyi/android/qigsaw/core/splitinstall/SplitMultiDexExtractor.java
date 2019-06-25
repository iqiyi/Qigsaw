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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class SplitMultiDexExtractor implements Closeable {

    private static final String TAG = "Split:MultiDexExtractor";
    private static final String DEX_PREFIX = "classes";
    private static final String EXTRACTED_NAME_EXT = ".classes";
    private static final String PREFS_FILE = "split.multidex.version";
    private static final String KEY_TIME_STAMP = "timestamp";
    private static final String KEY_CRC = "crc";
    private static final String KEY_DEX_NUMBER = "dex.number";
    private static final String KEY_DEX_CRC = "dex.crc.";
    private static final String KEY_DEX_TIME = "dex.time.";
    private static final long NO_VALUE = -1L;
    private static final String LOCK_FILENAME = "SplitMultiDex.lock";
    private final File sourceApk;
    private final long sourceCrc;
    private final File dexDir;
    private final RandomAccessFile lockRaf;
    private final FileChannel lockChannel;
    private final FileLock cacheLock;

    SplitMultiDexExtractor(File sourceApk, File dexDir) throws IOException {
        SplitLog.i(TAG, "SplitMultiDexExtractor(" + sourceApk.getPath() + ", " + dexDir.getPath() + ")");
        this.sourceApk = sourceApk;
        this.dexDir = dexDir;
        this.sourceCrc = getZipCrc(sourceApk);
        File lockFile = new File(dexDir, LOCK_FILENAME);
        this.lockRaf = new RandomAccessFile(lockFile, "rw");
        try {
            this.lockChannel = this.lockRaf.getChannel();
            try {
                SplitLog.i(TAG, "Blocking on lock " + lockFile.getPath());
                this.cacheLock = this.lockChannel.lock();
            } catch (RuntimeException | Error | IOException var5) {
                closeQuietly(this.lockChannel);
                throw var5;
            }
            SplitLog.i(TAG, lockFile.getPath() + " locked");
        } catch (RuntimeException | Error | IOException var6) {
            closeQuietly(this.lockRaf);
            throw var6;
        }
    }

    List<? extends File> load(Context context, String prefsKeyPrefix, boolean forceReload) throws IOException {
        SplitLog.i(TAG, "SplitMultiDexExtractor.load(" + this.sourceApk.getPath() + ", " + forceReload + ", " + prefsKeyPrefix + ")");
        if (!this.cacheLock.isValid()) {
            throw new IllegalStateException("SplitMultiDexExtractor was closed");
        } else {
            List files;
            if (!forceReload && !isModified(context, this.sourceApk, this.sourceCrc, prefsKeyPrefix)) {
                try {
                    files = loadExistingExtractions(context, prefsKeyPrefix);
                } catch (IOException var6) {
                    SplitLog.w(TAG, "Failed to reload existing extracted secondary dex files, falling back to fresh extraction", var6);
                    files = this.performExtractions();
                    putStoredApkInfo(context, prefsKeyPrefix, getTimeStamp(this.sourceApk), this.sourceCrc, files);
                }
            } else {
                if (forceReload) {
                    SplitLog.i(TAG, "Forced extraction must be performed.");
                } else {
                    SplitLog.i(TAG, "Detected that extraction must be performed.");
                }
                files = this.performExtractions();
                putStoredApkInfo(context, prefsKeyPrefix, getTimeStamp(this.sourceApk), this.sourceCrc, files);
            }

            SplitLog.i(TAG, "load found " + files.size() + " secondary dex files");
            return files;
        }
    }

    public void close() throws IOException {
        this.cacheLock.release();
        this.lockChannel.close();
        this.lockRaf.close();
    }

    private List<? extends File> loadExistingExtractions(Context context, String prefsKeyPrefix) throws IOException {
        SplitLog.i(TAG, "loading existing secondary dex files");
        String extractedFilePrefix = sourceApk.getName() + EXTRACTED_NAME_EXT;
        SharedPreferences multiDexPreferences = getMultiDexPreferences(context);
        int totalDexNumber = multiDexPreferences.getInt(prefsKeyPrefix + KEY_DEX_NUMBER, 1);
        List<ExtractedDex> files = new ArrayList<>(totalDexNumber - 1);

        for (int secondaryNumber = 2; secondaryNumber <= totalDexNumber; ++secondaryNumber) {
            String fileName = extractedFilePrefix + secondaryNumber + SplitConstants.DOT_ZIP;
            SplitMultiDexExtractor.ExtractedDex extractedFile = new SplitMultiDexExtractor.ExtractedDex(dexDir, fileName);
            if (!extractedFile.isFile()) {
                throw new IOException("Missing extracted secondary dex file '" + extractedFile.getPath() + "'");
            }
            extractedFile.crc = getZipCrc(extractedFile);
            long expectedCrc = multiDexPreferences.getLong(prefsKeyPrefix + KEY_DEX_CRC + secondaryNumber, NO_VALUE);
            long expectedModTime = multiDexPreferences.getLong(prefsKeyPrefix + KEY_DEX_TIME + secondaryNumber, NO_VALUE);
            long lastModified = extractedFile.lastModified();
            if (expectedModTime != lastModified || expectedCrc != extractedFile.crc) {
                throw new IOException("Invalid extracted dex: " + extractedFile + " (key \"" + prefsKeyPrefix + "\"), expected modification time: " + expectedModTime + ", modification time: " + lastModified + ", expected crc: " + expectedCrc + ", file crc: " + extractedFile.crc);
            }
            files.add(extractedFile);
        }
        SplitLog.i(TAG, "Existing secondary dex files loaded");
        return files;
    }

    private static boolean isModified(Context context, File archive, long currentCrc, String prefsKeyPrefix) {
        SharedPreferences prefs = getMultiDexPreferences(context);
        return prefs.getLong(prefsKeyPrefix + KEY_TIME_STAMP, -NO_VALUE) != getTimeStamp(archive) || prefs.getLong(prefsKeyPrefix + KEY_CRC, -NO_VALUE) != currentCrc;
    }

    private static long getTimeStamp(File archive) {
        long timeStamp = archive.lastModified();
        if (timeStamp == -NO_VALUE) {
            --timeStamp;
        }

        return timeStamp;
    }

    private static long getZipCrc(File archive) throws IOException {
        long computedValue = ZipCrcUtil.getZipCrc(archive);
        if (computedValue == -NO_VALUE) {
            --computedValue;
        }
        return computedValue;
    }


    private List<ExtractedDex> performExtractions() throws IOException {
        String extractedFilePrefix = this.sourceApk.getName() + EXTRACTED_NAME_EXT;
        this.clearDexDir();
        List<ExtractedDex> files = new ArrayList<>();
        ZipFile apk = new ZipFile(this.sourceApk);
        try {
            int secondaryNumber = 2;
            for (ZipEntry dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + SplitConstants.DOT_DEX); dexFile != null; dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + SplitConstants.DOT_DEX)) {
                String fileName = extractedFilePrefix + secondaryNumber + SplitConstants.DOT_ZIP;
                SplitMultiDexExtractor.ExtractedDex extractedFile = new SplitMultiDexExtractor.ExtractedDex(this.dexDir, fileName);
                files.add(extractedFile);
                SplitLog.i(TAG, "Extraction is needed for file " + extractedFile);
                int numAttempts = 0;
                boolean isExtractionSuccessful = false;
                while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isExtractionSuccessful) {
                    ++numAttempts;
                    extract(apk, dexFile, extractedFile, extractedFilePrefix);
                    try {
                        extractedFile.crc = getZipCrc(extractedFile);
                        isExtractionSuccessful = true;
                    } catch (IOException var18) {
                        isExtractionSuccessful = false;
                        SplitLog.w(TAG, "Failed to read crc from " + extractedFile.getAbsolutePath(), var18);
                    }
                    SplitLog.i(TAG, "Extraction " + (isExtractionSuccessful ? "succeeded" : "failed") + " '" + extractedFile.getAbsolutePath() + "': length " + extractedFile.length() + " - crc: " + extractedFile.crc);
                    if (!isExtractionSuccessful) {
                        extractedFile.delete();
                        if (extractedFile.exists()) {
                            SplitLog.w(TAG, "Failed to delete corrupted secondary dex '" + extractedFile.getPath() + "'");
                        }
                    }
                }
                if (!isExtractionSuccessful) {
                    throw new IOException("Could not create zip file " + extractedFile.getAbsolutePath() + " for secondary dex (" + secondaryNumber + ")");
                }
                ++secondaryNumber;
            }
        } finally {
            try {
                apk.close();
            } catch (IOException var17) {
                SplitLog.w(TAG, "Failed to close resource", var17);
            }

        }
        return files;
    }

    private static void putStoredApkInfo(Context context, String keyPrefix, long timeStamp, long crc, List<ExtractedDex> extractedDexes) {
        SharedPreferences prefs = getMultiDexPreferences(context);
        Editor edit = prefs.edit();
        edit.putLong(keyPrefix + KEY_TIME_STAMP, timeStamp);
        edit.putLong(keyPrefix + KEY_CRC, crc);
        edit.putInt(keyPrefix + KEY_DEX_NUMBER, extractedDexes.size() + 1);
        int extractedDexId = 2;
        for (Iterator var10 = extractedDexes.iterator(); var10.hasNext(); ++extractedDexId) {
            SplitMultiDexExtractor.ExtractedDex dex = (SplitMultiDexExtractor.ExtractedDex) var10.next();
            edit.putLong(keyPrefix + KEY_DEX_CRC + extractedDexId, dex.crc);
            edit.putLong(keyPrefix + KEY_DEX_TIME + extractedDexId, dex.lastModified());
        }
        edit.apply();
    }

    private static SharedPreferences getMultiDexPreferences(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_MULTI_PROCESS);
    }

    private void clearDexDir() {
        File[] files = this.dexDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return !pathname.getName().equals(LOCK_FILENAME);
            }
        });
        if (files == null) {
            SplitLog.w(TAG, "Failed to list secondary dex dir content (" + this.dexDir.getPath() + ").");
        } else {
            File[] var2 = files;
            int var3 = files.length;
            for (int var4 = 0; var4 < var3; ++var4) {
                File oldFile = var2[var4];
                SplitLog.i(TAG, "Trying to delete old file " + oldFile.getPath() + " of size " + oldFile.length());
                if (!oldFile.delete()) {
                    SplitLog.w(TAG, "Failed to delete old file " + oldFile.getPath());
                } else {
                    SplitLog.i(TAG, "Deleted old file " + oldFile.getPath());
                }
            }
        }
    }

    private static void extract(ZipFile apk, ZipEntry dexFile, File extractTo, String extractedFilePrefix) throws IOException {
        InputStream in = apk.getInputStream(dexFile);
        ZipOutputStream out;
        File tmp = File.createTempFile("tmp-" + extractedFilePrefix, SplitConstants.DOT_ZIP, extractTo.getParentFile());
        SplitLog.i(TAG, "Extracting " + tmp.getPath());
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            try {
                ZipEntry classesDex = new ZipEntry("classes.dex");
                classesDex.setTime(dexFile.getTime());
                out.putNextEntry(classesDex);
                byte[] buffer = new byte[16384];

                for (int length = in.read(buffer); length != -1; length = in.read(buffer)) {
                    out.write(buffer, 0, length);
                }
                out.closeEntry();
            } finally {
                closeQuietly(out);
            }
            if (!tmp.setReadOnly()) {
                throw new IOException("Failed to mark readonly \"" + tmp.getAbsolutePath() + "\" (tmp of \"" + extractTo.getAbsolutePath() + "\")");
            }
            SplitLog.i(TAG, "Renaming to " + extractTo.getPath());
            if (!tmp.renameTo(extractTo)) {
                throw new IOException("Failed to rename \"" + tmp.getAbsolutePath() + "\" to \"" + extractTo.getAbsolutePath() + "\"");
            }
        } finally {
            closeQuietly(in);
            tmp.delete();
        }

    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException var2) {
            SplitLog.w(TAG, "Failed to close resource", var2);
        }

    }

    private static class ExtractedDex extends File {
        long crc = -NO_VALUE;

        ExtractedDex(File dexDir, String fileName) {
            super(dexDir, fileName);
        }
    }

    static final class ZipCrcUtil {

        static long getZipCrc(File apk) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(apk, "r");
            long var3;
            try {
                CentralDirectory dir = findCentralDirectory(raf);
                var3 = computeCrcOfCentralDir(raf, dir);
            } finally {
                raf.close();
            }
            return var3;
        }

        private static CentralDirectory findCentralDirectory(RandomAccessFile raf) throws IOException, ZipException {
            long scanOffset = raf.length() - 22L;
            if (scanOffset < 0L) {
                throw new ZipException("File too short to be a zip file: " + raf.length());
            } else {
                long stopOffset = scanOffset - 65536L;
                if (stopOffset < 0L) {
                    stopOffset = 0L;
                }

                int endSig = Integer.reverseBytes(101010256);

                do {
                    raf.seek(scanOffset);
                    if (raf.readInt() == endSig) {
                        raf.skipBytes(2);
                        raf.skipBytes(2);
                        raf.skipBytes(2);
                        raf.skipBytes(2);
                        CentralDirectory dir = new CentralDirectory();
                        dir.size = (long) Integer.reverseBytes(raf.readInt()) & 4294967295L;
                        dir.offset = (long) Integer.reverseBytes(raf.readInt()) & 4294967295L;
                        return dir;
                    }

                    --scanOffset;
                } while (scanOffset >= stopOffset);

                throw new ZipException("End Of Central Directory signature not found");
            }
        }

        private static long computeCrcOfCentralDir(RandomAccessFile raf, CentralDirectory dir) throws IOException {
            CRC32 crc = new CRC32();
            long stillToRead = dir.size;
            raf.seek(dir.offset);
            int length = (int) Math.min(16384L, stillToRead);
            byte[] buffer = new byte[16384];

            for (length = raf.read(buffer, 0, length); length != -1; length = raf.read(buffer, 0, length)) {
                crc.update(buffer, 0, length);
                stillToRead -= (long) length;
                if (stillToRead == 0L) {
                    break;
                }

                length = (int) Math.min(16384L, stillToRead);
            }

            return crc.getValue();
        }

    }

    static class CentralDirectory {
        long offset;
        long size;

        CentralDirectory() {

        }
    }

}
