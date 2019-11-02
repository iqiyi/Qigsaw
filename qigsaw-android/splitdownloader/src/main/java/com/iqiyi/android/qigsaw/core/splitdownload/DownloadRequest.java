/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.android.qigsaw.core.splitdownload;

import android.os.Parcel;
import android.os.Parcelable;

public final class DownloadRequest implements Parcelable {

    private final String url;

    private final String fileDir;

    private final String fileName;

    private final String moduleName;

    private final String fileMD5;

    private DownloadRequest(Parcel in) {
        url = in.readString();
        fileDir = in.readString();
        fileName = in.readString();
        moduleName = in.readString();
        fileMD5 = in.readString();
    }

    public static final Creator<DownloadRequest> CREATOR = new Creator<DownloadRequest>() {
        @Override
        public DownloadRequest createFromParcel(Parcel in) {
            return new DownloadRequest(in);
        }

        @Override
        public DownloadRequest[] newArray(int size) {
            return new DownloadRequest[size];
        }
    };

    public static Builder newBuilder() {
        return new Builder();
    }

    private DownloadRequest(Builder builder) {
        this.fileDir = builder.fileDir;
        this.url = builder.url;
        this.fileName = builder.fileName;
        this.moduleName = builder.moduleName;
        this.fileMD5 = builder.fileMD5;
    }

    public String getUrl() {
        return url;
    }

    public String getFileDir() {
        return fileDir;
    }

    public String getFileName() {
        return fileName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(fileDir);
        dest.writeString(fileName);
        dest.writeString(moduleName);
    }

    public static class Builder {

        private String url;

        private String fileDir;

        private String fileName;

        private String moduleName;

        private String fileMD5;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder fileDir(String fileDir) {
            this.fileDir = fileDir;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileMD5(String fileMD5) {
            this.fileMD5 = fileMD5;
            return this;
        }

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(this);
        }

    }
}
