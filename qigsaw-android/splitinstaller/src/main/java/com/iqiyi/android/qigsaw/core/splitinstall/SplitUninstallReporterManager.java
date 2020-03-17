package com.iqiyi.android.qigsaw.core.splitinstall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.splitreport.SplitUninstallReporter;

import java.util.concurrent.atomic.AtomicReference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitUninstallReporterManager {

    private static final AtomicReference<SplitUninstallReporter> sUninstallReporterRef = new AtomicReference<>();

    public static void install(@NonNull SplitUninstallReporter uninstallReporter) {
        sUninstallReporterRef.compareAndSet(null, uninstallReporter);
    }

    @Nullable
    public static SplitUninstallReporter getUninstallReporter() {
        return sUninstallReporterRef.get();
    }

}
