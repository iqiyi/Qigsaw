package com.split.signature;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

class X509CertificateWrapper extends X509Certificate {
    private final X509Certificate a;

    X509CertificateWrapper(X509Certificate var1) {
        this.a = var1;
    }

    public Set<String> getCriticalExtensionOIDs() {
        return this.a.getCriticalExtensionOIDs();
    }

    public byte[] getExtensionValue(String var1) {
        return this.a.getExtensionValue(var1);
    }

    public Set<String> getNonCriticalExtensionOIDs() {
        return this.a.getNonCriticalExtensionOIDs();
    }

    public boolean hasUnsupportedCriticalExtension() {
        return this.a.hasUnsupportedCriticalExtension();
    }

    public void checkValidity() throws CertificateNotYetValidException, CertificateExpiredException {
        this.a.checkValidity();
    }

    public void checkValidity(Date var1) throws CertificateNotYetValidException, CertificateExpiredException {
        this.a.checkValidity(var1);
    }

    public int getVersion() {
        return this.a.getVersion();
    }

    public BigInteger getSerialNumber() {
        return this.a.getSerialNumber();
    }

    public Principal getIssuerDN() {
        return this.a.getIssuerDN();
    }

    public Principal getSubjectDN() {
        return this.a.getSubjectDN();
    }

    public Date getNotBefore() {
        return this.a.getNotBefore();
    }

    public Date getNotAfter() {
        return this.a.getNotAfter();
    }

    public byte[] getTBSCertificate() throws CertificateEncodingException {
        return this.a.getTBSCertificate();
    }

    public byte[] getSignature() {
        return this.a.getSignature();
    }

    public String getSigAlgName() {
        return this.a.getSigAlgName();
    }

    public String getSigAlgOID() {
        return this.a.getSigAlgOID();
    }

    public byte[] getSigAlgParams() {
        return this.a.getSigAlgParams();
    }

    public boolean[] getIssuerUniqueID() {
        return this.a.getIssuerUniqueID();
    }

    public boolean[] getSubjectUniqueID() {
        return this.a.getSubjectUniqueID();
    }

    public boolean[] getKeyUsage() {
        return this.a.getKeyUsage();
    }

    public int getBasicConstraints() {
        return this.a.getBasicConstraints();
    }

    public byte[] getEncoded() throws CertificateEncodingException {
        return this.a.getEncoded();
    }

    public void verify(PublicKey var1) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        this.a.verify(var1);
    }

    public void verify(PublicKey var1, String var2) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        this.a.verify(var1, var2);
    }

    public String toString() {
        return this.a.toString();
    }

    public PublicKey getPublicKey() {
        return this.a.getPublicKey();
    }
}
