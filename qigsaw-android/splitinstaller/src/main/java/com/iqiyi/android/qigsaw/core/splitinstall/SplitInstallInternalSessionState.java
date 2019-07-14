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

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.iqiyi.android.qigsaw.core.splitdownload.DownloadRequest;
import com.iqiyi.android.qigsaw.core.splitrequest.splitinfo.SplitInfo;

import java.util.ArrayList;
import java.util.List;

final class SplitInstallInternalSessionState {

    private final List<String> moduleNames;

    private long bytesDownloaded;

    private long totalBytesToDownload;

    private int errorCode;

    private int status;

    private int sessionId;

    private PendingIntent userConfirmationIntent;

    private List<Intent> splitFileIntents;

    final List<SplitInfo> needInstalledSplits;

    final List<DownloadRequest> downloadRequests;

    SplitInstallInternalSessionState(int sessionId,
                                     List<String> moduleNames,
                                     List<SplitInfo> needInstalledSplits,
                                     List<DownloadRequest> downloadRequests) {
        this.sessionId = sessionId;
        this.moduleNames = moduleNames;
        this.needInstalledSplits = needInstalledSplits;
        this.downloadRequests = downloadRequests;
    }

    List<String> moduleNames() {
        return moduleNames;
    }

    void setBytesDownloaded(long bytesDownloaded) {
        if (this.bytesDownloaded != bytesDownloaded) {
            this.bytesDownloaded = bytesDownloaded;
        }
    }

    void setTotalBytesToDownload(long totalBytesToDownload) {
        this.totalBytesToDownload = totalBytesToDownload;
    }

    void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    int status() {
        return status;
    }

    void setStatus(int status) {
        if (this.status != status) {
            this.status = status;
        }
    }

    int sessionId() {
        return sessionId;
    }

    void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    void setUserConfirmationIntent(PendingIntent userConfirmationIntent) {
        this.userConfirmationIntent = userConfirmationIntent;
    }

    void setSplitFileIntents(List<Intent> splitFileIntents) {
        this.splitFileIntents = splitFileIntents;
    }

    static Bundle transform2Bundle(SplitInstallInternalSessionState sessionState) {
        Bundle bundle;
        (bundle = new Bundle()).putInt("session_id", sessionState.sessionId());
        bundle.putInt("status", sessionState.status());
        bundle.putInt("error_code", sessionState.errorCode);
        bundle.putLong("total_bytes_to_download", sessionState.totalBytesToDownload);
        bundle.putLong("bytes_downloaded", sessionState.bytesDownloaded);
        bundle.putStringArrayList("module_names", (ArrayList<String>) sessionState.moduleNames());
        bundle.putParcelable("user_confirmation_intent", sessionState.userConfirmationIntent);
        bundle.putParcelableArrayList("split_file_intents", (ArrayList<? extends Parcelable>) sessionState.splitFileIntents);
        return bundle;
    }

}
