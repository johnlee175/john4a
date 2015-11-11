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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The log file manager, for manage info like current log files, which log file closing.
 * @author John Kenrinus Lee
 * @version 2015-11-09
 */
public final class LogFileManager {
    private static final ConcurrentHashMap<String, String> sTypePathMap = new ConcurrentHashMap<>();

    /**
     * put manage info data, just use for the log framework
     * @param type the class name of log which are using
     * @param path the path of the current log file which are using
     */
    static void putTypePath(String type, String path) {
        sTypePathMap.put(type, path);
    }

    /**
     * @return if enable log to file, it indicate which file start logging now, you should use the result readonly.
     */
    public static Collection<String> getCurrentPaths() {
        return sTypePathMap.values();
    }
}
