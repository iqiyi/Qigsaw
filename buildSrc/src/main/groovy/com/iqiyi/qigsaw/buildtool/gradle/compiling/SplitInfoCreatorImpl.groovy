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

package com.iqiyi.qigsaw.buildtool.gradle.compiling

import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitInfoCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader
import org.gradle.api.Project

class SplitInfoCreatorImpl implements SplitInfoCreator {

    final Project appProject

    final String splitName

    final String versionName

    final Integer versionCode

    final int minApiLevel

    final String variantName

    final File splitApk

    final File splitManifestFile

    final List<String> dfDependencies

    final boolean releaseSplitApk

    final List<String> restrictWorkProcessesForSplits

    SplitInfoCreatorImpl(Project appProject,
                         String variantName,
                         String splitName,
                         String versionName,
                         Integer versionCode,
                         int minApiLevel,
                         List<String> dfDependencies,
                         File splitApk,
                         File splitManifestFile,
                         boolean releaseSplitApk,
                         List<String> restrictWorkProcessesForSplits) {
        this.appProject = appProject
        this.variantName = variantName
        this.splitName = splitName
        this.versionName = versionName
        this.versionCode = versionCode
        this.minApiLevel = minApiLevel
        this.dfDependencies = dfDependencies
        this.splitApk = splitApk
        this.splitManifestFile = splitManifestFile
        this.releaseSplitApk = releaseSplitApk
        this.restrictWorkProcessesForSplits = restrictWorkProcessesForSplits
    }

    @Override
    SplitInfo create() {
        String md5 = FileUtils.getMD5(splitApk)
        SplitInfo splitInfo = new SplitInfo(
                splitName,
                splitApk,
                md5,
                minApiLevel,
                versionName + "@" + versionCode,
        )
        splitInfo.dependencies = dfDependencies.isEmpty() ? null : dfDependencies
        ManifestReader manifestReader = new ManifestReaderImpl(splitManifestFile)
        splitInfo.builtIn = !manifestReader.readOnDemand() || !releaseSplitApk
        List<String> processes = new ArrayList<>()
        Set<ComponentInfo> activities = manifestReader.readActivities()
        activities.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        Set<String> services = manifestReader.readServices()
        services.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        Set<String> receivers = manifestReader.readReceivers()
        receivers.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        Set<String> providers = manifestReader.readProviders()
        providers.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        if (!restrictWorkProcessesForSplits.empty) {
            if (restrictWorkProcessesForSplits.contains(splitName)) {
                splitInfo.workProcesses = processes.isEmpty() ? null : processes
            }
        }
        //extract split apk
        String splitExtractPath = QigsawAppBasePlugin.QIGSAW_INTERMEDIATES + "splits_extraction" + File.separator + variantName.uncapitalize() + File.separator + splitName
        File splitExtractDir = appProject.mkdir(splitExtractPath)
        if (splitExtractDir.exists()) {
            splitExtractDir.deleteDir()
        }
        splitExtractDir.mkdirs()
        FileUtils.unZipFile(splitApk, splitExtractDir.absolutePath)
        File[] dexFiles = splitExtractDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.endsWith(".dex") && file.name.startsWith("classes")
            }
        })
        splitInfo.dexNumber = (dexFiles != null ? dexFiles.length : 0)
        splitInfo.nativeLibraries = createLibInfo(splitExtractDir)
        return splitInfo
    }

    static List<SplitInfo.LibInfo> createLibInfo(File splitExtractDir) {
        File[] files = splitExtractDir.listFiles()
        File libDir = null
        for (File file : files) {
            if (file.isDirectory() && "lib".equals(file.name)) {
                libDir = file
                break
            }
        }
        if (libDir == null) {
            return null
        }
        File[] abiDirs = libDir.listFiles()
        List<SplitInfo.LibInfo> nativeLibraries = new ArrayList<>(abiDirs.length)
        for (File abiDir : abiDirs) {
            String abiName = abiDir.name
            File[] soFiles = abiDir.listFiles()
            SplitInfo.LibInfo libInfo = new SplitInfo.LibInfo()
            libInfo.abi = abiName
            List<SplitInfo.LibInfo.Lib> jniLibs = new ArrayList<>()
            for (File soFile : soFiles) {
                if (soFile.name.endsWith(".so")) {
                    String md5 = FileUtils.getMD5(soFile)
                    SplitInfo.LibInfo.Lib lib = new SplitInfo.LibInfo.Lib()
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
