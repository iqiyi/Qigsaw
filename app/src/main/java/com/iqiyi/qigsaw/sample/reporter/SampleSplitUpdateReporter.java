package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitUpdateReporter;

import java.util.List;

public class SampleSplitUpdateReporter extends DefaultSplitUpdateReporter {

    public SampleSplitUpdateReporter(Context context) {
        super(context);
    }


    @Override
    public void onUpdateOK(String oldSplitInfoVersion, String newSplitInfoVersion, List<String> updateSplits) {
        super.onUpdateOK(oldSplitInfoVersion, newSplitInfoVersion, updateSplits);
    }

    @Override
    public void onUpdateFailed(String oldSplitInfoVersion, String newSplitInfoVersion, int errorCode) {
        super.onUpdateFailed(oldSplitInfoVersion, newSplitInfoVersion, errorCode);
    }

    @Override
    public void onNewSplitInfoVersionLoaded(String newSplitInfoVersion) {
        super.onNewSplitInfoVersionLoaded(newSplitInfoVersion);
    }
}
