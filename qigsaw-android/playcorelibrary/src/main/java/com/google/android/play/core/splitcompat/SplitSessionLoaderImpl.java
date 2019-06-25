package com.google.android.play.core.splitcompat;

import android.content.Intent;

import com.google.android.play.core.splitinstall.SplitSessionLoader;
import com.google.android.play.core.splitinstall.SplitSessionStatusChanger;

import java.util.List;
import java.util.concurrent.Executor;

final class SplitSessionLoaderImpl implements SplitSessionLoader {

    private final Executor mExecutor;

    SplitSessionLoaderImpl(Executor executor) {
        this.mExecutor = executor;
    }

    @Override
    public void load(List<Intent> splitFileIntents, SplitSessionStatusChanger changer) {
        if (!SplitCompat.hasInstance()) {
            throw new IllegalStateException("Ingestion should only be called in SplitCompat mode.");
        } else {
            this.mExecutor.execute(new SplitLoadSessionTask(splitFileIntents, changer));
        }
    }

}
