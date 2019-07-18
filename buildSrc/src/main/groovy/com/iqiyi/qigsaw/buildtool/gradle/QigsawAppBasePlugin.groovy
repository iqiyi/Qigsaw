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

package com.iqiyi.qigsaw.buildtool.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.iqiyi.qigsaw.buildtool.gradle.extension.QigsawSplitExtension
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import com.iqiyi.qigsaw.buildtool.gradle.task.*
import com.iqiyi.qigsaw.buildtool.gradle.transform.ComponentInfoCreatorTransform
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.util.VersionNumber

class QigsawAppBasePlugin extends QigsawPlugin {

    public static final String QIGSAW_INTERMEDIATES = "build/intermediates/qigsaw/"

    @Override
    void apply(Project project) {
        project.extensions.create("qigsawSplit", QigsawSplitExtension)

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('generateQigsawApk: Android Application plugin required')
        }
        def versionAGP = VersionNumber.parse(AGPCompat.getAndroidGradlePluginVersionCompat())
        if (versionAGP < VersionNumber.parse("3.2.0")) {
            throw new GradleException('generateQigsawApk: Android Gradle Version is required 3.2.0 at least!')
        }
        //extends from AppExtension
        BaseAppModuleExtension android = project.extensions.getByType(BaseAppModuleExtension)

        ComponentInfoCreatorTransform commonInfoCreatorTransform = new ComponentInfoCreatorTransform(project)
        android.registerTransform(commonInfoCreatorTransform)

