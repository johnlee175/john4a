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

import java.io.Closeable;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * A common sink which using nio and will pre-open a new file async so that swap file smooth.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public final class DiskBlockSliceSink implements Closeable {
    /**
     * A interface which will indicate how to define the file name if writing to file perhaps.
     * @author John Kenrinus Lee
     * @version 2016-02-18
     */
    public interface FilePathFactory {
        String getNextFilePath();
    }

    private final Object LOCK = new byte[0];

    private final long mFileFixSize;
    private final float mPreOpenThreshold;
    private final int mSyncPeriod;
    private final FilePathFactory mPathFactory;
    private final Handler mHandler;

    private FileChannel mFileChannel;
    private FileChannel mFileChannelBak;
    private boolean mIsPreOpenCalled;
    private boolean mHadPreOpened;
    private int mOffset;
    private int mSyncRemainingCount;

    /**
     * @param pFileFixSize the block file size, the minimum value is 1K;
     * @param pPreOpenThreshold a value between 0.0f(exclude) and 1.0f(exclude)
     *                          which indicate pre-open a new one file
     *                          when current file's remaining rate smaller than the threshold,
     *                          the default value is 0.2f;
     * @param pSyncPeriod flush or force frequency, the minimum value is 1,
     *                    flush or force after per-write;
     * @param pPathFactory see {@link FilePathFactory}, must not be null;
     */
    public DiskBlockSliceSink(long pFileFixSize, float pPreOpenThreshold, int pSyncPeriod,
                              FilePathFactory pPathFactory) {
        if (pFileFixSize > 1024L) {
            mFileFixSize = pFileFixSize;
        } else {
            mFileFixSize = 1024L;
            System.err.println("Using default FileFixSize.");
        }
        if (pPreOpenThreshold > 0.0F && pPreOpenThreshold < 1.0F) {
            mPreOpenThreshold = pPreOpenThreshold;
        } else {
            mPreOpenThreshold = 0.2F;
            System.err.println("Using default PreOpenThreshold.");
        }
        if (pSyncPeriod > 0) {
            mSyncPeriod = pSyncPeriod;
        } else {
            mSyncPeriod = 1;
            System.err.println("Using default SyncPeriod.");
        }
        if (pPathFactory == null) {
            throw new NullPointerException("DiskBlockSliceSink constructor need a FilePathFactory instance, "
                    + "but got null.");
        }
        mPathFactory = pPathFactory;
        HandlerThread handlerThread = new HandlerThread("DiskBlockSliceSink-SwapFile");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    public final void close() {
        synchronized(LOCK) {
            lastClose();
            mHandler.getLooper().quit();
        }
    }

    public final void write(byte[] bytesSrc) {
        write(ByteBuffer.wrap(bytesSrc, 0, bytesSrc.length));
    }

    public final void write(byte[] bytesSrc, int byteOffset, int byteCount) {
        write(ByteBuffer.wrap(bytesSrc, byteOffset, byteCount));
    }

    public final void write(ByteBuffer byteBufferSrc) {
        synchronized(LOCK) {
            if (mFileChannel == null) {
                firstOpen();
            }
            final long size = mFileFixSize - mOffset;
            if (size / (float) mFileFixSize <= mPreOpenThreshold && !mIsPreOpenCalled) {
                preOpen();
                mIsPreOpenCalled = true;
            }
            final int count = byteBufferSrc.remaining();
            if (count < size) {
                write0(byteBufferSrc, count);
                forceFlushSync();
            } else {
                close0();
                open0();
                write(byteBufferSrc);
            }
        }
    }

    /** request close current written file, and swap to a new one */
    public final void turnToNextFile() {
        synchronized(LOCK) {
            if (mHadPreOpened) {
                close0();
                open0();
            } else {
                lastClose();
                firstOpen();
            }
        }
    }

    private void write0(ByteBuffer byteBuffer, int remaining) {
        try {
            mFileChannel.write(byteBuffer);
            mOffset += remaining;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileChannel openFileChannel() {
        try {
            //9025.487 ± 53.311  ns/op
            //106592.926 ± 4086.916  ops/s
//            return new FileOutputStream(mPathFactory.getNextFilePath()).getChannel();
            //5074.877 ± 27.325  ns/op
            //199157.991 ± 8375.167  ops/s
            return new RandomAccessFile(mPathFactory.getNextFilePath(), "rwd").getChannel();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void firstOpen() {
        mFileChannel = openFileChannel();
        mOffset = 0;
        mSyncRemainingCount = 0;
    }

    private void preOpen() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mFileChannelBak = openFileChannel();
                mHadPreOpened = true;
            }
        });
    }

    private void open0() {
        while (!mHadPreOpened) {
            Thread.yield();
        }
        mHadPreOpened = false;
        mFileChannel = mFileChannelBak;
        mFileChannelBak = null;
        mOffset = 0;
        mSyncRemainingCount = 0;
        mIsPreOpenCalled = false;
    }

    private void close0() {
        final FileChannel tempChannel = mFileChannel;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tempChannel != null && tempChannel.isOpen()) {
                    try {
                        tempChannel.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private void lastClose() {
        if (mFileChannel != null && mFileChannel.isOpen()) {
            try {
                mFileChannel.close();
            } catch (Exception ignored) {
            }
            mFileChannel = null;
        }
    }

    private void forceFlushSync() {
        ++mSyncRemainingCount;
        if (mSyncRemainingCount >= mSyncPeriod) {
            try {
//                mFileChannel.force(true);
                mSyncRemainingCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
