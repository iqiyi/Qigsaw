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
import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.SplitOutputFile
import com.iqiyi.qigsaw.buildtool.gradle.SplitOutputFileManager
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitApkProcessor
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.SplitJsonFileCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TopoSort
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class QigsawAssembleTask extends DefaultTask {

    String qigsawId

    String variantName

    File assetsDir

    File mergeJniLibDir

    File packageOutputDir

    File baseManifestFile

    List<String> dfClassPaths

    List<File> qigsawIntermediates = new ArrayList<>()

    void initArgs(String qigsawId,
                  String variantName,
                  File assetsDir,
                  File mergeJniLib,
                  File packageOutputDir,
                  File baseManifestFile,
                  List<String> dfClassPaths) {
        this.qigsawId = qigsawId
        this.variantName = variantName
        this.assetsDir = assetsDir
        this.mergeJniLibDir = mergeJniLib
        this.packageOutputDir = packageOutputDir
        this.baseManifestFile = baseManifestFile
        this.dfClassPaths = dfClassPaths
    }

    @TaskAction
    void makeSplitInfoFile() {
        makeSplitInfoFileInternal()
    }

    void deleteIntermediates() {
        qigsawIntermediates.each {
            if (it.exists()) {
                it.delete()
            }
        }
    }

    void makeSplitInfoFileInternal() {
        Map<String, SplitInfo> splitInfoMap = new HashMap<>()
        Set<SplitOutputFile> splitOutputFiles = SplitOutputFileManager.getInstance().getOutputFiles()
        for (SplitOutputFile splitOutputFile : splitOutputFiles) {
            if (!splitOutputFile.variantName.equals(variantName)) {
                continue
            }
            Project splitProject = splitOutputFile.splitProject
            String splitName = splitProject.name
            File splitManifest = splitOutputFile.splitManifest
            File splitApk = splitOutputFile.splitApk
            if (splitApk == null || splitManifest == null) {
                throw new RuntimeException("Can not find output files of " + splitName + " " + splitApk + " " + splitManifest)
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
            File splitSignedApk = splitProcessor.signSplitAPKIfNeed(splitApk)
            //create split info
            SplitInfo splitInfo = splitProcessor.createSplitInfo(splitName, splitProject.extensions.android, dfDependencies, splitManifest, splitSignedApk)
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
        SplitJsonFileCreator detailsCreator = new SplitDetailsCreatorImpl(
                qigsawId,
                copyToAssets,
                getProject(),
                packageOutputDir,
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
        File splitJsonFile = detailsCreator.createSplitDetailsJsonFile(splits)
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
