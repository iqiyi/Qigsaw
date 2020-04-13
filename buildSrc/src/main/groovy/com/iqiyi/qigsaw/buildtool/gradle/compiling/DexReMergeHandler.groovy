/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.qigsaw.buildtool.gradle.compiling

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.LoggerWrapper

import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.builder.dexing.DexMergerTool
import com.android.builder.dexing.DexingType
import com.android.ide.common.blame.Message
import com.android.ide.common.blame.MessageReceiver
import com.android.ide.common.blame.ParsingProcessOutputHandler
import com.android.ide.common.blame.parser.DexParser
import com.android.ide.common.blame.parser.ToolOutputParser
import com.android.ide.common.process.ProcessOutput
import com.android.ide.common.process.ProcessOutputHandler
import com.google.common.collect.ImmutableList
import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.FileUtils
import org.gradle.api.Project

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

class DexReMergeHandler {

    final File outputDir

    final VariantScope variantScope

    final ForkJoinPool forkJoinPool

    final ReMergeMessageReceiver messageR

    final LoggerWrapper logger

    final String r8TaskName

    DexReMergeHandler(Project project, ApplicationVariant variant) {
        this.outputDir = new File(project.buildDir.absolutePath + File.separator + "intermediates/qigsaw/remerge" + File.separator + variant.name)
        this.variantScope = variant.variantData.scope
        this.messageR = new ReMergeMessageReceiver()
        this.forkJoinPool = new ForkJoinPool()
        this.logger = LoggerWrapper.getLogger(DexReMergeHandler)
        this.r8TaskName = "transformClassesAndResourcesWithR8For${variant.name.capitalize()}"
    }

    void reMerge(List<File> dexFiles) {
        if (DexingType.LEGACY_MULTIDEX != variantScope.getDexingType()) {
            return
        }
        Iterable mainDexListArtifact = variantScope
                .getArtifacts()
                .getFinalArtifactFiles(
                        InternalArtifactType.LEGACY_MULTIDEX_MAIN_DEX_LIST)
        Path mainDexPath = null
        if (mainDexListArtifact.empty) {
            try {
                File mainDexListFile = variantScope.getArtifacts().appendArtifact(InternalArtifactType.MAIN_DEX_LIST_FOR_BUNDLE, r8TaskName, "mainDexList.txt")
                mainDexPath = mainDexListFile.toPath()
            } catch (Exception e) {
                //ignored
            }
        } else {
            Class<?> class_BuildableArtifactUtil = Class.forName("com.android.build.gradle.internal.api.artifact.BuildableArtifactUtil")
            Class<?> class_BuildableArtifact = Class.forName("com.android.build.api.artifact.BuildableArtifact")
            Method method_singlePath = class_BuildableArtifactUtil.getDeclaredMethod("singlePath", class_BuildableArtifact)
            method_singlePath.setAccessible(true)
            mainDexPath = method_singlePath.invoke(null, mainDexListArtifact)
        }
        List<Path> dexPaths = new ArrayList<>()
        dexFiles.each {
            dexPaths.add(it.toPath())
        }
        ProcessOutputHandler outputHandler =
                new ParsingProcessOutputHandler(
                        new ToolOutputParser(new DexParser(), Message.Kind.ERROR, logger),
                        new ToolOutputParser(new DexParser(), logger),
                        messageR)
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        outputDir.mkdirs()
        List<ForkJoinTask<Void>> mergeTasks = ImmutableList.of(submitForMerging(outputHandler.createOutput(), outputDir, dexPaths, mainDexPath))
        // now wait for all merge tasks completion
        File originDir = null
        mergeTasks.each {
            it.join()
        }
        for (File dexFile : dexFiles) {
            originDir = dexFile.getParentFile()
            dexFile.delete()
        }
        // copy new dex to origin dir
        outputDir.listFiles().each {
            File oldDexFile = new File(originDir.getPath() + File.separator + it.name)
            println("old dex path :" + oldDexFile.absolutePath)
            FileUtils.copyFile(it, oldDexFile)
        }
    }

    ForkJoinTask<Void> submitForMerging(
            @NonNull ProcessOutput output,
            @NonNull File dexOutputDir,
            @NonNull Iterable<Path> dexArchives,
            @Nullable Path mainDexList) {

        return forkJoinPool.submit(getDexMergerTransformCallable(output, dexOutputDir, dexArchives, mainDexList))
    }

    Callable getDexMergerTransformCallable(@NonNull ProcessOutput output,
                                           @NonNull File dexOutputDir,
                                           @NonNull Iterable<Path> dexArchives,
                                           @Nullable Path mainDexList) {
        Class<?> classDexMergerTransformCallable = null
        try {
            classDexMergerTransformCallable = Class.forName("com.android.build.gradle.internal.transforms.DexMergerTransformCallable")
        } catch (ClassNotFoundException e) {
            //ignored
        }
        if (classDexMergerTransformCallable == null) {
            throw new RuntimeException("Can't find class 'com.android.build.gradle.internal.transforms.DexMergerTransformCallable'")
        }

        Constructor constructor = null
        try {
            constructor = classDexMergerTransformCallable.getDeclaredConstructor(
                    MessageReceiver.class,
                    DexingType.class,
                    ProcessOutput.class,
                    File.class,
                    Iterator.class,
                    Path.class,
                    ForkJoinPool.class,
                    DexMergerTool.class,
                    int.class,
                    boolean.class)
        } catch (NoSuchMethodException e) {
            try {
                constructor = classDexMergerTransformCallable.getDeclaredConstructor(
                        MessageReceiver.class,
                        DexingType.class,
                        ProcessOutput.class,
                        File.class,
                        Iterable.class,
                        Path.class,
                        ForkJoinPool.class,
                        DexMergerTool.class,
                        int.class,
                        boolean.class)
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace()
            }
        }
        if (constructor == null) {
            throw new RuntimeException("Can't get constructor method of DexMergerTransformCallable");
        }
        constructor.setAccessible(true)
        Callable callable = null
        try {
            callable = constructor.newInstance(messageR,
                    this.variantScope.getDexingType(),
                    output,
                    dexOutputDir,
                    dexArchives.iterator(),
                    mainDexList,
                    forkJoinPool,
                    this.variantScope.getDexMerger(),
                    this.variantScope.getMinSdkVersion().getApiLevel(),
                    false)
        } catch (Exception e) {
            try {
                callable = constructor.newInstance(messageR,
                        this.variantScope.getDexingType(),
                        output,
                        dexOutputDir,
                        dexArchives,
                        mainDexList,
                        forkJoinPool,
                        this.variantScope.getDexMerger(),
                        this.variantScope.getMinSdkVersion().getApiLevel(),
                        false)
            } catch (Exception e1) {
                //ignored
            }
        }
        if (callable == null) {
            throw new RuntimeException("Can't create instance of DexMergerTransformCallable")
        }
        return callable
    }

    static class ReMergeMessageReceiver implements MessageReceiver {

        @Override
        void receiveMessage(Message message) {

        }
    }

}
