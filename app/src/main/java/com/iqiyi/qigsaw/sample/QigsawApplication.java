package com.iqiyi.qigsaw.sample;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.support.multidex.MultiDex;

import com.iqiyi.android.qigsaw.core.SplitConfiguration;
import com.iqiyi.android.qigsaw.core.Qigsaw;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoad;
import com.iqiyi.qigsaw.sample.downloader.SampleDownloader;
import com.iqiyi.qigsaw.sample.reporter.SampleLogger;
import com.iqiyi.qigsaw.sample.reporter.SampleSplitInstallReporter;
import com.iqiyi.qigsaw.sample.reporter.SampleSplitLoadReporter;
import com.iqiyi.qigsaw.sample.reporter.SampleSplitUpdateReporter;

public class QigsawApplication extends Application {

//    private static final String[] forbiddenWorkProcesses = {":qigsaw"};

    private static final String[] workProcesses = {":qigsaw"};

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
        SplitConfiguration configuration = SplitConfiguration.newBuilder()
                .splitLoadMode(SplitLoad.MULTIPLE_CLASSLOADER)
                .workProcesses(workProcesses)
//                .forbiddenWorkProcesses(forbiddenWorkProcesses)
                .logger(new SampleLogger())
                .verifySignature(true)
                .loadReporter(new SampleSplitLoadReporter(this))
                .manifestPackageName(base.getPackageName())
                .installReporter(new SampleSplitInstallReporter(this))
                .updateReporter(new SampleSplitUpdateReporter(this))
                .obtainUserConfirmationDialogClass(SampleObtainUserConfirmationDialog.class)
                .build();
        Qigsaw.install(this, new SampleDownloader(), configuration);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Qigsaw.onApplicationCreated();
    }

    @Override
    public Resources getResources() {
        Qigsaw.onApplicationGetResources(super.getResources());
        return super.getResources();
    }
}
