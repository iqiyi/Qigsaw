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

import java.util.Collection;
import java.util.List;

final class SplitDetails {

    private final String qigsawId;

    private final String appVersionName;

    private final List<String> abiFilters;

    private final List<String> updateSplits;

    private final List<String> splitEntryFragments;

    private final SplitInfoListing splitInfoListing;

    SplitDetails(String qigsawId,
                 String appVersionName,
                 List<String> abiFilters,
                 List<String> updateSplits,
                 List<String> splitEntryFragments,
                 SplitInfoListing splitInfoListing) {
        this.qigsawId = qigsawId;
        this.appVersionName = appVersionName;
        this.abiFilters = abiFilters;
        this.updateSplits = updateSplits;
        this.splitEntryFragments = splitEntryFragments;
        this.splitInfoListing = splitInfoListing;
    }

    String getQigsawId() {
        return qigsawId;
    }

    String getAppVersionName() {
        return appVersionName;
    }

    List<String> getAbiFilters() {
        return abiFilters;
    }

    List<String> getUpdateSplits() {
        return updateSplits;
    }

    List<String> getSplitEntryFragments() {
        return splitEntryFragments;
    }

    SplitInfoListing getSplitInfoListing() {
        return splitInfoListing;
    }

    boolean verifySplitInfoListing() {
        if (splitInfoListing != null
                && splitInfoListing.getSplitInfoMap() != null) {
            boolean verified = true;
            Collection<SplitInfo> splits = splitInfoListing.getSplitInfoMap().values();
            for (SplitInfo splitInfo : splits) {
                if (!splitInfo.isValid()) {
                    verified = false;
                }
            }
            return verified;
        }
        return false;
    }
}
