package com.google.android.play.core.splitcompat;

import android.app.Application;
import android.content.Context;

public abstract class SplitCompatApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SplitCompat.install(this);
    }
}
