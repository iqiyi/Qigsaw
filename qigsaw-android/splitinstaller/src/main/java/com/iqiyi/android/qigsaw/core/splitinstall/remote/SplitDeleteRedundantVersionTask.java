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

package com.iqiyi.android.qigsaw.core.splitinstall.remote;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

final class SplitDeleteRedundantVersionTask implements Runnable {

    private static final int MAX_SPLIT_CACHE_SIZE = 1;

    private static final String TAG = "SplitDeleteRedundantVersionTask";

    private final Collection<SplitInfo> allSplits;

    SplitDeleteRedundantVersionTask(Collection<SplitInfo> allSplits) {
        this.allSplits = allSplits;
    }

    @Override
    public void run() {
        if (allSplits != null) {
            for (SplitInfo splitInfo : allSplits) {
                File splitDir = SplitPathManager.require().getSplitDir(splitInfo);
                File splitRootDir = SplitPathManager.require().getSplitRootDir(splitInfo);
                File installedMarkFile = SplitPathManager.require().getSplitMarkFile(splitInfo);
                deleteRedundantSplitVersionDirs(splitDir, splitRootDir, installedMarkFile);
            }
        }
    }

    private void deleteRedundantSplitVersionDirs(final File currentSplitVersionDir, final File splitRootDir, final File installedMarkFile) {
        final String splitName = splitRootDir.getName();
        File[] files = splitRootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory() && !pathname.equals(currentSplitVersionDir)) {
                    SplitLog.i(TAG, "Split %s version %s has been installed!", splitName, pathname.getName());
                    return installedMarkFile.exists();
                }
                return false;
            }
        });
        if (files != null && files.length > MAX_SPLIT_CACHE_SIZE) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.lastModified() < o2.lastModified()) {
                        return 1;
                    } else if (o1.lastModified() == o2.lastModified()) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });
            for (int i = MAX_SPLIT_CACHE_SIZE; i < files.length; i++) {
                SplitLog.i(TAG, "Split %s version %s is redundant, so we try to delete it", splitName, files[i].getName());
                FileUtil.deleteDir(files[i]);
            }
        }
    }
}
