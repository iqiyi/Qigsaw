package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.SdkConstants
import com.android.build.gradle.api.ApplicationVariant
import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.CommandUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ZipUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry

class SplitBaseApkForABIsTask extends DefaultTask {

    ApplicationVariant baseVariant

    SplitApkSigner apkSigner

    @Input
    boolean use7z

    @Input
    Set<String> dynamicFeaturesNames

    @InputFile
    File baseAppCpuAbiListFile

    @InputFiles
    List<File> baseApkFiles

    @InputDirectory
    File splitApksDir

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
            unzipBaseApkDir.deleteDir()
        }
        if (baseApksDir.exists()) {
            baseApksDir.deleteDir()
        }
        File baseApk = baseApkFiles[0]
        Properties properties = new Properties()
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
        boolean isSigningNeed = baseVariant.buildType.signingConfig != null && baseVariant.buildType.signingConfig.isSigningReady()
        abiList.each { String abi ->
            File unzipBaseApkDirForAbi = new File(unzipBaseApkDir, abi)
            if (unzipBaseApkDirForAbi.exists()) {
                unzipBaseApkDirForAbi.deleteDir()
            }
            HashMap<String, Integer> compress = ZipUtils.unzipApk(baseApk, unzipBaseApkDirForAbi)
            if (QigsawAppBasePlugin.CUSTOM_SUPPORTED_ABIS.contains(abi)) {
                File baseAppCpuAbiListFileForAbi = new File(unzipBaseApkDirForAbi, "assets/${baseAppCpuAbiListFile.name}")
                baseAppCpuAbiListFileForAbi.write("abiList=${abi}")
                File[] libDirs = new File(unzipBaseApkDirForAbi, "lib").listFiles()
                libDirs.each { File abiDir ->
                    if (abiDir.name != abi) {
                        abiDir.deleteDir()
                    }
                }
                dynamicFeaturesNames.each { String splitName ->
                    File baseApkQigsawAssetsDir = new File(unzipBaseApkDirForAbi, "assets/qigsaw")
                    File[] files = baseApkQigsawAssetsDir.listFiles()
                    files.each { File file ->
                        if (file.name.startsWith("${splitName}") && !file.name.startsWith("${splitName}-none") && file.name.endsWith(SdkConstants.DOT_ZIP)) {
                            file.delete()
                            File splitSignedApk = new File(splitApksDir, "${splitName}-${abi}-signed${SdkConstants.DOT_ANDROID_PACKAGE}")
                            if (splitSignedApk.exists()) {
                                File destSplitApk = new File(baseApkQigsawAssetsDir, "${splitName}-${abi}${SdkConstants.DOT_ZIP}")
                                FileUtils.copyFile(splitSignedApk, destSplitApk)
                                String entryName = "assets/qigsaw/${splitName}-${abi}${SdkConstants.DOT_ZIP}"
                                compress.put(entryName, ZipEntry.STORED)
                            } else {
                                SplitLogger.e("Split apk ${splitSignedApk} isn't found, just ignored!")
                            }
                        }
                    }
                }
            }
            File unsignedBaseApk = new File(baseApksDir, "${project.name}-${baseVariant.name.uncapitalize()}-${abi}-${use7z ? "7z" : "non7z"}-unsigned${SdkConstants.DOT_ANDROID_PACKAGE}")
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
