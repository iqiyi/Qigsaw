package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.SdkConstants
import com.android.builder.model.SigningConfig
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.CommandUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
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
    Set<String> dynamicFeaturesNames

    @InputFile
    File baseAppCpuAbiListFile

    @InputFiles
    List<File> baseApkFiles

    @OutputDirectory
    File packageAppDir

    @OutputDirectory
    File baseApksDir

    @TaskAction
    void splitBaseApk() {
        if (baseApkFiles.size() > 1) {
            throw new GradleException("Qigsaw Error: Qigsaw don't support multi-apks.")
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
        abiList.each { String abi ->
            if (SUPPORTED_ABIS.contains(abi)) {
                // Copy base apk
                File copyBaseApk = new File(baseApksDir, "${project.name}-${baseVariant.name.uncapitalize()}-${abi}${SdkConstants.DOT_ANDROID_PACKAGE}")
                if (!copyBaseApk.parentFile.exists()) {
                    copyBaseApk.parentFile.mkdirs()
                }
                if (copyBaseApk.exists()) {
                    copyBaseApk.delete()
                }
                FileUtils.copyFile(baseApk, copyBaseApk)
                String copyBaseApkPath = copyBaseApk.getAbsolutePath()

                // Delete signature related files
                runCmd("zip", "-d", copyBaseApkPath, "META-INF/CERT.RSA")
                runCmd("zip", "-d", copyBaseApkPath, "META-INF/CERT.SF")
                runCmd("zip", "-d", copyBaseApkPath, "META-INF/MANIFEST.MF")

                Set<String> masterSplitHandleFlags= new HashSet<>()
                abiList.each { String ABI ->
                    if (abi != ABI) {
                        // Delete other ABI's lib
                        runCmd("zip", "-d", copyBaseApkPath, "lib/${ABI}/**")

                        // Delete other ABI's built-in splits (include master)
                        dynamicFeaturesNames.each { String splitName ->
                            if (!masterSplitHandleFlags.contains(splitName)) {
                                runCmd("zip", "-d", copyBaseApkPath, "assets/qigsaw/${splitName}-master**.zip")
                                masterSplitHandleFlags.add(splitName)
                            }
                            runCmd("zip", "-d", copyBaseApkPath, "assets/qigsaw/${splitName}-${ABI}**.zip")
                        }
                    }
                }

                // Update base apk cpu abi list file
                File baseAppCpuAbiListFileForAbi = new File(baseApksDir,"assets/${baseAppCpuAbiListFile.name}")
                if (!baseAppCpuAbiListFileForAbi.parentFile.exists()) {
                    baseAppCpuAbiListFileForAbi.parentFile.mkdirs()
                }
                if (baseAppCpuAbiListFileForAbi.exists()) {
                    baseAppCpuAbiListFileForAbi.delete()
                }
                baseAppCpuAbiListFileForAbi.write("abiList=${abi}")
                // ProcessBuilder execute multi commands
                File baseAppCpuAbiScript = new File(baseApksDir,"baseAppCpuAbiScript")
                if (baseAppCpuAbiScript.exists()) {
                    baseAppCpuAbiScript.delete()
                }
                baseAppCpuAbiScript.write("#!/usr/bin/env bash\ncd \$1\nzip -d \$2 \$3\nzip -m \$2 \$3")
                runCmd("chmod", "755", baseAppCpuAbiScript.getAbsolutePath())
                runCmd(baseAppCpuAbiScript.getAbsolutePath(), copyBaseApk.getParent(), copyBaseApk.getName(), "assets/${baseAppCpuAbiListFile.name}")

                // Resign apk if need
                SigningConfig signingConfig = null
                try {
                    signingConfig = apkSigner.getSigningConfig()
                } catch (Throwable ignored) {
                }
                boolean isSigningNeed = signingConfig != null && signingConfig.isSigningReady()
                if (isSigningNeed) {
                    File signedBaseApk = new File(baseApksDir, "${project.name}-${baseVariant.name.uncapitalize()}-${abi}-signed${SdkConstants.DOT_ANDROID_PACKAGE}")
                    if (signedBaseApk.exists()) {
                        signedBaseApk.delete()
                    }
                    apkSigner.signApkIfNeed(copyBaseApk, signedBaseApk)
                    File destBaseApk = new File(packageAppDir, signedBaseApk.name)
                    if (destBaseApk.exists()) {
                        destBaseApk.delete()
                    }
                    FileUtils.copyFile(signedBaseApk, destBaseApk)
                } else {
                    File destBaseApk = new File(packageAppDir, copyBaseApk.name)
                    if (destBaseApk.exists()) {
                        destBaseApk.delete()
                    }
                    FileUtils.copyFile(copyBaseApk, destBaseApk)
                }
            }
        }
    }

    private static void runCmd(String... cmd) {
        try {
            String cmdResult = CommandUtils.runCmd(cmd)
            SplitLogger.w("Run command ${cmd} successfully, result: " + cmdResult)
        } catch (Throwable e) {
            SplitLogger.w("Run command ${cmd} error: " + e)
        }
    }
}