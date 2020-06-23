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
    public void onStartInstallOK(@NonNull List<SplitBriefInfo> installOKSplits, long cost) {
        super.onStartInstallOK(installOKSplits, cost);
        for (SplitBriefInfo info : installOKSplits) {
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
    public void onStartInstallFailed(@NonNull List<SplitBriefInfo> installOKSplits, @NonNull SplitInstallError installErrorSplit, long cost) {
        super.onStartInstallFailed(installOKSplits, installErrorSplit, cost);
    }

    @Override
    public void onDeferredInstallOK(@NonNull List<SplitBriefInfo> installOKSplits, long cost) {
        super.onDeferredInstallOK(installOKSplits, cost);
    }

    @Override
    public void onDeferredInstallFailed(@NonNull List<SplitBriefInfo> installOKSplits, @NonNull List<SplitInstallError> installErrorSplit, long cost) {
        super.onDeferredInstallFailed(installOKSplits, installErrorSplit, cost);
    }
}
