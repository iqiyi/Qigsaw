package com.google.android.play.core.splitinstall;

public class SplitInstallException extends RuntimeException {

    private final int errorCode;

    SplitInstallException(int errorCode) {
        super((new StringBuilder(32)).append("Split Install Error: ").append(errorCode).toString());
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}
