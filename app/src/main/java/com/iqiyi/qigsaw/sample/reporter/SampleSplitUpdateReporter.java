package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitUpdateReporter;

import java.util.List;

public class SampleSplitUpdateReporter extends DefaultSplitUpdateReporter {

    public SampleSplitUpdateReporter(Context context) {
        super(context);
    }

    @Override
    public void onUpdateOK(String newSplitInfoVersion, List<String> updateSplits) {
        super.onUpdateOK(newSplitInfoVersion, updateSplits);
    }

    @Override
    public void onUpdateFailed(String newSplitInfoVersion, int errorCode) {
        super.onUpdateFailed(newSplitInfoVersion, errorCode);
    }

    @Override
    public void onNewSplitInfoVersionLoaded(String newSplitInfoVersion) {
        super.onNewSplitInfoVersionLoaded(newSplitInfoVersion);
    }
}
