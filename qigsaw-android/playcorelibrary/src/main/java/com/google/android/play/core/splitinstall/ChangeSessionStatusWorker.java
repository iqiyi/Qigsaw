package com.google.android.play.core.splitinstall;

final class ChangeSessionStatusWorker implements Runnable {

    private final SplitSessionStatusChanger changer;

    private final int status;

    private final int errorCode;

    ChangeSessionStatusWorker(SplitSessionStatusChanger changer, int status) {
        this(changer, status, 0);
    }

    ChangeSessionStatusWorker(SplitSessionStatusChanger changer, int status, int errorCode) {
        this.changer = changer;
        this.status = status;
        this.errorCode = errorCode;
    }

    @Override
    public void run() {
        if (errorCode != 0) {
            changer.mRegistry.notifyListeners(changer.sessionState.a(status, errorCode));
        } else {
            changer.mRegistry.notifyListeners(changer.sessionState.a(status));
        }
    }
}
