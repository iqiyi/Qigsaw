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

package com.iqiyi.qigsaw.buildtool.gradle.task

import java.util.concurrent.ConcurrentHashMap

final class SplitDependencyStatistics {

    private final ConcurrentHashMap<String, List<String>> dependenciesMap = new ConcurrentHashMap<>()

    private static SplitDependencyStatistics sInstance

    static SplitDependencyStatistics getInstance() {
        synchronized (SplitDependencyStatistics.class) {
            if (sInstance == null) {
                sInstance = new SplitDependencyStatistics()
            }
            return sInstance
        }
    }

    void putDependencies(String projectName, String variantName, List<String> dependencies) {
        String key = "${projectName}_${variantName}"
        dependenciesMap.put(key, dependencies)
    }

    List<String> getDependencies(String projectName, String variantName) {
        String key = "${projectName}_${variantName}"
        return dependenciesMap.get(key)
    }

    void clear() {
        dependenciesMap.clear()
    }
}
