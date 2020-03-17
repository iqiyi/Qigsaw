package com.iqiyi.qigsaw.sample;

import android.app.AppComponentFactory;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.iqiyi.android.qigsaw.core.splitload.SplitDelegateClassLoaderFactory;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class SampleSplitAppComponentFactory extends AppComponentFactory {

    private ClassLoader classLoader;

    @NonNull
    @Override
    public ClassLoader instantiateClassLoader(@NonNull ClassLoader cl, @NonNull ApplicationInfo aInfo) {
        ClassLoader preCl = super.instantiateClassLoader(cl, aInfo);
        if (classLoader == null) {
            classLoader = SplitDelegateClassLoaderFactory.instantiateClassLoader(preCl);
        }
        return classLoader;
    }
}
