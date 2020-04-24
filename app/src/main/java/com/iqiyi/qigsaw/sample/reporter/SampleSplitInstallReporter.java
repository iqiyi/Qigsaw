package com.iqiyi.qigsaw.sample.reporter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.iqiyi.android.qigsaw.core.splitreport.DefaultSplitInstallReporter;
import com.iqiyi.android.qigsaw.core.splitreport.SplitBriefInfo;
import com.iqiyi.android.qigsaw.core.splitreport.SplitInstallError;

import java.util.List;

public class SampleSplitInstallReporter extends DefaultSplitInstallReporter {

    private static final String TAG = "SampleSplitInstallReporter";

    public SampleSplitInstallReporter(Context context) {
        super(context);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onStartInstallOK(@NonNull List<SplitBriefInfo> installedSplits, long cost) {
        super.onStartInstallOK(installedSplits, cost);
        for (SplitBriefInfo info : installedSplits) {
            if (info.getInstallFlag() == SplitBriefInfo.ALREADY_INSTALLED) {
                Log.d(TAG, String.format("Split %s has been installed, don't need delivery this result", info.splitName));
            } else if (info.getInstallFlag() == SplitBriefInfo.FIRST_INSTALLED) {
                Log.d(TAG, String.format("Split %s is installed firstly, you can delivery this result", info.splitName));
            } else {
                //Oops, it can't happen.
            }
        }
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
