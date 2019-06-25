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

package com.iqiyi.android.qigsaw.core.splitinstall;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManager;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfoManagerService;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitCleanService extends IntentService {

    private static final String TAG = "SplitCleanService";

    private static final int MAX_SPLIT_CACHE_SIZE = 1;

    public SplitCleanService() {
        super("qigsaw_split_clean");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            doClean();
        } catch (Exception e) {
            //ignored
        }
    }

    private void doClean() {
        SplitPathManager.require().clearCache();
        SplitInfoManager infoManager = SplitInfoManagerService.getInstance();
        if (infoManager != null) {
            Collection<SplitInfo> splitInfoList = infoManager.getAllSplitInfo(this);
            if (splitInfoList != null) {
                for (SplitInfo splitInfo : splitInfoList) {
                    File splitDir = SplitPathManager.require().getSplitDir(splitInfo);
                    File splitRootDir = SplitPathManager.require().getSplitRootDir(splitInfo);
                    deleteRedundantSplitVersionDirs(splitDir, splitRootDir);
                }
            }
        }
    }

    private void deleteRedundantSplitVersionDirs(final File currentSplitVersionDir, File splitRootDir) {
        final String splitName = splitRootDir.getName();
        File[] files = splitRootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory() && !pathname.equals(currentSplitVersionDir)) {
                    File markFile = new File(pathname, String.valueOf(splitName.hashCode()));
                    SplitLog.i(TAG, "Split %s version %s has been installed!", splitName, pathname.getName());
                    return markFile.exists();
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
                SplitLog.i(TAG, "Split %s version %s is redundant, so wen try to delete it", splitName, files[i].getName());
                FileUtil.deleteDir(files[i]);
            }
        }
    }
}
