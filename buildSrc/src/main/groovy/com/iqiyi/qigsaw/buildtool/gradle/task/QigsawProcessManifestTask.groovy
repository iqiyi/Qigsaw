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
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.SplitLogger
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.Pair

class QigsawProcessManifestTask extends DefaultTask {

    final SAXReader saxReader = new SAXReader()

    @Input
    Set<String> dynamicFeatureNames

    @InputDirectory
    File splitManifestDir

    @InputFile
    File mergedManifestFile

    @InputFile
    @Optional
    File bundleManifestFile

    @TaskAction
    void processBaseManifest() {
        List<Pair<String, Node>> splitProviderNodes = getSplitProviderNodes()
        if (mergedManifestFile != null && mergedManifestFile.exists()) {
            SplitLogger.w("Start to modify base merged-manifest file " + mergedManifestFile)
            Document mergedManifestDoc = saxReader.read(mergedManifestFile)
            modifyBaseManifestContent(mergedManifestDoc, mergedManifestFile, splitProviderNodes)
        }
        if (bundleManifestFile != null && bundleManifestFile.exists()) {
            SplitLogger.w("Start to modify base bundle-manifest file " + bundleManifestFile)
            Document bundleManifestDoc = saxReader.read(bundleManifestFile)
            modifyBaseManifestContent(bundleManifestDoc, bundleManifestFile, splitProviderNodes)
        }
    }

    static void modifyBaseManifestContent(Document document, File xmlFile, List<Pair<String, Node>> splitProviderNodes) {
        Element rootEle = document.getRootElement()
        List<? extends Node> appProviderNodes = rootEle.selectNodes("//provider")
        if (appProviderNodes != null && !appProviderNodes.empty) {
            if (splitProviderNodes != null && !splitProviderNodes.empty) {
                for (Node appProviderNode : appProviderNodes) {
                    Element appProviderEle = appProviderNode
                    for (Pair<String, Node> splitProviderNode : splitProviderNodes) {
                        Element splitProviderEle = splitProviderNode.right()
                        String appProviderName = appProviderEle.attribute("name").value
                        String splitProviderName = splitProviderEle.attribute("name").value
                        if (appProviderName == splitProviderName) {
                            appProviderEle.attribute("name").setValue(appProviderName + "_Decorated_" + splitProviderNode.left())
                        }
                    }
                }
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint()
        format.setEncoding("UTF-8")
        XMLWriter writer = new XMLWriter(
                new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8"), format)
        writer.write(document)
        writer.close()
    }

    private List<Pair<String, Node>> getSplitProviderNodes() {
        List<Pair<String, Node>> splitProviderNodes = new ArrayList<>()
        List<File> splitManifests = new ArrayList<>()
        dynamicFeatureNames.each {
            File splitManifest = new File(splitManifestDir, it + SdkConstants.DOT_XML)
            if (!splitManifest.exists()) {
                throw new GradleException("Qigsaw Error: ${splitManifest} does not exist!")
            }
            splitManifests.add(splitManifest)
        }
        for (File splitManifest : splitManifests) {
            if (splitManifest.exists()) {
                String splitName = splitManifest.name.split("\\.")[0]
                Document splitManifestDoc = saxReader.read(splitManifest)
                Element splitRootEle = splitManifestDoc.getRootElement()
                List<? extends Node> providers = splitRootEle.selectNodes("//provider")
                if (providers != null && !providers.empty) {
                    for (Node provideNode : providers) {
                        splitProviderNodes.add(Pair.of(splitName, provideNode))
                    }
                }
            }
        }
        return splitProviderNodes
    }
}