        project.afterEvaluate {
            //if AAPT2 is disable, package id of plugin resources can not be customized.
            if (!AGPCompat.isAapt2EnabledCompat(project)) {
                throw new GradleException('generateQigsawApk: AAPT2 required')
            }
            def dynamicFeatures = android.dynamicFeatures
            File assetsDir = null
            File baseManifestSrcFile = null
            android.sourceSets.each {
                if (it.name.equals("main")) {
                    assetsDir = it.getAssets().srcDirs.getAt(0)
                    baseManifestSrcFile = it.getManifest().srcFile
                }
            }
            //if there is no dynamic features, qigsaw would not work
            List<Project> featureProjects = new ArrayList<>()
            List<String> dynamicFeatureNames = new ArrayList<>()
            for (String dynamicFeature : dynamicFeatures) {
                Project featureProject = project.rootProject.project(dynamicFeature)
                featureProjects.add(featureProject)
                dynamicFeatureNames.add(featureProject.name)
            }

            BaseAppInfoGetterImpl infoGetter = new BaseAppInfoGetterImpl(project, baseManifestSrcFile)
            String versionName = infoGetter.versionName
            String packageName = infoGetter.packageName
            String qigsawId = versionName + "_" + getQigsawId(project)
            String splitInfoVersion = versionName + "_" + project.extensions.qigsawSplit.splitInfoVersion

            //config qigsaw tasks
            android.getApplicationVariants().all { variant ->
                ApplicationVariant appVariant = variant
                String variantName = appVariant.name.capitalize()
                def variantData = appVariant.variantData
                Task generateAssetsTask = AGPCompat.getGenerateAssetsTask(project, variantName)
                if (generateAssetsTask == null) {
                    throw new RuntimeException("Cannot found generateAssetsTask, current variantName: " + variantName)
                }

                Task mergeAssetsTask = AGPCompat.getMergeAssetsTask(project, variantName)
                Task processManifestTask = AGPCompat.getProcessManifestTask(project, variantName)
                Task packageTask = AGPCompat.getPackageTask(project, variantName)
                Task generateBuildConfigTask = AGPCompat.getGenerateBuildConfigTask(project, variantName)

                QigsawBuildConfigGenerator generator = new QigsawBuildConfigGenerator(generateBuildConfigTask)
                generator.injectFields("DEFAULT_SPLIT_INFO_VERSION", splitInfoVersion)
                generator.injectFields("QIGSAW_ID", qigsawId)
                generator.injectFields("DYNAMIC_FEATURES", dynamicFeatureNames)

                boolean proguardEnable = appVariant.getBuildType().isMinifyEnabled()
                if (proguardEnable) {
                    QigsawProguardConfigTask proguardConfigTask = project.tasks.create("qigsawProcess${variantName}Proguard", QigsawProguardConfigTask)
                    proguardConfigTask.applicationVariant = appVariant
                    proguardConfigTask.packageName = packageName
                    Task r8Task = AGPCompat.getR8Task(project, variantName)
                    Task proguardTask = AGPCompat.getProguardTask(project, variantName)
                    if (proguardTask != null) {
                        proguardTask.dependsOn proguardConfigTask
                    } else {
                        if (r8Task != null) {
                            r8Task.dependsOn proguardConfigTask
                        }
                    }
                    proguardConfigTask.mustRunAfter processManifestTask
                }
                boolean multiDexEnabled = variantData.variantConfiguration.isMultiDexEnabled()
                if (multiDexEnabled) {
                    def multidexTask = AGPCompat.getMultiDexTask(project, variantName)
                    if (multidexTask != null) {
                        removeRulesAboutMultidex(multidexTask, appVariant)
                    }
                }
                QigsawAssembleTask qigsawAssembleTask = project.tasks.create(QIGSAW_ASSEMBLE_TASK_PREFIX + variantName,
                        QigsawAssembleTask,
                        assetsDir,
                        variantName,
                        versionName,
                        dynamicFeatures,
                        qigsawId
                )

                Task showDependencies = createShowDependenciesTask(project, variantName)

                showDependencies.doLast {

                    Map<String, List<String>> dynamicFeatureDependenciesMap = new HashMap<>()

                    List<String> dynamicFeaturesClassPaths = new ArrayList<>(featureProjects.size())

                    featureProjects.each {
                        Project dynamicFeatureProject = it
                        String str = "${dynamicFeatureProject.group}:${dynamicFeatureProject.name}:${dynamicFeatureProject.version}"
                        dynamicFeaturesClassPaths.add(str)
                    }

                    featureProjects.each {
                        Project dynamicFeatureProject = it
                        Configuration configuration = dynamicFeatureProject.configurations."${variant.name}CompileClasspath"
                        configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.each {
                            def identifier = it.module.id
                            List<String> dynamicFeatureDependencies = new ArrayList<>(0)
                            String classPath = "${identifier.group}:${identifier.name}:${identifier.version}"
                            if (dynamicFeaturesClassPaths.contains(classPath)) {
                                dynamicFeatureDependencies.add(identifier.name)
                            }
                            if (!dynamicFeatureDependencies.isEmpty()) {
                                dynamicFeatureDependenciesMap.put(dynamicFeatureProject.name, dynamicFeatureDependencies)
                            }
                        }
                    }

                    qigsawAssembleTask.dynamicFeatureDependenciesMap = dynamicFeatureDependenciesMap
                }

                qigsawAssembleTask.setGroup(QIGSAW)

                //set task dependency
                if (hasQigsawTask(project)) {

                    featureProjects.each {
                        Project dynamicFeatureProject = it
                        try {
                            configQigsawAssembleTaskDependencies(dynamicFeatureProject, variantName, qigsawAssembleTask)
                            println("dynamic feature project has been evaluated!")
                        } catch (Exception e) {
                            println("dynamic feature project has not been evaluated!")
                            it.afterEvaluate {
                                configQigsawAssembleTaskDependencies(dynamicFeatureProject, variantName, qigsawAssembleTask)
                            }
                        }
                    }

                    Task assembleTask = AGPCompat.getAssemble(appVariant)
                    qigsawAssembleTask.dependsOn showDependencies
                    generateAssetsTask.finalizedBy(qigsawAssembleTask)
                    mergeAssetsTask.mustRunAfter(qigsawAssembleTask)
                    qigsawAssembleTask.finalizedBy(assembleTask)
                    assembleTask.doLast {
                        qigsawAssembleTask.clearQigsawIntermediates()
                    }
                    processManifestTask.doLast {
                        SplitProviderProcessor providerProcessor = new SplitProviderProcessor(project, dynamicFeatureNames, variantName)
                        providerProcessor.process()
                    }
                    Task dexSplitterTask = AGPCompat.getDexSplitterTask(project, variantName)
                    packageTask.doFirst {
                        if (dexSplitterTask != null) {
                            println("start to remerge dex files!")
                            def startTime = new Date()
                            List<File> dexFiles = new ArrayList<>()
                            inputs.files.each { file ->
                                file.listFiles().each { x ->
                                    if (x.absolutePath.endsWith(".dex")) {
                                        dexFiles.add(x)
                                    }
                                }
                            }
                            DexReMergeHandler handler = new DexReMergeHandler(project, appVariant)
                            handler.reMerge(dexFiles)
                            def endTime = new Date()
                            println endTime.getTime() - startTime.getTime() + "ms"
                        }
                    }
                }
            }
        }
    }

    private static void configQigsawAssembleTaskDependencies(Project dynamicFeatureProject, String baseAppVariant, Task qigsawAssembleTask) {
        AppExtension dynamicFeatureAndroid = dynamicFeatureProject.extensions.getByType(AppExtension)
        dynamicFeatureAndroid.applicationVariants.all { variant ->
            ApplicationVariant appVariant = variant
            if (appVariant.name.equalsIgnoreCase(baseAppVariant)) {
                qigsawAssembleTask.dependsOn AGPCompat.getAssemble(appVariant)
            }
        }
    }

    static Task createShowDependenciesTask(Project project, String variantName) {
        String taskName = "showDependencies${variantName}"
        return project.tasks.create(taskName)
    }

    static void removeRulesAboutMultidex(Task multidexTask, ApplicationVariant appVariant) {
        multidexTask.doFirst {
            AdjustManifestKeepHandler handler = new AdjustManifestKeepHandler(project, appVariant)
            handler.execute()
        }
    }

    static String getQigsawId(Project project) {
        try {
            String gitRev = 'git rev-parse --short HEAD'.execute(null, project.rootDir).text.trim()
            if (gitRev == null) {
                return "NO_GIT"
            }
            return gitRev
        } catch (Exception e) {
            return "NO_GIT"
        }
    }

}
