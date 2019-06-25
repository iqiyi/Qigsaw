package com.iqiyi.android.qigsaw.core.common;

public class SplitLog {

    private static final String TAG = "Split.SplitLog";

    private SplitLog() {

    }

    private static Logger defaultLogger = new Logger() {

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
    };
    private static Logger splitLogImp = defaultLogger;

    public static void setSplitLogImp(Logger imp) {
        splitLogImp = imp;
    }

    public static Logger getImpl() {
        return splitLogImp;
    }

    public static void v(final String tag, final String msg, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.v(tag, msg, obj);
        }
    }

    public static void v(final String tag, final String msg, final Throwable error) {
        if (splitLogImp != null) {
            splitLogImp.v(tag, msg, error);
        }
    }

    public static void e(final String tag, final String msg, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.e(tag, msg, obj);
        }
    }

    public static void e(final String tag, final String msg, final Throwable error) {
        if (splitLogImp != null) {
            splitLogImp.e(tag, msg, error);
        }
    }

    public static void w(final String tag, final String msg, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.w(tag, msg, obj);
        }
    }

    public static void w(final String tag, final String msg, final Throwable error) {
        if (splitLogImp != null) {
            splitLogImp.w(tag, msg, error);
        }
    }

    public static void i(final String tag, final String msg, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.i(tag, msg, obj);
        }
    }

    public static void i(final String tag, final String msg, final Throwable error) {
        if (splitLogImp != null) {
            splitLogImp.i(tag, msg, error);
        }
    }

    public static void d(final String tag, final String msg, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.d(tag, msg, obj);
        }
    }

    public static void d(final String tag, final String msg, final Throwable error) {
        if (splitLogImp != null) {
            splitLogImp.d(tag, msg, error);
        }
    }

    public static void printErrStackTrace(String tag, Throwable tr, final String format, final Object... obj) {
        if (splitLogImp != null) {
            splitLogImp.printErrStackTrace(tag, tr, format, obj);
        }
    }


    public interface Logger {

        void v(final String tag, final String msg, final Object... obj);

        void i(final String tag, final String msg, final Object... obj);

        void w(final String tag, final String msg, final Object... obj);

        void d(final String tag, final String msg, final Object... obj);

        void e(final String tag, final String msg, final Object... obj);

        void v(final String tag, final String msg, final Throwable throwable);

        void i(final String tag, final String msg, final Throwable throwable);

        void w(final String tag, final String msg, final Throwable throwable);

        void d(final String tag, final String msg, final Throwable throwable);

        void e(final String tag, final String msg, final Throwable throwable);

        void printErrStackTrace(String tag, Throwable tr, final String format, final Object... obj);

    }
}
