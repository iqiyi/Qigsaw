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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.iqiyi.android.qigsaw.core.common.ProcessUtil;
import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.splitdownload.Downloader;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitApkInstaller;
import com.iqiyi.android.qigsaw.core.splitinstall.SplitInstallReporterManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadReporterManager;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitUpdateReporter;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitUpdateReporterManager;

import java.util.concurrent.atomic.AtomicReference;

public class Qigsaw {

    private static final AtomicReference<Qigsaw> sReference = new AtomicReference<>();

    private final Context context;

    private final Downloader downloader;

    private final String currentProcessName;

    private final SplitConfiguration splitConfiguration;

    private Qigsaw(Context context,
                   Downloader downloader,
                   @NonNull SplitConfiguration splitConfiguration) {
        this.context = context;
        this.downloader = downloader;
        this.splitConfiguration = splitConfiguration;
        this.currentProcessName = ProcessUtil.getProcessName(context);
    }

    private static Qigsaw instance() {
        if (sReference.get() == null) {
            throw new RuntimeException("Have you invoke Qigsaw#install(...)?");
        }
        return sReference.get();
    }

    /**
     * You are expected to install qigsaw in {@link Application#attachBaseContext(Context)} method.
     *
     * @param context    we will use the application context
     * @param downloader some apps have their own downloader, so qigsaw just provide interface of download operation.
     */
    public static void install(@NonNull Context context, @NonNull Downloader downloader) {
        SplitConfiguration configuration = SplitConfiguration.newBuilder().build();
        install(context, downloader, configuration);
    }

    /**
     * You are expected to install qigsaw in {@link Application#attachBaseContext(Context)} method.
     *
     * @param context       we will use the application context
     * @param downloader    some apps have their own downloader, so qigsaw just provide interface of download operation.
     * @param configuration {@link SplitConfiguration}
     */
    public static void install(@NonNull Context context,
                               @NonNull Downloader downloader,
                               @NonNull SplitConfiguration configuration) {
        if (sReference.compareAndSet(null, new Qigsaw(context, downloader, configuration))) {
            Qigsaw.instance().onBaseContextAttached();
        }
    }

    private void onBaseContextAttached() {
        SplitBaseInfoProvider.setPackageName(context.getPackageName());
        SplitLoadReporterManager.install(splitConfiguration.loadReporter == null ? new DefaultSplitLoadReporter(context) : splitConfiguration.loadReporter);
        SplitInstallReporterManager.install(splitConfiguration.installReporter == null ? new DefaultSplitInstallReporter(context) : splitConfiguration.installReporter);
        SplitUpdateReporterManager.install(splitConfiguration.updateReporter == null ? new DefaultSplitUpdateReporter(context) : splitConfiguration.updateReporter);
        //init SplitLoadManager and hook PatchCLassLoader.
        boolean qigsawMode = SplitBaseInfoProvider.isQigsawMode();
        SplitLoadManagerService.install(
                context,
                currentProcessName,
                splitConfiguration.splitLoadMode,
                qigsawMode,
                splitConfiguration.workProcesses,
                splitConfiguration.forbiddenWorkProcesses);
        SplitLoadManagerService.getInstance().injectPathClassloader();
        AABExtension.getInstance().createAndActiveSplitApplication(context, qigsawMode);
        SplitCompat.install(context);
    }

    private void onCreated() {
        AABExtension.getInstance().onApplicationCreate();
        //only work in main process!
        if (context.getPackageName().equals(currentProcessName)) {
            SplitApkInstaller.install(
                    context,
                    downloader,
                    splitConfiguration.obtainUserConfirmationDialogClass,
                    splitConfiguration.verifySignature);
            SplitApkInstaller.startUninstallSplits(context);
            Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {

                @Override
                public boolean queueIdle() {
                    cleanStaleSplits(context);
                    return false;
                }
            });
        }
        SplitLoadManagerService.getInstance().loadInstalledSplitsWhenAppLaunches();
    }

    public static void onApplicationCreated() {
        Qigsaw.instance().onCreated();
    }

    /**
     * Called when {@link Application#getResources()} is invoked.
     *
     * @param resources the return value of {@link Application#getResources()}.
     */
    public static void onApplicationGetResources(Resources resources) {
        if (SplitLoadManagerService.hasInstance() && resources != null) {
            SplitLoadManagerService.getInstance().getResources(resources);
        }
    }

    /**
     * Update split info version. If new split does not equal to current version, qigsaw would update it.
     *
     * @param context
     * @param newSplitInfoVersion new split info version.
     * @param newSplitInfoPath    path of new split info file.
     * @return if {@code true} start service successfully, otherwise {@code false}.
     */
    public static boolean updateSplits(Context context,
                                       @NonNull String newSplitInfoVersion,
                                       @NonNull String newSplitInfoPath) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, "com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitUpdateService");
            intent.putExtra(SplitConstants.NEW_SPLIT_INFO_VERSION, newSplitInfoVersion);
            intent.putExtra(SplitConstants.NEW_SPLIT_INFO_PATH, newSplitInfoPath);
            context.startService(intent);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Clean stale disk cache of all splits.
     */
    private static void cleanStaleSplits(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName(context, "com.iqiyi.android.qigsaw.core.splitinstall.SplitCleanService");
            context.startService(intent);
        } catch (Exception e) {
            //ignored
        }
    }

}
