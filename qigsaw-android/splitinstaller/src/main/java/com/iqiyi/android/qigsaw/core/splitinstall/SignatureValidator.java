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

package com.iqiyi.android.qigsaw.core.splitinstall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.iqiyi.android.qigsaw.core.common.SplitLog;
import com.split.signature.G;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SignatureValidator {

    private static final String TAG = "SignatureValidator";

    private SignatureValidator() {

    }

    static boolean validateSplit(Context context, File splitFile) {
        Signature[] signatures;
        ArrayList<X509Certificate> signatureList;
        if ((signatures = getAppSignature(context)) == null) {
            signatureList = null;
        } else {
            ArrayList<X509Certificate> temp = new ArrayList<>();
            for (Signature signature : signatures) {
                X509Certificate x509Certificate;
                if ((x509Certificate = decodeCertificate(signature)) != null) {
                    temp.add(x509Certificate);
                }
            }
            signatureList = temp;
        }
        if (signatureList != null && !signatureList.isEmpty()) {
            return a(splitFile.getAbsolutePath(), signatureList);
        }
        return false;
    }

    private static boolean a(String var1, List<X509Certificate> var2) {
        X509Certificate[][] var3;
        try {
            var3 = G.a(var1);
        } catch (Exception var14) {
            SplitLog.e(TAG, "Downloaded split " + var1 + " is not signed.", var14);
            return false;
        }

        if (var3 != null && var3.length != 0 && var3[0].length != 0) {
            X509Certificate[][] var5 = var3;
            if (var2.isEmpty()) {
                SplitLog.e(TAG, "No certificates found for app.");
                return false;
            } else {
                Iterator var7 = var2.iterator();
                boolean var9;
                do {
                    if (!var7.hasNext()) {
                        return true;
                    }

                    X509Certificate var8 = (X509Certificate) var7.next();
                    var9 = false;
                    X509Certificate[][] var10 = var5;
                    int var11 = var5.length;

                    for (int var12 = 0; var12 < var11; ++var12) {
                        if (var10[var12][0].equals(var8)) {
                            var9 = true;
                            break;
                        }
                    }
                } while (var9);

                SplitLog.i(TAG, "There's an app certificate that doesn't sign the split.");
                return false;
            }
        } else {
            SplitLog.e(TAG, "Downloaded split " + var1 + " is not signed.");
            return false;
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private static Signature[] getAppSignature(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
        } catch (Throwable var1) {
            return null;
        }
    }

    private static X509Certificate decodeCertificate(Signature var0) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(var0.toByteArray()));
        } catch (CertificateException var2) {
            SplitLog.e(TAG, "Cannot decode certificate.", var2);
            return null;
        }
    }

}
