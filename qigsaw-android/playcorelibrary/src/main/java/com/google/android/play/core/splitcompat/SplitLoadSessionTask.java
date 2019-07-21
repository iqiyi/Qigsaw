package com.google.android.play.core.splitcompat;

import android.content.Intent;

import com.google.android.play.core.splitinstall.SplitSessionStatusChanger;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;
import com.iqiyi.android.qigsaw.core.splitload.listener.OnSplitLoadListener;

import java.util.List;

final class SplitLoadSessionTask implements Runnable, OnSplitLoadListener {

    private final List<Intent> splitFileIntents;

    private final SplitSessionStatusChanger changer;

    SplitLoadSessionTask(List<Intent> splitFileIntents, SplitSessionStatusChanger changer) {
        this.splitFileIntents = splitFileIntents;
        this.changer = changer;
    }

    @Override
    public void run() {
        //load installed splits
        SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
        if (loadManager != null) {
            Runnable splitLoadTask = loadManager.createSplitLoadTask(splitFileIntents, this);
            splitLoadTask.run();
        }
    }

    @Override
    public void onCompleted() {
        changer.changeStatus(SplitInstallSessionStatus.INSTALLED);
    }

    @Override
    public void onFailed(int errorCode) {
        changer.changeStatus(SplitInstallSessionStatus.FAILED, errorCode);
    }
}
