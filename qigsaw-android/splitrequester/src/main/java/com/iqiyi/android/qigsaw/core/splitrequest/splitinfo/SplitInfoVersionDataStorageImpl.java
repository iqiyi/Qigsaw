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

package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;

final class SplitInfoVersionDataStorageImpl implements SplitInfoVersionDataStorage {

    private static final String NEW_VERSION = "newVersion";

    private static final String OLD_VERSION = "oldVersion";

    private static final String TAG = "SplitInfoVersionStorageImpl";

    private final File versionDataFile;

    private static final String VERSION_DATA_NAME = "version.info";

    private static final String VERSION_DATA_LOCK_NAME = "version.lock";

    private final RandomAccessFile lockRaf;

    private final FileChannel lockChannel;

    private final FileLock cacheLock;

    SplitInfoVersionDataStorageImpl(File rootDir) throws IOException {
        this.versionDataFile = new File(rootDir, VERSION_DATA_NAME);
        File lockFile = new File(rootDir, VERSION_DATA_LOCK_NAME);
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

    @Override
    public SplitInfoVersionData readVersionData() {
        if (!cacheLock.isValid()) {
            throw new IllegalStateException("SplitInfoVersionDataStorage was closed");
        } else {
            if (versionDataFile.exists()) {
                return readVersionDataProperties(versionDataFile);
            }
        }
        return null;
    }

    @Override
    public boolean updateVersionData(SplitInfoVersionData versionData) {
        if (!cacheLock.isValid()) {
            throw new IllegalStateException("SplitInfoVersionDataStorage was closed");
        } else {
            return updateVersionDataProperties(versionDataFile, versionData);
        }
    }

    @Override
    public void close() throws IOException {
        lockChannel.close();
        lockRaf.close();
        cacheLock.release();
    }

    private static SplitInfoVersionData readVersionDataProperties(File versionDataFile) {
        boolean isReadPatchSuccessful = false;
        int numAttempts = 0;
        String oldVer = null;
        String newVer = null;
        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isReadPatchSuccessful) {
            numAttempts++;
            Properties properties = new Properties();
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(versionDataFile);
                properties.load(inputStream);
                oldVer = properties.getProperty(OLD_VERSION);
                newVer = properties.getProperty(NEW_VERSION);
            } catch (IOException e) {
                SplitLog.w(TAG, "read property failed, e:" + e);
            } finally {
                FileUtil.closeQuietly(inputStream);
            }

            if (oldVer == null || newVer == null) {
                continue;
            }
            isReadPatchSuccessful = true;
        }

        if (isReadPatchSuccessful) {
            return new SplitInfoVersionData(oldVer, newVer);
        }
        return null;
    }


    private boolean updateVersionDataProperties(File versionDataFile, SplitInfoVersionData versionData) {
        if (versionDataFile == null || versionData == null) {
            return false;
        }
        SplitLog.i(TAG, "updateVersionDataProperties file path:"
                + versionDataFile.getAbsolutePath()
                + " , oldVer:"
                + versionData.oldVersion
                + ", newVer:"
                + versionData.newVersion);

        boolean isWritePatchSuccessful = false;
        int numAttempts = 0;

        File parentFile = versionDataFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isWritePatchSuccessful) {
            numAttempts++;

            Properties newProperties = new Properties();
            newProperties.put(OLD_VERSION, versionData.oldVersion);
            newProperties.put(NEW_VERSION, versionData.newVersion);
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(versionDataFile, false);
                String comment = "from old version:" + versionData.oldVersion + " to new version:" + versionData.newVersion;
                newProperties.store(outputStream, comment);
            } catch (Exception e) {
                SplitLog.w(TAG, "write property failed, e:" + e);
            } finally {
                FileUtil.closeQuietly(outputStream);
            }

            SplitInfoVersionData tempInfo = readVersionDataProperties(versionDataFile);

            isWritePatchSuccessful = tempInfo != null && tempInfo.oldVersion.equals(versionData.oldVersion) && tempInfo.newVersion.equals(versionData.newVersion);
            if (!isWritePatchSuccessful) {
                versionDataFile.delete();
            }
        }
        return isWritePatchSuccessful;
    }

}
