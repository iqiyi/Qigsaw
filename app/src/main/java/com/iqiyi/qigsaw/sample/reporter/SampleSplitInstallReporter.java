package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;

import java.util.List;

public class SampleSplitInstallReporter extends DefaultSplitInstallReporter {

    public SampleSplitInstallReporter(Context context) {
        super(context);
    }

    @Override
    public void onStartInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost) {
        super.onStartInstallOK(installedSplits, cost);
    }

    @Override
    public void onStartInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull SplitInstallError error, long cost) {
        super.onStartInstallFailed(installedSplits, error, cost);
    }

    @Override
    public void onDeferredInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost) {
        super.onDeferredInstallOK(installedSplits, cost);
    }

    @Override
    public void onDeferredInstallFailed(@NonNull List<SplitBriefInfo> installedSplits, @NonNull List<SplitInstallError> errors, long cost) {
        super.onDeferredInstallFailed(installedSplits, errors, cost);
    }
}
