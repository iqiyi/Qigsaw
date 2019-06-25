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

package com.iqiyi.android.qigsaw.core.splitinstall;

import android.os.Build;

import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.File;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SplitInstallerInternals {

    private static String currentInstructionSet = null;

    private SplitInstallerInternals() {

    }

    static boolean isVMMultiDexCapable(String versionString) {
        boolean isMultiDexCapable = false;
        if (versionString != null) {
            Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(versionString);
            if (matcher.matches()) {
                try {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = Integer.parseInt(matcher.group(2));
                    isMultiDexCapable = major > 2 || major == 2 && minor >= 1;
                } catch (NumberFormatException var5) {
                    //ignored
                }
            }
        }
        SplitLog.i("Split:MultiDex", "VM with version " + versionString + (isMultiDexCapable ? " has multidex support" : " does not have multidex support"));
        return isMultiDexCapable;
    }

    static String optimizedPathFor(File path, File optimizedDirectory) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String currentInstructionSet;
            try {
                currentInstructionSet = getCurrentInstructionSet();
            } catch (Exception e) {
                throw new RuntimeException("getCurrentInstructionSet fail:", e);
            }
            File parentFile = path.getParentFile();
            String fileName = path.getName();
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                fileName = fileName.substring(0, index);
            }
            return parentFile.getAbsolutePath() + "/oat/"
                    + currentInstructionSet + "/" + fileName + SplitConstants.DOT_ODEX;
        }
        String fileName = path.getName();
        if (!fileName.endsWith(SplitConstants.DOT_DEX)) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                fileName += SplitConstants.DOT_DEX;
            } else {
                StringBuilder sb = new StringBuilder(lastDot + 4);
                sb.append(fileName, 0, lastDot);
                sb.append(SplitConstants.DOT_DEX);
                fileName = sb.toString();
            }
        }
        File result = new File(optimizedDirectory, fileName);
        return result.getPath();
    }

    private static String getCurrentInstructionSet() throws Exception {
        if (currentInstructionSet != null) {
            return currentInstructionSet;
        }
        Class<?> clazz = Class.forName("dalvik.system.VMRuntime");
        Method currentGet = clazz.getDeclaredMethod("getCurrentInstructionSet");
        currentInstructionSet = (String) currentGet.invoke(null);
        return currentInstructionSet;
    }

    static boolean shouldAcceptEvenIfOptFileIllegal(File file) {
        return ("vivo".equalsIgnoreCase(Build.MANUFACTURER)
                || "oppo".equalsIgnoreCase(Build.MANUFACTURER))
                && (!file.exists() || file.length() == 0);
    }

}
