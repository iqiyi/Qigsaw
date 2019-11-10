package com.google.android.play.core.splitinstall.model;

public @interface SplitInstallErrorCode {

    int NO_ERROR = 0;
    /**
     * Too many sessions are running for current app, existing sessions must be resolved first.
     */
    int ACTIVE_SESSIONS_LIMIT_EXCEEDED = -1;
    /**
     * A requested module is not available (to this user/device, for the installed apk).
     */
    int MODULE_UNAVAILABLE = -2;
    /**
     * Request is otherwise invalid.
     */
    int INVALID_REQUEST = -3;
    /**
     * Requested session is not found.
     */
    int SESSION_NOT_FOUND = -4;
    /**
     * Split Install API is not available.
     */
    int API_NOT_AVAILABLE = -5;
    /**
     * Network error: unable to obtain split details
     */
    int NETWORK_ERROR = -6;
    /**
     * Download not permitted under current device circumstances (e.g. in background)
     */
    int ACCESS_DENIED = -7;
    /**
     * Requested session contains modules from an existing active session and also new modules.
     */
    int INCOMPATIBLE_WITH_EXISTING_SESSION = -8;
    /**
     * Service handling split install has died.
     * This error code will be sent with session id = -1 and failed session status when the service handling split install has died. Upon receiving this error code the app should retry the request.
     */
    int SERVICE_DIED = -9;
    /**
     * Unknown error processing split install.
     */
    int INTERNAL_ERROR = -100;
}
