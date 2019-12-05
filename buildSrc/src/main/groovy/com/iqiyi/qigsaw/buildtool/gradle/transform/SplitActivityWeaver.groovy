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
import static org.objectweb.asm.Opcodes.INVOKESPECIAL
import static org.objectweb.asm.Opcodes.INVOKESTATIC

class SplitActivityWeaver implements SplitComponentWeaver {

    @Override
    byte[] weave(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor cv = new ActivityClassVisitor(Opcodes.ASM5, cw)
        cr.accept(cv, Opcodes.ASM5)
        return cw.toByteArray()
    }

    static class ActivityClassVisitor extends ClassVisitor implements Opcodes {

        String superName

        boolean needInsert = true

        ActivityClassVisitor(int api, ClassVisitor cv) {
            super(api, cv)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            this.superName = superName
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("getResources".equals(name)) {
                needInsert = false
                return new ChangeOnCreateMethodVisitor(Opcodes.ASM5, cv.visitMethod(access, name, desc, signature, exceptions), superName)
            }
            return super.visitMethod(access, name, desc, signature, exceptions)
        }

        @Override
        void visitEnd() {
            if (needInsert) {
                insertGetResourcesMethod(cv)
            }
            super.visitEnd()
        }

        void insertGetResourcesMethod(ClassWriter cw) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getResources", "()Landroid/content/res/Resources;", null, null)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, superName, "getResources", "()Landroid/content/res/Resources;", false)
            mv.visitMethodInsn(INVOKESTATIC, CLASS_WOVEN, METHOD_WOVEN, "(Landroid/app/Activity;Landroid/content/res/Resources;)V", false)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, superName, "getResources", "()Landroid/content/res/Resources;", false)
            mv.visitInsn(ARETURN)
            mv.visitMaxs(2, 1)
            mv.visitEnd()
        }
    }

    static class ChangeOnCreateMethodVisitor extends MethodVisitor {

        String superClassName

        ChangeOnCreateMethodVisitor(int api, MethodVisitor mv, String superClassName) {
            super(api, mv)
            this.superClassName = superClassName
        }

        @Override
        void visitCode() {
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, superClassName, "getResources", "()Landroid/content/res/Resources;", false)
            mv.visitMethodInsn(INVOKESTATIC, CLASS_WOVEN, METHOD_WOVEN, "(Landroid/app/Activity;Landroid/content/res/Resources;)V", false)
            super.visitCode()
        }
    }
}
