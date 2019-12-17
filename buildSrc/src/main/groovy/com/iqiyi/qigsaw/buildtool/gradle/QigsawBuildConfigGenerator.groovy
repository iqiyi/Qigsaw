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

package com.iqiyi.qigsaw.buildtool.gradle

import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.builder.internal.ClassFieldImpl
import org.gradle.api.Task

import java.lang.reflect.Field
import java.util.function.Supplier

class QigsawBuildConfigGenerator {

    Task generateBuildConfigTask

    private Map<String, Object> injectedFieldMap = new HashMap<>()

    QigsawBuildConfigGenerator(Task generateBuildConfigTask) {
        this.generateBuildConfigTask = generateBuildConfigTask
    }

    void injectFields(String key, String value) {
        if (injectedFieldMap.getAt(key) != null) {
            return
        }
        injectedFieldMap.put(key, value)
        Field field = GenerateBuildConfig.class.getDeclaredField("items")
        field.setAccessible(true)
        Object itemsObj = field.get(generateBuildConfigTask)
        Supplier<List<Object>> items = (Supplier<List<Object>>) itemsObj
        items.get().add(new ClassFieldImpl(String.class.name, key, "\"" + value + "\""))
    }

    void injectFields(String key, List<String> value) {
        if (injectedFieldMap.getAt(key) != null) {
            return
        }
        injectedFieldMap.put(key, value)
        Field field = GenerateBuildConfig.class.getDeclaredField("items")
        field.setAccessible(true)
        Object itemsObj = field.get(generateBuildConfigTask)
        Supplier<List<Object>> items = (Supplier<List<Object>>) itemsObj
        if (!value.empty) {
            List<String> tempList = new ArrayList<>()
            for (String str : value) {
                tempList.add("\"" + str + "\"")
            }
            items.get().add(new ClassFieldImpl("String[]", key, "{" + tempList.join(",") + "}"))
        }
    }
}
