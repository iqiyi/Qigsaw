package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;

import java.util.List;

public class SampleSplitInstallReporter extends DefaultSplitInstallReporter {

    public SampleSplitInstallReporter(Context context) {
        super(context);
    }

    @Override
    public void onStartInstallOK(List<String> requestModuleNames, long cost) {
        super.onStartInstallOK(requestModuleNames, cost);
    }

    @Override
    public void onStartInstallFailed(List<String> requestModuleNames, @NonNull SplitInstallError error, long cost) {
        super.onStartInstallFailed(requestModuleNames, error, cost);
    }

    @Override
    public void onDeferredInstallOK(List<String> requestModuleNames, long cost) {
        super.onDeferredInstallOK(requestModuleNames, cost);
    }

    @Override
    public void onDeferredInstallFailed(List<String> requestModuleNames, @NonNull List<SplitInstallError> errors, long cost) {
        super.onDeferredInstallFailed(requestModuleNames, errors, cost);
    }
}
