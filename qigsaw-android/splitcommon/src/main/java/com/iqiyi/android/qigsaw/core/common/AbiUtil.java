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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AbiUtil {

    private static final String TAG = "Split:AbiUtil";

    private static final String armv5 = "armeabi";

    private static final String armv7 = "armeabi-v7a";

    private static final String armv8 = "arm64-v8a";

    private static final String x86 = "x86";

    private static final String x86_64 = "x86_64";

    private static List<String> abis;

    private static AtomicReference<String> basePrimaryAbi = new AtomicReference<>();

    private static AtomicReference<String> currentInstructionSet = new AtomicReference<>();

    private static List<String> getSupportedAbis() {
        if (abis != null) {
            return abis;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abis = Arrays.asList(Build.SUPPORTED_ABIS);
        } else {
            abis = Arrays.asList(Build.CPU_ABI, Build.CPU_ABI2);
        }
        return abis;
    }

    @SuppressLint("DiscouragedPrivateApi")
    private static String getCurrentInstructionSet() {
        if (!TextUtils.isEmpty(currentInstructionSet.get())) {
            return currentInstructionSet.get();
        } else {
            try {
                Class<?> clazz = Class.forName("dalvik.system.VMRuntime");
                Method currentGet = clazz.getDeclaredMethod("getCurrentInstructionSet");
                currentGet.setAccessible(true);
                currentInstructionSet.compareAndSet(null, (String) currentGet.invoke(null));
            } catch (Throwable ignored) {

            }
            return currentInstructionSet.get();
        }
    }

    private static String findPrimaryAbiFromCurrentInstructionSet(String currentInstructionSet) {
        if (TextUtils.isEmpty(currentInstructionSet)) {
            return null;
        }
        switch (currentInstructionSet) {
            case x86:
                return x86;
            case x86_64:
                return x86_64;
            case "arm64":
                return armv8;
            default:
                return null;
        }
    }

    private static String findPrimaryAbiFromProperties(Context context) {
        try {
            InputStream is = context.getAssets().open("base.app.cpu.abilist.properties");
            Properties properties = new Properties();
            properties.load(is);
            String abis = properties.getProperty("abiList");
            if (!TextUtils.isEmpty(abis)) {
                String[] abiArray = abis.split(",");
                Set<String> abiList = new HashSet<>();
                Collections.addAll(abiList, abiArray);
                if (!abiList.isEmpty()) {
                    Set<String> sortedAbis = sortAbis(abiList);
                    return findBasePrimaryAbi(sortedAbis);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String findPrimaryAbiFromBaseApk(Context context) {
        String baseApk = context.getApplicationInfo().sourceDir;
        ZipFile zipFile = null;
        Set<String> apkSupportedAbis = new HashSet<>();
        try {
            zipFile = new ZipFile(baseApk);
            Enumeration e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String entryName = entry.getName();
                if (entryName.charAt(0) < 'l') {
                    continue;
                }
                if (entryName.charAt(0) > 'l') {
                    continue;
                }
                if (!entryName.startsWith("lib/")) {
                    continue;
                }
                if (!entryName.endsWith(SplitConstants.DOT_SO)) {
                    continue;
                }
                String[] abiDirNames = entryName.split("/");
                if (abiDirNames.length == 3) {
                    apkSupportedAbis.add(abiDirNames[1]);
                }
            }
        } catch (IOException e) {
            SplitLog.w(TAG, "Failed to open base apk " + baseApk, e);
        } finally {
            if (zipFile != null) {
                FileUtil.closeQuietly(zipFile);
            }
        }
        Set<String> sortedAbis = sortAbis(apkSupportedAbis);
        SplitLog.i(TAG, "sorted abis: " + sortedAbis);
        return findBasePrimaryAbi(sortedAbis);
    }

    private static Set<String> sortAbis(Set<String> abis) {
        if (abis.isEmpty() || abis.size() == 1) {
            return abis;
        }
        Set<String> ret = new HashSet<>(abis.size());
        if (abis.contains(armv8)) {
            ret.add(armv8);
        }
        if (abis.contains(armv7)) {
            ret.add(armv7);
        }
        if (abis.contains(armv5)) {
            ret.add(armv5);
        }
        if (abis.contains(x86)) {
            ret.add(x86);
        }
        if (abis.contains(x86_64)) {
            ret.add(x86_64);
        }
        return ret;
    }

    public static String getBasePrimaryAbi(@NonNull Context context) {
        if (!TextUtils.isEmpty(basePrimaryAbi.get())) {
            return basePrimaryAbi.get();
        }
        synchronized (AbiUtil.class) {
            ApplicationInfo info = context.getApplicationInfo();
            try {
                Field primaryCpuAbi_Field = ApplicationInfo.class.getField("primaryCpuAbi");
                primaryCpuAbi_Field.setAccessible(true);
                basePrimaryAbi.compareAndSet(null, (String) primaryCpuAbi_Field.get(info));
                SplitLog.i(TAG, "Succeed to get primaryCpuAbi %s from ApplicationInfo.", basePrimaryAbi);
            } catch (Throwable e) {
                SplitLog.w(TAG, "Failed to get primaryCpuAbi from ApplicationInfo.", e);
            }
            if (TextUtils.isEmpty(basePrimaryAbi.get())) {
                String currentInstructionSet = getCurrentInstructionSet();
                basePrimaryAbi.compareAndSet(null, findPrimaryAbiFromCurrentInstructionSet(currentInstructionSet));
                if (TextUtils.isEmpty(basePrimaryAbi.get())) {
                    basePrimaryAbi.compareAndSet(null, findPrimaryAbiFromProperties(context));
                    if (TextUtils.isEmpty(basePrimaryAbi.get())) {
                        basePrimaryAbi.compareAndSet(null, findPrimaryAbiFromBaseApk(context));
                        SplitLog.i(TAG, "Succeed to get primaryCpuAbi %s from BaseApk.", basePrimaryAbi);
                    } else {
                        SplitLog.i(TAG, "Succeed to get primaryCpuAbi %s from Properties.", basePrimaryAbi);
                    }
                } else {
                    SplitLog.i(TAG, "Succeed to get primaryCpuAbi %s from CurrentInstructionSet.", basePrimaryAbi);
                }
            }
            return basePrimaryAbi.get();
        }
    }

    public static String findBasePrimaryAbi(Collection<String> sortedAbis) {
        List<String> supportedAbis = getSupportedAbis();
        if (sortedAbis == null || sortedAbis.isEmpty()) {
            return supportedAbis.get(0);
        } else {
            for (String abi : supportedAbis) {
                if (sortedAbis.contains(abi)) {
                    return abi;
                }
            }
        }
        throw new RuntimeException("No supported abi for this device.");
    }

    public static String findSplitPrimaryAbi(@NonNull String basePrimaryAbi, @NonNull List<String> splitAbis) {
        if (splitAbis.contains(basePrimaryAbi)) {
            return basePrimaryAbi;
        }
        if (basePrimaryAbi.contains(armv8)) {
            return splitAbis.contains(armv8) ? armv8 : null;
        } else if (basePrimaryAbi.contains(x86_64)) {
            return splitAbis.contains(x86_64) ? x86_64 : null;
        } else if (basePrimaryAbi.contains(x86)) {
            if (splitAbis.contains(x86)) {
                return x86;
            }
            if (splitAbis.contains(armv5)) {
                return armv5;
            }
        } else if (basePrimaryAbi.contains(armv7)) {
            if (splitAbis.contains(armv7)) {
                return armv7;
            }
            if (splitAbis.contains(armv5)) {
                return armv5;
            }
        } else if (basePrimaryAbi.contains(armv5)) {
            if (splitAbis.contains(armv5)) {
                return armv5;
            }
            List<String> supportedAbis = getSupportedAbis();
            if (supportedAbis.contains(armv7)) {
                if (splitAbis.contains(armv7)) {
                    return armv7;
                }
            }
        }
        return null;
    }
}
