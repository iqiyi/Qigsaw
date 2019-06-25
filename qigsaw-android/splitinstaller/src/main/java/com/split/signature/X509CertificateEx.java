package com.split.signature;

import java.security.cert.X509Certificate;

final class X509CertificateEx extends X509CertificateWrapper {
    private byte[] a;

    X509CertificateEx(X509Certificate var1, byte[] var2) {
        super(var1);
        this.a = var2;
    }

    public byte[] getEncoded() {
        return this.a;
    }
}
