## Qigsaw

![](https://img.shields.io/badge/license-MIT-brightgreen.svg?style=flat)
![](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat)
![](https://img.shields.io/badge/release-1.3.3-red.svg?style=flat)

Qigsaw is a dynamic modularization library which is based on [Android App Bundles](https://developer.android.com/guide/app-bundle/). It supports dynamic delivery for split APK without reinstalling the base one.

[README 中文版](./README.zh-CN.md)

![qigsaw](./assets/qigsaw.png)

## Getting started

Import qigsaw-gradle-plugin as a dependency in your main `build.gradle` in the root of your project:

```gradle
buildscript {
    dependencies {
        classpath 'com.iqiyi.android.qigsaw:gradle-plugin:1.3.3'
    }
}
```
Then "apply" the plugin and import dependencies by adding the following lines to your `app/build.gradle`.

```gradle
dependencies {
    //qigsaw core lib
    implementation "com.iqiyi.android.qigsaw:splitcore:1.3.3"
}
...
...
apply plugin: 'com.iqiyi.qigsaw.application'
```
At Last, "apply" another plugin by adding the following lines to your `dynamicfeature/build.gradle`.

```gradle
...
...
apply plugin: 'com.iqiyi.qigsaw.dynamicfeature'
```
Considering that every app has its own downloader, qigsaw just provides an interface Downloader and you are expected to implement it. Learn more from the sample [SampleDownloader](./app/src/main/java/com/iqiyi/qigsaw/sample/downloader/SampleDownloader.java).

Qigsaw-gradle-plugin will upload split APKs which require dynamic delivery during compilation, so you have to implement SplitApkUploader to upload split APKs to your own CND server. Learn more from the sample [SampleSplitApkUploader](./qigsaw-android-sample/buildSrc/src/main/groovy/com/iqiyi/qigsaw/buildtool/gradle/sample/upload/SampleSplitApkUploader.groovy).

How to install qigsaw? Learn more from the sample [QigsawApplication](./app/src/main/java/com/iqiyi/qigsaw/sample/QigsawApplication.java).

For proguard, we have already made the proguard config automatically via qigsaw-gradle-plugin.

For multiDex, learn more from the sample [multidexkeep.pro](./app/multidexkeep.pro).

For more qigsaw configurations, learn more from the sample [app/build.gradle](./app/build.gradle).

How to install split APKs? Qigsaw provides the same APIs to [Play Core Library](https://developer.android.com/guide/app-bundle/playcore#monitor_requests), so you may read google developer docs to install.

How to build base and split APKs? During development, you may use `qigsawAssembleDebug` task or just click `Run` app in Android Studio to build. When releasing your app, use `qigsawAssembleRelease` task to build.

How to update splits? Please see our [Split Update Guide](./SPLIT_UPDATE_GUIDE.MD).

## Known Issues
There are some issues which Qigsaw can't update or support at present.

1. Can't update split AndroidManifest.xml dynamically, for example adding Android Component.
2. Can't update base APK dynamically.
3. Doesn't support Android OS version lower than 4.0.
4. Doesn't support incremental update for split APK.
5. Learn more from the [Known issues](https://developer.android.com/guide/app-bundle/#known_issues) about Android App Bundle.

## Extensive Functions

Qigsaw supports some functions which Android App Bundle doesn't yet.

1. Supports to declare Application in split AndroidManifest. Qigsaw will invoke `Applicaton#attachBaseContext` and `Applicaton#onCreate` methods for split application.
2. Supports to declare ContentProvider in split AndroidManifest.

## Support

1. Learn more from [qigsaw-sample-android](./qigsaw-android-sample).
2. Study the source code.
3. Check [wiki](https://github.com/iqiyi/Qigsaw/wiki) or FAQ for help.
4. Contact us <a href="mailto:kisson_cjw@hotmail.com">kisson_cjw@hotmail.com</a>.
5. Join QQ group chat.

![qigsaw_qq_group_chat](./assets/qigsaw_qq_group_chat.jpeg)

## Contributing

For more information about contributing, issues or pull requests, please check our [Qigsaw Contributing Guide](./CONTRIBUTING.MD).

## License

Qigsaw is MIT licensed. Read the [LICENSE](./LICENSE) file for detail.
