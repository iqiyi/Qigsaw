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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface SplitInfoManager {

    /**
     * Get base app version name
     */
    String getBaseAppVersionName(Context context);

    /**
     * Get build version of qigsaw compilation
     */
    String getQigsawId(Context context);

    /**
     * @return a list of splits need to update.
     */
    List<String> getUpdateSplits(Context context);

    /**
     * get {@link SplitInfo} by split name.
     *
     * @param context
     * @param splitName name of split
     * @return {@link SplitInfo}
     */
    SplitInfo getSplitInfo(Context context, String splitName);

    /**
     * get a list of {@link SplitInfo}
     *
     * @param context
     * @param splitNames a list of split name.
     * @return a list of {@link SplitInfo}
     */
    List<SplitInfo> getSplitInfos(Context context, Collection<String> splitNames);

    /**
     * @param context get all split info
     * @return collection of {@link SplitInfo}
     */
    Collection<SplitInfo> getAllSplitInfo(Context context);

    /**
     * Create {@link SplitDetails} instance for new split info json file.
     *
     * @param newSplitInfoPath file path of new split info
     * @return details of all splits
     */
    SplitDetails createSplitDetailsForJsonFile(@NonNull String newSplitInfoPath);

    /**
     * Get split info version of current app version.
     *
     * @return version of split info
     */
    String getCurrentSplitInfoVersion();

    /**
     * Call this method if there are splits need to be updated.
     *
     * @param context
     * @param newSplitInfoVersion value of new split info version.
     * @param newSplitInfoFile    file of new split info.
     * @return result of update splits.
     */
    boolean updateSplitInfoVersion(Context context, String newSplitInfoVersion, File newSplitInfoFile);
}
