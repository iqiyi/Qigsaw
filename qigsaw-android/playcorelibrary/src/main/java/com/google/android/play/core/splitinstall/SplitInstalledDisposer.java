package com.google.android.play.core.splitinstall;

import android.os.Bundle;

import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

import java.util.ArrayList;
import java.util.Arrays;

final class SplitInstalledDisposer implements Runnable {

    private final SplitInstallManagerImpl splitInstallManager;

    private final SplitInstallRequest splitInstallRequest;

    SplitInstalledDisposer(SplitInstallManagerImpl splitInstallManager,
                           SplitInstallRequest request) {
        this.splitInstallManager = splitInstallManager;
        this.splitInstallRequest = request;
    }

    @Override
    public void run() {
        this.splitInstallManager.getRegistry().notifyListeners(
                SplitInstallSessionState.createFrom(makeInstalledSessionState(this.splitInstallRequest.getModuleNames().toArray(new String[0])))
        );
    }

    private Bundle makeInstalledSessionState(String[] modulesNames) {
        Bundle bundle;
        (bundle = new Bundle()).putInt("session_id", 0);
        bundle.putInt("status", SplitInstallSessionStatus.INSTALLED);
        bundle.putInt("error_code", 0);
        bundle.putStringArrayList("module_names", new ArrayList<>(Arrays.asList(modulesNames)));
        bundle.putLong("total_bytes_to_download", 0L);
        bundle.putLong("bytes_downloaded", 0L);
        return bundle;
    }
}
