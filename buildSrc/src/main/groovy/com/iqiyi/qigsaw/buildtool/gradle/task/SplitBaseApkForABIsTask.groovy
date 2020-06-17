package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.SdkConstants
import com.android.builder.model.SigningConfig
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.CommandUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ZipUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class SplitBaseApkForABIsTask extends DefaultTask {

    static final List<String> SUPPORTED_ABIS = ["armeabi", "armeabi-v7a", "arm64-v8a", "x86", "x86_64"]

    def baseVariant

    ApkSigner apkSigner

    @Input
    boolean use7z

    @Input
    Set<String> dynamicFeaturesNames

    @InputFile
    File baseAppCpuAbiListFile

    @InputFiles
    List<File> baseApkFiles

    @OutputDirectory
    File packageAppDir

    @OutputDirectory
    File baseApksDir

    @OutputDirectory
    File unzipBaseApkDir

    @TaskAction
    void splitBaseApk() {
        if (baseApkFiles.size() > 1) {
            throw new GradleException("Qigsaw Error: Qigsaw don't support multi-apks.")
        }
        if (unzipBaseApkDir.exists()) {
            FileUtils.deleteDir(unzipBaseApkDir)
        }
        if (baseApksDir.exists()) {
            FileUtils.deleteDir(baseApksDir)
        }
        File baseApk = baseApkFiles[0]
        Properties properties = new Properties()
        if (!baseAppCpuAbiListFile.exists()) {
            throw new GradleException("Unable to find file ${baseAppCpuAbiListFile.absolutePath}")
        }
        baseAppCpuAbiListFile.withInputStream {
            properties.load(it)
        }
        String abiListText = properties."abiList"
        List<String> abiList = abiListText != null ? abiListText.split(",") : null
        if (abiList == null || abiList.isEmpty()) {
            SplitLogger.e("Base apk ${baseApk.absolutePath} has no native-library abi folder, multiple apks don't need.")
            return
        }
        if (abiList.size() == 1) {
            SplitLogger.e("Base apk ${baseApk.absolutePath} has only one native-library abi folder, multiple apks don't need.")
            return
        }
        if (use7z) {
            abiList.add(abiList.join("-"))
        }
        SigningConfig signingConfig = null
        try {
            signingConfig = apkSigner.getSigningConfig()
        } catch (Throwable ignored) {

        }
        boolean isSigningNeed = signingConfig != null && signingConfig.isSigningReady()
        abiList.each { String abi ->
            File unzipBaseApkDirForAbi = new File(unzipBaseApkDir, abi)
            if (unzipBaseApkDirForAbi.exists()) {
                FileUtils.deleteDir(unzipBaseApkDirForAbi)
            }
            unzipBaseApkDirForAbi.mkdirs()
            HashMap<String, Integer> compress = ZipUtils.unzipApk(baseApk, unzipBaseApkDirForAbi)
            if (SUPPORTED_ABIS.contains(abi)) {
                File baseAppCpuAbiListFileForAbi = new File(unzipBaseApkDirForAbi, "assets/${baseAppCpuAbiListFile.name}")
                baseAppCpuAbiListFileForAbi.write("abiList=${abi}")
                File[] libDirs = new File(unzipBaseApkDirForAbi, "lib").listFiles()
                libDirs.each { File abiDir ->
                    if (abiDir.name != abi) {
                        FileUtils.deleteDir(abiDir)
                    }
                }
                dynamicFeaturesNames.each { String splitName ->
                    File baseApkQigsawAssetsDir = new File(unzipBaseApkDirForAbi, "assets/qigsaw")
                    File[] splitApkFiles = baseApkQigsawAssetsDir.listFiles(new FileFilter() {
                        @Override
                        boolean accept(File file) {
                            return file.name.endsWith(SdkConstants.DOT_ZIP)
                        }
                    })
                    if (splitApkFiles != null) {
                        splitApkFiles.each { File file ->
                            if (file.name.startsWith(splitName) && !file.name.contains(abi) && !file.name.startsWith("${splitName}-master")) {
                                file.delete()
                            }
                        }
                    }
                }
            }
            File unsignedBaseApk = new File(baseApksDir, "${project.name}-${baseVariant.name.uncapitalize()}-${abi}-${use7z ? "7z" : "non7z"}-unsigned${SdkConstants.DOT_ANDROID_PACKAGE}")
            if (!unsignedBaseApk.parentFile.exists()) {
                unsignedBaseApk.parentFile.mkdirs()
            }
            if (use7z) {
                run7zCmd("7za", "a", "-tzip", unsignedBaseApk.absolutePath, unzipBaseApkDirForAbi.absolutePath + File.separator + "*", "-mx9")
            } else {
                ZipUtils.zipFiles(Arrays.asList(unzipBaseApkDirForAbi.listFiles()), unzipBaseApkDirForAbi, unsignedBaseApk, compress)
            }
            if (isSigningNeed) {
                File signedBaseApk = new File(baseApksDir, "${project.name}-${baseVariant.name.uncapitalize()}-${abi}-${use7z ? "7z" : "non7z"}-signed${SdkConstants.DOT_ANDROID_PACKAGE}")
                apkSigner.signApkIfNeed(unsignedBaseApk, signedBaseApk)
                File destBaseApk = new File(packageAppDir, signedBaseApk.name)
                if (destBaseApk.exists()) {
                    destBaseApk.delete()
                }
                FileUtils.copyFile(signedBaseApk, destBaseApk)
            } else {
                File destBaseApk = new File(packageAppDir, unsignedBaseApk.name)
                if (destBaseApk.exists()) {
                    destBaseApk.delete()
                }
                FileUtils.copyFile(unsignedBaseApk, destBaseApk)
            }
        }
    }

    static void run7zCmd(String... cmd) {
        try {
            String cmdResult = CommandUtils.runCmd(cmd)
            SplitLogger.w("Run command successfully, result: " + cmdResult)
        } catch (Throwable e) {
            throw new GradleException("'7za' command is not found, have you install 7zip?", e)
        }
    }
}
