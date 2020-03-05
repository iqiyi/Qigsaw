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

import androidx.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitInfo {

    private final String splitName;

    private final String url;

    private final String md5;

    private final String splitVersion;

    private final String appVersion;

    private final long size;

    private final boolean builtIn;

    private final boolean hasLibs;

    private final LibInfo primaryLibInfo;

    private final int dexNumber;

    private final boolean isMultiDex;

    private final int minSdkVersion;

    private final List<String> workProcesses;

    private final List<String> dependencies;

    SplitInfo(String splitName,
              String appVersion,
              String version,
              String url,
              String md5,
              long size,
              boolean builtIn,
              int minSdkVersion,
              int dexNumber,
              List<String> workProcesses,
              List<String> dependencies,
              boolean hasLibs,
              LibInfo primaryLibInfo) {
        this.splitName = splitName;
        this.appVersion = appVersion;
        this.splitVersion = version;
        this.url = url;
        this.md5 = md5;
        this.size = size;
        this.builtIn = builtIn;
        this.minSdkVersion = minSdkVersion;
        this.dexNumber = dexNumber;
        this.workProcesses = workProcesses;
        this.dependencies = dependencies;
        this.hasLibs = hasLibs;
        this.primaryLibInfo = primaryLibInfo;
        this.isMultiDex = dexNumber > 1;
    }

    public String getSplitName() {
        return splitName;
    }

    public String getUrl() {
        return url;
    }

    public String getMd5() {
        return md5;
    }

    public String getSplitVersion() {
        return splitVersion;
    }

    public long getSize() {
        return size;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public LibInfo getLibInfo() {
        if (hasLibs && primaryLibInfo == null) {
            throw new RuntimeException("No supported abi for split " + splitName);
        }
        return primaryLibInfo;
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

    public boolean hasLibs() {
        return hasLibs;
    }

    public List<String> getWorkProcesses() {
        return workProcesses;
    }

    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    boolean isValid() {
        return !TextUtils.isEmpty(url) && checkLibInfo()
                && !TextUtils.isEmpty(splitName) && !TextUtils.isEmpty(md5)
                && size > 0;
    }

    public String getAppVersion() {
        return appVersion;
    }

    private boolean checkLibInfo() {
        if (primaryLibInfo != null && primaryLibInfo.libs != null
                && !primaryLibInfo.libs.isEmpty()) {
            if (TextUtils.isEmpty(primaryLibInfo.abi)) {
                return false;
            }
            for (LibInfo.Lib lib : primaryLibInfo.libs) {
                if (TextUtils.isEmpty(lib.name)
                        || TextUtils.isEmpty(lib.md5)) {
                    return false;
                }
                if (!lib.name.startsWith("lib")
                        && !lib.name.endsWith(".so")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static class LibInfo {

        private final String abi;

        private final List<Lib> libs;

        LibInfo(String abi, List<Lib> libs) {
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
