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

package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.SdkConstants
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitDetails
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.CommandUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.QigsawLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TypeClassFileParser
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ZipUtils
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploader
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploaderInstance
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry

class QigsawUploadSplitApkTask extends DefaultTask {

    static final String OLD_APK_EXTRACTION = "old_apk_extraction"

    static final String NEW_SPLIT_INFO = "new_split_info"

    static final String STORED_FILES = "store_files"

    static final String NEW_APKS = "new_apks"

    @InputFile
    @Optional
    File oldApk

    @Input
    String variantName

    @OutputDirectory
    File outputDir

    @OutputDirectory
    File packageOutputDir

    void initArgs(File oldApk, File outputDir, File packageOutputDir, String variantName) {
        this.oldApk = oldApk
        this.variantName = variantName
        this.outputDir = outputDir
        this.packageOutputDir = packageOutputDir
    }

    @TaskAction
    void uploadSplitApk() {
        SplitApkUploader splitApkUploader = SplitApkUploaderInstance.get()
        if (splitApkUploader == null) {
            QigsawLogger.e("SplitApkUploader has not been initialized!!!")
            return
        }
        if (oldApk == null || !oldApk.exists() || !oldApk.isFile() || oldApk.length() <= 0) {
            throw new GradleException("old apk file is illegal!")
        }
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        outputDir.mkdirs()
        String oldApkExtractionPath = outputDir.absolutePath + File.separator + OLD_APK_EXTRACTION
        Map<String, Integer> compressData = ZipUtils.unZipAPk(oldApk.absolutePath, oldApkExtractionPath)
        File oldApkAssetsDir = new File(oldApkExtractionPath + File.separator + "assets")
        if (!oldApkAssetsDir.exists()) {
            throw new GradleException("Can't find assets directory in old apk file!")
        }
        File[] oldSplitJsonFiles = oldApkAssetsDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.startsWith("qigsaw_") && file.name.endsWith(SdkConstants.DOT_JSON)
            }
        })
        if (oldSplitJsonFiles == null || oldSplitJsonFiles.length != 1) {
            throw new GradleException("Can't find old split json in old apk file!")
        }
        File oldSplitJsonFile = oldSplitJsonFiles[0]
        SplitDetails splitDetails = TypeClassFileParser.parseFile(oldSplitJsonFile, SplitDetails)
        List<SplitInfo> splitInfoList = splitDetails.splits
        boolean assetsBuiltIn = splitDetails.builtInUrlPrefix.startsWith("assets://")
        File oldApkLibDir = new File(oldApkExtractionPath + File.separator + "lib")
        String splitApkSuffix = assetsBuiltIn ? SdkConstants.DOT_ZIP : SdkConstants.DOT_NATIVE_LIBS
        splitInfoList.each {
            if (it.onDemand) {
                File splitApk = assetsBuiltIn ? new File(oldApkAssetsDir, it.splitName + splitApkSuffix) :
                        new File(oldApkLibDir, splitDetails.abiFilters.getAt(0) + File.separator + "libsplit_${it.splitName + splitApkSuffix}")
                if (!splitApk.exists()) {
                    throw new GradleException("Failed to find split ${it.splitName} apk file ${splitApk.absolutePath}")
                }
                try {
                    String splitApkUrl = splitApkUploader.uploadSync(project, splitApk, it.splitName)
                    if (splitApkUrl == null || !splitApkUrl.startsWith("http")) {
                        throw new GradleException("Split apk url ${splitApkUrl} is illegal!")
                    }
                    QigsawLogger.w("Succeed to upload ${it.splitName} to ${splitApkUrl}")
                    it.url = splitApkUrl
                    it.builtIn = false
                    if (splitApk.delete()) {
                        QigsawLogger.w("Succeed to delete split apk ${splitApk.absolutePath}")
                    }
                } catch (Throwable e) {
                    throw new GradleException("Unable to upload split apk ${splitApk.absolutePath}", e)
                }
            }
        }
        File newSplitInfoDir = new File(outputDir, NEW_SPLIT_INFO)
        newSplitInfoDir.mkdirs()
        File newSplitJsonFile = new File(newSplitInfoDir, oldSplitJsonFile.name)
        if (!FileUtils.createFileForTypeClass(splitDetails, newSplitJsonFile)) {
            throw new GradleException("Failed to create new split json file!")
        }
        if (!oldSplitJsonFile.delete()) {
            throw new GradleException("Failed to delete old split json file ${oldSplitJsonFile.absolutePath}")
        }
        FileUtils.copyFile(newSplitJsonFile, oldSplitJsonFile)
        File newApksDir = new File(outputDir, NEW_APKS)
        newApksDir.mkdirs()
        Collection<File> resFiles = new ArrayList<>(0)
        Collections.addAll(resFiles, new File(oldApkExtractionPath).listFiles())
        File unsignedApk = new File(newApksDir, oldApk.name.replace(".apk", "_unsigned.apk"))
        ZipUtils.zipFiles(resFiles, new File(oldApkExtractionPath), unsignedApk, compressData)
        SplitApkSigner apkSigner = new SplitApkSigner(project, variantName)
        File signedApk = new File(newApksDir, oldApk.name.replace(".apk", "_signed.apk"))
        try {
            apkSigner.signAPKIfNeed(unsignedApk, signedApk)
        } catch (Throwable ignored) {
            QigsawLogger.e("Can't find signingConfigs in app build.gradle")
        }
        //generate 7zip format apk files
        File unsigned7zaApk = new File(newApksDir, oldApk.name.replace(".apk", "_7za_unsigned.apk"))
        boolean createOk = run7zCmd("7za", "a", "-tzip", unsigned7zaApk.absolutePath, oldApkExtractionPath + File.separator + "*", "-mx9")
        if (createOk) {
            List<String> storedFiles = new ArrayList<>()
            for (String name : compressData.keySet()) {
                File file = new File(oldApkExtractionPath, name)
                if (!file.exists()) {
                    continue
                }
                int method = compressData.get(name)
                if (method == ZipEntry.STORED) {
                    storedFiles.add(name)
                }
            }
            if (storedFiles.size() > 0) {
                File storeFilesDir = new File(outputDir, STORED_FILES)
                storeFilesDir.mkdirs()
                for (String name : storedFiles) {
                    File storeFile = new File(storeFilesDir, name)
                    File parent = storeFile.getParentFile()
                    if (parent != null && (!parent.exists())) {
                        parent.mkdirs()
                    }
                    FileUtils.copyFile(new File(oldApkExtractionPath, name), storeFile)
                }
                boolean updateOk = run7zCmd("7za", "a", "-tzip", unsigned7zaApk.absolutePath, storeFilesDir.absolutePath + File.separator + "*", "-mx0")
                if (updateOk) {
                    File signed7zaApk = new File(newApksDir, oldApk.name.replace(".apk", "_7za_signed.apk"))
                    try {
                        apkSigner.signAPKIfNeed(unsigned7zaApk, signed7zaApk)
                    } catch (Throwable ignored) {
                        QigsawLogger.e("Can't find signingConfigs in app build.gradle")
                    }
                }
            }
        }
        //copy target products to package output dir.
        if (packageOutputDir.exists()) {
            packageOutputDir.deleteDir()
        }
        File qigsawApksOutputDir = new File(packageOutputDir, "qigsaw_apks")
        qigsawApksOutputDir.mkdirs()
        FileUtils.copyFile(newSplitJsonFile, new File(packageOutputDir, newSplitJsonFile.name))
        if (newApksDir.listFiles() != null) {
            newApksDir.listFiles().each {
                FileUtils.copyFile(it, new File(qigsawApksOutputDir, it.name))
            }
        }
    }

    static boolean run7zCmd(String... cmd) {
        try {
            String cmdResult = CommandUtils.runCmd(cmd)
            QigsawLogger.w("Run command successfully, result: " + cmdResult)
            return true
        } catch (Throwable e) {
            QigsawLogger.e("'7za' command is not found, have you install 7zip?", e)
        }
        return false
    }
}
