package com.iqiyi.qigsaw.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class QigsawInstaller extends Activity {

    private static final String TAG = "QigsawInstaller";

    public static final int INSTALL_REQUEST_CODE = 10;

    private static final int USER_CONFIRMATION_REQ_CODE = 11;

    public static final String KEY_MODULE_NAMES = "moduleNames";

    private SplitInstallManager mInstallManager;

    private boolean mFirstStartup = true;

    private ArrayList<String> mModuleNames;

    private ProgressBar mProgress;

    private TextView mProgressText;

    private int mSessionId;

    private int mStatus;

    private boolean startInstallFlag;

    private final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    private SplitInstallStateUpdatedListener myListener = new SplitInstallStateUpdatedListener() {

        @Override
        public void onStateUpdate(SplitInstallSessionState state) {
            if (mModuleNames == null) {
                return;
            }
            if (state.moduleNames().containsAll(mModuleNames) && mModuleNames.containsAll(state.moduleNames())) {
                mStatus = state.status();
                switch (state.status()) {
                    case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                        onRequiresUserConfirmation(state);
                        break;
                    case SplitInstallSessionStatus.CANCELING:
                        break;
                    case SplitInstallSessionStatus.CANCELED:
                        break;
                    case SplitInstallSessionStatus.PENDING:
                        onPending(state);
                        break;
                    case SplitInstallSessionStatus.DOWNLOADING:
                        onDownloading(state);
                        break;
                    case SplitInstallSessionStatus.DOWNLOADED:
                        onDownloaded();
                        break;
                    case SplitInstallSessionStatus.INSTALLING:
                        onInstalling();
                        break;
                    case SplitInstallSessionStatus.INSTALLED:
                        onInstalled();
                        break;
                    case SplitInstallSessionStatus.FAILED:
                        onFailed();
                        break;
                    default:
                        break;
                }
            }


        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qigsaw_installer);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mInstallManager = SplitInstallManagerFactory.create(this);
        ArrayList<String> moduleNames = getIntent().getStringArrayListExtra(KEY_MODULE_NAMES);
        if (moduleNames == null || moduleNames.isEmpty()) {
            finish();
        } else {
            mModuleNames = moduleNames;
        }
        mProgress = findViewById(R.id.qigsaw_installer_progress);
        mProgressText = findViewById(R.id.qigsaw_installer_status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInstallManager.registerListener(myListener);
        if (mFirstStartup) {
            startInstall();
        }
        mFirstStartup = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInstallManager.unregisterListener(myListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USER_CONFIRMATION_REQ_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    break;
                case RESULT_CANCELED:
                    break;
            }
        }
    }

    private void startInstall() {
        if (mInstallManager.getInstalledModules().containsAll(mModuleNames)) {
            onInstalled();
            return;
        }
        SplitInstallRequest.Builder builder = SplitInstallRequest.newBuilder();
        for (String module : mModuleNames) {
            builder.addModule(module);
        }
        SplitInstallRequest request = builder.build();
        mInstallManager.startInstall(request).addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                mSessionId = integer;
                startInstallFlag = true;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                startInstallFlag = true;
                if (e instanceof SplitInstallException) {
                    int errorCode = ((SplitInstallException) e).getErrorCode();
                    switch (errorCode) {
                        case SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION:
                            handleInstallRequestError(getString(R.string.installer_error_incompatible_with_existing_session), false);
                            break;
                        case SplitInstallErrorCode.SERVICE_DIED:
                            handleInstallRequestError(getString(R.string.installer_error_service_died));
                            break;
                        case SplitInstallErrorCode.NETWORK_ERROR:
                            handleInstallRequestError(getString(R.string.installer_error_network_error));
                            break;
                        case SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED:
                            checkForActiveDownloads();
                            break;
                        case SplitInstallErrorCode.INTERNAL_ERROR:
                            handleInstallRequestError(getString(R.string.installer_error_internal_error));
                            break;
                        case SplitInstallErrorCode.SESSION_NOT_FOUND:
                            //ignored
                            break;
                        case SplitInstallErrorCode.INVALID_REQUEST:
                            handleInstallRequestError(getString(R.string.installer_error_invalid_request));
                            break;
                        case SplitInstallErrorCode.API_NOT_AVAILABLE:
                            break;
                        case SplitInstallErrorCode.MODULE_UNAVAILABLE:
                            handleInstallRequestError(getString(R.string.installer_error_module_unavailable));
                            break;
                        case SplitInstallErrorCode.ACCESS_DENIED:
                            handleInstallRequestError(getString(R.string.installer_error_access_denied));
                            break;
                        default:
                            break;
                    }
                    onFailed();
                }
            }
        });
    }


    private void handleInstallRequestError(String msg) {
        handleInstallRequestError(msg, true);
    }

    private void handleInstallRequestError(String msg, boolean finish) {
        if (!TextUtils.isEmpty(msg)) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
        if (finish) {
            updateProgressMessage(msg);
            finish();
        }
    }

    void checkForActiveDownloads() {
        mInstallManager
                // Returns a SplitInstallSessionState object for each active session as a List.
                .getSessionStates()
                .addOnCompleteListener(new OnCompleteListener<List<SplitInstallSessionState>>() {
                    @Override
                    public void onComplete(Task<List<SplitInstallSessionState>> task) {
                        if (task.isSuccessful()) {
                            for (SplitInstallSessionState state : task.getResult()) {
                                if (state.status() == SplitInstallSessionStatus.DOWNLOADING) {
                                    mInstallManager.cancelInstall(state.sessionId()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(Task<Void> task) {
                                            startInstall();
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
    }

    private void onRequiresUserConfirmation(SplitInstallSessionState state) {
        try {
            startIntentSenderForResult(state.resolutionIntent().getIntentSender(), USER_CONFIRMATION_REQ_CODE, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    private void onPending(SplitInstallSessionState state) {
        updateProgressMessage(getString(R.string.installer_pending));
        mProgress.setMax(Long.valueOf(state.totalBytesToDownload()).intValue());
    }

    @SuppressLint("StringFormatInvalid")
    private void onDownloading(SplitInstallSessionState state) {
        mProgress.setProgress(Long.valueOf(state.bytesDownloaded()).intValue());
        double progress = (double) (state.bytesDownloaded() / state.totalBytesToDownload());
        updateProgressMessage(getString(R.string.installer_downloading) + decimalFormat.format(progress) + "%");
    }

    private void onDownloaded() {
        updateProgressMessage(getString(R.string.installer_downloaded));
    }

    private void onInstalling() {
        updateProgressMessage(getString(R.string.installer_installing));
    }

    private void onInstalled() {
        updateProgressMessage(getString(R.string.installer_installed));
        onInstallOK();
        Intent data = new Intent();
        data.putExtra(KEY_MODULE_NAMES, mModuleNames);
        setResult(RESULT_OK, data);
        finish();
    }

    private void onFailed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void updateProgressMessage(String message) {
        mProgressText.setText(message);
    }

    protected void onInstallOK() {

    }

    @Override
    public void onBackPressed() {
        if (mStatus == SplitInstallSessionStatus.UNKNOWN) {
            Log.d(TAG, "Split download is not started!");
            super.onBackPressed();
            return;
        }
        if (mStatus == SplitInstallSessionStatus.CANCELING
                || mStatus == SplitInstallSessionStatus.DOWNLOADED
                || mStatus == SplitInstallSessionStatus.INSTALLING) {
            // ignore back on these status
            return;
        }
        if (!startInstallFlag) {
            return;
        }
        if (mSessionId != 0) {
            mInstallManager.cancelInstall(mSessionId).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Cancel task successfully, session id :" + mSessionId);
                    if (!isFinishing()) {
                        finish();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.d(TAG, "Cancel task failed, session id :" + mSessionId);
                    if (!isFinishing()) {
                        finish();
                    }
                }
            });
        } else {
            super.onBackPressed();
        }
    }
}
