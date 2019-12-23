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

    List<String> dfNames

    String applicationId

    @OutputDirectory
    File sourceOutputDir

    void initArgs(boolean qigsawMode,
                  String qigsawId,
                  String versionName,
                  String defaultSplitInfoVersion,
                  List<String> dfNames) {
        this.qigsawMode = qigsawMode
        this.qigsawId = qigsawId
        this.versionName = versionName
        this.defaultSplitInfoVersion = defaultSplitInfoVersion
        this.dfNames = dfNames
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

        List<String> dfNameJoinList = new ArrayList<>()
        for (String dfName : dfNames) {
            dfNameJoinList.add("\"" + dfName + "\"")
        }
        generator
                .addField(
                "boolean",
                "QIGSAW_MODE",
                qigsawMode ? "Boolean.parseBoolean(\"true\")" : "false")
                .addField("String", "QIGSAW_ID", '"' + qigsawId + '"')
                .addField("String", "VERSION_NAME", '"' + versionName + '"')
                .addField("String", "DEFAULT_SPLIT_INFO_VERSION", '"' + defaultSplitInfoVersion + '"')
                .addField("String[]", "DYNAMIC_FEATURES", "{" + dfNameJoinList.join(",") + "}")
        generator.generate()
    }
}
