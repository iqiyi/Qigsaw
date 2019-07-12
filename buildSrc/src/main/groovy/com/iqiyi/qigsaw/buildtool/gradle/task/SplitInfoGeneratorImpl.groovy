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

import com.android.build.gradle.AppExtension
import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfoGenerator
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReader
import org.gradle.api.Project

class SplitInfoGeneratorImpl implements SplitInfoGenerator {

    Project appProject

    AppExtension android

    String variantName

    Map<String, List<String>> dynamicFeatureDependenciesMap

    SplitInfoGeneratorImpl(Project appProject,
                           AppExtension splitExtension,
                           String variantName,
                           Map<String, List<String>> dynamicFeatureDependenciesMap) {
        this.dynamicFeatureDependenciesMap = dynamicFeatureDependenciesMap
        this.appProject = appProject
        this.android = splitExtension
        this.variantName = variantName
    }

    @Override
    SplitInfo generate(String splitName, File splitApk, File splitManifest) {
        String versionName = android.defaultConfig.versionName
        if (versionName == null) {
            throw new RuntimeException("Dynamic feature ${splitName} version name is not set!")
        }
        Integer versionCode = android.defaultConfig.versionCode
        if (versionCode == null) {
            versionCode = 0
        }
        String md5 = FileUtils.getMD5(splitApk)
        SplitInfo splitInfo = new SplitInfo(
                splitName,
                splitApk,
                md5,
                android.defaultConfig.minSdkVersion.apiLevel,
                versionName + "@" + versionCode,
        )
        splitInfo.dependencies = dynamicFeatureDependenciesMap.get(splitName)
        ManifestReader manifestReader = new ManifestReaderImpl(splitManifest)
        boolean releaseSplitApk = appProject.extensions.qigsawSplit.releaseSplitApk
        splitInfo.builtIn = !manifestReader.readOnDemand() || !releaseSplitApk

        List<String> processes = new ArrayList<>()
        List<ComponentInfo> activities = manifestReader.readActivities()
        activities.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        List<String> services = manifestReader.readServices()
        services.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        List<String> receivers = manifestReader.readReceivers()
        receivers.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        List<String> providers = manifestReader.readProviders()
        providers.each {
            if (!processes.contains(it.process)) {
                processes.add(it.process)
            }
        }
        List<String> restrictWorkProcessesForSplits = appProject.extensions.qigsawSplit.restrictWorkProcessesForSplits

        if (restrictWorkProcessesForSplits != null && !restrictWorkProcessesForSplits.empty) {
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
        splitInfo.libInfo = createLibInfo(splitExtractDir)
        return splitInfo
    }

    static SplitInfo.LibInfo createLibInfo(File splitExtractDir) {
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
        if (abiDirs.length > 1) {
            throw new RuntimeException("More than one abi is not allowed in dynamic-feature module!")
        }
        File abiDir = abiDirs[0]
        String abiName = abiDir.name
        File[] soFiles = abiDir.listFiles()
        SplitInfo.LibInfo libInfo = new SplitInfo.LibInfo()
        libInfo.abi = abiName
        List<SplitInfo.LibInfo.Lib> libs = new ArrayList<>()
        for (File soFile : soFiles) {
            if (soFile.name.endsWith(".so")) {
                String md5 = FileUtils.getMD5(soFile)
                SplitInfo.LibInfo.Lib lib = new SplitInfo.LibInfo.Lib()
                lib.name = soFile.name
                lib.md5 = md5
                lib.size = soFile.length()
                libs.add(lib)
            }
        }
        libInfo.libs = libs
        return libInfo
    }
}
