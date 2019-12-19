package com.iqiyi.qigsaw.buildtool.gradle.task

import com.iqiyi.qigsaw.buildtool.gradle.compiling.QigsawConfigGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateQigsawConfig extends DefaultTask {

    String qigsawId

    String versionName

    boolean qigsawMode

    String defaultSplitInfoVersion

    List<String> dynamicFeatures

    String applicationId

    @OutputDirectory
    File sourceOutputDir

    void initArgs(boolean qigsawMode,
                  String qigsawId,
                  String versionName,
                  String defaultSplitInfoVersion,
                  List<String> dynamicFeatures) {
        this.qigsawMode = qigsawMode
        this.qigsawId = qigsawId
        this.versionName = versionName
        this.defaultSplitInfoVersion = defaultSplitInfoVersion
        this.dynamicFeatures = dynamicFeatures
    }

    void setSourceOutputDir(File sourceOutputDir) {
        this.sourceOutputDir = sourceOutputDir
    }

    void setApplicationId(String applicationId) {
        this.applicationId = applicationId
    }

    @TaskAction
    void generate() throws IOException {
        QigsawConfigGenerator generator = new QigsawConfigGenerator(sourceOutputDir, applicationId)
        File qigsawConfigFile = generator.getQigsawConfigFile()
        if (qigsawConfigFile.exists()) {
            qigsawConfigFile.delete()
        }
        List<String> jointList = new ArrayList<>()
        for (String dfName : dynamicFeatures) {
            jointList.add("\"" + dfName + "\"")
        }
        generator
                .addField(
                "boolean",
                "QIGSAW_MODE",
                qigsawMode ? "Boolean.parseBoolean(\"true\")" : "false")
                .addField("String", "QIGSAW_ID", '"' + qigsawId + '"')
                .addField("String", "VERSION_NAME", '"' + versionName + '"')
                .addField("String", "DEFAULT_SPLIT_INFO_VERSION", '"' + defaultSplitInfoVersion + '"')
                .addField("String[]", "DYNAMIC_FEATURES", "{" + jointList.join(",") + "}")
        generator.generate()
    }
}
