package com.iqiyi.android.qigsaw.core.common;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.zip.ZipFile;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class FileUtil {

    private FileUtil() {

    }

    private static final String TAG = "Split.FileUtil";

    public static void copyFile(InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        try {
            byte[] buffer = new byte[16384];
            for (int length = bufferedInput.read(buffer); length != -1; length = bufferedInput.read(buffer)) {
                bufferedOutput.write(buffer, 0, length);
            }
            bufferedOutput.flush();
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    public static void createFileSafely(@NonNull File file) throws IOException {
        if (!file.exists()) {
            boolean isCreationSuccessful = false;
            int numAttempts = 0;
            Exception cause = null;
            while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isCreationSuccessful) {
                numAttempts++;
                try {
                    if (!file.createNewFile()) {
                        SplitLog.w(TAG, "File %s already exists", file.getAbsolutePath());
                    }
                    isCreationSuccessful = true;
                } catch (Exception e) {
                    isCreationSuccessful = false;
                    cause = e;
                }
            }
            if (!isCreationSuccessful) {
                throw new IOException("Failed to create file " + file.getAbsolutePath(), cause);
            } else {
                SplitLog.v(TAG, "Succeed to create file " + file.getAbsolutePath());
            }
        }
    }

    public static synchronized boolean deleteFileSafelyLock(@NonNull File file, File lockFile) throws IOException {
        if (!file.exists()) {
            return true;
        }
        FileLockHelper fileLock = null;
        boolean ret;
        try {
            fileLock = FileLockHelper.getFileLock(lockFile);
            ret = deleteFileSafely(file);
        } catch (IOException e) {
            throw new IOException("Failed to lock file " + lockFile.getAbsolutePath());
        } finally {
            if (lockFile != null) {
                closeQuietly(fileLock);
            }
        }
        return ret;
    }

    public static synchronized void createFileSafelyLock(@NonNull File file, File lockFile) throws IOException {
        if (file.exists()) {
            return;
        }
        FileLockHelper fileLock = null;
        try {
            fileLock = FileLockHelper.getFileLock(lockFile);
            try {
                createFileSafely(file);
            } catch (IOException e) {
                throw new IOException("Failed to create file " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IOException("Failed to lock file " + lockFile.getAbsolutePath());
        } finally {
            if (lockFile != null) {
                closeQuietly(fileLock);
            }
        }
    }

    public static boolean deleteFileSafely(@NonNull File file) {
        if (!file.exists()) {
            return true;
        }
        boolean isDeleteSuccessful = false;
        int numAttempts = 0;
        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isDeleteSuccessful) {
            numAttempts++;
            if (file.delete()) {
                isDeleteSuccessful = true;
            }
        }
        SplitLog.d(TAG, "%s to delete file: " + file.getAbsolutePath(), isDeleteSuccessful ? "Succeed" : "Failed");
        return isDeleteSuccessful;
    }

    public static void copyFile(File source, File dest) throws IOException {
        copyFile(new FileInputStream(source), new FileOutputStream(dest));
    }

    /**
     * Get the md5 for the file. calling getMD5(FileInputStream is, int bufLen) inside.
     *
     * @param file
     */
    public static String getMD5(final File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            return getMD5(fin);
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(fin);
        }
    }

    /**
     * Get the md5 for inputStream.
     * This method costs less memory. It reads bufLen bytes from the FileInputStream once.
     *
     * @param is
     */
    public static String getMD5(final InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder md5Str = new StringBuilder(32);

            byte[] buf = new byte[1024 * 100];
            int readCount;
            while ((readCount = bis.read(buf)) != -1) {
                md.update(buf, 0, readCount);
            }

            byte[] hashValue = md.digest();

            for (int i = 0; i < hashValue.length; i++) {
                md5Str.append(Integer.toString((hashValue[i] & 0xff) + 0x100, 16).substring(1));
            }
            return md5Str.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Closes the given {@code obj}. Suppresses any exceptions.
     */
    @SuppressLint("NewApi")
    public static void closeQuietly(Object obj) {
        if (obj == null) return;
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else if (Build.VERSION.SDK_INT >= 19 && obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else if (obj instanceof ZipFile) {
            try {
                ((ZipFile) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else {
            throw new IllegalArgumentException("obj: " + obj + " cannot be closed.");
        }
    }

    public static boolean isLegalFile(File file) {
        return file != null && file.exists() && file.canRead() && file.isFile() && file.length() > 0;
    }

    public static boolean deleteDir(File file) {
        return deleteDir(file, true);
    }

    public static boolean deleteDir(File file, boolean deleteRootDir) {
        if (file == null || (!file.exists())) {
            return false;
        }
        if (file.isFile()) {
            deleteFileSafely(file);
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    deleteDir(subFile);
                }
                if (deleteRootDir) {
                    deleteFileSafely(file);
                }
            }
        }
        return true;
    }
}
