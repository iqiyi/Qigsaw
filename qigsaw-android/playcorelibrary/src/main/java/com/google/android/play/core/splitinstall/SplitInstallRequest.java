package com.google.android.play.core.splitinstall;

import java.util.ArrayList;
import java.util.List;

public class SplitInstallRequest {

    private final List<String> moduleNames;

    public static SplitInstallRequest.Builder newBuilder() {
        return new SplitInstallRequest.Builder();
    }

    private SplitInstallRequest(Builder builder) {
        this.moduleNames = new ArrayList<>(builder.moduleNames);
    }

    /**
     * Get requested modules.
     */
    public List<String> getModuleNames() {
        return moduleNames;
    }

    public String toString() {
        String var1 = String.valueOf(this.moduleNames);
        return (new StringBuilder(34 + String.valueOf(var1).length())).append("SplitInstallRequest{modulesNames=").append(var1).append("}").toString();
    }

    /**
     * A builder for a request to install some splits.
     */
    public static class Builder {

        private final List<String> moduleNames;

        private Builder() {
            this.moduleNames = new ArrayList<>();
        }

        public Builder addModule(String moduleName) {
            this.moduleNames.add(moduleName);
            return this;
        }

        public SplitInstallRequest build() {
            return new SplitInstallRequest(this);
        }
    }
}
