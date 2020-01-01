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
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.internal.InstallUtils
import com.android.builder.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceConnector
import com.android.builder.testing.api.DeviceProvider
import com.android.sdklib.AndroidVersion
import com.android.utils.ILogger
import com.google.common.base.MoreObjects
import com.google.common.collect.ImmutableList
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

class QigsawInstallTask extends DefaultTask {

    BaseVariantData variantData

    @InputFile
    File installApkFile

    @TaskAction
    void installApk() {
        File adbExecutable = null
        try {
            adbExecutable = variantData.scope.globalScope.sdkHandler.sdkInfo.adb
        } catch (Throwable e) {
            try {
                adbExecutable = variantData.scope.globalScope.sdkComponents.adbExecutableProvider.get()
            } catch (Throwable ignored) {

            }
        }
        if (adbExecutable == null) {
            throw new GradleException("> Task :Qigsaw don't support current AGP version, adbExecutable is null!")
        }
        int timeOutInMs = variantData.scope.globalScope.extension.adbOptions.timeOutInMs
        String projectName = variantData.scope.globalScope.project.name
        Collection<String> installOptions = variantData.scope.globalScope.extension.adbOptions.installOptions
        final ILogger iLogger = new LoggerWrapper(getLogger())
        DeviceProvider deviceProvider =
                new ConnectedDeviceProvider(adbExecutable, timeOutInMs, iLogger)
        deviceProvider.init()

        try {
            GradleVariantConfiguration variantConfig = variantData.getVariantConfiguration()

            install(
                    projectName,
                    variantConfig.getFullName(),
                    deviceProvider,
                    variantConfig.getMinSdkVersion(),
                    installApkFile,
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
