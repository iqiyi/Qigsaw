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

package com.iqiyi.android.qigsaw.core.extension;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.iqiyi.android.qigsaw.core.common.SplitLog;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public abstract class ContentProviderProxy extends ContentProvider {

    private static final String TAG = "Split:ContentProviderProxy";

    private ContentProvider realContentProvider;

    private static final String NAME_SUFFIX = "_Decorated";

    private ProviderInfo providerInfo;

    public ContentProviderProxy() {

    }

    private ContentProvider ensureRealContentProvider() {
        if (realContentProvider != null) {
            return realContentProvider;
        }
        try {
            String className = getClass().getName();
            String realContentProviderClassName = className.split(NAME_SUFFIX)[0];
            realContentProvider = (ContentProvider) Class.forName(realContentProviderClassName).newInstance();
            realContentProvider.attachInfo(getContext(), providerInfo);
            return realContentProvider;
        } catch (Exception e) {
            SplitLog.w(TAG, "Failed to create ContentProvider " + providerInfo.name, e);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        this.providerInfo = new ProviderInfo(info);
        super.attachInfo(context, info);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (ensureRealContentProvider() != null) {
            realContentProvider.onConfigurationChanged(newConfig);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.query(uri, projection, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable Bundle queryArgs, @Nullable CancellationSignal cancellationSignal) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.query(uri, projection, queryArgs, cancellationSignal);
        }
        return super.query(uri, projection, queryArgs, cancellationSignal);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder, @Nullable CancellationSignal cancellationSignal) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        }
        return super.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.getType(uri);
        }
        return null;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.applyBatch(operations);
        }
        return super.applyBatch(operations);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public Uri canonicalize(@NonNull Uri url) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.canonicalize(url);
        }
        return super.canonicalize(url);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public Uri uncanonicalize(@NonNull Uri url) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.uncanonicalize(url);
        }
        return super.uncanonicalize(url);
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openAssetFile(uri, mode);
        }
        return super.openAssetFile(uri, mode);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openAssetFile(uri, mode, signal);
        }
        return super.openAssetFile(uri, mode, signal);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, @Nullable Bundle opts) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openTypedAssetFile(uri, mimeTypeFilter, opts);
        }
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri, @NonNull String mimeTypeFilter, @Nullable Bundle opts, @Nullable CancellationSignal signal) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openTypedAssetFile(uri, mimeTypeFilter, opts, signal);
        }
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts, signal);
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openFile(uri, mode);
        }
        return super.openFile(uri, mode);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openFile(uri, mode, signal);
        }
        return super.openFile(uri, mode, signal);
    }

    @NonNull
    @Override
    public <T> ParcelFileDescriptor openPipeHelper(@NonNull Uri uri, @NonNull String mimeType, @Nullable Bundle opts, @Nullable T args, @NonNull PipeDataWriter<T> func) throws FileNotFoundException {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.openPipeHelper(uri, mimeType, opts, args, func);
        }
        return super.openPipeHelper(uri, mimeType, opts, args, func);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean refresh(Uri uri, @Nullable Bundle args, @Nullable CancellationSignal cancellationSignal) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.refresh(uri, args, cancellationSignal);
        }
        return super.refresh(uri, args, cancellationSignal);
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.call(method, arg, extras);
        }
        return super.call(method, arg, extras);
    }

    @Nullable
    @Override
    public String[] getStreamTypes(@NonNull Uri uri, @NonNull String mimeTypeFilter) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.getStreamTypes(uri, mimeTypeFilter);
        }
        return super.getStreamTypes(uri, mimeTypeFilter);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.insert(uri, values);
        }
        return null;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (ensureRealContentProvider() != null) {
            realContentProvider.onTrimMemory(level);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (ensureRealContentProvider() != null) {
            realContentProvider.onLowMemory();
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.bulkInsert(uri, values);
        }
        return super.bulkInsert(uri, values);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.delete(uri, selection, selectionArgs);
        }
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (ensureRealContentProvider() != null) {
            return realContentProvider.update(uri, values, selection, selectionArgs);
        }
        return 0;
    }
}
