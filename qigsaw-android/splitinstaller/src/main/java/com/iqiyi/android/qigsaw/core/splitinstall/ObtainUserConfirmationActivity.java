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

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.iqiyi.android.qigsaw.core.splitdownload.DownloadRequest;
import com.iqiyi.android.qigsaw.core.splitinstall.remote.SplitInstallSupervisor;

import java.text.DecimalFormat;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public class ObtainUserConfirmationActivity extends Activity {

    private boolean fromUserClick;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obtain_user_confirmation);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setFinishOnTouchOutside(false);
        final int sessionId = getIntent().getIntExtra("sessionId", 0);
        final List<DownloadRequest> downloadRequests = getIntent().getParcelableArrayListExtra("downloadRequests");
        long realTotalBytesNeedToDownload = getIntent().getLongExtra("realTotalBytesNeedToDownload", 0L);
        final SplitInstallSupervisor installService = SplitApkInstaller.getSplitInstallSupervisor(getApplicationContext());
        if (sessionId == 0 || downloadRequests == null || realTotalBytesNeedToDownload <= 0 || installService == null) {
            finish();
        } else {
            TextView descText = findViewById(R.id.user_conformation_tv);
            DecimalFormat df = new DecimalFormat("#.00");
            double convert = realTotalBytesNeedToDownload / (1024f * 1024f);
            descText.setText(String.format(getString(R.string.prompt_desc), df.format(convert)));
            findViewById(R.id.user_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fromUserClick) {
                        return;
                    }
                    fromUserClick = true;
                    installService.continueInstallWithUserConfirmation(sessionId, downloadRequests);
                    setResult(RESULT_OK);
                    finish();
                }
            });
            findViewById(R.id.user_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fromUserClick) {
                        return;
                    }
                    fromUserClick = true;
                    installService.cancelInstallWithoutUserConfirmation(sessionId);
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        }
    }
}
