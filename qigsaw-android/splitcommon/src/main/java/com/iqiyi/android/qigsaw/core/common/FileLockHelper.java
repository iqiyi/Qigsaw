package com.iqiyi.android.qigsaw.core.common;

import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * Source code from tinker.
 */
class FileLockHelper implements Closeable {
    private static final int LOCK_WAIT_EACH_TIME = 10;
    private static final String TAG = "Split.FileLockHelper";
    private final FileOutputStream outputStream;
    private final FileLock fileLock;

    private FileLockHelper(File lockFile) throws IOException {
        outputStream = new FileOutputStream(lockFile);

        int numAttempts = 0;
        boolean isGetLockSuccess;
        FileLock localFileLock = null;
        //just wait twice,
        Exception saveException = null;
        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS) {
            numAttempts++;
            try {
                localFileLock = outputStream.getChannel().lock();
                isGetLockSuccess = (localFileLock != null);
                if (isGetLockSuccess) {
                    break;
                }

            } catch (Exception e) {
                saveException = e;
                Log.e(TAG, "getInfoLock Thread failed time:" + LOCK_WAIT_EACH_TIME);
            }

            //it can just sleep 0, afraid of cpu scheduling
            try {
                Thread.sleep(LOCK_WAIT_EACH_TIME);
            } catch (Exception ignore) {
                Log.e(TAG, "getInfoLock Thread sleep exception", ignore);
            }
        }

        if (localFileLock == null) {
            throw new IOException("Tinker Exception:FileLockHelper lock file failed: " + lockFile.getAbsolutePath(), saveException);
        }
        fileLock = localFileLock;
    }

    static FileLockHelper getFileLock(File lockFile) throws IOException {
        return new FileLockHelper(lockFile);
    }

    @Override
    public void close() throws IOException {
        try {
            if (fileLock != null) {
                fileLock.release();
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
}
