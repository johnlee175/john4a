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
package com.johnsoft.library.template;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Simple Delayed object
 * @author John Kenrinus Lee
 * @version 2016-12-28
 */
public class DelayedItem<T> implements Delayed {
    private final long timeMillis;
    public final T item;

    /**
     * @param t portable composite object
     * @param timeMillis the delay time
     * @param hasBase if false, parameter timeMillis will be auto add the value of System.currentTimeMillis()
     */
    public DelayedItem(T t, long timeMillis, boolean hasBase) {
        this.item = t;
        this.timeMillis = hasBase ? timeMillis : (timeMillis + System.currentTimeMillis());
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(timeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        if (o instanceof DelayedItem) {
            if (this.timeMillis < ((DelayedItem) o).timeMillis) {
                return -1;
            } else if (this.timeMillis > ((DelayedItem) o).timeMillis) {
                return 1;
            } else {
                return 0;
            }
        }
        final long d = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return d == 0 ? 0 : d < 0 ? -1 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DelayedItem<?> delayedItem = (DelayedItem<?>) o;
        return item != null ? item.equals(delayedItem.item) : delayedItem.item == null;
    }

    @Override
    public int hashCode() {
        return item != null ? item.hashCode() : 0;
    }
}
