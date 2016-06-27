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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author John Kenrinus Lee
 * @version 2016-05-20
 */
public final class SharedReference {
    private final AtomicInteger refCount = new AtomicInteger(0);
    private final Runnable createRunnable;
    private final Runnable destroyRunnable;

    public SharedReference(Runnable createRunnable, Runnable destroyRunnable) {
        this.createRunnable = createRunnable;
        this.destroyRunnable = destroyRunnable;
    }

    public void addReference() {
        if (refCount.getAndIncrement() == 0 && createRunnable != null) {
            createRunnable.run();
        }
    }

    public void reduceReference() {
        final int currCount = refCount.decrementAndGet();
        if (currCount == 0 && destroyRunnable != null) {
            destroyRunnable.run();
        }
        if (currCount < 0) {
            refCount.set(0);
            throw new IllegalStateException("There is no reference to reduce.");
        }
    }

    public void reduceReferenceOrNoOp() {
        int currCount = refCount.get();
        if (currCount > 0) {
            currCount = refCount.decrementAndGet();
            if (currCount == 0 && destroyRunnable != null) {
                destroyRunnable.run();
            }
        }
    }

    public int getReferenceCount() {
        return refCount.get();
    }
}
