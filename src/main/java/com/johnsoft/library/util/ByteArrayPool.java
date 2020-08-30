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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 以模拟Stack栈方式实现的byte[]对象池, 并配以定期销毁长时间未用的多余对象的功能
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public final class ByteArrayPool {
    private final Object LOCK = new byte[0];
    private final LinkedList<Entry> mStack;
    private final CleanupThread mCleanupThread;
    private final int mPoolCapacity;
    private final int mBufferSize;
    private final long mCleanPeriod;

    /**
     * 如果参数设置错误, 将采用默认值
     * @param poolCapacity 池大小, 默认值是16
     * @param bufferSize 每个byte[]的长度, 默认值是4k
     * @param cleanPeriod 清理周期, 毫秒计, 默认值是1分钟
     */
    public ByteArrayPool(int poolCapacity, int bufferSize, long cleanPeriod) {
        if (poolCapacity > 1 && poolCapacity <= 1024) {
            mPoolCapacity = poolCapacity;
        } else {
            System.err.println("Error set poolCapacity reason: poolCapacity <= 1 or poolCapacity > 1024.");
            mPoolCapacity = 16;
        }
        if (bufferSize >= 8) {
            mBufferSize = bufferSize;
        } else {
            System.err.println("Error set bufferSize reason: bufferSize < 8.");
            mBufferSize = 4096;
        }
        if (cleanPeriod >= 1000L) {
            mCleanPeriod = cleanPeriod;
        } else {
            System.err.println("Error set cleanPeriod reason: cleanPeriod < 1000.");
            mCleanPeriod = 60000L;
        }
        mStack = new LinkedList<>();
        mCleanupThread = new CleanupThread();
        mCleanupThread.start();
    }

    public boolean isEmpty() {
        synchronized(LOCK) {
            return mStack.isEmpty();
        }
    }

    public boolean isFull() {
        synchronized(LOCK) {
            return mPoolCapacity == mStack.size();
        }
    }

    /** 复用池中对象, 当池中没有对象时, 分配一个新的字节数组 */
    public byte[] obtain() {
        if (isEmpty()) {
            return new byte[mBufferSize];
        } else {
            synchronized(LOCK) {
                return mStack.removeFirst().buffer;
            }
        }
    }

    /** 回收对象, 当池满了时, 直接丢弃以参数方式传入的bytes, 此时返回false, 否则返回true, 表示回收成功 */
    public boolean recycle(byte[] bytes) {
        if (!isFull()) {
            Entry entry = new Entry(bytes, System.currentTimeMillis());
            synchronized(LOCK) {
                mStack.addFirst(entry);
            }
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
        mCleanupThread.interrupt();
        try {
            mCleanupThread.join(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(LOCK) {
            mStack.clear();
        }
    }

    private class Entry {
        final byte[] buffer;
        final long lastUsedTime;

        Entry(byte[] buffer, long lastUsedTime) {
            if (buffer == null) {
                throw new NullPointerException("Create ByteArrayPool.Entry with null byte[]");
            }
            this.buffer = buffer;
            this.lastUsedTime = lastUsedTime;
        }
    }

    private class CleanupThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized(LOCK) {
                    final long currentTime = System.currentTimeMillis();
                    // 使用倒序不完全遍历(从栈底开始), 因为越是靠近栈顶越是常用,
                    // 当发现超过一定时间未用的池对象时则移除回收, 直到发现那个非超时的即可停止
                    final Iterator<Entry> descendingIterator = mStack.descendingIterator();
                    while (descendingIterator.hasNext()) {
                        if (isInterrupted()) {
                            break;
                        } else {
                            Entry entry = descendingIterator.next();
                            if (currentTime - entry.lastUsedTime > mCleanPeriod) {
                                descendingIterator.remove();
                            } else {
                                break;
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(mCleanPeriod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}