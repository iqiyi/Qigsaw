package com.iqiyi.qigsaw.buildtool.gradle.sample

import com.iqiyi.qigsaw.buildtool.gradle.QigsawAppBasePlugin
import com.iqiyi.qigsaw.buildtool.gradle.sample.extension.SplitUploadExtension
import com.iqiyi.qigsaw.buildtool.gradle.upload.SplitApkUploaderInstance
import com.iqiyi.qigsaw.buildtool.gradle.sample.upload.SampleSplitApkUploader
import org.gradle.api.Project

class QigsawAppPlugin extends QigsawAppBasePlugin {

    @Override
    void apply(Project project) {
        super.apply(project)
        SplitApkUploaderInstance.set(new SampleSplitApkUploader())
        project.extensions.create("splitUpload", SplitUploadExtension)
    }
}
