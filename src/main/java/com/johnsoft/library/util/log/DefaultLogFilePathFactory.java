/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.util.log;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.johnsoft.library.annotation.ThreadSafe;
import com.johnsoft.library.util.ConcurrentDateFormat;

/**
 * @author John Kenrinus Lee
 * @version 2015-10-30
 * @see LogFilePathFactory
 */
@ThreadSafe
public class DefaultLogFilePathFactory implements LogFilePathFactory {
    protected final AtomicInteger mLastFileIndex = new AtomicInteger(0);
    protected final String mDirPath;
    protected final String mPrefix;

    /**
     * @param dir where the log files store, a good strategy is only create one folder on sdcard,
     *            and all log files store there.
     * @param prefix which mark id,
     *            example TelephonyManager.getDeviceId(), Build.SERIAL, mac address or UUID created self for unique.
     */
    public DefaultLogFilePathFactory(File dir, String prefix) {
        mDirPath = dir.getAbsolutePath();
        mPrefix = prefix;
    }

    @Override
    public String getNextFilePath() {
        String middleTime = ConcurrentDateFormat.getUnsigned().format(new Date());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mDirPath).append(File.pathSeparator).append(mPrefix)
                .append(".").append(middleTime).append(".")
                .append(mLastFileIndex.incrementAndGet());
        return stringBuilder.toString();
    }
}