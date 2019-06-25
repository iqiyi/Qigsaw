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
    public void onLoadOKUnderProcessStarting(List<String> requestModuleNames, String processName, long cost) {
        super.onLoadOKUnderProcessStarting(requestModuleNames, processName, cost);
    }

    @Override
    public void onLoadFailedUnderProcessStarting(List<String> requestModuleNames, String processName, List<SplitLoadError> errors, long cost) {
        super.onLoadFailedUnderProcessStarting(requestModuleNames, processName, errors, cost);
    }

    @Override
    public void onLoadOKUnderUserTriggering(List<String> requestModuleNames, String processName, long cost) {
        super.onLoadOKUnderUserTriggering(requestModuleNames, processName, cost);
    }

    @Override
    public void onLoadFailedUnderUserTriggering(List<String> requestModuleNames, String processName, List<SplitLoadError> errors, long cost) {
        super.onLoadFailedUnderUserTriggering(requestModuleNames, processName, errors, cost);
    }
}
