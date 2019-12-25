#your dex.loader patterns here

-keep class com.iqiyi.android.qigsaw.core.Qigsaw {
    <init>(...);
    void install(...);
    void onAppGetResources(Resources);
}

-keep class * implements com.iqiyi.android.qigsaw.core.splitdownload.Downloader {
    <init>(...);
}

# ${yourApplicationId}.QigsawConfig, QigsawVersion >= 1.2.2
-keep class com.iqiyi.qigsaw.sample.QigsawConfig {
    *;
}

# ${yourPackageNameInManifest}.BuildConfig QigsawVersion < 1.2.2
-keep class com.iqiyi.qigsaw.sample.BuildConfig {
    *;
}

-keep class com.iqiyi.android.qigsaw.core.extension.ComponentInfo {
    *;
}
