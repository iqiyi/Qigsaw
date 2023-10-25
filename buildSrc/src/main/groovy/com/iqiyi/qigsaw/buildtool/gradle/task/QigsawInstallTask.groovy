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

import com.android.annotations.NonNull
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.internal.InstallUtils
import com.android.builder.testing.api.DeviceConnector
import com.android.builder.testing.api.DeviceProvider
import com.android.sdklib.AndroidVersion
import com.android.utils.ILogger
import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableList
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.AGPCompat
import org.gradle.util.VersionNumber

import java.util.concurrent.Callable

class QigsawInstallTask extends DefaultTask {

    BaseVariantData variantData

    def versionAGP

    @InputFiles
    List<File> baseApkFiles

    @TaskAction
    void installApk() {
        if (baseApkFiles.size() > 1) {
            throw new GradleException("Qigsaw Error: Qigsaw don't support multi-apks.")
        }
        if (versionAGP < VersionNumber.parse("4.0.0")) {
            installBelowAGP4()
        } else if (versionAGP < VersionNumber.parse("4.1.0")) {
            installAboveAGP4()
        } else {
            installAboveAGP41()
        }
    }

    void installAboveAGP4() {
        File adbExecutable = AGPCompat.getAdbExecutable(variantData, project)
        int timeOutInMs = variantData.scope.globalScope.extension.adbOptions.timeOutInMs
        String projectName = variantData.scope.globalScope.project.name
        Collection<String> installOptions = variantData.scope.globalScope.extension.adbOptions.installOptions
        final ILogger iLogger = new LoggerWrapper(getLogger())
        DeviceProvider deviceProvider = AGPCompat.createDeviceProviderCompat(adbExecutable, timeOutInMs, iLogger)
        if (deviceProvider == null) {
            throw new GradleException("Qigsaw has not adapt this AGP version yet!")
        }
        deviceProvider.use(new Callable<Object>() {
            @Override
            Object call() throws Exception {
                install(
                        projectName,
                        variantData.getName(),
                        deviceProvider,
                        variantData.getVariantDslInfo().getMinSdkVersion(),
                        apkFile,
                        installOptions,
                        timeOutInMs,
                        logger
                )
                return null
            }
        })
    }

    void installAboveAGP41() {
        File adbExecutable = AGPCompat.getAdbExecutable(variantData, project)
        int timeOutInMs = variantData.globalScope.extension.adbOptions.timeOutInMs
        String projectName = project.name
        Collection<String> installOptions = variantData.globalScope.extension.adbOptions.installOptions
        final ILogger iLogger = new LoggerWrapper(getLogger())
        DeviceProvider deviceProvider = AGPCompat.createDeviceProviderCompat(adbExecutable, timeOutInMs, iLogger)
        if (deviceProvider == null) {
            throw new GradleException("Qigsaw has not adapt this AGP version yet!")
        }
        deviceProvider.use(new Callable<Object>() {
            @Override
            Object call() throws Exception {
                install(
                        projectName,
                        variantData.variantSources.getFullName(),
                        deviceProvider,
                        variantData.getVariantDslInfo().getMinSdkVersion(),
                        baseApkFiles.get(0),
                        installOptions,
                        timeOutInMs,
                        logger
                )
                return null
            }
        })
    }


    void installBelowAGP4() {
        File adbExecutable = AGPCompat.getAdbExecutable(variantData, project)
        int timeOutInMs = variantData.scope.globalScope.extension.adbOptions.timeOutInMs
        String projectName = variantData.scope.globalScope.project.name
        Collection<String> installOptions = variantData.scope.globalScope.extension.adbOptions.installOptions
        final ILogger iLogger = new LoggerWrapper(getLogger())
        DeviceProvider deviceProvider = AGPCompat.createDeviceProviderCompat(adbExecutable, timeOutInMs, iLogger)
        if (deviceProvider == null) {
            throw new GradleException("Qigsaw has not adapt this AGP version yet!")
        }
        deviceProvider.init()
        try {
            install(
                    projectName,
                    variantData.getVariantConfiguration().getFullName(),
                    deviceProvider,
                    variantData.getVariantConfiguration().getMinSdkVersion(),
                    baseApkFiles.get(0),
                    installOptions,
                    timeOutInMs,
                    logger)
        } finally {
            deviceProvider.terminate()
        }
    }

    static void install(
            @NonNull String projectName,
            @NonNull String variantName,
            @NonNull DeviceProvider deviceProvider,
            @NonNull AndroidVersion minSkdVersion,
            @NonNull File apkFile,
            @NonNull Collection<String> installOptions,
            int timeOutInMs,
            @NonNull Logger logger) {
        ILogger iLogger = new LoggerWrapper(logger)
        int successfulInstallCount = 0
        List<? extends DeviceConnector> devices = deviceProvider.getDevices()
        for (final DeviceConnector device : devices) {
            if (InstallUtils.checkDeviceApiLevel(
                    device, minSkdVersion, iLogger, projectName, variantName)) {

                final Collection<String> extraArgs =
                        MoreObjects.firstNonNull(installOptions, ImmutableList.<String> of())
                device.installPackage(apkFile, extraArgs, timeOutInMs, iLogger)
                successfulInstallCount++
            }
        }

        if (successfulInstallCount == 0) {
            throw new GradleException("Failed to install on any devices.")
        } else {
            logger.quiet(
                    "Installed on {} {}.",
                    successfulInstallCount,
                    successfulInstallCount == 1 ? "device" : "devices")
        }
    }
}
