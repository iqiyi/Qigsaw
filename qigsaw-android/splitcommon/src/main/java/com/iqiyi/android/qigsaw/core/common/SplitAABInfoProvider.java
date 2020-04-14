/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.android.qigsaw.core.common;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitAABInfoProvider {

    private static final String TAG = "SplitAABInfoProvider";

    private Context context;

    private final String packageName;

    public SplitAABInfoProvider(Context context) {
        this.packageName = context.getPackageName();
        this.context = context;
    }

    public Set<String> getInstalledSplitsForAAB() {
        Set<String> installedModules = getFusedModules();
        if (Build.VERSION.SDK_INT < 21) {
            return installedModules;
        }
        String[] splitNames = getSplitInstallInfo();
        if (splitNames == null) {
            SplitLog.d(TAG, "No splits are found or app cannot be found in package manager.");
            return installedModules;
        }
        String logMsg = "Split names are: ";
        String splitNamesStr = Arrays.toString(splitNames);
        SplitLog.d(TAG, splitNamesStr.length() != 0 ? logMsg.concat(splitNamesStr) : logMsg);
        for (String splitName : splitNames) {
            if (!splitName.startsWith("config.")) {
                installedModules.add(cutSplitName(splitName));
            }
        }
        return installedModules;
    }

    private Set<String> getFusedModules() {
        Set<String> fusedModules = new HashSet<>();
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (Throwable e) {
            SplitLog.printErrStackTrace(TAG, e, "App is not found in PackageManager");
            return fusedModules;
        }
        if (appInfo != null && appInfo.metaData != null) {
            String fusedName;
            if ((fusedName = appInfo.metaData.getString("shadow.bundletool.com.android.dynamic.apk.fused.modules")) != null && !fusedName.isEmpty()) {
                Collections.addAll(fusedModules, fusedName.split(",", -1));
                fusedModules.remove("");
                return fusedModules;
            } else {
                SplitLog.d(TAG, "App has no fused modules.");
                return fusedModules;
            }
        } else {
            SplitLog.d(TAG, "App has no applicationInfo or metaData");
            return fusedModules;
        }
    }

    private String cutSplitName(String splitName) {
        return splitName.split("\\.config\\.")[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String[] getSplitInstallInfo() {
        try {
            PackageInfo packageInfo;
            return (packageInfo = context.getPackageManager().getPackageInfo(packageName, 0)) != null ? packageInfo.splitNames : null;
        } catch (Throwable var2) {
            SplitLog.printErrStackTrace(TAG, var2, "App is not found in PackageManager");
            return null;
        }
    }

}
