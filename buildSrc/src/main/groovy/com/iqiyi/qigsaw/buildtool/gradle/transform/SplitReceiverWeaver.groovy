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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import static org.objectweb.asm.Opcodes.ALOAD
import static org.objectweb.asm.Opcodes.INVOKESTATIC

class SplitReceiverWeaver implements SplitComponentWeaver {

    @Override
    byte[] weave(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor cv = new ReceiverClassVisitor(Opcodes.ASM5, cw)
        cr.accept(cv, Opcodes.ASM5)
        return cw.toByteArray()
    }

    static class ReceiverClassVisitor extends ClassVisitor {

        ReceiverClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("onReceive".equals(name)) {
                return new ChangeOnReceiveMethodVisitor(Opcodes.ASM5,
                        cv.visitMethod(access, name, desc, signature, exceptions))
            }
            return super.visitMethod(access, name, desc, signature, exceptions)
        }
    }


    static class ChangeOnReceiveMethodVisitor extends MethodVisitor {

        ChangeOnReceiveMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv)
        }

        @Override
        void visitCode() {
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(INVOKESTATIC, CLASS_WOVEN, METHOD_WOVEN,
                    "(Landroid/content/BroadcastReceiver;Landroid/content/Context;)V", false)
            super.visitCode()
        }

    }

}
