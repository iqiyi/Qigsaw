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

import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoad;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUninstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;

public class SplitConfiguration {

    final int splitLoadMode;

    final String[] workProcesses;

    final String[] forbiddenWorkProcesses;

    final SplitInstallReporter installReporter;

    final SplitLoadReporter loadReporter;

    final SplitUpdateReporter updateReporter;

    final SplitUninstallReporter uninstallReporter;

    final Class<? extends ObtainUserConfirmationDialog> obtainUserConfirmationDialogClass;

    final boolean verifySignature;

    public static SplitConfiguration.Builder newBuilder() {
        return new SplitConfiguration.Builder();
    }

    private SplitConfiguration(Builder builder) {
        if (builder.forbiddenWorkProcesses != null && builder.workProcesses != null) {
            throw new RuntimeException("forbiddenWorkProcesses and workProcesses can't be set at the same time, you should choose one of them.");
        }
        this.splitLoadMode = builder.splitLoadMode;
        this.forbiddenWorkProcesses = builder.forbiddenWorkProcesses;
        this.installReporter = builder.installReporter;
        this.loadReporter = builder.loadReporter;
        this.updateReporter = builder.updateReporter;
        this.uninstallReporter = builder.uninstallReporter;
        this.obtainUserConfirmationDialogClass = builder.obtainUserConfirmationDialogClass;
        this.workProcesses = builder.workProcesses;
        this.verifySignature = builder.verifySignature;
    }

    public static class Builder {

        private int splitLoadMode = SplitLoad.MULTIPLE_CLASSLOADER;

        private String[] workProcesses;

        private String[] forbiddenWorkProcesses;

        private SplitInstallReporter installReporter;

        private SplitLoadReporter loadReporter;

        private SplitUpdateReporter updateReporter;

        private SplitUninstallReporter uninstallReporter;

        private Class<? extends ObtainUserConfirmationDialog> obtainUserConfirmationDialogClass;

        private boolean verifySignature = true;

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

        /**
         * You can decide to use single or multiple class loader mode to load splits, see {@link SplitLoad} to know more details.
         */
        public Builder splitLoadMode(@SplitLoad.SplitLoadMode int splitLoadMode) {
            this.splitLoadMode = splitLoadMode;
            return this;
        }

        /**
         * Processes(main process always work) which are permitted to load all installed splits during application launch time.
         * This is method can't be invoked with {@link Builder#forbiddenWorkProcesses(String[])} together.
         */
        public Builder workProcesses(@NonNull String[] workProcesses) {
            if (workProcesses.length > 0) {
                this.workProcesses = workProcesses;
            }
            return this;
        }

        /**
         * Processes which are forbidden to load all installed splits during application launch time.
         * This is method can't be invoked with {@link Builder#workProcesses(String[])} together.
         */
        public Builder forbiddenWorkProcesses(@NonNull String[] forbiddenWorkProcesses) {
            if (forbiddenWorkProcesses.length > 0) {
                this.forbiddenWorkProcesses = forbiddenWorkProcesses;
            }
            return this;
        }

        /**
         * Report installing status when splits are fully installed.
         */
        public Builder installReporter(@NonNull SplitInstallReporter installReporter) {
            this.installReporter = installReporter;
            return this;
        }

        /**
         * Report loading status when splits are fully loaded.
         */
        public Builder loadReporter(@NonNull SplitLoadReporter loadReporter) {
            this.loadReporter = loadReporter;
            return this;
        }

        /**
         * Report uninstall status when splits are fully uninstalled.
         */
        public Builder uninstallReporter(@NonNull SplitUninstallReporter uninstallReporter) {
            this.uninstallReporter = uninstallReporter;
            return this;
        }

        /**
         * Report updating status when split info version is fully updated.
         */
        public Builder updateReporter(@NonNull SplitUpdateReporter updateReporter) {
            this.updateReporter = updateReporter;
            return this;
        }

        /**
         * Customized dialog for requiring user confirmation whether allowed to download splits while using mobile data
         */
        public Builder obtainUserConfirmationDialogClass(@NonNull Class<? extends ObtainUserConfirmationDialog> clazz) {
            this.obtainUserConfirmationDialogClass = clazz;
            return this;
        }

        /**
         * Whether need to verify signature of split apk. if {@code true} split apk is verified to match the base apk,
         * otherwise ignore signature verification for split apks.
         */
        public Builder verifySignature(boolean verifySignature) {
            this.verifySignature = verifySignature;
            return this;
        }

        public SplitConfiguration build() {
            return new SplitConfiguration(this);
        }
    }


}
