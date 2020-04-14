package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;
import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitLoadReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

import java.util.List;

public class SampleSplitLoadReporter extends DefaultSplitLoadReporter {

    public SampleSplitLoadReporter(Context context) {
        super(context);
    }

    @Override
    public void onLoadOK(String processName, @NonNull List<SplitBriefInfo> loadedSplits, long cost) {
        super.onLoadOK(processName, loadedSplits, cost);
    }

    @Override
    public void onLoadFailed(String processName, @NonNull List<SplitBriefInfo> loadedSplits, @NonNull List<SplitLoadError> errors, long cost) {
        super.onLoadFailed(processName, loadedSplits, errors, cost);
    }
}
