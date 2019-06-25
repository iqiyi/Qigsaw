package com.google.android.play.core.splitinstall;

import android.content.Context;

public class SplitInstallManagerFactory {

    public SplitInstallManagerFactory() {

    }

    /**
     * Creates an instance of {@link SplitInstallManager}.
     */
    public static SplitInstallManager create(Context context) {
        return new SplitInstallManagerImpl(new SplitInstallService(context), context);
    }

}
