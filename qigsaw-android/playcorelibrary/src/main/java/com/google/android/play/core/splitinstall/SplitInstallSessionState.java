package com.google.android.play.core.splitinstall;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

public class SplitInstallSessionState {

    private final List<String> moduleNames;

    private final long bytesDownloaded;

    private final long totalBytesToDownload;

    private final int errorCode;

    private final int status;

    private final int sessionId;

    private final PendingIntent userConfirmationIntent;

    List<Intent> splitFileIntents;

    final SplitInstallSessionState a(int status) {
        return new SplitInstallSessionState(this.sessionId(), status, this.errorCode(), this.bytesDownloaded(), this.totalBytesToDownload(), this.moduleNames(), this.resolutionIntent(), this.splitFileIntents);
    }

    final SplitInstallSessionState a(int status, int errorCode) {
        return new SplitInstallSessionState(this.sessionId(), status, errorCode, this.bytesDownloaded(), this.totalBytesToDownload(), this.moduleNames(), this.resolutionIntent(), this.splitFileIntents);
    }

    static SplitInstallSessionState createFrom(Bundle bundle) {
        return new SplitInstallSessionState(
                bundle.getInt("session_id"),
                bundle.getInt("status"),
                bundle.getInt("error_code"),
                bundle.getLong("bytes_downloaded"),
                bundle.getLong("total_bytes_to_download"),
                bundle.getStringArrayList("module_names"),
                (PendingIntent) bundle.getParcelable("user_confirmation_intent"),
                bundle.<Intent>getParcelableArrayList("split_file_intents"));
    }

    private SplitInstallSessionState(int sessionId,
                                     int status,
                                     int errorCode,
                                     long bytesDownloaded,
                                     long totalBytesToDownload,
                                     List<String> moduleNames,
                                     PendingIntent userConfirmationIntent,
                                     List<Intent> splitFileIntents) {
        this.sessionId = sessionId;
        this.status = status;
        this.errorCode = errorCode;
        this.bytesDownloaded = bytesDownloaded;
        this.totalBytesToDownload = totalBytesToDownload;
        this.moduleNames = moduleNames;
        this.userConfirmationIntent = userConfirmationIntent;
        this.splitFileIntents = splitFileIntents;
    }

    public List<String> moduleNames() {
        return moduleNames;
    }

    public int sessionId() {
        return sessionId;
    }

    public long bytesDownloaded() {
        return bytesDownloaded;
    }

    public long totalBytesToDownload() {
        return totalBytesToDownload;
    }

    public int errorCode() {
        return errorCode;
    }

    public int status() {
        return status;
    }

    public final PendingIntent resolutionIntent() {
        return this.userConfirmationIntent;
    }

    @Override
    public final String toString() {
        int sessionId = this.sessionId;
        int status = this.status;
        int errorCode = this.errorCode;
        long bytesDownloaded = this.bytesDownloaded;
        long totalBytesToDownload = this.totalBytesToDownload;
        String moduleNamesStr = String.valueOf(moduleNames);
        return (new StringBuilder(183 + String.valueOf(moduleNamesStr).length()))
                .append("SplitInstallSessionState{sessionId=")
                .append(sessionId)
                .append(", status=")
                .append(status)
                .append(", errorCode=")
                .append(errorCode)
                .append(", bytesDownloaded=")
                .append(bytesDownloaded)
                .append(",totalBytesToDownload=")
                .append(totalBytesToDownload)
                .append(",moduleNames=")
                .append(moduleNamesStr)
                .append("}")
                .toString();
    }
}
