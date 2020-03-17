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

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Arrays;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AbiUtil {

    private static final String armv5 = "armeabi";

    private static final String armv7 = "armeabi-v7a";

    private static final String armv8 = "arm64-v8a";

    private static final String x86 = "x86";

    private static final String x86_64 = "x86_64";

    private static List<String> abis;

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

    public static String findBasePrimaryAbi(@Nullable List<String> baseAbis) {
        List<String> supportedAbis = getSupportedAbis();
        if (baseAbis == null) {
            return supportedAbis.get(0);
        } else {
            for (String abi : supportedAbis) {
                if (baseAbis.contains(abi)) {
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
