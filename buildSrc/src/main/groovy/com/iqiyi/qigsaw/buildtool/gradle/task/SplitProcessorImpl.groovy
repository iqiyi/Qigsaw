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

package com.iqiyi.qigsaw.buildtool.gradle.task

import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.ide.common.signing.CertificateInfo
import com.android.ide.common.signing.KeystoreHelper
import com.google.common.base.Preconditions
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitInfoGenerator
import com.iqiyi.qigsaw.buildtool.gradle.internal.splits.SplitProcessor
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

import java.security.PrivateKey
import java.security.cert.X509Certificate

class SplitProcessorImpl implements SplitProcessor {

    private Project appProject

    private AppExtension android

    private String variantName

    private Map<String, List<String>> dynamicFeatureDependenciesMap

    SplitProcessorImpl(Project appProject,
                       AppExtension splitExtension,
                       String variantName,
                       Map<String, List<String>> dynamicFeatureDependenciesMap) {
        this.dynamicFeatureDependenciesMap = dynamicFeatureDependenciesMap
        this.appProject = appProject
        this.android = splitExtension
        this.variantName = variantName
    }

    @Override
    final File signSplitAPKIfNeed(File splitApk) {
        ApkVerifier apkVerifier = new ApkVerifier.Builder(splitApk).build()
        if (!apkVerifier.verify().verified) {
            AppExtension appAndroid = appProject.extensions.android
            SigningConfig signingConfig
            try {
                signingConfig = appAndroid.signingConfigs.getByName(variantName.uncapitalize())
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
            File signedApk = new File(splitApk.path.toString() + ".signed")
            ApkSigner apkSigner = signerBuilder
                    .setInputApk(splitApk)
                    .setOutputApk(signedApk)
                    .setV1SigningEnabled(signingConfig.isV1SigningEnabled())
                    .setV2SigningEnabled(signingConfig.isV2SigningEnabled())
                    .build()
            apkSigner.sign()
            return signedApk
        }
        return splitApk
    }

    @Override
    final SplitInfo createSplitInfo(String splitName, File splitSignedApk, File splitManifest) {
        SplitInfoGenerator infoGenerator = new SplitInfoGeneratorImpl(appProject, android, variantName, dynamicFeatureDependenciesMap)
        SplitInfo splitInfo = infoGenerator.generate(splitName, splitSignedApk, splitManifest)
        return splitInfo
    }
}
