package com.iqiyi.qigsaw.sample.reporter;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitUninstallReporter;

import java.util.List;

public class SampleSplitUninstallReporter extends DefaultSplitUninstallReporter {

    public SampleSplitUninstallReporter(Context context) {
        super(context);
    }

    @Override
    public void onSplitUninstallOK(List<String> uninstalledSplits, long cost) {
        super.onSplitUninstallOK(uninstalledSplits, cost);
    }
}
