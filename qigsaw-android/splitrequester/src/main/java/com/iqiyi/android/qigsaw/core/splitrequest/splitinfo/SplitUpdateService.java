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

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.text.TextUtils;

import com.iqiyi.android.qigsaw.core.common.SplitBaseInfoProvider;
import com.iqiyi.android.qigsaw.core.common.SplitConstants;
import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateErrorCode;
import com.iqiyi.android.qigsaw.core.splitreport.SplitUpdateReporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class SplitUpdateService extends IntentService {

    private static final String TAG = "SplitUpdateService";

    public SplitUpdateService() {
        super("qigsaw_split_update");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            SplitLog.w(TAG, "SplitUpdateService receiver null intent!");
            return;
        }
        SplitInfoManager manager = SplitInfoManagerService.getInstance();
        if (manager == null) {
            SplitLog.w(TAG, "SplitInfoManager has not been created!");
            return;
        }
        Collection<SplitInfo> currentSplits = manager.getAllSplitInfo(this);
        if (currentSplits == null) {
            SplitLog.w(TAG, "Failed to get splits info of current split-info version!");
            return;
        }
        //parse split-info version.
        String newSplitInfoVersion = intent.getStringExtra(SplitConstants.NEW_SPLIT_INFO_VERSION);
        //parse split-info file path.
        String newSplitInfoPath = intent.getStringExtra(SplitConstants.NEW_SPLIT_INFO_PATH);
        String oldSplitInfoVersion = manager.getCurrentSplitInfoVersion();
        //parse registered class name of receiver.
        if (TextUtils.isEmpty(newSplitInfoVersion)) {
            SplitLog.w(TAG, "New split-info version null");
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_VERSION_NULL);
            return;
        }

        if (TextUtils.isEmpty(newSplitInfoPath)) {
            SplitLog.w(TAG, "New split-info path null");
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_PATH_NULL);
            return;
        }
        File newSplitInfoFile = new File(newSplitInfoPath);
        if (!newSplitInfoFile.exists() || !newSplitInfoFile.canWrite()) {
            SplitLog.w(TAG, "New split-info file %s is invalid", newSplitInfoPath);
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_FILE_INVALID);
            return;
        }

        if (newSplitInfoVersion.equals(manager.getCurrentSplitInfoVersion())) {
            SplitLog.w(TAG, "New split-info version %s is equals to current version!", newSplitInfoVersion);
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_VERSION_EXISTED);
            return;
        }
        SplitDetails splitDetails = manager.createSplitDetailsForJsonFile(newSplitInfoPath);
        if (splitDetails == null || !splitDetails.verifySplitInfoListing()) {
            SplitLog.w(TAG, "Failed to parse SplitDetails for new split info file!");
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_INVALID);
            return;
        }
        String qigsawId = splitDetails.getQigsawId();
        if (TextUtils.isEmpty(qigsawId) || !qigsawId.equals(SplitBaseInfoProvider.getQigsawId())) {
            SplitLog.w(TAG, "New qigsaw-id is not equal to current app, so we could't update splits!");
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_QIGSAW_ID_MISMATCH);
            return;
        }
        ArrayList<String> updateSplits = (ArrayList<String>) splitDetails.getUpdateSplits();
        if (updateSplits == null || updateSplits.isEmpty()) {
            SplitLog.w(TAG, "There are no splits need to be updated!");
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.ERROR_SPLIT_INFO_NOT_CHANGED);
            return;
        }
        SplitLog.w(TAG, "Success to check update request, updatedSplitInfoPath: %s, updatedSplitInfoVersion: %s", newSplitInfoPath, newSplitInfoVersion);
        if (manager.updateSplitInfoVersion(getApplicationContext(), newSplitInfoVersion, newSplitInfoFile)) {
            onUpdateOK(oldSplitInfoVersion, newSplitInfoVersion, updateSplits);
        } else {
            onUpdateError(oldSplitInfoVersion, newSplitInfoVersion, SplitUpdateErrorCode.INTERNAL_ERROR);
        }
    }

    private void onUpdateOK(String oldSplitInfoVersion, String newSplitInfoVersion, List<String> updateSplits) {
        SplitUpdateReporter updateReporter = SplitUpdateReporterManager.getUpdateReporter();
        if (updateReporter != null) {
            updateReporter.onUpdateOK(oldSplitInfoVersion, newSplitInfoVersion, updateSplits);
        }
    }

    private void onUpdateError(String oldSplitInfoVersion, String newSplitInfoVersion, int errorCode) {
        SplitUpdateReporter updateReporter = SplitUpdateReporterManager.getUpdateReporter();
        if (updateReporter != null) {
            updateReporter.onUpdateFailed(oldSplitInfoVersion, newSplitInfoVersion, errorCode);
        }
    }
}
