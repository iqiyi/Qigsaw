package com.iqiyi.qigsaw.buildtool.gradle.internal.tool

import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/*source code from https://github.com/shwenzhang/AndResGuard*/

class ZipUtils {

    private static final int BUFFER = 8192

    static boolean checkDirectory(File dir) {
        FileUtils.deleteDir(dir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return true
    }

    @SuppressWarnings("rawtypes")
    static HashMap<String, Integer> unzipApk(File apkFile, File outputDir) throws IOException {
        checkDirectory(outputDir)
        ZipFile zipFile = new ZipFile(apkFile)
        Enumeration emu = zipFile.entries()
        HashMap<String, Integer> compress = new HashMap<>()
        try {
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement()
                if (entry.isDirectory()) {
                    new File(outputDir, entry.getName()).mkdirs()
                    continue
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry))

                File file = new File(outputDir, entry.getName())

                File parent = file.getParentFile()
                if (parent != null && (!parent.exists())) {
                    parent.mkdirs()
                }
                String compatibaleResult = entry.getName()
                if (compatibaleResult.contains("\\")) {
                    compatibaleResult = compatibaleResult.replace("\\", "/")
                }
                compress.put(compatibaleResult, entry.getMethod())
                FileOutputStream fos = new FileOutputStream(file)
                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER)

                byte[] buf = new byte[BUFFER]
                int len
                while ((len = bis.read(buf, 0, BUFFER)) != -1) {
                    fos.write(buf, 0, len)
                }
                bos.flush()
                bos.close()
                bis.close()
            }
        } finally {
            zipFile.close()
        }
        return compress
    }

    static void zipFiles(
            Collection<File> resFileList, File baseFolder, File outputZip, HashMap<String, Integer> compressData)
            throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputZip), BUFFER))
        for (File resFile : resFileList) {
            if (resFile.exists()) {
                if (resFile.getAbsolutePath().contains(baseFolder.getAbsolutePath())) {
                    String relativePath = baseFolder.toURI().relativize(resFile.getParentFile().toURI()).getPath()
                    // remove slash at end of relativePath
                    if (relativePath.length() > 1) {
                        relativePath = relativePath.substring(0, relativePath.length() - 1)
                    } else {
                        relativePath = ""
                    }
                    zipFile(resFile, zipOut, relativePath, compressData)
                } else {
                    zipFile(resFile, zipOut, "", compressData)
                }
            }
        }
        zipOut.close()
    }

    static void zipFile(File resFile, ZipOutputStream zipout, String rootpath, HashMap<String, Integer> compressData) throws IOException {
        rootpath = rootpath + (rootpath.trim().length() == 0 ? "" : File.separator) + resFile.getName()
        if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles()
            for (File file : fileList) {
                zipFile(file, zipout, rootpath, compressData)
            }
        } else {
            final byte[] fileContents = readContents(resFile)
            //compat for linux
            if (rootpath.contains("\\")) {
                rootpath = rootpath.replace("\\", "/")
            }
            if (!compressData.containsKey(rootpath)) {
                System.err.printf(String.format("do not have the compress data path =%s in resource.asrc\n", rootpath));
                return
            }
            int compressMethod = compressData.get(rootpath)
            ZipEntry entry = new ZipEntry(rootpath)
            if (compressMethod == ZipEntry.DEFLATED) {
                entry.setMethod(ZipEntry.DEFLATED)
            } else {
                entry.setMethod(ZipEntry.STORED)
                entry.setSize(fileContents.length)
                final CRC32 checksumCalculator = new CRC32()
                checksumCalculator.update(fileContents)
                entry.setCrc(checksumCalculator.getValue())
            }
            zipout.putNextEntry(entry)
            zipout.write(fileContents)
            zipout.flush()
            zipout.closeEntry()
        }
    }

    static byte[] readContents(final File file) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream()
        final int bufferSize = 4096
        try {
            final FileInputStream ins = new FileInputStream(file)
            final BufferedInputStream bIn = new BufferedInputStream(ins)
            int length
            byte[] buffer = new byte[bufferSize]
            byte[] bufferCopy
            while ((length = bIn.read(buffer, 0, bufferSize)) != -1) {
                bufferCopy = new byte[length]
                System.arraycopy(buffer, 0, bufferCopy, 0, length)
                output.write(bufferCopy)
            }
            bIn.close()
        } finally {
            output.close()
        }
        return output.toByteArray()
    }

}

