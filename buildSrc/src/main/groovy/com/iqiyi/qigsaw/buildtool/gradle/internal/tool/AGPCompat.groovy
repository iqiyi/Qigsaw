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

package com.iqiyi.qigsaw.buildtool.gradle.internal.tool

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.Task

class AGPCompat {

    static File getMergedManifestDirCompat(Project project, String variantName) {
        Task processManifest = getProcessManifestTask(project, variantName)
        File mergedManifestDir = null
        if (processManifest != null) {
            processManifest.outputs.files.each {
                if (it.toPath().toString().contains(File.separator + "merged_manifests" + File.separator)) {
                    mergedManifestDir = it.toPath().toFile()
                }
            }
        }
        return mergedManifestDir
    }

    static File getBundleManifestDirCompat(Project project, String variantName) {
        Task processManifest = getProcessManifestTask(project, variantName)
        File bundleManifestDir = null
        if (processManifest != null) {
            processManifest.outputs.files.each {
                if (it.toPath().toString().contains(File.separator + "bundle_manifest" + File.separator)) {
                    bundleManifestDir = it.toPath().toFile()
                }
            }
        }
        return bundleManifestDir
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
     * get android gradle plugin version by reflect
     */
    static String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception ignored) {

        }
        return version
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

    static Task getProcessManifestTask(Project project, String variantName) {
        String mergeManifestTaskName = "process${variantName}Manifest"
        return project.tasks.findByName(mergeManifestTaskName)
    }

    static Task getAssemble(ApplicationVariant variant) {
        try {
            return variant.assembleProvider.get()
        } catch (Exception e) {
            return variant.assemble
        }
    }

    static Task getPackageApplication(ApplicationVariant variant) {
        Task packageApplicationTask
        try {
            packageApplicationTask = variant.getPackageApplicationProvider().get()
        } catch (Exception e) {
            packageApplicationTask = variant.packageApplication
        }
        return packageApplicationTask
    }

    static Task getGenerateBuildConfigTask(Project project, String variantName) {
        String taskName = "generate${variantName}BuildConfig"
        return project.tasks.findByName(taskName)
    }

    static Task getR8Task(Project project, String variantName) {
        String r8TaskName = "transformClassesAndResourcesWithR8For${variantName}"
        return project.tasks.findByName(r8TaskName)
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

}
