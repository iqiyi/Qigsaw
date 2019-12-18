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

import com.android.build.gradle.api.ApplicationVariant
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.Pair

class SplitContentProviderProcessor {

    final String variantName

    final SAXReader saxReader

    final List<Project> dfProjects

    SplitContentProviderProcessor(String variantName, List<Project> dfProjects) {
        this.variantName = variantName
        this.dfProjects = dfProjects
        this.saxReader = new SAXReader()
    }

    void process(File baseManifestFile, File bundleManifestFile) {
        List<Pair<String, Node>> splitProviderNodes = getSplitProviderNode()
        if (baseManifestFile != null && baseManifestFile.exists()) {
            Document mergedManifestDoc = saxReader.read(baseManifestFile)
            removeSplitProviders(mergedManifestDoc, baseManifestFile, splitProviderNodes)
        }
        if (bundleManifestFile != null && bundleManifestFile.exists()) {
            Document bundleManifestDoc = saxReader.read(bundleManifestFile)
            removeSplitProviders(bundleManifestDoc, bundleManifestFile, splitProviderNodes)
        }
    }

    static void removeSplitProviders(Document document, File xmlFile, List<Pair<String, Node>> splitProviderNodes) {
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
        for (Project dfProject : dfProjects) {
            def dfAndroid = dfProject.extensions.android
            File splitManifestFile = null
            dfAndroid.applicationVariants.all { ApplicationVariant variant ->
                String dfVariantName = variant.name.capitalize()
                if (dfVariantName.equals(variantName)) {
                    Task processManifestTask = AGPCompat.getProcessManifestTask(dfProject, dfVariantName)
                    splitManifestFile = AGPCompat.getMergedManifestFileCompat(processManifestTask)
                    if (splitManifestFile.exists()) {
                        Document splitManifestDoc = saxReader.read(splitManifestFile)
                        Element splitRootEle = splitManifestDoc.getRootElement()
                        List<? extends Node> providers = splitRootEle.selectNodes("//provider")
                        if (providers != null && !providers.empty) {
                            for (Node provideNode : providers) {
                                splitProviderNodes.add(Pair.of(dfProject.name, provideNode))
                            }
                        }
                    }
                }
            }
        }
        return splitProviderNodes
    }

}
