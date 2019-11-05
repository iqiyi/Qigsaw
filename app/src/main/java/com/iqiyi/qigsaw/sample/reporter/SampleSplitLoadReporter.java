package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

import java.util.List;

public class SampleSplitLoadReporter extends DefaultSplitLoadReporter {

    public SampleSplitLoadReporter(Context context) {
        super(context);
    }

    @Override
    public void onLoadOK(String processName, List<SplitBriefInfo> requestSplits, long cost) {
        super.onLoadOK(processName, requestSplits, cost);
    }

    @Override
    public void onLoadFailed(String processName, List<SplitBriefInfo> requestSplits, List<SplitLoadError> errors, long cost) {
        super.onLoadFailed(processName, requestSplits, errors, cost);
    }
}
