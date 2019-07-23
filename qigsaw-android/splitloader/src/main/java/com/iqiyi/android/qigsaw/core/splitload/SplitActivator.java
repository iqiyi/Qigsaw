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

package com.iqiyi.android.qigsaw.core.splitload;

import android.app.Application;

import com.iqiyi.android.qigsaw.core.extension.AABExtension;
import com.iqiyi.android.qigsaw.core.extension.AABExtensionException;
import com.iqiyi.android.qigsaw.core.splitreport.SplitLoadError;

final class SplitActivator {

    private final AABExtension aabExtension;

    SplitActivator(AABExtension aabExtension) {
        this.aabExtension = aabExtension;
    }

    void activate(String splitName) throws SplitLoadException {
        Application app;
        try {
            app = aabExtension.createApplication(splitName);
        } catch (AABExtensionException e) {
            throw new SplitLoadException(SplitLoadError.ACTIVATE_APPLICATION_FAILED, e);
        }

        try {
            aabExtension.activateSplitProviders(splitName);
        } catch (AABExtensionException e) {
            throw new SplitLoadException(SplitLoadError.ACTIVATE_PROVIDERS_FAILED, e);
        }
        if (app != null) {
            app.onCreate();
        }
    }
}
