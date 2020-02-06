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

import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitInfoProcessor
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ZipUtils

class SplitInfoProcessorImpl implements SplitInfoProcessor {

    final SplitInfo rawSplitInfo

    final File splitsExtractionOutputDir

    SplitInfoProcessorImpl(SplitInfo rawSplitInfo, File splitsExtractionOutputDir) {
        this.rawSplitInfo = rawSplitInfo
        this.splitsExtractionOutputDir = splitsExtractionOutputDir
    }

    @Override
    SplitInfo processSplitInfo(File splitApkFile, File SplitManifestFile, boolean releaseSplitApk, List<String> restrictWorkProcessesForSplits) {
        if (splitsExtractionOutputDir.exists()) {
            splitsExtractionOutputDir.deleteDir()
        }
        ZipUtils.unZipAPk(splitApkFile.absolutePath, splitsExtractionOutputDir.absolutePath)
        ManifestReader manifestReader = new ManifestReaderImpl(SplitManifestFile)
        rawSplitInfo.md5 = FileUtils.getMD5(splitApkFile)
        rawSplitInfo.size = splitApkFile.length()
        String splitApplicationName = manifestReader.readApplicationName().name
        rawSplitInfo.applicationName = (splitApplicationName == null || splitApplicationName.length() == 0 ? null : splitApplicationName)
        rawSplitInfo.onDemand = manifestReader.readOnDemand()
        rawSplitInfo.builtIn = !manifestReader.readOnDemand() || !releaseSplitApk
        List<String> splitProcesses = new ArrayList<>()
        Set<ComponentInfo> activities = manifestReader.readActivities()
        activities.each {
            if (!splitProcesses.contains(it.process)) {
                splitProcesses.add(it.process)
            }
        }
        Set<String> services = manifestReader.readServices()
        services.each {
            if (!splitProcesses.contains(it.process)) {
                splitProcesses.add(it.process)
            }
        }
        Set<String> receivers = manifestReader.readReceivers()
        receivers.each {
            if (!splitProcesses.contains(it.process)) {
                splitProcesses.add(it.process)
            }
        }
        Set<String> providers = manifestReader.readProviders()
        providers.each {
            if (!splitProcesses.contains(it.process)) {
                splitProcesses.add(it.process)
            }
        }
        if (!restrictWorkProcessesForSplits.empty) {
            if (restrictWorkProcessesForSplits.contains(rawSplitInfo.splitName)) {
                rawSplitInfo.workProcesses = splitProcesses.isEmpty() ? null : splitProcesses
            }
        }
        File[] dexFiles = splitsExtractionOutputDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.name.endsWith(".dex") && file.name.startsWith("classes")
            }
        })
        rawSplitInfo.dexNumber = (dexFiles != null ? dexFiles.length : 0)
        rawSplitInfo.nativeLibraries = createSplitLibInfo()
        return rawSplitInfo
    }

    List<SplitInfo.LibInfo> createSplitLibInfo() {
        File[] files = splitsExtractionOutputDir.listFiles()
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
