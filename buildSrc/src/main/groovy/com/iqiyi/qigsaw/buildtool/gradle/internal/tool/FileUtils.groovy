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

package com.iqiyi.qigsaw.buildtool.gradle.internal.tool

import com.google.gson.Gson

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class FileUtils {

    static final int BUFFER = 8192

    static String getMD5(File file) {
        MessageDigest digest
        try {
            digest = MessageDigest.getInstance("MD5")
        } catch (NoSuchAlgorithmException e) {
            return null
        }
        InputStream is
        try {
            is = new FileInputStream(file)
        } catch (FileNotFoundException e) {
            return null
        }

        byte[] buffer = new byte[BUFFER]
        int read
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
            byte[] md5sum = digest.digest()
            BigInteger bigInt = new BigInteger(1, md5sum)
            String output = bigInt.toString(16)
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0')
            return output
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e)
        } finally {
            closeQuietly(is)
        }
    }

    static boolean createFileForTypeClass(Object typeClass, File dest) {
        try {
            Gson gson = new Gson()
            String splitDetailsStr = gson.toJson(typeClass)
            dest.createNewFile()
            BufferedOutputStream osm = new BufferedOutputStream(new FileOutputStream(dest))
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(osm))
            writer.write(splitDetailsStr)
            writer.close()
            osm.close()
        } catch (Throwable e) {
            return false
        }
        return true
    }

    static void copyFile(InputStream source, OutputStream dest)
            throws IOException {
        InputStream is = source
        OutputStream os = dest
        try {
            byte[] buffer = new byte[BUFFER]
            int length
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length)
            }
        } finally {
            closeQuietly(is)
            closeQuietly(os)
        }
    }

    static void copyFile(File source, File dest, boolean log)
            throws IOException {
        copyFile(new FileInputStream(source), new FileOutputStream(dest))
        if (log) {
            QigsawLogger.w("Succeed to copy ${source.absolutePath} to ${dest.absolutePath}")
        }
    }

    static void copyFile(File source, File dest)
            throws IOException {
        copyFile(source, dest, true)
    }

    static void closeQuietly(Closeable obj) {
        if (obj != null) {
            try {
                obj.close()
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }
}
