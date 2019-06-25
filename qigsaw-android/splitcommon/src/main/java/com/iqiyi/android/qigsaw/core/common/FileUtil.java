package com.iqiyi.android.qigsaw.core.common;


import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.RestrictTo;

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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class FileUtil {

    private FileUtil() {

    }

    private static final String TAG = "SplitFileUtil";

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

    public static boolean safeDeleteFile(File file) {
        if (file == null) {
            return true;
        }

        if (file.exists()) {
            SplitLog.i(TAG, "safeDeleteFile, try to delete path: " + file.getPath());

            boolean deleted = file.delete();
            if (!deleted) {
                SplitLog.e(TAG, "Failed to delete file, try to delete when exit. path: " + file.getPath());
                file.deleteOnExit();
            }
            return deleted;
        }
        return true;
    }

    public static boolean deleteDir(File file) {
        return deleteDir(file, true);
    }

    public static boolean deleteDir(File file, boolean deleteRootDir) {
        if (file == null || (!file.exists())) {
            return false;
        }
        if (file.isFile()) {
            safeDeleteFile(file);
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    deleteDir(subFile);
                }
                if (deleteRootDir) {
                    safeDeleteFile(file);
                }
            }
        }
        return true;
    }
}
