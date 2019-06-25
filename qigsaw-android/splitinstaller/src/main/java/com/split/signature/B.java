package com.split.signature;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;

final class B implements A {
    private final FileChannel a;
    private final long b;
    private final long c;

    B(FileChannel var1, long var2, long var4) {
        this.a = var1;
        this.b = var2;
        this.c = var4;
    }

    public long a() {
        return this.c;
    }

    public void a(MessageDigest[] var1, long var2, int var4) throws IOException {
        long var5 = this.b + var2;
        MappedByteBuffer var7;
        (var7 = this.a.map(MapMode.READ_ONLY, var5, (long) var4)).load();
        MessageDigest[] var8 = var1;
        int var9 = var1.length;

        for (int var10 = 0; var10 < var9; ++var10) {
            MessageDigest var11 = var8[var10];
            var7.position(0);
            var11.update(var7);
        }

    }
}
