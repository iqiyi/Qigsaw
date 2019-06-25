package com.split.signature;

import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class G implements A {
    private final ByteBuffer a;

    private static Pair<ByteBuffer, Long> a(RandomAccessFile var0, int var1) throws IOException {
        if (var1 >= 0 && var1 <= 65535) {
            long var2;
            if ((var2 = var0.length()) < 22L) {
                return null;
            } else {
                ByteBuffer var4;
                long var5;
                int var10000;
                label33:
                {
                    var1 = (int) Math.min((long) var1, var2 - 22L);
                    (var4 = ByteBuffer.allocate(var1 + 22)).order(ByteOrder.LITTLE_ENDIAN);
                    var5 = var2 - (long) var4.capacity();
                    var0.seek(var5);
                    var0.readFully(var4.array(), var4.arrayOffset(), var4.capacity());
                    ByteBuffer var9 = var4;
                    a(var4);
                    int var10;
                    if ((var10 = var4.capacity()) >= 22) {
                        int var11 = Math.min(var10 - 22, 65535);
                        int var12 = var10 - 22;

                        for (int var13 = 0; var13 < var11; ++var13) {
                            int var14 = var12 - var13;
                            if (var9.getInt(var14) == 101010256) {
                                int var17 = var14 + 20;
                                if ((var9.getShort(var17) & '\uffff') == var13) {
                                    var10000 = var14;
                                    break label33;
                                }
                            }
                        }
                    }

                    var10000 = -1;
                }

                int var7 = var10000;
                if (var10000 == -1) {
                    return null;
                } else {
                    var4.position(var7);
                    ByteBuffer var8;
                    (var8 = var4.slice()).order(ByteOrder.LITTLE_ENDIAN);
                    return Pair.create(var8, var5 + (long) var7);
                }
            }
        } else {
            throw new IllegalArgumentException((new StringBuilder(27)).append("maxCommentSize: ").append(var1).toString());
        }
    }

    private static void a(ByteBuffer var0) {
        if (var0.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    private static long a(ByteBuffer var0, int var1) {
        return (long) var0.getInt(var1) & 4294967295L;
    }

    public static X509Certificate[][] a(String var0) throws IOException, D {
        RandomAccessFile var1 = new RandomAccessFile(var0, "r");

        X509Certificate[][] var3;
        try {
            X509Certificate[][] var2 = a(var1);
            var1.close();
            var3 = var2;
        } finally {
            try {
                var1.close();
            } catch (IOException var8) {

            }

        }

        return var3;
    }

    private static X509Certificate[][] a(RandomAccessFile var0) throws IOException, D {
        C var1 = b(var0);
        return a(var0.getChannel(), var1);
    }

    private static C b(RandomAccessFile var0) throws IOException, D {
        boolean var10000;
        ByteBuffer var2;
        long var3;
        label16:
        {
            Pair var1;
            var2 = (ByteBuffer) (var1 = c(var0)).first;
            var3 = (Long) var1.second;
            long var15;
            if ((var15 = var3 - 20L) >= 0L) {
                var0.seek(var15);
                if (var0.readInt() == 1347094023) {
                    var10000 = true;
                    break label16;
                }
            }

            var10000 = false;
        }

        if (var10000) {
            throw new D("ZIP64 APK not supported");
        } else {
            long var5 = a(var2, var3);
            Pair var7;
            ByteBuffer var8 = (ByteBuffer) (var7 = a(var0, var5)).first;
            long var9 = (Long) var7.second;
            ByteBuffer var11 = d(var8);
            return new C(var11, var9, var5, var3, var2);
        }
    }

    private static X509Certificate[][] a(FileChannel fileChannel, C cVar) {
        int i = 0;
        Map hashMap = new HashMap();
        List arrayList = new ArrayList();
        Throwable e;
        try {
            CertificateFactory instance = CertificateFactory.getInstance("X.509");
            try {
                ByteBuffer b = b(cVar.a);
                while (b.hasRemaining()) {
                    i++;
                    try {
                        arrayList.add(a(b(b), hashMap, instance));
                    } catch (IOException e2) {
                        e = e2;
                    } catch (BufferUnderflowException e3) {
                        e = e3;
                    } catch (SecurityException e4) {
                        e = e4;
                    }
                }
                if (i <= 0) {
                    throw new SecurityException("No signers found");
                } else if (hashMap.isEmpty()) {
                    throw new SecurityException("No content digests found");
                } else {
                    a(hashMap, fileChannel, cVar.b, cVar.c, cVar.d, cVar.e);
                    return (X509Certificate[][]) arrayList.toArray(new X509Certificate[arrayList.size()][]);
                }
            } catch (Throwable e5) {
                throw new SecurityException("Failed to read list of signers", e5);
            }
        } catch (Throwable e52) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e52);
        }
    }


    private static X509Certificate[] a(ByteBuffer var0, Map<Integer, byte[]> var1, CertificateFactory var2) throws IOException {
        ByteBuffer var3 = b(var0);
        ByteBuffer var4 = b(var0);
        byte[] var5 = c(var0);
        int var6 = 0;
        int var7 = -1;
        byte[] var8 = null;
        ArrayList var9 = new ArrayList();

        while (var4.hasRemaining()) {
            ++var6;

            try {
                ByteBuffer var10;
                if ((var10 = b(var4)).remaining() < 8) {
                    throw new SecurityException("Signature record too short");
                }

                int var11 = var10.getInt();
                var9.add(var11);
                if (a(var11) && (var7 == -1 || a(var11, var7) > 0)) {
                    var7 = var11;
                    var8 = c(var10);
                }
            } catch (BufferUnderflowException | IOException var31) {
                throw new SecurityException((new StringBuilder(45)).append("Failed to parse signature record #").append(var6).toString(), var31);
            }
        }

        if (var7 == -1) {
            if (var6 == 0) {
                throw new SecurityException("No signatures found");
            } else {
                throw new SecurityException("No supported signatures found");
            }
        } else {
            String var32 = e(var7);
            Pair var33;
            String var12 = (String) (var33 = f(var7)).first;
            AlgorithmParameterSpec var13 = (AlgorithmParameterSpec) var33.second;

            boolean var14;
            try {
                PublicKey var15 = KeyFactory.getInstance(var32).generatePublic(new X509EncodedKeySpec(var5));
                Signature var16;
                (var16 = Signature.getInstance(var12)).initVerify(var15);
                if (var13 != null) {
                    var16.setParameter(var13);
                }

                var16.update(var3);
                var14 = var16.verify(var8);
            } catch (InvalidKeySpecException | InvalidKeyException | InvalidAlgorithmParameterException | SignatureException | NoSuchAlgorithmException var30) {
                throw new SecurityException((new StringBuilder(27 + String.valueOf(var12).length())).append("Failed to verify ").append(var12).append(" signature").toString(), var30);
            }

            if (!var14) {
                throw new SecurityException(String.valueOf(var12).concat(" signature did not verify"));
            } else {
                byte[] var34 = null;
                var3.clear();
                ByteBuffer var35 = b(var3);
                ArrayList var17 = new ArrayList();
                int var18 = 0;

                while (var35.hasRemaining()) {
                    ++var18;

                    try {
                        ByteBuffer var19;
                        if ((var19 = b(var35)).remaining() < 8) {
                            throw new IOException("Record too short");
                        }

                        int var20 = var19.getInt();
                        var17.add(var20);
                        if (var20 == var7) {
                            var34 = c(var19);
                        }
                    } catch (BufferUnderflowException | IOException var29) {
                        throw new IOException((new StringBuilder(42)).append("Failed to parse digest record #").append(var18).toString(), var29);
                    }
                }

                if (!var9.equals(var17)) {
                    throw new SecurityException("Signature algorithms don't match between digests and signatures records");
                } else {
                    int var36 = b(var7);
                    byte[] var37;
                    if ((var37 = (byte[]) var1.put(var36, var34)) != null && !MessageDigest.isEqual(var37, var34)) {
                        throw new SecurityException(c(var36).concat(" contents digest does not match the digest specified by a preceding signer"));
                    } else {
                        ByteBuffer var21 = b(var3);
                        ArrayList var22 = new ArrayList();
                        int var23 = 0;

                        while (var21.hasRemaining()) {
                            ++var23;
                            byte[] var24 = c(var21);

                            X509Certificate var25;
                            try {
                                var25 = (X509Certificate) var2.generateCertificate(new ByteArrayInputStream(var24));
                            } catch (CertificateException var28) {
                                throw new SecurityException((new StringBuilder(41)).append("Failed to decode certificate #").append(var23).toString(), var28);
                            }

                            X509CertificateEx var38 = new X509CertificateEx(var25, var24);
                            var22.add(var38);
                        }

                        if (var22.isEmpty()) {
                            throw new SecurityException("No certificates listed");
                        } else {
                            byte[] var39 = ((X509Certificate) var22.get(0)).getPublicKey().getEncoded();
                            if (!Arrays.equals(var5, var39)) {
                                throw new SecurityException("Public key mismatch between certificate and signature record");
                            } else {
                                return (X509Certificate[]) var22.toArray(new X509Certificate[var22.size()]);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void a(Map<Integer, byte[]> var0, FileChannel var1, long var2, long var4, long var6, ByteBuffer var8) {
        if (var0.isEmpty()) {
            throw new SecurityException("No digests provided");
        } else {
            B var9 = new B(var1, 0L, var2);
            B var10 = new B(var1, var4, var6 - var4);
            (var8 = var8.duplicate()).order(ByteOrder.LITTLE_ENDIAN);
            a(var8);
            int var23 = var8.position() + 16;
            if (var2 >= 0L && var2 <= 4294967295L) {
                var8.putInt(var8.position() + var23, (int) var2);
                G var11 = new G(var8);
                int[] var12 = new int[var0.size()];
                int var13 = 0;

                int var15;
                for (Iterator var14 = var0.keySet().iterator(); var14.hasNext(); ++var13) {
                    var15 = (Integer) var14.next();
                    var12[var13] = var15;
                }

                byte[][] var27;
                try {
                    var27 = a(var12, new A[]{var9, var10, var11});
                } catch (DigestException var26) {
                    throw new SecurityException("Failed to compute digest(s) of contents", var26);
                }

                for (var15 = 0; var15 < var12.length; ++var15) {
                    int var16 = var12[var15];
                    byte[] var17 = (byte[]) var0.get(var16);
                    byte[] var18 = var27[var15];
                    if (!MessageDigest.isEqual(var17, var18)) {
                        throw new SecurityException(c(var16).concat(" digest of contents did not verify"));
                    }
                }

            } else {
                throw new IllegalArgumentException((new StringBuilder(47)).append("uint32 value of out range: ").append(var2).toString());
            }
        }
    }

    private static byte[][] a(int[] var0, A[] var1) throws DigestException {
        long var2 = 0L;
        A[] var4 = var1;
        int var5 = var1.length;

        int var6;
        for (var6 = 0; var6 < var5; ++var6) {
            A var7 = var4[var6];
            var2 += a(var7.a());
        }

        if (var2 >= 2097151L) {
            throw new DigestException((new StringBuilder(37)).append("Too many chunks: ").append(var2).toString());
        } else {
            int var29 = (int) var2;
            byte[][] var30 = new byte[var0.length][];

            for (var6 = 0; var6 < var0.length; ++var6) {
                int var8 = d(var0[var6]);
                byte[] var9;
                (var9 = new byte[5 + var29 * var8])[0] = 90;
                a(var29, var9, 1);
                var30[var6] = var9;
            }

            byte[] var31;
            (var31 = new byte[5])[0] = -91;
            int var32 = 0;
            MessageDigest[] var33 = new MessageDigest[var0.length];

            int var34;
            for (var34 = 0; var34 < var0.length; ++var34) {
                String var10 = c(var0[var34]);

                try {
                    var33[var34] = MessageDigest.getInstance(var10);
                } catch (NoSuchAlgorithmException var28) {
                    throw new RuntimeException(var10.concat(" digest not supported"), var28);
                }
            }

            var34 = 0;
            A[] var35 = var1;
            int var11 = var1.length;

            int var12;
            for (var12 = 0; var12 < var11; ++var12) {
                A var13 = var35[var12];
                long var14 = 0L;

                for (long var16 = var13.a(); var16 > 0L; ++var32) {
                    int var18;
                    a(var18 = (int) Math.min(var16, 1048576L), var31, 1);

                    int var19;
                    for (var19 = 0; var19 < var33.length; ++var19) {
                        var33[var19].update(var31);
                    }

                    try {
                        var13.a(var33, var14, var18);
                    } catch (IOException var27) {
                        throw new DigestException((new StringBuilder(59)).append("Failed to digest chunk #").append(var32).append(" of section #").append(var34).toString(), var27);
                    }

                    for (var19 = 0; var19 < var0.length; ++var19) {
                        int var20 = var0[var19];
                        byte[] var21 = var30[var19];
                        int var22 = d(var20);
                        MessageDigest var23;
                        int var24;
                        if ((var24 = (var23 = var33[var19]).digest(var21, 5 + var32 * var22, var22)) != var22) {
                            String var25 = var23.getAlgorithm();
                            throw new RuntimeException((new StringBuilder(46 + String.valueOf(var25).length())).append("Unexpected output size of ").append(var25).append(" digest: ").append(var24).toString());
                        }
                    }

                    var14 += (long) var18;
                    var16 -= (long) var18;
                }

                ++var34;
            }

            byte[][] var36 = new byte[var0.length][];

            for (var11 = 0; var11 < var0.length; ++var11) {
                var12 = var0[var11];
                byte[] var37 = var30[var11];
                String var38 = c(var12);

                MessageDigest var15;
                try {
                    var15 = MessageDigest.getInstance(var38);
                } catch (NoSuchAlgorithmException var26) {
                    throw new RuntimeException(var38.concat(" digest not supported"), var26);
                }

                byte[] var39 = var15.digest(var37);
                var36[var11] = var39;
            }

            return var36;
        }
    }

    private static Pair<ByteBuffer, Long> c(RandomAccessFile var0) throws IOException, D {
        Pair var1;
        Pair var7;
        if ((var1 = var0.length() < 22L ? null : ((var7 = a((RandomAccessFile) var0, 0)) != null ? var7 : a((RandomAccessFile) var0, 65535))) == null) {
            long var2 = var0.length();
            throw new D((new StringBuilder(102)).append("Not an APK file: ZIP End of Central Directory record not found in file with ").append(var2).append(" bytes").toString());
        } else {
            return var1;
        }
    }

    private static long a(ByteBuffer var0, long var1) throws D {
        a(var0);
        long var3;
        if ((var3 = a(var0, var0.position() + 16)) >= var1) {
            throw new D((new StringBuilder(122)).append("ZIP Central Directory offset out of range: ").append(var3).append(". ZIP End of Central Directory offset: ").append(var1).toString());
        } else {
            a(var0);
            long var5 = a(var0, var0.position() + 12);
            if (var3 + var5 != var1) {
                throw new D("ZIP Central Directory is not immediately followed by End of Central Directory");
            } else {
                return var3;
            }
        }
    }

    private static long a(long var0) {
        return (var0 + 1048576L - 1L) / 1048576L;
    }

    private static boolean a(int var0) {
        switch (var0) {
            case 257:
            case 258:
            case 259:
            case 260:
            case 513:
            case 514:
            case 769:
                return true;
            default:
                return false;
        }
    }

    private static int a(int var0, int var1) {
        int var2 = b(var0);
        int var3 = b(var1);
        return b(var2, var3);
    }

    private static int b(int var0, int var1) {
        switch (var0) {
            case 1:
                switch (var1) {
                    case 1:
                        return 0;
                    case 2:
                        return -1;
                    default:
                        throw new IllegalArgumentException((new StringBuilder(37)).append("Unknown digestAlgorithm2: ").append(var1).toString());
                }
            case 2:
                switch (var1) {
                    case 1:
                        return 1;
                    case 2:
                        return 0;
                    default:
                        throw new IllegalArgumentException((new StringBuilder(37)).append("Unknown digestAlgorithm2: ").append(var1).toString());
                }
            default:
                throw new IllegalArgumentException((new StringBuilder(37)).append("Unknown digestAlgorithm1: ").append(var0).toString());
        }
    }

    private static int b(int i) {
        switch (i) {
            case 257:
            case 259:
            case 513:
            case 769:
                return 1;
            case 258:
            case 260:
            case 514:
                return 2;
            default:
                String str = "Unknown signature algorithm: 0x";
                String valueOf = String.valueOf(Long.toHexString((long) i));
                throw new IllegalArgumentException(valueOf.length() != 0 ? str.concat(valueOf) : new String(str));
        }
    }

    private static String c(int var0) {
        switch (var0) {
            case 1:
                return "SHA-256";
            case 2:
                return "SHA-512";
            default:
                throw new IllegalArgumentException((new StringBuilder(44)).append("Unknown content digest algorthm: ").append(var0).toString());
        }
    }

    private static int d(int var0) {
        switch (var0) {
            case 1:
                return 32;
            case 2:
                return 64;
            default:
                throw new IllegalArgumentException((new StringBuilder(44)).append("Unknown content digest algorthm: ").append(var0).toString());
        }
    }

    private static String e(int i) {
        switch (i) {
            case 257:
            case 258:
            case 259:
            case 260:
                return "RSA";
            case 513:
            case 514:
                return "EC";
            case 769:
                return "DSA";
            default:
                String str = "Unknown signature algorithm: 0x";
                String valueOf = String.valueOf(Long.toHexString((long) i));
                throw new IllegalArgumentException(valueOf.length() != 0 ? str.concat(valueOf) : new String(str));
        }
    }

    private static Pair<String, ? extends AlgorithmParameterSpec> f(int i) {
        switch (i) {
            case 257:
                return Pair.create("SHA256withRSA/PSS", new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
            case 258:
                return Pair.create("SHA512withRSA/PSS", new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
            case 259:
                return Pair.create("SHA256withRSA", null);
            case 260:
                return Pair.create("SHA512withRSA", null);
            case 513:
                return Pair.create("SHA256withECDSA", null);
            case 514:
                return Pair.create("SHA512withECDSA", null);
            case 769:
                return Pair.create("SHA256withDSA", null);
            default:
                String str = "Unknown signature algorithm: 0x";
                String valueOf = String.valueOf(Long.toHexString((long) i));
                throw new IllegalArgumentException(valueOf.length() != 0 ? str.concat(valueOf) : new String(str));
        }
    }

    private static ByteBuffer a(ByteBuffer var0, int var1, int var2) {
        if (var2 < 8) {
            throw new IllegalArgumentException((new StringBuilder(38)).append("end < start: ").append(var2).append(" < 8").toString());
        } else {
            int var3 = var0.capacity();
            if (var2 > var0.capacity()) {
                throw new IllegalArgumentException((new StringBuilder(41)).append("end > capacity: ").append(var2).append(" > ").append(var3).toString());
            } else {
                int var4 = var0.limit();
                int var5 = var0.position();

                ByteBuffer var7;
                try {
                    var0.position(0);
                    var0.limit(var2);
                    var0.position(8);
                    ByteBuffer var6;
                    (var6 = var0.slice()).order(var0.order());
                    var7 = var6;
                } finally {
                    var0.position(0);
                    var0.limit(var4);
                    var0.position(var5);
                }

                return var7;
            }
        }
    }

    private static ByteBuffer b(ByteBuffer var0, int var1) {
        if (var1 < 0) {
            throw new IllegalArgumentException((new StringBuilder(17)).append("size: ").append(var1).toString());
        } else {
            int var2 = var0.limit();
            int var3;
            int var4;
            if ((var4 = (var3 = var0.position()) + var1) >= var3 && var4 <= var2) {
                var0.limit(var4);

                ByteBuffer var6;
                try {
                    ByteBuffer var5;
                    (var5 = var0.slice()).order(var0.order());
                    var0.position(var4);
                    var6 = var5;
                } finally {
                    var0.limit(var2);
                }

                return var6;
            } else {
                throw new BufferUnderflowException();
            }
        }
    }

    private static ByteBuffer b(ByteBuffer var0) throws IOException {
        int var1;
        if (var0.remaining() < 4) {
            var1 = var0.remaining();
            throw new IOException((new StringBuilder(93)).append("Remaining buffer too short to contain length of length-prefixed field. Remaining: ").append(var1).toString());
        } else if ((var1 = var0.getInt()) < 0) {
            throw new IllegalArgumentException("Negative length");
        } else if (var1 > var0.remaining()) {
            int var2 = var0.remaining();
            throw new IOException((new StringBuilder(101)).append("Length-prefixed field longer than remaining buffer. Field length: ").append(var1).append(", remaining: ").append(var2).toString());
        } else {
            return b(var0, var1);
        }
    }

    private static byte[] c(ByteBuffer var0) throws IOException {
        int var1;
        if ((var1 = var0.getInt()) < 0) {
            throw new IOException("Negative length");
        } else if (var1 > var0.remaining()) {
            int var3 = var0.remaining();
            throw new IOException((new StringBuilder(90)).append("Underflow while reading length-prefixed value. Length: ").append(var1).append(", available: ").append(var3).toString());
        } else {
            byte[] var2 = new byte[var1];
            var0.get(var2);
            return var2;
        }
    }

    private static void a(int var0, byte[] var1, int var2) {
        var1[1] = (byte) var0;
        var1[2] = (byte) (var0 >>> 8);
        var1[3] = (byte) (var0 >>> 16);
        var1[4] = (byte) (var0 >>> 24);
    }

    private static Pair<ByteBuffer, Long> a(RandomAccessFile var0, long var1) throws D, IOException {
        if (var1 < 32L) {
            throw new D((new StringBuilder(87)).append("APK too small for APK Signing Block. ZIP Central Directory offset: ").append(var1).toString());
        } else {
            ByteBuffer var3;
            (var3 = ByteBuffer.allocate(24)).order(ByteOrder.LITTLE_ENDIAN);
            var0.seek(var1 - (long) var3.capacity());
            var0.readFully(var3.array(), var3.arrayOffset(), var3.capacity());
            if (var3.getLong(8) == 2334950737559900225L && var3.getLong(16) == 3617552046287187010L) {
                long var4;
                if ((var4 = var3.getLong(0)) >= (long) var3.capacity() && var4 <= 2147483639L) {
                    int var6 = (int) (var4 + 8L);
                    long var7;
                    if ((var7 = var1 - (long) var6) < 0L) {
                        throw new D((new StringBuilder(59)).append("APK Signing Block offset out of range: ").append(var7).toString());
                    } else {
                        ByteBuffer var9;
                        (var9 = ByteBuffer.allocate(var6)).order(ByteOrder.LITTLE_ENDIAN);
                        var0.seek(var7);
                        var0.readFully(var9.array(), var9.arrayOffset(), var9.capacity());
                        long var10;
                        if ((var10 = var9.getLong(0)) != var4) {
                            throw new D((new StringBuilder(103)).append("APK Signing Block sizes in header and footer do not match: ").append(var10).append(" vs ").append(var4).toString());
                        } else {
                            return Pair.create(var9, var7);
                        }
                    }
                } else {
                    throw new D((new StringBuilder(57)).append("APK Signing Block size out of range: ").append(var4).toString());
                }
            } else {
                throw new D("No APK Signing Block before ZIP Central Directory");
            }
        }
    }

    private static ByteBuffer d(ByteBuffer var0) throws D {
        e(var0);
        ByteBuffer var1 = a(var0, 8, var0.capacity() - 24);
        int var2 = 0;

        while (var1.hasRemaining()) {
            ++var2;
            if (var1.remaining() < 8) {
                throw new D((new StringBuilder(70)).append("Insufficient data to read size of APK Signing Block entry #").append(var2).toString());
            }

            long var3;
            if ((var3 = var1.getLong()) < 4L || var3 > 2147483647L) {
                throw new D((new StringBuilder(76)).append("APK Signing Block entry #").append(var2).append(" size out of range: ").append(var3).toString());
            }

            int var5 = (int) var3;
            int var6 = var1.position() + var5;
            if (var5 > var1.remaining()) {
                int var8 = var1.remaining();
                throw new D((new StringBuilder(91)).append("APK Signing Block entry #").append(var2).append(" size out of range: ").append(var5).append(", available: ").append(var8).toString());
            }

            if (var1.getInt() == 1896449818) {
                return b(var1, var5 - 4);
            }

            var1.position(var6);
        }

        throw new D("No APK Signature Scheme v2 block in APK Signing Block");
    }

    private static void e(ByteBuffer var0) {
        if (var0.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    private G(ByteBuffer var1) {
        this.a = var1.slice();
    }

    public long a() {
        return (long) this.a.capacity();
    }

    public void a(MessageDigest[] var1, long var2, int var4) {
        ByteBuffer var6 = this.a;
        ByteBuffer var5;
        synchronized (this.a) {
            this.a.position((int) var2);
            this.a.limit((int) var2 + var4);
            var5 = this.a.slice();
        }

        MessageDigest[] var11 = var1;
        int var7 = var1.length;

        for (int var8 = 0; var8 < var7; ++var8) {
            MessageDigest var9 = var11[var8];
            var5.position(0);
            var9.update(var5);
        }

    }
}
