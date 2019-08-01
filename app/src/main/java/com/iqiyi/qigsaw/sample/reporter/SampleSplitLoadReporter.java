package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

import java.util.List;

public class SampleSplitLoadReporter extends DefaultSplitLoadReporter {

    public SampleSplitLoadReporter(Context context) {
        super(context);
    }

    @Override
    public void onLoadOK(List<String> requestModuleNames, String processName, long cost) {
        super.onLoadOK(requestModuleNames, processName, cost);
    }

    @Override
    public void onLoadFailed(List<String> requestModuleNames, String processName, List<SplitLoadError> errors, long cost) {
        super.onLoadFailed(requestModuleNames, processName, errors, cost);
    }

}
