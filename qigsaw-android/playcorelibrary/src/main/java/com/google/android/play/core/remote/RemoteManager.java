package com.google.android.play.core.remote;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import androidx.annotation.RestrictTo;

import com.google.android.play.core.splitcompat.util.PlayCore;
import com.google.android.play.core.tasks.TaskWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class RemoteManager<T extends IInterface> {

    private static final Map<String, Handler> sHandlerMap = Collections.synchronizedMap(new HashMap<String, Handler>());

    final Context mContext;

    final PlayCore mPlayCore;

    private final String mKey;

    boolean mBindingService;

    final List<RemoteTask> mPendingTasks = new ArrayList<>();

    private final Intent mSplitInstallServiceIntent;

    final IRemote<T> mRemote;

    private final WeakReference<OnBinderDiedListener> mOnBinderDiedListenerWkRef;

    private final IBinder.DeathRecipient mDeathRecipient = new DeathRecipientImpl(this);

    ServiceConnection mServiceConnection;

    T mIInterface;

    public RemoteManager(Context context, PlayCore playCore, String key, Intent intent, IRemote<T> remote, OnBinderDiedListener onBinderDiedListener) {
        this.mContext = context;
        this.mPlayCore = playCore;
        this.mKey = key;
        this.mSplitInstallServiceIntent = intent;
        this.mRemote = remote;
        this.mOnBinderDiedListenerWkRef = new WeakReference<>(onBinderDiedListener);
    }

    private Handler getHandler() {
        Handler handler;
        synchronized (sHandlerMap) {
            if (!sHandlerMap.containsKey(this.mKey)) {
                HandlerThread handlerThread = new HandlerThread(this.mKey, THREAD_PRIORITY_BACKGROUND);
                handlerThread.start();
                sHandlerMap.put(this.mKey, new Handler(handlerThread.getLooper()));
            }
            handler = sHandlerMap.get(this.mKey);
        }
        return handler;
    }

    public void bindService(RemoteTask task) {
        post(new BindServiceTask(this, task));
    }

    void bindServiceInternal(RemoteTask remoteTask) {
        if (this.mIInterface == null && !this.mBindingService) {
            mPlayCore.info("Initiate binding to the service.");
            this.mPendingTasks.add(remoteTask);
            this.mServiceConnection = new ServiceConnectionImpl(this);
            this.mBindingService = true;
            if (!this.mContext.bindService(this.mSplitInstallServiceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE)) {
                this.mPlayCore.info("Failed to bind to the service.");
                this.mBindingService = false;
                for (RemoteTask splitRemoteTask : mPendingTasks) {
                    TaskWrapper taskWrapper = splitRemoteTask.getTask();
                    if (taskWrapper != null) {
                        taskWrapper.setException(new RemoteServiceException());
                    }
                }
                this.mPendingTasks.clear();
            }
        } else {
            if (this.mBindingService) {
                this.mPlayCore.info("Waiting to bind to the service.");
                this.mPendingTasks.add(remoteTask);
                return;
            }
            remoteTask.run();
        }
    }

    public void unbindService() {
        this.post((new UnbindServiceTask(this)));
    }

    void linkToDeath() {
        this.mPlayCore.info("linkToDeath");
        try {
            this.mIInterface.asBinder().linkToDeath(this.mDeathRecipient, 0);
        } catch (Throwable e) {
            this.mPlayCore.info("linkToDeath failed");
        }
    }

    void unlinkToDeath() {
        this.mPlayCore.info("unlinkToDeath");
        this.mIInterface.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
    }

    public T getIInterface() {
        return this.mIInterface;
    }

    void post(RemoteTask task) {
        this.getHandler().post(task);
    }

    void reportBinderDeath() {
        this.mPlayCore.info("reportBinderDeath");
        OnBinderDiedListener onBinderDiedListener = this.mOnBinderDiedListenerWkRef.get();
        if (onBinderDiedListener != null) {
            this.mPlayCore.info("calling onBinderDied");
            onBinderDiedListener.onBinderDied();
        }
    }

}
