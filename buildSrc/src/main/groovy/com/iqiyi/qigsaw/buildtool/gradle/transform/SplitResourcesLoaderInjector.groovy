package com.iqiyi.qigsaw.buildtool.gradle.transform

import com.android.SdkConstants
import com.android.ide.common.internal.WaitableExecutor
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ManifestReaderImpl

import java.nio.file.*
import java.util.regex.Matcher

class SplitResourcesLoaderInjector {

    WaitableExecutor waitableExecutor

    Set<ComponentInfo> activities

    Set<ComponentInfo> services

    Set<ComponentInfo> receivers

    SplitActivityWeaver activityWeaver

    SplitServiceWeaver serviceWeaver

    SplitReceiverWeaver receiverWeaver

    SplitResourcesLoaderInjector(WaitableExecutor waitableExecutor, File splitManifest) {
        this.waitableExecutor = waitableExecutor
        ManifestReader manifestReader = new ManifestReaderImpl(splitManifest)
        this.activities = manifestReader.readActivities()
        this.services = manifestReader.readServices()
        this.receivers = manifestReader.readReceivers()
        this.activityWeaver = new SplitActivityWeaver()
        this.serviceWeaver = new SplitServiceWeaver()
        this.receiverWeaver = new SplitReceiverWeaver()
    }

    void injectDir(File outputDir) {
        Files.walk(outputDir.toPath(), Integer.MAX_VALUE).filter {
            Files.isRegularFile(it)
        }.each { Path path ->
            File file = path.toFile()
            if (file.name.endsWith("jar")) {
                injectJar(file)
            } else if (file.name.endsWith(SdkConstants.DOT_CLASS)) {
                this.waitableExecutor.execute {
                    String className = file.absolutePath.substring(outputDir.absolutePath.length() + 1, file.absolutePath.length() - SdkConstants.DOT_CLASS.length())
                            .replaceAll(Matcher.quoteReplacement(File.separator), '.')
                    byte[] bytes = injectClass(path, className)
                    if (bytes != null) {
                        Files.write(path, bytes, StandardOpenOption.WRITE)
                    }
                }
            }
        }
    }

    void injectJar(File jar) {
        this.waitableExecutor.execute {
            Map<String, String> zipProperties = ['create': 'false']
            URI zipDisk = URI.create("jar:${jar.toURI().toString()}")
            FileSystem zipFs = null
            try {
                zipFs = FileSystems.newFileSystem(zipDisk, zipProperties)
                Path root = zipFs.rootDirectories.iterator().next()
                Files.walk(root, Integer.MAX_VALUE).filter {
                    Files.isRegularFile(it)
                }.each { Path path ->
                    String pathString = path.toString().substring(1).replace("\\", "/")
                    if (!pathString.endsWith(SdkConstants.DOT_CLASS)) {
                        return
                    }
                    byte[] bytes = injectClass(path, pathString.replaceAll(Matcher.quoteReplacement(File.separator), '.'))
                    if (bytes != null) {
                        Files.write(path, bytes, StandardOpenOption.WRITE)
                    }
                }
            } catch (e) {
                e.printStackTrace()
            } finally {
                FileUtils.closeQuietly(zipFs)
            }
        }
    }

    byte[] injectClass(Path path, String className) {
        byte[] ret = null
        if (isActivity(className)) {
            println("Inject activity " + className)
            ret = new SplitActivityWeaver().weave(path.newInputStream())
        } else if (isService(className)) {
            println("Inject service " + className)
            ret = serviceWeaver.weave(path.newInputStream())
        } else if (isReceiver(className)) {
            println("Inject receiver " + className)
            ret = receiverWeaver.weave(path.newInputStream())
        }
        return ret
    }

    boolean isActivity(String className) {
        boolean isActivity = false
        if (!activities.isEmpty()) {
            activities.each {
                if (it.name.equals(className)) {
                    isActivity = true
                }
            }
        }
        return isActivity
    }

    boolean isService(String className) {
        boolean isService = false
        if (!services.isEmpty()) {
            services.each {
                if (it.name.equals(className)) {
                    isService = true
                }
            }
        }
        return isService
    }

    boolean isReceiver(String className) {
        boolean isReceiver = false
        if (!receivers.isEmpty()) {
            receivers.each {
                if (it.name.equals(className)) {
                    isReceiver = true
                }
            }
        }
        return isReceiver
    }
}
