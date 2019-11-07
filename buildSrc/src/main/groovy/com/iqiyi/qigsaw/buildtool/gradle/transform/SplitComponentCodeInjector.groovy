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

package com.iqiyi.qigsaw.buildtool.gradle.transform

import com.iqiyi.qigsaw.buildtool.gradle.internal.tool.ComponentInfo
import javassist.CannotCompileException
import javassist.CtClass
import javassist.NotFoundException

import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SplitComponentCodeInjector {

    Set<ComponentInfo> activities

    Set<ComponentInfo> services

    Set<ComponentInfo> receivers

    SplitComponentCodeInjector(Set<ComponentInfo> activities,
                               Set<ComponentInfo> services,
                               Set<ComponentInfo> receivers) {
        this.activities = activities
        this.services = services
        this.receivers = receivers
    }

    void injectCode(List<CtClass> box, File jarFile) throws CannotCompileException, IOException, NotFoundException {
        ZipOutputStream outputStream = new JarOutputStream(new FileOutputStream(jarFile))
        SplitActivityWeaver activityWeaver = new SplitActivityWeaver()
        SplitServiceWeaver serviceWeaver = new SplitServiceWeaver()
        SplitReceiverWeaver receiverWeaver = new SplitReceiverWeaver()
        for (CtClass ctClass : box) {
            String asmStyleClassName = ctClass.getName().replaceAll("\\.", "/")
            if (isActivity(ctClass)) {
                zipFile(activityWeaver.weave(ctClass.toBytecode()), outputStream, asmStyleClassName + ".class")
            } else if (isService(ctClass)) {
                zipFile(serviceWeaver.weave(ctClass.toBytecode()), outputStream, asmStyleClassName + ".class")
            } else if (isReceiver(ctClass)) {
                zipFile(receiverWeaver.weave(ctClass.toBytecode()), outputStream, asmStyleClassName + ".class")
            } else {
                zipFile(ctClass.toBytecode(), outputStream, ctClass.getName().replaceAll("\\.", "/") + ".class")
            }
        }
        outputStream.close()
    }

    boolean isActivity(CtClass ctClass) {
        boolean isActivity = false
        if (!activities.isEmpty()) {
            activities.each {
                if (it.name.contains(ctClass.name)) {
                    isActivity = true
                }
            }
        }
        return isActivity
    }

    boolean isService(CtClass ctClass) {
        boolean isService = false
        if (!services.isEmpty()) {
            services.each {
                if (it.name.contains(ctClass.name)) {
                    isService = true
                }
            }
        }
        return isService
    }

    boolean isReceiver(CtClass ctClass) {
        boolean isReceiver = false
        if (!receivers.isEmpty()) {
            receivers.each {
                if (it.name.contains(ctClass.name)) {
                    isReceiver = true
                }
            }
        }
        return isReceiver
    }

    static void zipFile(byte[] classBytesArray, ZipOutputStream zos, String entryName) {
        long time = System.currentTimeMillis()
        try {
            ZipEntry entry = new ZipEntry(entryName)
            zos.putNextEntry(entry)
            zos.write(classBytesArray, 0, classBytesArray.length)
            zos.closeEntry()
            zos.flush()
        } catch (Exception ex) {
            ex.printStackTrace()
        }
        println("qigsaw ------ cost ${System.currentTimeMillis() - time} to zip " + entryName)
    }
}
