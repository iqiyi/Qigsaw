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

package com.iqiyi.android.qigsaw.core.extension;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SplitComponentInfoProvider {

    private final Set<String> splitNames;

    private final Map<String, List<String>> splitComponentNameMap = new HashMap<>();

    SplitComponentInfoProvider(@NonNull Set<String> splitNames) {
        this.splitNames = splitNames;
    }

    /**
     * Gets split's application name by split name.
     * Qigsaw-Gradle-Plugin would write split application name in Class ComponentInfo.
     *
     * @param splitName name of split.
     * @return application name of split.
     */
    String getSplitApplicationName(String splitName) {
        return ComponentInfoManager.getSplitApplication(splitName);
    }

    @Nullable
    String getSplitNameForActivity(String className) {
        String targetSplitName = null;
        for (String splitName : splitNames) {
            String key = splitName + "_activity";
            List<String> result = splitComponentNameMap.get(key);
            if (result == null) {
                String[] activities = ComponentInfoManager.getSplitActivities(splitName);
                if (activities != null && activities.length > 0) {
                    result = Arrays.asList(activities);
                    splitComponentNameMap.put(key, result);
                } else {
                    splitComponentNameMap.put(key, Collections.<String>emptyList());
                }
            }
            if (result != null && !result.isEmpty()) {
                if (result.contains(className)) {
                    targetSplitName = splitName;
                }
            }
        }
        return targetSplitName;
    }

    @Nullable
    String getSplitNameForService(String className) {
        String targetSplitName = null;
        for (String splitName : splitNames) {
            String key = splitName + "_service";
            List<String> result = splitComponentNameMap.get(key);
            if (result == null) {
                String[] services = ComponentInfoManager.getSplitServices(splitName);
                if (services != null && services.length > 0) {
                    result = Arrays.asList(services);
                    splitComponentNameMap.put(key, result);
                } else {
                    splitComponentNameMap.put(key, Collections.<String>emptyList());
                }
            }
            if (result != null && !result.isEmpty()) {
                if (result.contains(className)) {
                    targetSplitName = splitName;
                }
            }
        }
        return targetSplitName;
    }

    @Nullable
    String getSplitNameForReceiver(String className) {
        String targetSplitName = null;
        for (String splitName : splitNames) {
            String key = splitName + "_receiver";
            List<String> result = splitComponentNameMap.get(key);
            if (result == null) {
                String[] receivers = ComponentInfoManager.getSplitReceivers(splitName);
                if (receivers != null && receivers.length > 0) {
                    result = Arrays.asList(receivers);
                    splitComponentNameMap.put(key, result);
                } else {
                    splitComponentNameMap.put(key, Collections.<String>emptyList());
                }
            }
            if (result != null && !result.isEmpty()) {
                if (result.contains(className)) {
                    targetSplitName = splitName;
                }
            }

        }
        return targetSplitName;
    }
}
