package com.google.android.play.core.splitinstall.model;

public @interface SplitInstallSessionStatus {
    int UNKNOWN = 0;
    /**
     * The download is pending and will be processed soon.
     */
    int PENDING = 1;
    /**
     * The download requires user confirmation.
     */
    int REQUIRES_USER_CONFIRMATION = 8;
    /**
     * The split download is in progress.
     */
    int DOWNLOADING = 2;
    /**
     * The split is downloaded but not yet installed.
     */
    int DOWNLOADED = 3;
    /**
     * The splits are being installed.
     */
    int INSTALLING = 4;
    /**
     * Installation is complete; the splits are available to the client app.
     */
    int INSTALLED = 5;
    /**
     * Split download or installation has failed.
     */
    int FAILED = 6;
    /**
     * The split download is being cancelled.
     */
    int CANCELING = 9;
    /**
     * The split download has been cancelled.
     */
    int CANCELED = 7;
}
