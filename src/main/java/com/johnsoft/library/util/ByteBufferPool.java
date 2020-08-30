package com.johnsoft.library.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 以模拟Stack栈方式实现的ByteBuffer对象池, 并配以定期销毁长时间未用的多余对象的功能. <br>
 * 注: 采用Stack栈, 频繁的obtain和recycle操作会对线程同步有比较高的要求,
 * 但之所以采用Stack栈而非队列, 主要是方便快速清理多余对象, 因为清理工作其实也要与obtain和recycle操作同步,
 * 清理时间越长, 性能的影响也越大, 所以很关键的考虑因素是这个对象是否占用资源和创建成本较高,
 * 如果占用资源不大, 进而清理需求不大, 并发要求比较高, 可以考虑采用队列替代栈.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public final class ByteBufferPool {
    private final byte[] LOCK = new byte[0];
    private final LinkedList<Entry> mStack;
    private final CleanupThread mCleanupThread;
    private final int mPoolCapacity;
    private final int mBufferSize;
    private final long mCleanPeriod;
    private boolean isDestroyed;

    /**
     * 如果参数设置错误, 将采用默认值
     * @param poolCapacity 池大小, 默认值是16
     * @param bufferSize 每个ByteBuffer的容量, 默认值是4k
     * @param cleanPeriod 清理周期, 毫秒计, 默认值是1分钟
     */
    public ByteBufferPool(int poolCapacity, int bufferSize, long cleanPeriod) {
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
            if (isDestroyed) {
                throw new IllegalStateException("Byte buffer pool had destroyed");
            }
            return mStack.isEmpty();
        }
    }

    public boolean isFull() {
        synchronized(LOCK) {
            if (isDestroyed) {
                throw new IllegalStateException("Byte buffer pool had destroyed");
            }
            return mPoolCapacity == mStack.size();
        }
    }

    public boolean isDestroyed() {
        synchronized(LOCK) {
            return isDestroyed;
        }
    }

    /** 复用池中对象, 当池中没有对象时, 分配一个本地字节序的非Direct的非wrap的ByteBuffer */
    public ByteBuffer obtain() {
        synchronized(LOCK) {
            if (isEmpty()) {
                return ByteBuffer.allocate(mBufferSize).order(ByteOrder.nativeOrder());
            } else {
                return mStack.removeFirst().buffer;
            }
        }
    }

    /** 回收对象, 当池满了时, 或重复归还时, 直接丢弃以参数方式传入的byteBuffer, 此时返回false, 否则返回true, 表示回收成功 */
    public boolean recycle(ByteBuffer byteBuffer) {
        synchronized(LOCK) {
            if (!isFull()) {
                // 判断是否重复归还
                for (Entry entry : mStack) {
                    if (entry != null && entry.buffer == byteBuffer) {
                        return false;
                    }
                }
                byteBuffer.clear();
                Entry entry = new Entry(byteBuffer, System.currentTimeMillis());
                mStack.addFirst(entry);
                return true;
            } else {
                return false;
            }
        }
    }

    public void destroy() {
        synchronized(LOCK) {
            if (!isDestroyed) {
                isDestroyed = true;
            } else {
                return;
            }
        }
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

    private static class Entry {
        final ByteBuffer buffer;
        final long lastUsedTime;

        Entry(ByteBuffer buffer, long lastUsedTime) {
            if (buffer == null) {
                throw new NullPointerException("Create ByteBufferPool.Entry with null ByteBuffer");
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
                    if (isDestroyed) {
                        return;
                    }
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