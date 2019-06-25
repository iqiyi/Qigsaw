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
import com.iqiyi.qigsaw.buildtool.gradle.task.*
import com.iqiyi.qigsaw.buildtool.gradle.transform.ComponentInfoCreatorTransform
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.VersionNumber

class QigsawAppBasePlugin extends QigsawPlugin {

    public static final String QIGSAW_INTERMEDIATES = "build/intermediates/qigsaw/"

    @Override
    void apply(Project project) {
        project.extensions.create("qigsawSplit", QigsawSplitExtension)

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('generateQigsawApk: Android Application plugin required')
        }
        def versionAGP = VersionNumber.parse(getAndroidGradlePluginVersionCompat())
        if (versionAGP < VersionNumber.parse("3.2.0")) {
            throw new GradleException('generateQigsawApk: Android Gradle Version is required 3.2.0 at least!')
        }
        //extends from AppExtension
        BaseAppModuleExtension android = project.extensions.getByType(BaseAppModuleExtension)

        ComponentInfoCreatorTransform commonInfoCreatorTransform = new ComponentInfoCreatorTransform(project)
        android.registerTransform(commonInfoCreatorTransform)

        project.afterEvaluate {
            //if AAPT2 is disable, package id of plugin resources can not be customized.
            if (!isAapt2EnabledCompat(project)) {
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
                Task generateAssetsTask = getGenerateAssetsTask(project, variantName)
                if (generateAssetsTask == null) {
                    throw new RuntimeException("Cannot found generateAssetsTask, current variantName: " + variantName)
                }

                Task mergeAssetsTask = getMergeAssetsTask(project, variantName)
                Task processManifestTask = getProcessManifestTask(project, variantName)
                Task packageTask = getPackageTask(project, variantName)
                Task generateBuildConfigTask = getGenerateBuildConfigTask(project, variantName)

                QigsawBuildConfigGenerator generator = new QigsawBuildConfigGenerator(generateBuildConfigTask)
                generator.injectFields("DEFAULT_SPLIT_INFO_VERSION", splitInfoVersion)
                generator.injectFields("QIGSAW_ID", qigsawId)
                generator.injectFields("DYNAMIC_FEATURES", dynamicFeatureNames)

                boolean proguardEnable = appVariant.getBuildType().isMinifyEnabled()
                if (proguardEnable) {
                    QigsawProguardConfigTask proguardConfigTask = project.tasks.create("qigsawProcess${variantName}Proguard", QigsawProguardConfigTask)
                    proguardConfigTask.applicationVariant = appVariant
                    proguardConfigTask.packageName = packageName
                    Task r8Task = getR8Task(project, variantName)
                    Task proguardTask = getProguardTask(project, variantName)
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
                    def multidexTask = getMultiDexTask(project, variantName)
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
                    generateAssetsTask.finalizedBy(qigsawAssembleTask)
                    mergeAssetsTask.mustRunAfter(qigsawAssembleTask)
                    qigsawAssembleTask.finalizedBy(appVariant.assembleProvider.get())
                    appVariant.assembleProvider.get().doLast {
                        qigsawAssembleTask.clearQigsawIntermediates()
                    }
                    Task dexSplitterTask = getDexSplitterTask(project, variantName)
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
                qigsawAssembleTask.dependsOn appVariant.assembleProvider.get()
            }
        }
    }

    static Task getGenerateBuildConfigTask(Project project, String variantName) {
        String taskName = "generate${variantName}BuildConfig"
        return project.tasks.findByName(taskName)
    }

    static Task getR8Task(Project project, String variantName) {
        String r8TaskName = "transformClassesAndResourcesWithR8For${variantName}"
        return project.tasks.findByName(r8TaskName)
    }

    static void removeRulesAboutMultidex(Task multidexTask, ApplicationVariant appVariant) {
        multidexTask.doFirst {
            AdjustManifestKeepHandler handler = new AdjustManifestKeepHandler(project, appVariant)
            handler.execute()
        }
    }

    static Task getMultiDexTask(Project project, String variantName) {
        String multiDexTaskName = "transformClassesWithMultidexlistFor${variantName}"
        return project.tasks.findByName(multiDexTaskName)
    }

    static Task getProguardTask(Project project, String variantName) {
        String proguardTaskName = "transformClassesAndResourcesWithProguardFor${variantName}"
        return project.tasks.findByName(proguardTaskName)
    }

    static Task getGenerateAssetsTask(Project project, String variantName) {
        String generateAssetsTaskName = "generate${variantName}Assets"
        return project.tasks.findByName(generateAssetsTaskName)
    }

    static Task getMergeAssetsTask(Project project, String variantName) {
        String mergeAssetsTaskName = "merge${variantName}Assets"
        return project.tasks.findByName(mergeAssetsTaskName)
    }

    static Task getPackageTask(Project project, String variantName) {
        String packageTaskName = "package${variantName}"
        return project.tasks.findByName(packageTaskName)
    }

    static Task getDexSplitterTask(Project project, String variantName) {
        String proguardTaskName = "transformDexWithDexSplitterFor${variantName}"
        return project.tasks.findByName(proguardTaskName)

    }

    /**
     * get android gradle plugin version by reflect
     */
    static String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception e) {

        }
        return version
    }

    /**
     * get whether aapt2 is enabled
     */
    static boolean isAapt2EnabledCompat(Project project) {
        if (getAndroidGradlePluginVersionCompat() >= '3.3.0') {
            //when agp' version >= 3.3.0, use aapt2 default and no way to switch to aapt.
            return true
        }
        boolean aapt2Enabled = false
        try {
            def projectOptions = getProjectOptions(project)
            Object enumValue = resolveEnumValue("ENABLE_AAPT2", Class.forName("com.android.build.gradle.options.BooleanOption"))
            aapt2Enabled = projectOptions.get(enumValue)
        } catch (Exception e) {
            //ignored
        }
        return aapt2Enabled
    }

    /**
     * get com.android.build.gradle.options.ProjectOptions obj by reflect
     */
    static def getProjectOptions(Project project) {
        try {
            def basePlugin = project.getPlugins().hasPlugin('com.android.application') ? project.getPlugins().findPlugin('com.android.application') : project.getPlugins().findPlugin('com.android.library')
            return Class.forName("com.android.build.gradle.BasePlugin").getMetaClass().getProperty(basePlugin, 'projectOptions')
        } catch (Exception e) {
        }
        return null
    }

    /**
     * get enum obj by reflect
     */
    static <T> T resolveEnumValue(String value, Class<T> type) {
        for (T constant : type.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(value)) {
                return constant
            }
        }
        return null
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
