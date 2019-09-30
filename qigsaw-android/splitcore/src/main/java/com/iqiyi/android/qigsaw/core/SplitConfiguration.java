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

import android.support.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;

public class SplitConfiguration {

    /**
     * processes which are forbidden to load all installed splits during application launch time, if null all processes would load.
     */
    final String[] forbiddenWorkProcesses;

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

    public static SplitConfiguration.Builder newBuilder() {
        return new SplitConfiguration.Builder();
    }

    private SplitConfiguration(Builder builder) {
        this.forbiddenWorkProcesses = builder.forbiddenWorkProcesses;
        this.manifestPackageName = builder.manifestPackageName;
        this.installReporter = builder.installReporter;
        this.loadReporter = builder.loadReporter;
        this.updateReporter = builder.updateReporter;
        this.obtainUserConfirmationDialogClass = builder.obtainUserConfirmationDialogClass;
    }

    public static class Builder {

        private String[] forbiddenWorkProcesses;

        private String manifestPackageName;

        private SplitInstallReporter installReporter;

        private SplitLoadReporter loadReporter;

        private SplitUpdateReporter updateReporter;

        private Class<? extends ObtainUserConfirmationDialog> obtainUserConfirmationDialogClass;

        private Builder() {
            this.obtainUserConfirmationDialogClass = DefaultObtainUserConfirmationDialog.class;
        }

        /**
         * Customized logger for {@link SplitLog}
         */
        public Builder logger(@NonNull SplitLog.Logger logger) {
            SplitLog.setSplitLogImp(logger);
            return this;
        }

        public Builder forbiddenWorkProcesses(@NonNull String[] forbiddenWorkProcesses) {
            this.forbiddenWorkProcesses = forbiddenWorkProcesses;
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

        public SplitConfiguration build() {
            return new SplitConfiguration(this);
        }
    }


}
