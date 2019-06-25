# Updating steps of Qigsaw splits

## Create a new branch for updating splits

Create a new branch from your tag.

## Change the version of the splits

After you commit code changes, modify the version of the splits in `dynamicfeature/build.gradlew`. (If the version isn't modified, the splits will not be updated.)

```
android {
    compileSdkVersion versions.compileSdk
    defaultConfig {
        minSdkVersion versions.minSdk
        targetSdkVersion versions.targetSdk
        versionCode 1
        //versionName "1.0.0"
        versionName "1.0.1"
    }
}
```

## Configurate old products and modify the version of Split-Info

1. Place `mapping.txt` in `{qigsawPath}`.
2. Place the old apk in `{qigsawPath}`.
3. Change the version of split-info to a new one.

```
qigsawSplit {
    
    /**
     * optional，default 'null'
     * if you want to update split version, oldApk must be set.
     */
    oldApk = "${qigsawPath}/app.apk"
    
    /**
     * optional, default '1.0.0'
     * when update splits, you need upgrade value of splitInfoVersion
     */
    //splitInfoVersion '1.0.0'
    splitInfoVersion '1.0.1'
    
    /**
     * optional，default 'null'
     * if you want to update splits, applyMapping must be set.
     */
    applyMapping = "${qigsawPath}/mapping.txt"
    
    /**
     * optional, default 'false'
     * Whether upload all split APKs to CDN, ignored 'onDemand' declared in split AndroidManifest file.
     */
    releaseSplitApk true
}
```

-  Selection rule of mapping.txt

When you are in a full and successive progress of updating, make sure to use the same mapping.txt from the previous one in this progress.

(e.g. 
When you update the splits in the first time, use the **mapping** from first released base apk;

when you update the splits in the second time, use the **mapping** from last update of splits.)


-   Selection rule of the old apk

In the situation of updating splits, please always apply the old apk that was released at first time.


-    About different channels of apk

Some Apps need different mapping.txt for different channels, so they need to be released separately for each channel.


## Upload new Split-Info json file

Upload new Split-Info json file to your server.
