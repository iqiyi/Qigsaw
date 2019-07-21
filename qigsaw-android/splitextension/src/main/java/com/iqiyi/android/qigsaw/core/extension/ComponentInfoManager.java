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

package com.iqiyi.android.qigsaw.core.extension;

import java.lang.reflect.Field;

final class ComponentInfoManager {

    /**
     * ComponentInfo is created by qigsaw-gradlew-plugin automatically.
     */
    private static final String CLASS_ComponentInfo = "com.iqiyi.android.qigsaw.core.extension.ComponentInfo";

    private static final String ACTIVITIES_SUFFIX = "_ACTIVITIES";

    private static final String SERVICES_SUFFIX = "_SERVICES";

    private static final String RECEIVERS_SUFFIX = "_RECEIVERS";

    private static final String APPLICATION_SUFFIX = "_APPLICATION";

    private static Class getComponentInfoClass() throws ClassNotFoundException {
        return Class.forName(CLASS_ComponentInfo);
    }

    static String getSplitApplication(String splitName) {
        String fieldName = splitName + APPLICATION_SUFFIX;
        try {
            Field field = getComponentInfoClass().getField(fieldName);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String[] getSplitActivities(String splitName) {
        String fieldName = splitName + ACTIVITIES_SUFFIX;
        try {
            Field field = getComponentInfoClass().getField(fieldName);
            field.setAccessible(true);
            String result = (String) field.get(null);
            if (result != null) {
                return result.split(",");
            }
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String[] getSplitServices(String splitName) {
        String fieldName = splitName + SERVICES_SUFFIX;
        try {
            Field field = getComponentInfoClass().getField(fieldName);
            field.setAccessible(true);
            String result = (String) field.get(null);
            if (result != null) {
                return result.split(",");
            }
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String[] getSplitReceivers(String splitName) {
        String fieldName = splitName + RECEIVERS_SUFFIX;
        try {
            Field field = getComponentInfoClass().getField(fieldName);
            field.setAccessible(true);
            String result = (String) field.get(null);
            if (result != null) {
                return result.split(",");
            }
        } catch (NoSuchFieldException e) {
            //
        } catch (IllegalAccessException e) {
            //
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
