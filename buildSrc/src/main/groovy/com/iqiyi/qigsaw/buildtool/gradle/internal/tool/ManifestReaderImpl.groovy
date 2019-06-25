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

class ManifestReaderImpl implements ManifestReader {

    def manifest

    ManifestReaderImpl(File manifest) {
        this.manifest = new XmlSlurper().parse(manifest)
    }

    @Override

    ComponentInfo readApplicationName() {
        String name = manifest.application.'@android:name'
        return new ComponentInfo(name)
    }

    @Override
    String readPackageName() {
        return manifest.'@package'
    }

    @Override
    String readVersionCode() {
        return manifest.'@android:versionCode'
    }

    @Override
    String readVersionName() {
        return manifest.'@android:versionName'
    }

    @Override
    List<ComponentInfo> readActivities() {
        List<ComponentInfo> activities = new ArrayList<>()
        manifest.application.activity.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            activities.add(new ComponentInfo(name, process))
        }
        return activities
    }

    @Override
    List<ComponentInfo> readServices() {
        List<ComponentInfo> services = new ArrayList<>()
        manifest.application.service.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            services.add(new ComponentInfo(name, process))
        }
        return services
    }

    @Override
    List<ComponentInfo> readReceivers() {
        List<ComponentInfo> receivers = new ArrayList<>()
        manifest.application.receiver.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            receivers.add(new ComponentInfo(name, process))
        }
        return receivers
    }

    @Override
    List<ComponentInfo> readProviders() {
        List<ComponentInfo> providers = new ArrayList<>()
        manifest.application.provider.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            providers.add(new ComponentInfo(name, process))
        }
        return providers
    }

    @Override
    boolean readOnDemand() {
        return Boolean.valueOf(manifest.module.'@dist:onDemand'.toString())
    }
}
