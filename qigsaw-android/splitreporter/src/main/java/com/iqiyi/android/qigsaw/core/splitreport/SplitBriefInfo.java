package com.iqiyi.android.qigsaw.core.splitreport;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

@Keep
public class SplitBriefInfo {

    public final String splitName;

    public final String version;

    public final boolean builtIn;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SplitBriefInfo(String splitName, String version, boolean builtIn) {
        this.splitName = splitName;
        this.version = version;
        this.builtIn = builtIn;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"splitName\":"
                + "\"" + splitName + "\","
                + "\"version\":"
                + "\"" + version + "\","
                + "\"builtIn\":" + builtIn +
                "}";
    }
}
