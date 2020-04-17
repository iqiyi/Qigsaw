package com.google.android.play.core.splitcompat.util;

import android.os.Process;
import androidx.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class PlayCore {

    private String mTag;

    public PlayCore(String tag) {
        String valueOf = "UID: [" + Process.myUid() + "]  PID: [" + Process.myPid() + "] ";
        String valueOf2 = String.valueOf(tag);
        this.mTag = valueOf2.length() != 0 ? valueOf.concat(valueOf2) : valueOf;
    }

    public int info(String msg, Object... params) {
        return this.log(Log.INFO, msg, params);
    }

    public int debug(String msg, Object... params) {
        return this.log(Log.DEBUG, msg, params);
    }

    public int warn(String msg, Object... params) {
        return this.log(Log.WARN, msg, params);
    }

    public int error(String msg, Object... params) {
        return this.log(Log.ERROR, msg, params);
    }

    public int error(Throwable error, String msg, Object... params) {
        return Log.isLoggable("PlayCore", Log.ERROR) ? Log.e("PlayCore", logInternal(this.mTag, msg, params), error) : 0;
    }

    private int log(int logLevel, String msg, Object[] params) {
        return Log.isLoggable("PlayCore", logLevel) ? Log.i("PlayCore", logInternal(this.mTag, msg, params)) : 0;
    }

    private static String logInternal(String tag, String msg, Object... params) {
        String label = tag + " : " + msg;
        if (params == null || params.length <= 0) {
            return label;
        }
        try {
            return String.format(Locale.US, label, params);
        } catch (Throwable e) {
            String str3 = "PlayCore";
            String str4 = "Unable to format ";
            String valueOf = label;
            Log.e(str3, valueOf.length() != 0 ? str4.concat(valueOf) : str4, e);
            valueOf = label;
            label = TextUtils.join(", ", params);
            return valueOf + " [" + label + "]";
        }
    }

}
