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

package com.iqiyi.android.qigsaw.core;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.iqiyi.android.qigsaw.core.splitinstall.SplitApkInstaller;
import com.iqiyi.android.qigsaw.core.splitinstall.remote.SplitInstallSupervisor;

import java.util.List;

public abstract class ObtainUserConfirmationDialog extends Activity {

    private int sessionId;

    private long realTotalBytesNeedToDownload;

    private List<String> moduleNames;

    private SplitInstallSupervisor installService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //parse intent parameters.
        sessionId = getIntent().getIntExtra("sessionId", 0);
        realTotalBytesNeedToDownload = getIntent().getLongExtra("realTotalBytesNeedToDownload", 0L);
        moduleNames = getIntent().getStringArrayListExtra("moduleNames");
        installService = SplitApkInstaller.getSplitInstallSupervisor();
    }

    protected void onUserConfirm() {
        if (installService != null) {
            if (installService.continueInstallWithUserConfirmation(sessionId)) {
                setResult(RESULT_OK);
            }
            finish();
        }
    }

    protected void onUserCancel() {
        if (installService != null) {
            if (installService.cancelInstallWithoutUserConfirmation(sessionId)) {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    }

    protected List<String> getModuleNames() {
        return moduleNames;
    }

    protected long getRealTotalBytesNeedToDownload() {
        return realTotalBytesNeedToDownload;
    }

    protected boolean checkInternParametersIllegal() {
        return sessionId == 0 || realTotalBytesNeedToDownload <= 0
                || moduleNames == null || moduleNames.isEmpty();
    }
}
