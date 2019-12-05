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

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import static org.objectweb.asm.Opcodes.ACC_PUBLIC
import static org.objectweb.asm.Opcodes.ALOAD
import static org.objectweb.asm.Opcodes.INVOKESPECIAL
import static org.objectweb.asm.Opcodes.INVOKESTATIC
import static org.objectweb.asm.Opcodes.RETURN

class SplitServiceWeaver implements SplitComponentWeaver {

    @Override
    byte[] weave(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor cv = new ServiceClassVisitor(Opcodes.ASM5, cw)
        cr.accept(cv, Opcodes.ASM5)
        return cw.toByteArray()
    }

    static class ServiceClassVisitor extends ClassVisitor {

        boolean needInsert = true

        String superClassName

        ServiceClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            this.superClassName = superName
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("onCreate".equals(name)) {
                needInsert = false
                return new ChangeOnCreateMethodVisitor(Opcodes.ASM5,
                        cv.visitMethod(access, name, desc, signature, exceptions))
            }
            return super.visitMethod(access, name, desc, signature, exceptions)
        }

        @Override
        void visitEnd() {
            if (needInsert) {
                insertOnCreateMethod(cv)
            }
            super.visitEnd()
        }

        void insertOnCreateMethod(ClassWriter cw) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "onCreate", "()V", null, null)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, superClassName, "onCreate", "()V", false)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESTATIC, CLASS_WOVEN, METHOD_WOVEN, "(Landroid/app/Service;)V", false)
            mv.visitInsn(RETURN)
            mv.visitMaxs(1, 1)
            mv.visitEnd()
        }

    }

    static class ChangeOnCreateMethodVisitor extends MethodVisitor {

        ChangeOnCreateMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return super.visitAnnotation(desc, visible)
        }

        @Override
        void visitCode() {
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESTATIC, CLASS_WOVEN, METHOD_WOVEN, "(Landroid/app/Service;)V", false)
            super.visitCode()
        }

    }

}
