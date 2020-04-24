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

package com.iqiyi.android.qigsaw.core.splitreport;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@Keep
public class SplitBriefInfo {

    public static final int UNKNOWN = 0;

    public static final int FIRST_INSTALLED = 1;

    public static final int ALREADY_INSTALLED = 2;

    public final String splitName;

    public final String version;

    public final boolean builtIn;

    private int installFlag = UNKNOWN;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SplitBriefInfo(@NonNull String splitName, @NonNull String version, boolean builtIn) {
        this.splitName = splitName;
        this.version = version;
        this.builtIn = builtIn;
    }

    public int getInstallFlag() {
        return installFlag;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setInstallFlag(int installFlag) {
        this.installFlag = installFlag;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof SplitBriefInfo)) {
            return false;
        }
        SplitBriefInfo briefInfo = (SplitBriefInfo) obj;
        if (splitName.equals(briefInfo.splitName)
                && version.equals(briefInfo.version)
                && (builtIn == briefInfo.builtIn)) {
            return true;
        }
        return super.equals(obj);
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
