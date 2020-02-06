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

package com.iqiyi.qigsaw.buildtool.gradle.internal.tool

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.ide.common.signing.CertificateInfo
import com.android.ide.common.signing.KeystoreHelper
import com.google.common.base.Preconditions
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

import java.security.PrivateKey
import java.security.cert.X509Certificate

class SplitApkSigner {

    final Project baseProject

    final String variantName

    SplitApkSigner(Project project, String variantName) {
        this.baseProject = project
        this.variantName = variantName
    }

    File signAPKIfNeed(File unsignedApk, File signedApk) {
        ApkVerifier apkVerifier = new ApkVerifier.Builder(unsignedApk).build()
        if (!apkVerifier.verify().verified) {
            SigningConfig signingConfig
            try {
                signingConfig = baseProject.extensions.android.signingConfigs.getByName(variantName.uncapitalize())
            } catch (UnknownDomainObjectException e) {
                //Catch block
                throw new RuntimeException("Can't get " + variantName.uncapitalize() + " signingConfigs in app project", e)
            }
            CertificateInfo certificateInfo = KeystoreHelper.getCertificateInfo(
                    signingConfig.getStoreType(),
                    Preconditions.checkNotNull(signingConfig.getStoreFile()),
                    Preconditions.checkNotNull(signingConfig.getStorePassword()),
                    Preconditions.checkNotNull(signingConfig.getKeyPassword()),
                    Preconditions.checkNotNull(signingConfig.getKeyAlias()))
            PrivateKey key = certificateInfo.getKey()
            X509Certificate certificate = certificateInfo.getCertificate()
            ApkSigner.SignerConfig signerConfig = new ApkSigner.SignerConfig.Builder("CERT", key, [certificate]).build()
            ApkSigner.Builder signerBuilder = new ApkSigner.Builder([signerConfig])
            if (signedApk == null) {
                signedApk = new File(unsignedApk.path.toString() + ".signed")
            }
            ApkSigner apkSigner = signerBuilder
                    .setInputApk(unsignedApk)
                    .setOutputApk(signedApk)
                    .setV1SigningEnabled(signingConfig.isV1SigningEnabled())
                    .setV2SigningEnabled(signingConfig.isV2SigningEnabled())
                    .build()
            apkSigner.sign()
            return signedApk
        }
        return unsignedApk
    }

}
