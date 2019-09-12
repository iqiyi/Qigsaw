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

package com.iqiyi.android.qigsaw.core;

import android.app.Application;
import android.support.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;

public class SplitConfiguration {

    /**
     * processes that load all installed splits during application launch time, if null all processes would load.
     */
    final String[] workProcesses;

    /**
     * if the package name you declared in app manifest does not match applicationId in app/build.gradle,
     * you have to set it.
     */
    final String manifestPackageName;

    /**
     * Report installing status when splits are fully installed.
     */
    final SplitInstallReporter installReporter;

    /**
     * Report loading status when splits are fully loaded.
     */
    final SplitLoadReporter loadReporter;

    /**
     * Report updating status when split info version is fully updated.
     */
    final SplitUpdateReporter updateReporter;

    /**
     * Customized dialog for requiring user confirmation whether allowed to download splits while using mobile data
     */
    final Class<? extends ObtainUserConfirmationDialog> obtainUserConfirmationDialogClass;

    /**
     * Load all installed splits during {@link Application#onCreate()},
     * if {@code true}, this operation maybe affect launch performance of process.
     */
    final boolean loadInstalledSplitsOnApplicationCreate;

    public static SplitConfiguration.Builder newBuilder() {
        return new SplitConfiguration.Builder();
    }

    private SplitConfiguration(Builder builder) {
        this.workProcesses = builder.workProcesses;
        this.manifestPackageName = builder.manifestPackageName;
        this.installReporter = builder.installReporter;
        this.loadReporter = builder.loadReporter;
        this.updateReporter = builder.updateReporter;
        this.obtainUserConfirmationDialogClass = builder.obtainUserConfirmationDialogClass;
        this.loadInstalledSplitsOnApplicationCreate = builder.loadInstalledSplitsOnApplicationCreate;
    }

    public static class Builder {

        private String[] workProcesses;

        private String manifestPackageName;

        private SplitInstallReporter installReporter;

        private SplitLoadReporter loadReporter;

        private SplitUpdateReporter updateReporter;

        private boolean loadInstalledSplitsOnApplicationCreate;

        private Class<? extends ObtainUserConfirmationDialog> obtainUserConfirmationDialogClass;

        private Builder() {
            this.loadInstalledSplitsOnApplicationCreate = true;
            this.obtainUserConfirmationDialogClass = DefaultObtainUserConfirmationDialog.class;
        }

        /**
         * Customized logger for {@link SplitLog}
         */
        public Builder logger(@NonNull SplitLog.Logger logger) {
            SplitLog.setSplitLogImp(logger);
            return this;
        }

        public Builder workProcesses(@NonNull String[] workProcesses) {
            this.workProcesses = workProcesses;
            return this;
        }

        public Builder manifestPackageName(@NonNull String manifestPackageName) {
            this.manifestPackageName = manifestPackageName;
            return this;
        }

        public Builder installReporter(@NonNull SplitInstallReporter installReporter) {
            this.installReporter = installReporter;
            return this;
        }

        public Builder loadReporter(@NonNull SplitLoadReporter loadReporter) {
            this.loadReporter = loadReporter;
            return this;
        }

        public Builder updateReporter(@NonNull SplitUpdateReporter updateReporter) {
            this.updateReporter = updateReporter;
            return this;
        }

        public Builder obtainUserConfirmationDialogClass(@NonNull Class<? extends ObtainUserConfirmationDialog> clazz) {
            this.obtainUserConfirmationDialogClass = clazz;
            return this;
        }

        public Builder loadInstalledSplitsOnApplicationCreate(boolean value) {
            this.loadInstalledSplitsOnApplicationCreate = value;
            return this;
        }

        public SplitConfiguration build() {
            return new SplitConfiguration(this);
        }
    }


}
