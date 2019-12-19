package com.iqiyi.qigsaw.sample;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.support.multidex.MultiDex;
import android.util.Log;

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

    private static final String TAG = "QigsawApplication";

    private static final String[] workProcesses = {":qigsaw"};

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
        //Using QigsawConfig.java to get same info about splits, for example
        Log.d(TAG, String.format("There are %d splits in your app!", QigsawConfig.DYNAMIC_FEATURES.length));
        SplitConfiguration configuration = SplitConfiguration.newBuilder()
                .splitLoadMode(SplitLoad.MULTIPLE_CLASSLOADER)
                .workProcesses(workProcesses)
//                .forbiddenWorkProcesses(forbiddenWorkProcesses)
                .logger(new SampleLogger())
                .verifySignature(true)
                .loadReporter(new SampleSplitLoadReporter(this))
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
