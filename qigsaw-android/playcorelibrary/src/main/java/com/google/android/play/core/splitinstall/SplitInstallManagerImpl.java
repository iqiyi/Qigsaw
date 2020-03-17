package com.google.android.play.core.splitinstall;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.Task;
import com.google.android.play.core.tasks.Tasks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SplitInstallManagerImpl implements SplitInstallManager {

    private static final String TAG = "SplitInstallManagerImpl";

    private final SplitInstallService mInstallService;

    private final Handler mMainHandler;

    private SplitInstallListenerRegistry mRegistry;

    private final Context context;

    private final String packageName;

    SplitInstallManagerImpl(SplitInstallService installService, Context context) {
        this(installService, context, context.getPackageName());
    }

    private SplitInstallManagerImpl(SplitInstallService installService, Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
        this.mInstallService = installService;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mRegistry = new SplitInstallListenerRegistry(context);
    }

    @Override
    public void registerListener(SplitInstallStateUpdatedListener listener) {
        getRegistry().registerListener(listener);
    }

    @Override
    public void unregisterListener(SplitInstallStateUpdatedListener listener) {
        getRegistry().unregisterListener(listener);
    }

    @Override
    public Task<Integer> startInstall(SplitInstallRequest request) {
        if (getInstalledModules().containsAll(request.getModuleNames())) {
            mMainHandler.post(new SplitInstalledDisposer(this, request));
            return Tasks.createTaskAndSetResult(0);
        } else {
            return mInstallService.startInstall(request.getModuleNames());
        }
    }

    @Override
    public boolean startConfirmationDialogForResult(SplitInstallSessionState sessionState, Activity activity, int requestCode)
            throws IntentSender.SendIntentException {
        if (sessionState.status() == SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION && sessionState.resolutionIntent() != null) {
            activity.startIntentSenderForResult(sessionState.resolutionIntent().getIntentSender(),
                    requestCode, null, 0, 0, 0);
            return true;
        }
        return false;
    }

    @Override
    public Task<Void> cancelInstall(int sessionId) {
        return mInstallService.cancelInstall(sessionId);
    }

    @Override
    public Task<SplitInstallSessionState> getSessionState(int sessionId) {
        return mInstallService.getSessionState(sessionId);
    }

    @Override
    public Task<List<SplitInstallSessionState>> getSessionStates() {
        return mInstallService.getSessionStates();
    }

    @Override
    public Task<Void> deferredInstall(List<String> moduleNames) {
        return mInstallService.deferredInstall(moduleNames);
    }

    @Override
    public Task<Void> deferredUninstall(List<String> moduleNames) {
        return mInstallService.deferredUninstall(moduleNames);
    }

    @Override
    public Set<String> getInstalledModules() {
        Set<String> installedSplitInstallInfo = getInstalledSplitInstallInfo();
        if (installedSplitInstallInfo != null && !installedSplitInstallInfo.isEmpty()) {
            return installedSplitInstallInfo;
        }
        return LoadedSplitFetcherSingleton.get().loadedSplits();
    }

    private Set<String> getInstalledSplitInstallInfo() {
        Set<String> installedModules = getFusedModules();
        if (Build.VERSION.SDK_INT < 21) {
            return installedModules;
        }
        String[] splitNames = getSplitInstallInfo();
        if (splitNames == null) {
            Log.d(TAG, "No splits are found or app cannot be found in package manager.");
            return installedModules;
        }
        String logMsg = "Split names are: ";
        String splitNamesStr = Arrays.toString(splitNames);
        Log.d(TAG, splitNamesStr.length() != 0 ? logMsg.concat(splitNamesStr) : logMsg);
        for (String splitName : splitNames) {
            if (!splitName.startsWith("config.")) {
                installedModules.add(cutSplitName(splitName));
            }
        }
        return installedModules;
    }

    private Set<String> getFusedModules() {
        Set<String> fusedModules = new HashSet<>();
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (Throwable e) {
            Log.w(TAG, "App is not found in PackageManager");
            return fusedModules;
        }
        if (appInfo.metaData != null) {
            String fusedName;
            if ((fusedName = appInfo.metaData.getString("shadow.bundletool.com.android.dynamic.apk.fused.modules")) != null && !fusedName.isEmpty()) {
                Collections.addAll(fusedModules, fusedName.split(",", -1));
                fusedModules.remove("");
                return fusedModules;
            } else {
                Log.d(TAG, "App has no fused modules.");
                return fusedModules;
            }
        } else {
            Log.d(TAG, "App has no applicationInfo or metaData");
            return fusedModules;
        }
    }

    private String cutSplitName(String splitName) {
        return splitName.split("\\.config\\.")[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String[] getSplitInstallInfo() {
        try {
            PackageInfo packageInfo;
            return (packageInfo = context.getPackageManager().getPackageInfo(packageName, 0)) != null ? packageInfo.splitNames : null;
        } catch (Throwable var2) {
            Log.d(TAG, "App is not found in PackageManager");
            return null;
        }
    }

    SplitInstallListenerRegistry getRegistry() {
        return mRegistry;
    }
}
