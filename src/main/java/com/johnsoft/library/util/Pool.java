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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * a simple pool
 *
 * @author John Kenrinus Lee
 * @version 2014-06-01
 */
public abstract class Pool<T> {
    /**
     * Each object created will be in list, because of we need destroy all object we created.
     */
    private List<T> allCreatedObjectList;
    /**
     * Indicate which objects are being used, after using will be removed from set.
     */
    private Set<T> usingSet;
    private int maxSize;

    /**
     * @param maxSize indicate how many T object live
     */
    public Pool(int maxSize) {
        this.maxSize = maxSize;
        allCreatedObjectList = new ArrayList<T>(maxSize);
        usingSet = new HashSet<T>(maxSize);
    }

    /**
     * get a object from pool, need call {@link #unuse(Object)} after using.
     *
     * @see #unuse(Object)
     */
    public final synchronized T use() {
        for (int i = 0; i != allCreatedObjectList.size(); ++i) {
            T t = allCreatedObjectList.get(i);
            // objects in set are being used, objects in list are created for usable,
            // so idle objects are in list but not in set.
            if (!usingSet.contains(t) && t != null) {
                usingSet.add(t);
                return t;
            }
        }
        if (allCreatedObjectList.size() <= maxSize) {
            T t = create();
            allCreatedObjectList.add(t);
            usingSet.add(t);
            return t;
        } else {
            if (usingSet.size() >= maxSize) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i != allCreatedObjectList.size(); ++i) {
                T t = allCreatedObjectList.get(i);
                // objects in set are being used, objects in list are created for usable,
                // so idle objects are in list but not in set.
                if (!usingSet.contains(t) && t != null) {
                    usingSet.add(t);
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * give back object to pool when after using
     */
    public final synchronized void unuse(T t) {
        usingSet.remove(t);
        notifyAll();
    }

    /**
     * you call this method just when you discard the pool
     */
    public final synchronized void destory() {
        usingSet.clear();
        for (T t : allCreatedObjectList) {
            release(t);
        }
        allCreatedObjectList.clear();
        usingSet = null;
        allCreatedObjectList = null;
    }

    /**
     * indicate how the T object create
     */
    protected abstract T create();

    /**
     * indicate how the T object destruct
     */
    protected abstract void release(T t);
}