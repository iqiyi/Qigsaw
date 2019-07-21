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

import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.Project
import org.gradle.internal.Pair

class SplitProviderProcessor {

    Project project

    def dynamicFeatures

    String variantName

    SAXReader saxReader = new SAXReader()

    SplitProviderProcessor(Project project,
                           def dynamicFeatures,
                           String variantName) {
        this.project = project
        this.dynamicFeatures = dynamicFeatures
        this.variantName = variantName
    }

    void process() {
        File mergedManifestDir = AGPCompat.getMergedManifestDirCompat(project, variantName)
        File bundleManifestDir = AGPCompat.getBundleManifestDirCompat(project, variantName)
        File mergedManifest = new File(mergedManifestDir, "AndroidManifest.xml")
        File bundleManifest = new File(bundleManifestDir, "AndroidManifest.xml")
        List<Pair<String, Node>> splitProviderNodes = getSplitProviderNode()
        if (mergedManifest.exists()) {
            Document mergedManifestDoc = saxReader.read(new File(mergedManifestDir, "AndroidManifest.xml"))
            removeSplitProviders(mergedManifestDoc, mergedManifest, splitProviderNodes)
        }
        if (bundleManifest.exists()) {
            Document bundleManifestDoc = saxReader.read(new File(bundleManifestDir, "AndroidManifest.xml"))
            removeSplitProviders(bundleManifestDoc, bundleManifest, splitProviderNodes)
        }
    }

    private void removeSplitProviders(Document document, File xmlFile, List<Pair<String, Node>> splitProviderNodes) {
        if (!xmlFile.exists()) {
            project.logger.error("AndroidManifest file " + xmlFile.absolutePath + " is not existed")
            return
        }
        Element rootEle = document.getRootElement()
        List<? extends Node> appProviderNodes = rootEle.selectNodes("//provider")
        if (appProviderNodes != null && !appProviderNodes.empty) {
            if (splitProviderNodes != null && !splitProviderNodes.empty) {
                for (Node appNode : appProviderNodes) {
                    Element appEle = appNode
                    for (Pair<String, Node> splitNode : splitProviderNodes) {
                        Element splitEle = splitNode.right()
                        String appProviderName = appEle.attribute("name").value
                        String splitProviderName = splitEle.attribute("name").value
                        if (appProviderName.equals(splitProviderName)) {
                            appEle.attribute("name").setValue(appProviderName + "_Decorated_" + splitNode.left())
                        }
                    }
                }
            }
        }

        OutputFormat format = OutputFormat.createPrettyPrint()
        format.setEncoding("UTF-8")
        XMLWriter writer = new XMLWriter(
                new OutputStreamWriter(new FileOutputStream(xmlFile)), format)
        writer.write(document)
        writer.close()
    }

    private List<Pair<String, Node>> getSplitProviderNode() {
        List<Pair<String, Node>> splitProviderNodes = new ArrayList<>()
        for (String dynamicFeature : dynamicFeatures) {
            Project dynamicFeatureProject = project.rootProject.project(dynamicFeature)
            File splitManifestDir = AGPCompat.getMergedManifestDirCompat(dynamicFeatureProject, variantName)
            File splitManifest = new File(splitManifestDir, "AndroidManifest.xml")
            if (splitManifest.exists()) {
                Document splitManifestDoc = saxReader.read(splitManifest)
                Element splitRootEle = splitManifestDoc.getRootElement()
                List<? extends Node> providers = splitRootEle.selectNodes("//provider")
                if (providers != null && !providers.empty) {
                    for (Node provideNode : providers) {
                        splitProviderNodes.add(Pair.of(dynamicFeature, provideNode))
                    }
                }
            }
        }
        return splitProviderNodes
    }

}
