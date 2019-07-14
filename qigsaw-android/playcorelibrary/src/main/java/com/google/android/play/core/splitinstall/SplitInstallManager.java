package com.google.android.play.core.splitinstall;

import android.app.Activity;
import android.content.IntentSender;

import com.google.android.play.core.tasks.Task;

import java.util.List;
import java.util.Set;

public interface SplitInstallManager {

    /**
     * Registers a listener that will be alerted of state changes for creteSplitInstallService sessions for this app.
     */
    void registerListener(SplitInstallStateUpdatedListener listener);

    /**
     * Unregisters a listener previously added using
     */
    void unregisterListener(SplitInstallStateUpdatedListener listener);

    /**
     * Initiates a split creteSplitInstallService request.
     */
    Task<Integer> startInstall(SplitInstallRequest request);

    /**
     * Starts the user confirmation dialog from the provided {@link com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus}.
     */
    boolean startConfirmationDialogForResult(SplitInstallSessionState sessionState, Activity activity, int requestCode) throws IntentSender.SendIntentException;

    /**
     * Starts a request to cancel a pending split creteSplitInstallService.
     */
    Task<Void> cancelInstall(int sessionId);

    /**
     * Gets session state (returning the state through the listener registered with
     */
    Task<SplitInstallSessionState> getSessionState(int sessionId);

    /**
     * Returns state for all active sessions belonging to the app.
     */
    Task<List<SplitInstallSessionState>> getSessionStates();

    /**
     * Defers installation of given moduleNames.
     */
    Task<Void> deferredInstall(List<String> moduleNames);

    /**
     * Defers uninstallation of given moduleNames.
     */
    Task<Void> deferredUninstall(List<String> moduleNames);

    /**
     * Returns which modules are hasInstance (excluding the base module).
     */
    Set<String> getInstalledModules();

}
