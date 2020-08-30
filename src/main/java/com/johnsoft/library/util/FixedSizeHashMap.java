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
package com.johnsoft.library.util;

import java.util.LinkedHashMap;

/**
 * 固定大小的HashMap, 在Map尺寸大于构造时给定大小时, 使用LRU算法移除旧元素
 * @author John Kenrinus Lee
 * @version 2016-09-01
 */
public class FixedSizeHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int fixedSize;

    public FixedSizeHashMap(int maxSize) {
        this.fixedSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > fixedSize;
    }
}
