package com.iqiyi.qigsaw.sample.reporter;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

public class SampleLogger implements SplitLog.Logger {

    @Override
    public void v(final String tag, final String format, final Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        android.util.Log.v(tag, log);
    }

    @Override
    public void i(final String tag, final String format, final Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        android.util.Log.i(tag, log);
    }

    @Override
    public void d(final String tag, final String format, final Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        android.util.Log.d(tag, log);
    }

    @Override
    public void w(final String tag, final String format, final Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        android.util.Log.w(tag, log);
    }

    @Override
    public void e(final String tag, final String format, final Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        android.util.Log.e(tag, log);
    }

    @Override
    public void v(String tag, String msg, Throwable throwable) {
        android.util.Log.v(tag, msg, throwable);
    }

    @Override
    public void i(String tag, String msg, Throwable throwable) {
        android.util.Log.i(tag, msg, throwable);
    }

    @Override
    public void w(String tag, String msg, Throwable throwable) {
        android.util.Log.w(tag, msg, throwable);
    }

    @Override
    public void d(String tag, String msg, Throwable throwable) {
        android.util.Log.d(tag, msg, throwable);
    }

    @Override
    public void e(String tag, String msg, Throwable throwable) {
        android.util.Log.e(tag, msg, throwable);
    }

    @Override
    public void printErrStackTrace(String tag, Throwable tr, String format, Object... params) {
        String log = (params == null || params.length == 0) ? format : String.format(format, params);
        if (log == null) {
            log = "";
        }
        log += "  " + android.util.Log.getStackTraceString(tr);
        android.util.Log.e(tag, log);
    }
}
