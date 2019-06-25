package com.google.android.play.core.splitinstall;

import com.google.android.play.core.remote.OnBinderDiedListener;

final class OnBinderDiedListenerImpl implements OnBinderDiedListener {

    private final SplitInstallService mSplitInstallService;

    OnBinderDiedListenerImpl(SplitInstallService splitInstallService) {
        this.mSplitInstallService = splitInstallService;
    }

    @Override
    public void onBinderDied() {
        mSplitInstallService.onBinderDied();
    }
}
