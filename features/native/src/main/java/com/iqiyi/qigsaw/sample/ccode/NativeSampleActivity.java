package com.iqiyi.qigsaw.sample.ccode;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.play.core.splitinstall.SplitInstallHelper;

public class NativeSampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_sample);
        SplitInstallHelper.loadLibrary(this, "hello-jni");
        ((TextView) (findViewById(R.id.hello_textview))).setText(stringFromJNI());
    }

    public native String stringFromJNI();
}
