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

package com.iqiyi.qigsaw.buildtool.gradle.internal.tool

import com.google.gson.Gson

class TypeClassFileParser {

    static Object parseFile(File file, Class typeClass) {
        String str = readInputStreamContent(new FileInputStream(file))
        return parseTypeClass(str, typeClass)
    }

    private static String readInputStreamContent(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is))
        StringBuilder stringBuffer = new StringBuilder()
        String str
        while ((str = br.readLine()) != null) {
            stringBuffer.append(str)
        }
        FileUtils.closeQuietly(is)
        FileUtils.closeQuietly(br)
        return stringBuffer.toString()
    }

    private static Object parseTypeClass(String splitDetailsStr, Class typeClass) {
        Gson gson = new Gson()
        return gson.fromJson(splitDetailsStr, typeClass)
    }
}
