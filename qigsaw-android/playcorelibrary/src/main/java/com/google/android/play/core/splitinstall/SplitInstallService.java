package com.google.android.play.core.splitinstall;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.play.core.remote.OnBinderDiedListener;
import com.google.android.play.core.remote.RemoteManager;
import com.google.android.play.core.splitcompat.util.PlayCore;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceProxy;
import com.google.android.play.core.tasks.Task;
import com.google.android.play.core.tasks.TaskWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SplitInstallService {

    static final PlayCore playCore = new PlayCore(SplitInstallService.class.getSimpleName());

    private final Context mContext;

    final String mPackageName;

    final RemoteManager<ISplitInstallServiceProxy> mSplitRemoteManager;

    SplitInstallService(Context context) {
        this(context, context.getPackageName());
    }

    private SplitInstallService(Context context, String packageName) {
        final OnBinderDiedListener onBinderDiedListener = new OnBinderDiedListenerImpl(this);
        this.mContext = context;
        this.mPackageName = packageName;
        Intent splitInstallServiceIntent = new Intent("com.iqiyi.android.play.core.splitinstall.BIND_SPLIT_INSTALL_SERVICE").setPackage(packageName);
        this.mSplitRemoteManager = new RemoteManager<ISplitInstallServiceProxy>(context.getApplicationContext(), playCore, "SplitInstallService", splitInstallServiceIntent, SplitRemoteImpl.sInstance, onBinderDiedListener);
    }

    Task<Integer> startInstall(List<String> moduleNames) {
        playCore.info("startInstall(%s)", moduleNames);
        TaskWrapper<Integer> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new StartInstallTask(this, taskWrapper, moduleNames, taskWrapper));
        return taskWrapper.getTask();
    }

    Task<Void> deferredInstall(List<String> moduleNames) {
        playCore.info("deferredInstall(%s)", moduleNames);
        TaskWrapper<Void> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new DeferredInstallTask(this, taskWrapper, moduleNames, taskWrapper));
        return taskWrapper.getTask();
    }

    Task<Void> deferredUninstall(List<String> moduleNames) {
        playCore.info("deferredUninstall(%s)", moduleNames);
        TaskWrapper<Void> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new DeferredUninstallTask(this, taskWrapper, moduleNames, taskWrapper));
        return taskWrapper.getTask();
    }

    Task<SplitInstallSessionState> getSessionState(int sessionId) {
        playCore.info("getSessionState(%d)", sessionId);
        TaskWrapper<SplitInstallSessionState> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new GetSessionStateTask(this, taskWrapper, sessionId, taskWrapper));
        return taskWrapper.getTask();
    }

    Task<List<SplitInstallSessionState>> getSessionStates() {
        playCore.info("getSessionStates");
        TaskWrapper<List<SplitInstallSessionState>> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new GetSessionStatesTask(this, taskWrapper, taskWrapper));
        return taskWrapper.getTask();
    }

    Task<Void> cancelInstall(int sessionId) {
        playCore.info("cancelInstall(%d)", sessionId);
        TaskWrapper<Void> taskWrapper = new TaskWrapper<>();
        mSplitRemoteManager.bindService(new CancelInstallTask(this, taskWrapper, sessionId, taskWrapper));
        return taskWrapper.getTask();
    }

    static List<Bundle> wrapModuleNames(Collection<String> moduleNames) {
        ArrayList<Bundle> var1 = new ArrayList<>(moduleNames.size());
        for (String var3 : moduleNames) {
            Bundle var4;
            (var4 = new Bundle()).putString("module_name", var3);
            var1.add(var4);
        }
        return var1;
    }

    static Bundle wrapVersionCode() {
        Bundle bundle = new Bundle();
        bundle.putInt("playcore_version_code", 10010);
        return bundle;
    }

    void onBinderDied() {
        playCore.info("onBinderDied");
        Bundle bundle = new Bundle();
        bundle.putInt("session_id", -1);
        bundle.putInt("status", SplitInstallSessionStatus.FAILED);
        bundle.putInt("error_code", SplitInstallErrorCode.SERVICE_DIED);
        Intent intent = new Intent();
        intent.setPackage(this.mPackageName);
        intent.setAction("com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService");
        intent.putExtra("session_state", bundle);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        if (Build.VERSION.SDK_INT >= 26) {
            intent.addFlags(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);
        }
        this.mContext.sendBroadcast(intent);
    }


}
