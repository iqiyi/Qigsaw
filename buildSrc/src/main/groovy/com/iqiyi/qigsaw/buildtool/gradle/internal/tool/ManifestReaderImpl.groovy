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

import com.google.common.collect.ImmutableSet
import com.iqiyi.qigsaw.buildtool.gradle.internal.entity.ComponentInfo
import com.iqiyi.qigsaw.buildtool.gradle.internal.model.ManifestReader

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
    Set<ComponentInfo> readActivities() {
        ImmutableSet.Builder activities = ImmutableSet.builder()
        manifest.application.activity.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            activities.add(new ComponentInfo(name, process))
        }
        return activities.build()
    }

    @Override
    Set<ComponentInfo> readServices() {
        ImmutableSet.Builder services = ImmutableSet.builder()
        manifest.application.service.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            services.add(new ComponentInfo(name, process))
        }
        return services.build()
    }

    @Override
    Set<ComponentInfo> readReceivers() {
        ImmutableSet.Builder receivers = ImmutableSet.builder()
        manifest.application.receiver.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            receivers.add(new ComponentInfo(name, process))
        }
        return receivers.build()
    }

    @Override
    Set<ComponentInfo> readProviders() {
        ImmutableSet.Builder providers = ImmutableSet.builder()
        manifest.application.provider.each {
            String name = it.'@android:name'.toString()
            String process = it.'@android:process'.toString()
            providers.add(new ComponentInfo(name, process))
        }
        return providers.build()
    }

    @Override
    boolean readOnDemand() {
        return Boolean.valueOf(manifest.module.'@dist:onDemand'.toString())
    }
}
