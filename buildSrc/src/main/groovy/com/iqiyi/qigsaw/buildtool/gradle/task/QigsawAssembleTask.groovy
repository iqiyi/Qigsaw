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
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitDetailsCreator
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.TopoSort
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class QigsawAssembleTask extends DefaultTask {

    def dynamicFeatures

    String variantName

    File assetsDir

    List<File> intermediates = new ArrayList<>()

    String qigsawId

    String versionName

    Map<String, List<String>> dynamicFeatureDependenciesMap

    @Inject
    QigsawAssembleTask(File assetsDir,
                       String variantName,
                       String versionName,
                       def dynamicFeatures,
                       String qigsawId) {
        this.assetsDir = assetsDir
        this.variantName = variantName
        this.versionName = versionName
        this.dynamicFeatures = dynamicFeatures
        this.qigsawId = qigsawId
        if (!this.assetsDir.exists()) {
            this.assetsDir.mkdirs()
        }
    }

    @TaskAction
    void processSplitAPKs() {
        processSplitAPKsInternal()
    }

    void processSplitAPKsInternal() {
        Map<String, SplitInfo> splitInfoMap = new HashMap<>()
        //get version name and version code of base app project!
        for (String dynamicFeature : dynamicFeatures) {
            Project dynamicFeatureProject = project.rootProject.project(dynamicFeature)
            String splitName = dynamicFeatureProject.name
            AppExtension android = dynamicFeatureProject.extensions.getByType(AppExtension)
            File splitApk = null
            File splitManifest = null

            android.applicationVariants.all { variant ->
                ApplicationVariant appVariant = variant
                Task assembleTask = AGPCompat.getAssemble(appVariant)
                if (assembleTask.name.endsWith(variantName)) {
                    appVariant.outputs.each {
                        splitApk = it.outputFile
                        File mergedManifestDir = AGPCompat.getMergedManifestDirCompat(dynamicFeatureProject, appVariant.name.capitalize())
                        splitManifest = new File(mergedManifestDir, "AndroidManifest.xml")
                    }
                }
            }
            if (splitApk == null || splitManifest == null) {
                throw new RuntimeException("Can not find output files of " + dynamicFeature + " " + splitApk + " " + splitManifest)
            }
            SplitProcessorImpl splitProcessor = new SplitProcessorImpl(project, android, variantName, dynamicFeatureDependenciesMap)
            //sign split apk if needed
            File splitSignedApk = splitProcessor.signSplitAPKIfNeed(splitApk)
            SplitInfo splitInfo = splitProcessor.createSplitInfo(splitName, splitSignedApk, splitManifest)
            splitInfoMap.put(splitInfo.splitName, splitInfo)
        }
        SplitDetailsCreator detailsCreator = new SplitDetailsCreatorImpl(
                getProject(),
                variantName,
                versionName,
                qigsawId
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
        List<SplitInfo> splits = new ArrayList<>(dynamicFeatures.size())
        for (int i = topo.result.size() - 1; i >= 0; i--) {
            SplitInfo info = topo.result.get(i).val
            splitInfoMap.remove(info.splitName)
            splits.add(info)
        }
        splits.addAll(splitInfoMap.values())
        File splitDetailsFile = detailsCreator.createSplitDetailsJsonFile(splits)
        copyQigsawOutputsToAssetsDir(splits, splitDetailsFile)
    }

    void copyQigsawOutputsToAssetsDir(List<SplitInfo> splits, File splitDetailsFile) {
        File assetsSplitDetailsFile = new File(assetsDir, splitDetailsFile.name)
        if (assetsSplitDetailsFile.exists()) {
            assetsSplitDetailsFile.delete()
        }
        intermediates.add(assetsSplitDetailsFile)
        FileUtils.copyFile(splitDetailsFile, assetsSplitDetailsFile)
        for (SplitInfo info : splits) {
            File assetsSplitApk = new File(assetsDir, info.splitName + SdkConstants.DOT_ZIP)
            intermediates.add(assetsSplitApk)
            if (assetsSplitApk.exists()) {
                assetsSplitApk.delete()
            }
            if (info.builtIn) {
                FileUtils.copyFile(info.splitApk, assetsSplitApk)
            }
        }
    }

    void clearQigsawIntermediates() {
        intermediates.each {
            if (it.exists()) {
                it.delete()
            }
        }
    }

}
