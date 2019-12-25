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
import com.android.build.gradle.api.ApplicationVariant
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitApkProcessorImpl
import com.iqiyi.qigsaw.buildtool.gradle.compiling.SplitJsonFileCreatorImpl
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtensionHelper
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitApkProcessor
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitJsonFileCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TopoSort
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class QigsawAssembleTask extends DefaultTask {

    @Input
    String qigsawId

    @Input
    String splitInfoVersion

    @Input
    String oldApkPath

    @Input
    boolean releaseSplitApk

    @Input
    List<String> restrictWorkProcessesForSplits

    @Input
    List<String> dfClassPaths

    String variantName

    String flavorName

    String appVersionName

    File assetsDir

    File mergeJniLibDir

    File packageOutputDir

    File baseManifestFile

    List<Project> dfProjects

    List<File> qigsawIntermediates = new ArrayList<>()

    @OutputDirectory
    File outputDir

    QigsawAssembleTask() {
        this.oldApkPath = QigsawSplitExtensionHelper.getOldApk(project)
        this.releaseSplitApk = QigsawSplitExtensionHelper.getReleaseSplitApk(project)
        this.restrictWorkProcessesForSplits = QigsawSplitExtensionHelper.getRestrictWorkProcessesForSplits(project)
    }

    void initArgs(String qigsawId,
                  String splitInfoVersion,
                  String variantName,
                  String flavorName,
                  String appVersionName,
                  File assetsDir,
                  File mergeJniLib,
                  File packageOutputDir,
                  File baseManifestFile,
                  List<Project> dfProjects,
                  List<String> dfClassPaths) {
        this.qigsawId = qigsawId
        this.splitInfoVersion = splitInfoVersion
        this.variantName = variantName
        this.flavorName = flavorName
        this.appVersionName = appVersionName
        this.assetsDir = assetsDir
        this.mergeJniLibDir = mergeJniLib
        this.packageOutputDir = packageOutputDir
        this.baseManifestFile = baseManifestFile
        this.dfProjects = dfProjects
        this.dfClassPaths = dfClassPaths
    }

    @TaskAction
    void makeSplitJsonFile() {
        makeSplitJsonFileInternal()
    }

    void afterPackageApp() {
        qigsawIntermediates.each {
            if (it.name.endsWith(SdkConstants.DOT_JSON)) {
                File packageDirJsonFile = new File(packageOutputDir, it.name)
                if (packageDirJsonFile.exists()) {
                    packageDirJsonFile.delete()
                }
                FileUtils.copyFile(it, packageDirJsonFile)
            }
            if (it.exists()) {
                it.delete()
            }
        }
    }

    void makeSplitJsonFileInternal() {
        Map<String, SplitInfo> splitInfoMap = new HashMap<>()
        for (Project dfProject : dfProjects) {
            def dfAndroid = dfProject.extensions.android
            File splitManifestFile = null
            File splitApkFile = null
            String splitName = dfProject.name
            String dfFlavorName = null
            String dfVersionName = null
            Integer dfVersionCode = 0
            int minApiLevel = dfAndroid.defaultConfig.minSdkVersion.apiLevel
            dfAndroid.applicationVariants.all { ApplicationVariant variant ->
                String dfVariantName = variant.name.capitalize()
                dfFlavorName = variant.flavorName
                if (dfVariantName.equals(variantName)) {
                    variant.outputs.each {
                        splitApkFile = it.outputFile
                    }
                    Task processManifestTask = AGPCompat.getProcessManifestTask(dfProject, dfVariantName)
                    splitManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)
                    dfVersionName = variant.versionName
                    dfVersionCode = variant.versionCode
                }
            }
            if (dfFlavorName == null) {
                throw new RuntimeException("dynamic feature ${splitName} 'versionName' is not set!")
            }
            if (splitApkFile == null || splitManifestFile == null) {
                if ((flavorName != null && flavorName.length() > 0) && (dfFlavorName == null || dfFlavorName.length() == 0)) {
                    throw new GradleException("Qigsaw Error: Your app project has flavor ${flavorName}, " +
                            "dynamic feature project ${splitName} need set the same flavor config")
                } else {
                    throw new GradleException("Qigsaw Error: Can't find output files of project ${splitName}," +
                            " merged_manifest: ${splitManifestFile}, output_apk: ${splitApkFile}")
                }
            }
            List<String> allDependencies = SplitDependencyStatistics.getInstance().getDependencies(splitName, variantName)
            List<String> dfDependencies = new ArrayList<>()
            if (allDependencies != null) {
                allDependencies.each {
                    if (dfClassPaths.contains(it)) {
                        dfDependencies.add(it.split(":")[1])
                    }
                }
            }
            println("dynamic feature ${splitName} has dependencies: ${dfDependencies.toString()}")
            SplitApkProcessor splitProcessor = new SplitApkProcessorImpl(project, variantName)
            //sign split apk if needed
            File splitSignedApk = splitProcessor.signSplitAPKIfNeed(splitApkFile)
            //create split info
            SplitInfo splitInfo = splitProcessor.createSplitInfo(
                    splitName, dfVersionName,
                    dfVersionCode, minApiLevel,
                    dfDependencies, splitManifestFile,
                    splitSignedApk, releaseSplitApk, restrictWorkProcessesForSplits)
            splitInfoMap.put(splitInfo.splitName, splitInfo)
        }
        //get Abis that have been merged
        File[] abiDirs = mergeJniLibDir.listFiles()
        Set<String> abiDirNames = null
        boolean copyToAssets = true
        if (abiDirs != null) {
            ImmutableSet.Builder builder = ImmutableSet.builder()
            for (File abiDir : abiDirs) {
                builder.add(abiDir.name)
            }
            abiDirNames = builder.build()
        }
        Set<String> abiFilters = project.android.defaultConfig.ndk.abiFilters
        //check need copy splits to asset dir.
        if (abiFilters == null) {
            if (abiDirNames != null && abiDirNames.size() == 1) {
                copyToAssets = false
            }
        } else {
            if (abiDirNames != null) {
                if (abiFilters.size() == 1 && abiDirNames.containsAll(abiFilters)) {
                    copyToAssets = false
                }
            }
        }
        //fix abiFilters
        Set<String> fixedAbis
        if (abiFilters == null) {
            fixedAbis = abiDirNames
        } else {
            if (abiDirNames != null) {
                ImmutableSet.Builder fixedAbisBuilder = ImmutableSet.builder()
                abiFilters.each {
                    if (abiDirNames.contains(it)) {
                        fixedAbisBuilder.add(it)
                    }
                }
                fixedAbis = fixedAbisBuilder.build()
            } else {
                fixedAbis = abiFilters
            }
        }
        fixedAbis = sortAbis(fixedAbis)
        SplitJsonFileCreator detailsCreator = new SplitJsonFileCreatorImpl(
                qigsawId,
                appVersionName,
                splitInfoVersion,
                oldApkPath,
                copyToAssets,
                getProject(),
                outputDir,
                fixedAbis == null || fixedAbis.isEmpty() ? null : fixedAbis,
        )
        Map<String, TopoSort.Node> nodeMap = new HashMap<>()
        TopoSort.Graph graph = new TopoSort.Graph()
        Collection<SplitInfo> allSplits = splitInfoMap.values()
        for (SplitInfo info : allSplits) {
            if (nodeMap.get(info.splitName) == null) {
                nodeMap.put(info.splitName, new TopoSort.Node(info))
            }
            if (info.dependencies != null) {
                for (String dependency : info.dependencies) {
                    if (nodeMap.get(dependency) == null) {
                        nodeMap.put(dependency, new TopoSort.Node(splitInfoMap.get(dependency)))
                    }
                    graph.addNode(nodeMap.get(info.splitName), nodeMap.get(dependency))
                }
            }
        }
        TopoSort.KahnTopo topo = new TopoSort.KahnTopo(graph)
        topo.process()
        List<SplitInfo> splits = new ArrayList<>()
        for (int i = topo.result.size() - 1; i >= 0; i--) {
            SplitInfo info = topo.result.get(i).val
            splitInfoMap.remove(info.splitName)
            splits.add(info)
        }
        splits.addAll(splitInfoMap.values())
        File splitJsonFile = detailsCreator.createSplitJsonFile(splits)
        copySplitJsonFileAndSplitAPKs(splits, splitJsonFile, abiDirNames, copyToAssets)
    }

    void copySplitJsonFileAndSplitAPKs(List<SplitInfo> splits, File splitJsonFile, Set<String> abiDirNames, boolean copyToAssets) {
        if (!this.assetsDir.exists()) {
            this.assetsDir.mkdirs()
        }
        File outputJsonFile = new File(assetsDir, splitJsonFile.name)
        if (outputJsonFile.exists()) {
            outputJsonFile.delete()
        }

        FileUtils.copyFile(splitJsonFile, outputJsonFile)
        qigsawIntermediates.add(outputJsonFile)

        if (copyToAssets) {
            for (SplitInfo info : splits) {
                File assetsSplitApk = new File(assetsDir, info.splitName + SdkConstants.DOT_ZIP)
                if (assetsSplitApk.exists()) {
                    assetsSplitApk.delete()
                }
                if (info.builtIn) {
                    FileUtils.copyFile(info.splitApk, assetsSplitApk)
                    qigsawIntermediates.add(assetsSplitApk)
                }
            }
        } else {
            abiDirNames.each {
                for (SplitInfo info : splits) {
                    File jniSplitApk = new File(mergeJniLibDir, it + File.separator + "libsplit_" + info.splitName + SdkConstants.DOT_NATIVE_LIBS)
                    if (jniSplitApk.exists()) {
                        jniSplitApk.delete()
                    }
                    if (info.builtIn) {
                        FileUtils.copyFile(info.splitApk, jniSplitApk)
                        qigsawIntermediates.add(jniSplitApk)
                    }
                }
            }
        }
    }

    static Set<String> sortAbis(Set<String> abis) {
        if (abis == null || abis.isEmpty() || abis.size() == 1) {
            return abis
        }
        ImmutableSet.Builder builder = ImmutableSet.builder()
        if (abis.contains("arm64-v8a")) {
            builder.add("arm64-v8a")
        }
        if (abis.contains("armeabi-v7a")) {
            builder.add("armeabi-v7a")
        }
        if (abis.contains("armeabi")) {
            builder.add("armeabi")
        }
        if (abis.contains("x86")) {
            builder.add("x86")
        }
        if (abis.contains("x86_64")) {
            builder.add("x86_64")
        }
        return builder.build()
    }
}
