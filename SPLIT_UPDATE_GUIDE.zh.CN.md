# Qigsaw插件更新步骤

### 创建插件更新所需分支

为你的代码创建插件更新所需的分支。

### 修改插件版本号

完成插件代码更新后，在*dynamicfeature/build.gradlew*文件中修改插件版本号（如果不修改插件版本号插件将无法更新）。

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

### 配置文件并修改Split-Info版本号

1. 配置mapping文件。
2. 配置old apk，存放路径与mapping文件目录一直。
3. 修改插件信息版本号。

```
qigsawSplit {

    /**
     * 可选项，默认为'null'

     * 需要更新插件时，必须将上一版本的apk配置到指定路径

     */
    oldApk = "${qigsawPath}/app.apk"

    /**
     * 可选项，默认为'1.0.0'

     * 当需要更新插件时，必须修改splitInfoVersion的值

     */
    //splitInfoVersion '1.0.0'
    splitInfoVersion '1.0.1'

    /**
     * 可选项，默认为'null'

     * 当需要更新插件时, 必须配置applyMapping的值.

     */
    applyMapping = "${qigsawPath}/mapping.txt"

    /**
     * 可选项，默认为'false'

     * 是否将插件上传至CDN，true代表需要上传至CDN。
     * 插件是否上传，由两个因素决定：1.releaseSplitApk必须为true。2.dynamic-feature项目AndroidManifest.xml中onDemand为true

     */
    releaseSplitApk true
}
```

- mapping文件应用规则

插件首次更新，应用app首次发布生成的mapping文件。

插件二次更新，应用插件第一次更新生成的mapping文件。以此类推。

- old apk应用规则

插件更新情况下，始终应用app首次发布生成的apk文件。

- 关于不同渠道

某些应用不同渠道其mapping文件不一样，因此需要针对这些渠道分别打包发布。

### 上传插件信息文件

上传新生成的插件信息JSON文件至您的发布后台。
