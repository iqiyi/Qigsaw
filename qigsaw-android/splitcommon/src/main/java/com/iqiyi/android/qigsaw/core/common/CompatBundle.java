package com.iqiyi.android.qigsaw.core.common;

import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.ServiceLoader;

public class CompatBundle {
    @Nullable
    public static final ICompatBundle instance;

    static {
        ServiceLoader<ICompatBundle> compats = ServiceLoader.load(ICompatBundle.class);
        Iterator<ICompatBundle> iterator = compats.iterator();
        instance = iterator.hasNext() ? iterator.next() : null;
    }
}
