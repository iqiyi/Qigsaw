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
import com.android.builder.internal.ClassFieldImpl
import com.android.builder.model.ClassField
import com.google.common.base.Charsets
import com.google.common.collect.Lists
import com.google.common.io.Closer
import com.squareup.javawriter.JavaWriter

import javax.lang.model.element.Modifier

final class QigsawConfigGenerator {

    final String qigsawConfigPackageName

    final File genFolder

    static final String QIGSAW_CONFIG_NAME = "QigsawConfig.java"

    static final Set<Modifier> PUBLIC_FINAL = EnumSet.of(Modifier.PUBLIC, Modifier.FINAL)
    static final Set<Modifier> PUBLIC_STATIC_FINAL =
            EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

    final List<ClassField> mFields = Lists.newArrayList()

    QigsawConfigGenerator(@NonNull File genFolder, @NonNull String qigsawConfigPackageName) {
        this.qigsawConfigPackageName = qigsawConfigPackageName
        this.genFolder = genFolder
    }

    QigsawConfigGenerator addField(
            @NonNull String type, @NonNull String name, @NonNull String value) {
        mFields.add(new ClassFieldImpl(type, name, value))
        return this
    }

    /**
     * Returns a File representing where the QigsawConfig class will be.
     */
    File getFolderPath() {
        return new File(genFolder, qigsawConfigPackageName.replace(".", File.separator))
    }

    File getQigsawConfigFile() {
        File folder = getFolderPath()
        return new File(folder, QIGSAW_CONFIG_NAME)
    }

    /**
     * Generates the BuildConfig class.
     */
    void generate() throws IOException {
        File pkgFolder = getFolderPath()
        if (!pkgFolder.isDirectory()) {
            if (!pkgFolder.mkdirs()) {
                throw new RuntimeException("Failed to create " + pkgFolder.getAbsolutePath())
            }
        }

        File qigsawConfigJava = new File(pkgFolder, QIGSAW_CONFIG_NAME)

        Closer closer = Closer.create()
        try {
            FileOutputStream fos = closer.register(new FileOutputStream(qigsawConfigJava))
            OutputStreamWriter out = closer.register(new OutputStreamWriter(fos, Charsets.UTF_8))
            JavaWriter writer = closer.register(new JavaWriter(out))

            writer.emitJavadoc("Automatically generated file. DO NOT MODIFY")
                    .emitPackage(qigsawConfigPackageName)
                    .beginType("QigsawConfig", "class", PUBLIC_FINAL)

            for (ClassField field : mFields) {
                emitClassField(writer, field)
            }

            writer.endType()
        } catch (Throwable e) {
            throw closer.rethrow(e)
        } finally {
            closer.close()
        }
    }

    private static void emitClassField(JavaWriter writer, ClassField field) throws IOException {
        String documentation = field.getDocumentation()
        if (!documentation.isEmpty()) {
            writer.emitJavadoc(documentation)
        }
        for (String annotation : field.getAnnotations()) {
            writer.emitAnnotation(annotation)
        }
        writer.emitField(
                field.getType(),
                field.getName(),
                PUBLIC_STATIC_FINAL,
                field.getValue())
    }


}
