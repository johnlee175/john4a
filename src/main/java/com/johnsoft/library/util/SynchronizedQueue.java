package com.johnsoft.library.util;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步队列的一个实现, 区别于java类库中的BlockingQueue系列, 此类的阻塞是可中断的, 且仅在队列为空时阻塞;
 * 如果队列为满, 则移除当前的队列头(过时的数据)并在尾端加入新的数据, 类似循环队列;
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public class SynchronizedQueue<BUFFER> {
    private final Lock mLock;
    private final Condition mEmptyCondition;
    private final LinkedList<BUFFER> mQueue;
    private final int mCapacity;

    public SynchronizedQueue(int capacity) {
        mLock = new ReentrantLock(false);
        mEmptyCondition = mLock.newCondition();
        mQueue = new LinkedList<>();
        mCapacity = capacity;
    }

    public boolean isEmpty() throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            return mQueue.isEmpty();
        } finally {
            mLock.unlock();
        }
    }

    public boolean isFull() throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            return mCapacity == mQueue.size();
        } finally {
            mLock.unlock();
        }
    }

    /** 如果队列未满, 返回null, 否则返回队列头的那个数据 */
    public BUFFER enqueue(BUFFER buffer) throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            if (buffer == null) {
                throw new NullPointerException("call enqueue() with null buffer in SynchronizedQueue.");
            }
            BUFFER oldResult = null;
            if (isFull()) {
                oldResult = mQueue.removeFirst();
            }
            mQueue.addLast(buffer);
            mEmptyCondition.signalAll();
            return oldResult;
        } finally {
            mLock.unlock();
        }
    }

    public BUFFER dequeue() throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            while (isEmpty()) {
                mEmptyCondition.await();
            }
            return mQueue.removeFirst();
        } finally {
            mLock.unlock();
        }
    }

    public BUFFER peek() throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            return mQueue.peekFirst();
        } finally {
            mLock.unlock();
        }
    }

    public void clear() throws InterruptedException {
        try {
            mLock.lockInterruptibly();
            mQueue.clear();
        } finally {
            mLock.unlock();
        }
    }
}
