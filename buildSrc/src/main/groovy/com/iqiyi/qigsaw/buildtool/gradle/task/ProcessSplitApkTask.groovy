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
import com.android.tools.build.bundletool.model.Aapt2Command
import com.android.tools.build.bundletool.model.AndroidManifest
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ApkSigner
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ZipUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class ProcessSplitApkTask extends DefaultTask {

    ApkSigner apkSigner

    File aapt2File

    @Input
    boolean releaseSplitApk

    @Input
    @Optional
    Set<String> restrictWorkProcessesForSplits

    @Input
    int minApiLevel

    @Input
    String splitVersion

    @Input
    String applicationId

    @Input
    Set<String> splitProjectClassPaths

    @Input
    Set<String> splitProjectDependencies

    @InputFiles
    List<File> splitApks

    @InputDirectory
    File splitManifestDir

    @OutputDirectory
    File splitApksDir

    @OutputDirectory
    File splitInfoDir

    @OutputDirectory
    File unzipSplitApkBaseDir

    @TaskAction
    void processSplitApk() {
        if (splitApks.size() > 1) {
            throw new GradleException("Qigsaw Error: Qigsaw don't support multi-apks.")
        }
        File unzipSplitApkDir = new File(unzipSplitApkBaseDir, project.name)
        if (unzipSplitApkDir.exists()) {
            FileUtils.deleteDir(unzipSplitApkDir)
        }
        File sourceSplitApk = splitApks[0]
        HashMap<String, Integer> compressData = ZipUtils.unzipApk(sourceSplitApk, unzipSplitApkDir)
        Set<String> supportedABIs = new HashSet<>()
        File splitLibsDir = new File(unzipSplitApkDir, "lib")
        if (splitLibsDir.exists()) {
            splitLibsDir.listFiles(new FileFilter() {
                @Override
                boolean accept(File file) {
                    supportedABIs.add(file.name)
                    return false
                }
            })
        }
        List<SplitInfo.SplitApkData> apkDataList = new ArrayList<>()
        Aapt2Command aapt2 = Aapt2Command.createFromExecutablePath(aapt2File.toPath())
        File tmpDir = new File(splitApksDir, "tmp/${project.name}")
        tmpDir.mkdirs()
        supportedABIs.each { String abi ->
            File protoAbiApk = new File(tmpDir, project.name + "-${abi}-proto" + SdkConstants.DOT_ANDROID_PACKAGE)
            File binaryAbiApk = new File(tmpDir, project.name + "-${abi}-binary" + SdkConstants.DOT_ANDROID_PACKAGE)
            File configAndroidManifest = new File(tmpDir, SdkConstants.ANDROID_MANIFEST_XML)
            createSplitConfigApkAndroidManifest(project.name, abi, configAndroidManifest)
            Collection<File> resFiles = new ArrayList<>()
            resFiles.add(new File(splitLibsDir, abi))
            resFiles.add(configAndroidManifest)
            ZipUtils.zipFiles(resFiles, unzipSplitApkDir, protoAbiApk, compressData)
            aapt2.convertApkProtoToBinary(protoAbiApk.toPath(), binaryAbiApk.toPath())
            File signedAbiApk = new File(splitApksDir, project.name + "-${abi}" + SdkConstants.DOT_ANDROID_PACKAGE)
            if (signedAbiApk.exists()) {
                signedAbiApk.delete()
            }
            apkSigner.signApkIfNeed(binaryAbiApk, signedAbiApk)
            SplitInfo.SplitApkData configApkData = new SplitInfo.SplitApkData()
            configApkData.abi = abi
            configApkData.url = "assets://qigsaw/${project.name}-${abi + SdkConstants.DOT_ZIP}"
            configApkData.size = signedAbiApk.length()
            configApkData.md5 = FileUtils.getMD5(signedAbiApk)
            apkDataList.add(configApkData)
        }
        //create split master apk
        Collection<File> resFiles = new ArrayList<>()
        File[] files = unzipSplitApkDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name != "lib"
            }
        })
        Collections.addAll(resFiles, files)
        File unsignedMasterApk = new File(tmpDir, project.name + "-master-unsigned" + SdkConstants.DOT_ANDROID_PACKAGE)
        ZipUtils.zipFiles(resFiles, unzipSplitApkDir, unsignedMasterApk, compressData)
        File signedMasterApk = new File(splitApksDir, project.name + "-master" + SdkConstants.DOT_ANDROID_PACKAGE)
        apkSigner.signApkIfNeed(unsignedMasterApk, signedMasterApk)
        SplitInfo.SplitApkData masterApkData = new SplitInfo.SplitApkData()
        masterApkData.abi = "master"
        masterApkData.url = "assets://qigsaw/${project.name}-master${SdkConstants.DOT_ZIP}"
        masterApkData.size = signedMasterApk.length()
        masterApkData.md5 = FileUtils.getMD5(signedMasterApk)
        apkDataList.add(masterApkData)
        //create split native-library data list.
        List<SplitInfo.SplitLibData> libDataList = createSplitLibInfo(unzipSplitApkDir)
        //create split-info json file
        File splitInfoFile = new File(splitInfoDir, project.name + SdkConstants.DOT_JSON)
        if (splitInfoFile.exists()) {
            splitInfoFile.delete()
        }
        SplitInfo info = createSplitInfo(apkDataList, libDataList, unzipSplitApkDir)
        FileUtils.createFileForTypeClass(info, splitInfoFile)
        FileUtils.deleteDir(tmpDir)
    }

    void createSplitConfigApkAndroidManifest(String splitName, String abi, File androidManifestFile) {
        AndroidManifest androidManifest = AndroidManifest.createForConfigSplit(
                applicationId, splitVersion.split("@")[1].toInteger(), "${splitName}.config.${abi}", splitName, java.util.Optional.of(true))
        androidManifestFile.withOutputStream {
            it.write(androidManifest.manifestRoot.proto.toByteArray())
        }
    }

    SplitInfo createSplitInfo(List<SplitInfo.SplitApkData> apkDataList, List<SplitInfo.SplitLibData> libDataList, File unzipSplitApkDir) {
        Set<String> dependencies = new HashSet<>()
        splitProjectDependencies.each { String name ->
            if (splitProjectClassPaths.contains(name)) {
                dependencies.add(name.split(":")[1])
            }
        }
        File manifest = new File(splitManifestDir, project.name + SdkConstants.DOT_XML)
        if (!manifest.exists()) {
            throw new GradleException("Qigsaw Error: Split manifest ${manifest.absolutePath} is not existing!")
        }
        ManifestReader manifestReader = new ManifestReader(manifest)
        String splitApplicationName = manifestReader.readApplicationName()
        boolean onDemand = manifestReader.readOnDemand()
        boolean builtIn = !onDemand || !releaseSplitApk
        File[] dexFiles = unzipSplitApkDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.endsWith(".dex") && file.name.startsWith("classes")
            }
        })
        Set<String> splitWorkProcesses = new HashSet<>()
        Set<ComponentInfo> activities = manifestReader.readActivities()
        activities.each {
            splitWorkProcesses.add(it.process)
        }
        Set<ComponentInfo> services = manifestReader.readServices()
        services.each {
            splitWorkProcesses.add(it.process)
        }
        Set<ComponentInfo> receivers = manifestReader.readReceivers()
        receivers.each {
            splitWorkProcesses.add(it.process)
        }
        Set<ComponentInfo> providers = manifestReader.readProviders()
        providers.each {
            splitWorkProcesses.add(it.process)
        }
        if (restrictWorkProcessesForSplits != null && !restrictWorkProcessesForSplits.empty) {
            if (restrictWorkProcessesForSplits.contains(project.name)) {
                splitWorkProcesses = splitWorkProcesses.isEmpty() ? null : splitWorkProcesses
            }
        }
        SplitInfo splitInfo = new SplitInfo()
        splitInfo.splitName = project.name
        splitInfo.builtIn = builtIn
        splitInfo.minSdkVersion = minApiLevel
        splitInfo.dexNumber = (dexFiles != null ? dexFiles.length : 0)
        splitInfo.onDemand = onDemand
        splitInfo.version = splitVersion
        splitInfo.applicationName = splitApplicationName == "" ? null : splitApplicationName
        splitInfo.dependencies = dependencies.isEmpty() ? null : dependencies
        splitInfo.workProcesses = splitWorkProcesses.isEmpty() ? null : splitWorkProcesses
        splitInfo.apkData = apkDataList.isEmpty() ? null : apkDataList
        splitInfo.libData = libDataList.isEmpty() ? null : libDataList
        return splitInfo
    }

    static List<SplitInfo.SplitLibData> createSplitLibInfo(File unzipSplitApkDir) {
        List<SplitInfo.SplitLibData> nativeLibraries = new ArrayList<>(0)
        File[] files = unzipSplitApkDir.listFiles()
        File libDir = null
        for (File file : files) {
            if (file.isDirectory() && "lib" == file.name) {
                libDir = file
                break
            }
        }
        if (libDir == null) {
            return nativeLibraries
        }
        File[] abiDirs = libDir.listFiles()
        for (File abiDir : abiDirs) {
            String abiName = abiDir.name
            File[] soFiles = abiDir.listFiles()
            SplitInfo.SplitLibData libInfo = new SplitInfo.SplitLibData()
            libInfo.abi = abiName
            List<SplitInfo.SplitLibData.Lib> jniLibs = new ArrayList<>()
            for (File soFile : soFiles) {
                if (soFile.name.endsWith(SdkConstants.DOT_NATIVE_LIBS)) {
                    String md5 = FileUtils.getMD5(soFile)
                    SplitInfo.SplitLibData.Lib lib = new SplitInfo.SplitLibData.Lib()
                    lib.name = soFile.name
                    lib.md5 = md5
                    lib.size = soFile.length()
                    jniLibs.add(lib)
                }
            }
            libInfo.jniLibs = jniLibs
            nativeLibraries.add(libInfo)
        }
        return nativeLibraries
    }

}
