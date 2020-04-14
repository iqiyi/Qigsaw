package com.iqiyi.qigsaw.sample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DynamicFeatures";

    private static final String JAVA_SAMPLE_ACTIVITY = "com.iqiyi.qigsaw.sample.java.JavaSampleActivity";

    private static final String NATIVE_SAMPLE_ACTIVITY = "com.iqiyi.qigsaw.sample.ccode.NativeSampleActivity";

    private SplitInstallManager installManager;

    private LinearLayout buttonGroups;

    private LinearLayout progressbarGroups;

    private TextView progressText;

    private String moduleJava;

    private String moduleAssets;

    private String moduleNative;

    private SplitInstallStateUpdatedListener myListener = new SplitInstallStateUpdatedListener() {

        @Override
        public void onStateUpdate(SplitInstallSessionState state) {
            boolean multiInstall = state.moduleNames().size() > 1;
            if (state.status() == SplitInstallSessionStatus.INSTALLED) {
                for (String moduleName : state.moduleNames()) {
                    onSuccessfullyLoad(moduleName, !multiInstall);
                    toastAndLog(moduleName + " has been installed!!!!!");
                }
            } else if (state.status() == SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) {
                try {
                    startIntentSender(state.resolutionIntent().getIntentSender(), null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installManager = SplitInstallManagerFactory.create(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_load_java).setOnClickListener(this);
        findViewById(R.id.btn_load_assets).setOnClickListener(this);
        findViewById(R.id.btn_load_native).setOnClickListener(this);
        findViewById(R.id.btn_install_all_now).setOnClickListener(this);
        findViewById(R.id.btn_install_all_deferred).setOnClickListener(this);
        findViewById(R.id.btn_uninstall_all_deferred).setOnClickListener(this);
        buttonGroups = findViewById(R.id.button_groups);
        progressbarGroups = findViewById(R.id.progress_bar_groups);
        progressText = findViewById(R.id.progress_text);
        moduleJava = getString(R.string.module_feature_java);
        moduleAssets = getString(R.string.module_feature_assets);
        moduleNative = getString(R.string.module_feature_native);


    }

    @Override
    protected void onResume() {
        super.onResume();
        installManager.registerListener(myListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        installManager.unregisterListener(myListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_load_java:
                startQigsawInstaller(moduleJava);
                break;
            case R.id.btn_load_assets:
                startQigsawInstaller(moduleAssets);
                break;
            case R.id.btn_load_native:
                startQigsawInstaller(moduleNative);
                break;
            case R.id.btn_install_all_now:
                installAllFeaturesNow();
                break;
            case R.id.btn_install_all_deferred:
                installAllFeaturesDeferred();
                break;
            case R.id.btn_uninstall_all_deferred:
                uninstallAllFeaturesDeferred();
                break;
            default:
                break;
        }
    }

    private void startQigsawInstaller(String moduleName) {
        Intent intent = new Intent(this, QigsawInstaller.class);
        ArrayList<String> moduleNames = new ArrayList<>();
        moduleNames.add(moduleName);
        intent.putStringArrayListExtra(QigsawInstaller.KEY_MODULE_NAMES, moduleNames);
        startActivityForResult(intent, QigsawInstaller.INSTALL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QigsawInstaller.INSTALL_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    if (data != null) {
                        ArrayList<String> moduleNames = data.getStringArrayListExtra(QigsawInstaller.KEY_MODULE_NAMES);
                        if (moduleNames != null && moduleNames.size() == 1) {
                            loadAndLaunchModule(moduleNames.get(0));
                        }
                    }
                    break;
                case RESULT_CANCELED:
                    break;
                default:
                    break;
            }
        }

    }

    private void onSuccessfullyLoad(String moduleName, boolean launch) {
        if (launch) {
            if (moduleName.equals(moduleJava)) {
                launchActivity(JAVA_SAMPLE_ACTIVITY);
            } else if (moduleName.equals(moduleAssets)) {
                displayAssets();
            } else if (moduleName.equals(moduleNative)) {
                launchActivity(NATIVE_SAMPLE_ACTIVITY);
            }
        }
        displayButtons();
    }

    private void launchActivity(String className) {
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), className);
        startActivity(intent);
    }

    private void displayAssets() {
        try {
            Context context = createPackageContext(getPackageName(), 0);
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open("assets.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuffer = new StringBuilder();
            String str;
            while ((str = br.readLine()) != null) {
                stringBuffer.append(str);
            }
            try {
                is.close();
                br.close();
            } catch (IOException ie) {
                //ignored
            }
            new AlertDialog.Builder(this)
                    .setTitle("Asset Content")
                    .setMessage(stringBuffer.toString())
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installAllFeaturesNow() {
        final SplitInstallRequest request = SplitInstallRequest.newBuilder()
                .addModule(moduleJava)
                .addModule(moduleNative)
                .addModule(moduleAssets)
                .build();
        installManager.startInstall(request).addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                toastAndLog("Loading " + request.getModuleNames());
            }
        });
    }

    private void installAllFeaturesDeferred() {

        final List<String> modules = Arrays.asList(moduleJava, moduleAssets, moduleNative);

        installManager.deferredInstall(modules).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                toastAndLog("Deferred installation " + modules);
            }
        });
    }

    private void uninstallAllFeaturesDeferred() {

        final List<String> modules = Arrays.asList(moduleJava, moduleAssets, moduleNative);

        installManager.deferredUninstall(modules).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                toastAndLog("Deferred uninstallation " + modules);
            }
        });
    }

    private void toastAndLog(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        Log.d(TAG, text);
    }

    private void loadAndLaunchModule(String name) {
        updateProgressMessage("Loading module " + name);
        if (installManager.getInstalledModules().contains(name)) {
            updateProgressMessage("Already installed!");
            onSuccessfullyLoad(name, true);
            return;
        }
        SplitInstallRequest request = SplitInstallRequest.newBuilder().addModule(name).build();
        installManager.startInstall(request).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                toastAndLog(e.getMessage());
            }
        });
        updateProgressMessage("Starting install for " + name);
    }

    private void updateProgressMessage(String message) {
        if (progressbarGroups.getVisibility() != View.VISIBLE) {
            displayProgress();
        }
        progressText.setText(message);
    }

    private void displayButtons() {
        buttonGroups.setVisibility(View.VISIBLE);
        progressbarGroups.setVisibility(View.GONE);
    }

    private void displayProgress() {
        buttonGroups.setVisibility(View.VISIBLE);
        progressbarGroups.setVisibility(View.GONE);
    }
}
