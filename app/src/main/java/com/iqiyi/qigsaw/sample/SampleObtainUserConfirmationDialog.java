package com.iqiyi.qigsaw.sample;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iqiyi.android.qigsaw.core.ObtainUserConfirmationDialog;
import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.text.DecimalFormat;

public final class SampleObtainUserConfirmationDialog extends ObtainUserConfirmationDialog {

    private static final String TAG = "SampleObtainUserConfirmationDialog";

    private boolean fromUserClick;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkInternParametersIllegal()) {
            finish();
            return;
        }
        SplitLog.d(TAG, "Downloading splits %s need user to confirm." + getModuleNames().toString());
        setContentView(R.layout.activity_sample_obtain_user_confirmation);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setFinishOnTouchOutside(false);
        TextView descText = findViewById(R.id.sample_user_conformation_tv);
        DecimalFormat df = new DecimalFormat("#.00");
        double convert = getRealTotalBytesNeedToDownload() / (1024f * 1024f);
        descText.setText(String.format(getString(R.string.sample_prompt_desc), df.format(convert)));
        findViewById(R.id.sample_user_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fromUserClick) {
                    fromUserClick = true;
                    onUserConfirm();
                }
            }
        });
        findViewById(R.id.sample_user_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fromUserClick) {
                    fromUserClick = true;
                    onUserCancel();
                }
            }
        });
    }
}


