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

import android.app.Application;
import android.content.pm.ProviderInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

interface AABExtensionManager {

    /**
     * Remove providers of unloaded splits
     *
     * @param unLoadSplits list of unloaded splits.
     * @return map of split's {@link ProviderInfo}.
     */
    Map<String, List<ProviderInfo>> removeSplitProviders(Set<String> unLoadSplits) throws AABExtensionException;

    /**
     * Install providers of loaded splits.
     *
     * @param providers list of split's {@link ProviderInfo}.
     */
    void installSplitProviders(List<ProviderInfo> providers) throws AABExtensionException;

    /**
     * Create {@link Application} instance for split.
     *
     * @param splitName name of split
     * @return {@link Application}
     */
    Application createApplication(String splitName) throws AABExtensionException;

    /**
     * Invoke {@link Application#attach(android.content.Context)} method to activate split's application.
     *
     * @param application application instance of split.
     */
    void activeApplication(Application application) throws AABExtensionException;

    boolean isSplitActivities(String name);

    boolean isSplitServices(String name);

    boolean isSplitReceivers(String name);

}
