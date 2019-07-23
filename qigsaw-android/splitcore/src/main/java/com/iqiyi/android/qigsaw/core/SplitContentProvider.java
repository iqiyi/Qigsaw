package com.iqiyi.android.qigsaw.core;

import android.support.annotation.RestrictTo;

import com.iqiyi.android.qigsaw.core.extension.ContentProviderProxy;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManager;
import com.iqiyi.android.qigsaw.core.splitload.SplitLoadManagerService;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SplitContentProvider extends ContentProviderProxy {

    @Override
    protected boolean checkRealContentProviderInstallStatus(String splitName) {
        if (getRealContentProvider() != null) {
            return true;
        } else {
            if (SplitLoadManagerService.hasInstance()) {
                SplitLoadManager loadManager = SplitLoadManagerService.getInstance();
                loadManager.loadInstalledSplits();
                return getRealContentProvider() != null;
            }
        }
        return false;
    }
}
