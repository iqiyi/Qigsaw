package com.iqiyi.android.qigsaw.core.common;

import android.app.ActivityManager;
import android.content.Context;
import androidx.annotation.RestrictTo;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class ProcessUtil {

    private static final String TAG = "Split:ProcessUtil";

    public static void killAllOtherProcess(Context context) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcessList = am
                .getRunningAppProcesses();

        if (appProcessList == null) {
            return;
        }
        for (ActivityManager.RunningAppProcessInfo ai : appProcessList) {
            if (ai.uid == android.os.Process.myUid() && ai.pid != android.os.Process.myPid()) {
                android.os.Process.killProcess(ai.pid);
            }
        }
    }

    public static String getProcessName(Context context) {
        String processName = null;
        try {
            processName = getProcessNameClassical(context);
        } catch (Exception ignored) {
            //may be occur DeadSystemException
        }
        if (TextUtils.isEmpty(processName)) {
            processName = getProcessNameSecure();
            SplitLog.i(TAG, "Get process name: %s in secure mode.", processName);
        }
        return processName;
    }

    private static String getProcessNameSecure() {
        String processName = "";
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            processName = bufferedReader.readLine().trim();
            bufferedReader.close();
        } catch (Exception e) {
            //
        }
        return processName;
    }

    private static String getProcessNameClassical(Context context) {
        String processName = "";
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return processName;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcessList = manager
                .getRunningAppProcesses();
        if (appProcessList == null) {
            return processName;
        }
        for (ActivityManager.RunningAppProcessInfo process : appProcessList) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }

}
