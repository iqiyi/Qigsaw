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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.FileUtil;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitPathManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class SplitPendingUninstallManager {

    private static final String TAG = "PendingUninstallSplitsManager";

    private static final String VERSION_DATA_NAME = "uninstallsplits.info";

    private static final String PENDING_UNINSTALL_SPLITS = "pendingUninstallSplits";

    private final File pendingUninstallSplitsFile;

    private static final Object sLock = new Object();

    public SplitPendingUninstallManager() {
        File uninstallDir = SplitPathManager.require().getUninstallSplitsDir();
        this.pendingUninstallSplitsFile = new File(uninstallDir, VERSION_DATA_NAME);
    }

    public List<String> readPendingUninstallSplits() {
        synchronized (sLock) {
            if (pendingUninstallSplitsFile.exists()) {
                return readPendingUninstallSplitsInternal(pendingUninstallSplitsFile);
            }
            return null;
        }
    }

    public boolean deletePendingUninstallSplitsRecord() {
        synchronized (sLock) {
            if (pendingUninstallSplitsFile.exists()) {
                return FileUtil.deleteFileSafely(pendingUninstallSplitsFile);
            }
            return true;
        }
    }

    boolean recordPendingUninstallSplits(@NonNull List<String> pendingUninstallSplits) {
        synchronized (sLock) {
            return recordPendingUninstallSplitsInternal(pendingUninstallSplitsFile, pendingUninstallSplits);
        }
    }

    private List<String> readPendingUninstallSplitsInternal(File pendingUninstallSplitsFile) {
        List<String> uninstallInfoList = null;
        boolean isReadPatchSuccessful = false;
        int numAttempts = 0;
        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isReadPatchSuccessful) {
            numAttempts++;
            Properties properties = new Properties();
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(pendingUninstallSplitsFile);
                properties.load(inputStream);
                String uninstallSplitsStr = properties.getProperty(PENDING_UNINSTALL_SPLITS);
                if (uninstallSplitsStr != null) {
                    String[] uninstallSplitsArray = uninstallSplitsStr.split(",");
                    uninstallInfoList = new ArrayList<>();
                    Collections.addAll(uninstallInfoList, uninstallSplitsArray);
                }
                isReadPatchSuccessful = true;
            } catch (IOException e) {
                SplitLog.w(TAG, "read property failed, e:" + e);
            } finally {
                FileUtil.closeQuietly(inputStream);
            }
        }
        return uninstallInfoList;
    }

    private boolean recordPendingUninstallSplitsInternal(File pendingUninstallSplitsFile, List<String> pendingUninstallSplits) {
        if (pendingUninstallSplitsFile == null || pendingUninstallSplits == null) {
            return false;
        }
        List<String> tempUninstallSplits = new ArrayList<>(pendingUninstallSplits);
        SplitLog.i(TAG, "recordSplitUninstallInfo file path:"
                + pendingUninstallSplitsFile.getAbsolutePath()
                + " , uninstalls splits: "
                + tempUninstallSplits.toString());

        boolean isWritePatchSuccessful = false;

        int numAttempts = 0;

        if (pendingUninstallSplitsFile.exists()) {
            List<String> oldPendingUninstallSplits = readPendingUninstallSplits();
            if (oldPendingUninstallSplits != null) {
                if (oldPendingUninstallSplits.containsAll(tempUninstallSplits)) {
                    SplitLog.i(TAG, "Splits %s have been marked to uninstall!", tempUninstallSplits.toString());
                    return true;
                }
                tempUninstallSplits.addAll(oldPendingUninstallSplits);
                HashSet<String> tempSet = new HashSet<>(tempUninstallSplits);
                tempUninstallSplits.clear();
                tempUninstallSplits.addAll(tempSet);
                SplitLog.i(TAG, "Splits which need to be uninstalled have been updated, new pending uninstall splits: " + tempUninstallSplits.toString());
            }
        }

        while (numAttempts < SplitConstants.MAX_RETRY_ATTEMPTS && !isWritePatchSuccessful) {
            numAttempts++;
            Properties newProperties = new Properties();
            newProperties.put(PENDING_UNINSTALL_SPLITS, TextUtils.join(",", tempUninstallSplits));
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(pendingUninstallSplitsFile, false);
                String comment = "splits need to be uninstalled: " + tempUninstallSplits.toString();
                newProperties.store(outputStream, comment);
            } catch (Exception e) {
                SplitLog.w(TAG, "write property failed, e:" + e);
            } finally {
                FileUtil.closeQuietly(outputStream);
            }
            List<String> tempInfo = readPendingUninstallSplits();
            isWritePatchSuccessful = tempInfo != null && tempInfo.containsAll(tempUninstallSplits);
            if (!isWritePatchSuccessful) {
                pendingUninstallSplitsFile.delete();
            }
        }
        return isWritePatchSuccessful;
    }
}
