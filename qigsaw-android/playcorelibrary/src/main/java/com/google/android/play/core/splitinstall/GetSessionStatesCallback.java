package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.tasks.TaskWrapper;

import java.util.ArrayList;
import java.util.List;

final class GetSessionStatesCallback extends SplitInstallServiceCallbackImpl<List<SplitInstallSessionState>> {

    GetSessionStatesCallback(SplitInstallService splitInstallService, TaskWrapper<List<SplitInstallSessionState>> task) {
        super(splitInstallService, task);
    }

    @Override
    public void onGetSessionStates(List<Bundle> data) {
        super.onGetSessionStates(data);
        List<SplitInstallSessionState> sessionStates = new ArrayList<>(data.size());
        for (Bundle bundle : data) {
            sessionStates.add(SplitInstallSessionState.createFrom(bundle));
        }
        mTask.setResult(sessionStates);
    }

}
