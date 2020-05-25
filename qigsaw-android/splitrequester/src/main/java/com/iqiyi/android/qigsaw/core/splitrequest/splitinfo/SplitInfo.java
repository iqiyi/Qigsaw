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

package com.iqiyi.android.qigsaw.core.splitrequest.splitinfo;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import android.content.Context;

import com.iqiyi.android.qigsaw.core.common.AbiUtil;
import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitInfo {

    private final String splitName;

    private final String splitVersion;

    private final String appVersion;

    private final boolean builtIn;

    private final int dexNumber;

    private final boolean isMultiDex;

    private final int minSdkVersion;

    private final List<String> workProcesses;

    private final List<String> dependencies;

    private final List<SplitInfo.ApkData> apkDataList;

    private final List<SplitInfo.LibData> libDataList;

    private ApkData primaryApkData;

    private LibData primaryLibData;

    SplitInfo(String splitName,
              String appVersion,
              String version,
              boolean builtIn,
              int minSdkVersion,
              int dexNumber,
              List<String> workProcesses,
              List<String> dependencies,
              List<SplitInfo.ApkData> apkDataList,
              List<SplitInfo.LibData> libDataList) {
        this.splitName = splitName;
        this.appVersion = appVersion;
        this.splitVersion = version;
        this.builtIn = builtIn;
        this.minSdkVersion = minSdkVersion;
        this.isMultiDex = dexNumber > 1;
        this.dexNumber = dexNumber;
        this.workProcesses = workProcesses;
        this.dependencies = dependencies;
        this.apkDataList = apkDataList;
        this.libDataList = libDataList;
    }

    public String getSplitName() {
        return splitName;
    }

    public ApkData getPrimaryApkData(Context context) throws IOException {
        if (primaryApkData != null) {
            return primaryApkData;
        }
        String baseAbi = AbiUtil.getBasePrimaryAbi(context);
        if (builtIn) {
            if (libDataList == null) {
                String primaryBuiltInPath = SplitConstants.QIGSAW + "/" + splitName + "-" + SplitConstants.NONE_ABI + SplitConstants.DOT_ZIP;
                InputStream is = context.getAssets().open(primaryBuiltInPath);
                FileUtil.closeQuietly(is);
                primaryApkData = apkDataList.get(0);
            } else {
                for (SplitInfo.ApkData apkData : apkDataList) {
                    if (apkData.abi.contains(baseAbi)) {
                        String primaryBuiltInPath = SplitConstants.QIGSAW + "/" + splitName + "-" + apkData.abi + SplitConstants.DOT_ZIP;
                        try {
                            InputStream is = context.getAssets().open(primaryBuiltInPath);
                            FileUtil.closeQuietly(is);
                            primaryApkData = apkData;
                            break;
                        } catch (IOException ignored) {

                        }
                    }
                }
            }
            if (primaryApkData == null) {
                throw new IOException("Failed to find primary apk data for built-in split " + splitName);
            }
        } else {
            for (SplitInfo.ApkData apkData : apkDataList) {
                if (apkData.abi.equals(baseAbi) || SplitConstants.NONE_ABI.equals(apkData.abi)) {
                    primaryApkData = apkData;
                    break;
                }
            }
            if (primaryApkData == null) {
                throw new IOException("Failed to find primary apk data for remote split " + splitName);
            }
        }
        return primaryApkData;
    }

    public String getSplitVersion() {
        return splitVersion;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    @Nullable
    public LibData getPrimaryLibData(Context context) throws IOException {
        if (primaryLibData != null) {
            return primaryLibData;
        }
        String baseAbi = AbiUtil.getBasePrimaryAbi(context);
        if (libDataList == null) {
            return null;
        }
        List<String> splitABIs = new ArrayList<>();
        for (SplitInfo.LibData libData : libDataList) {
            splitABIs.add(libData.abi);
        }
        String splitPrimaryAbi = AbiUtil.findSplitPrimaryAbi(baseAbi, splitABIs);
        if (splitPrimaryAbi == null) {
            throw new IOException("No supported abi for split " + splitName);
        }
        for (SplitInfo.LibData libData : libDataList) {
            if (libData.abi.equals(splitPrimaryAbi)) {
                primaryLibData = libData;
                break;
            }
        }
        return primaryLibData;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isMultiDex() {
        return isMultiDex;
    }

    public boolean hasDex() {
        return dexNumber > 0;
    }

    public List<String> getWorkProcesses() {
        return workProcesses;
    }

    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public static class ApkData {

        private String abi;

        private String url;

        private String md5;

        private long size;

        ApkData(String abi, String url, String md5, long size) {
            this.abi = abi;
            this.url = url;
            this.md5 = md5;
            this.size = size;
        }

        public String getAbi() {
            return abi;
        }

        public String getUrl() {
            return url;
        }

        public String getMd5() {
            return md5;
        }

        public long getSize() {
            return size;
        }
    }

    public static class LibData {

        private final String abi;

        private final List<Lib> libs;

        LibData(String abi, List<Lib> libs) {
            this.abi = abi;
            this.libs = libs;
        }

        public String getAbi() {
            return abi;
        }

        public List<Lib> getLibs() {
            return libs;
        }

        public static class Lib {

            private final String name;

            private final String md5;

            private final long size;

            Lib(String name, String md5, long size) {
                this.name = name;
                this.md5 = md5;
                this.size = size;
            }

            public String getName() {
                return name;
            }

            public String getMd5() {
                return md5;
            }

            public long getSize() {
                return size;
            }
        }
    }
}
