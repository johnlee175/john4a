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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * A common sink which using nio mapped file, it will pre-open a new one async so that swap file smooth.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 * @Note 未解决一个问题, map一块大内存后, 文件尺寸固定在大内存大小, 尽管写入一小戳数据,
 *       在close时调用setLength可变为写入数据的大小, 但异常终止可能还未来得及调用close;
 *       另一个问题是MappedByteBuffer的内存回收问题;
 */
@Deprecated
public final class DiskBlockSliceSink2 implements Closeable {
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

    private RandomAccessFile mRandomAccessFile;
    private FileChannel mFileChannel;
    private MappedByteBuffer mMappedByteBuffer;
    private RandomAccessFile mRandomAccessFileBak;
    private FileChannel mFileChannelBak;
    private MappedByteBuffer mMappedByteBufferBak;
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
    public DiskBlockSliceSink2(long pFileFixSize, float pPreOpenThreshold, int pSyncPeriod,
                               FilePathFactory pPathFactory) {
        if (pFileFixSize > 1024L) {
            mFileFixSize = pFileFixSize;
        } else {
            mFileFixSize = 1024L;
        }
        if (pPreOpenThreshold > 0.0F && pPreOpenThreshold < 1.0F) {
            mPreOpenThreshold = pPreOpenThreshold;
        } else {
            mPreOpenThreshold = 0.2F;
        }
        if (pSyncPeriod > 0) {
            mSyncPeriod = pSyncPeriod;
        } else {
            mSyncPeriod = 1;
        }
        if (pPathFactory == null) {
            throw new NullPointerException("DiskBlockSliceSink2 constructor need a FilePathFactory instance, "
                    + "but got null.");
        }
        mPathFactory = pPathFactory;
        HandlerThread handlerThread = new HandlerThread("DiskBlockSliceSink2-SwapFile");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    public final void close() {
        synchronized(LOCK) {
            lastClose();
        }
        mHandler.getLooper().quit();
    }

    public final void write(byte[] bytesSrc, int byteOffset, int byteCount) {
        synchronized(LOCK) {
            if (mFileChannel == null) {
                firstOpen();
            }
            final int size = mMappedByteBuffer.remaining();
            if (size / (float) mFileFixSize <= mPreOpenThreshold && !mIsPreOpenCalled) {
                preOpen();
                mIsPreOpenCalled = true;
            }
            if (byteCount < size) {
                mMappedByteBuffer.put(bytesSrc, byteOffset, byteCount);
                mOffset += byteCount;
                forceFlush();
            } else {
                close0();
                open0();
                write(bytesSrc, byteOffset, byteCount);
            }
        }
    }

    public final void write(ByteBuffer byteBufferSrc) {
        synchronized(LOCK) {
            if (mFileChannel == null) {
                firstOpen();
            }
            final int size = mMappedByteBuffer.remaining();
            if (size / (float) mFileFixSize <= mPreOpenThreshold && !mIsPreOpenCalled) {
                preOpen();
                mIsPreOpenCalled = true;
            }
            final int count = byteBufferSrc.remaining();
            if (count < size) {
                mMappedByteBuffer.put(byteBufferSrc);
                mOffset += count;
                forceFlush();
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

    private void firstOpen() {
        try {
            mRandomAccessFile = new RandomAccessFile(mPathFactory.getNextFilePath(), "rwd");
            mFileChannel = mRandomAccessFile.getChannel();
            mMappedByteBuffer = mFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mFileFixSize);
            mMappedByteBuffer.order(ByteOrder.nativeOrder());
            mOffset = 0;
            mSyncRemainingCount = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void preOpen() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mRandomAccessFileBak = new RandomAccessFile(mPathFactory.getNextFilePath(), "rwd");
                    mFileChannelBak = mRandomAccessFileBak.getChannel();
                    mMappedByteBufferBak = mFileChannelBak.map(FileChannel.MapMode.READ_WRITE, 0, mFileFixSize);
                    mMappedByteBufferBak.order(ByteOrder.nativeOrder());
                    mHadPreOpened = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void open0() {
        while (!mHadPreOpened) {
            Thread.yield();
        }
        mHadPreOpened = false;
        mRandomAccessFile = mRandomAccessFileBak;
        mFileChannel = mFileChannelBak;
        mMappedByteBuffer = mMappedByteBufferBak;
        mRandomAccessFileBak = null;
        mFileChannelBak = null;
        mMappedByteBufferBak = null;
        mOffset = 0;
        mSyncRemainingCount = 0;
        mIsPreOpenCalled = false;
    }

    private void close0() {
        final RandomAccessFile tempFile = mRandomAccessFile;
        final FileChannel tempChannel = mFileChannel;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tempChannel != null && tempChannel.isOpen()) {
                    try {
                        tempFile.setLength(mOffset);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            tempChannel.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        });
    }

    private void lastClose() {
        if (mFileChannel != null && mFileChannel.isOpen()) {
            try {
                mRandomAccessFile.setLength(mOffset);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mFileChannel.close();
                } catch (IOException ignored) {
                }
            }
            mFileChannel = null;
        }
    }

    private void forceFlush() {
        ++mSyncRemainingCount;
        if (mSyncRemainingCount >= mSyncPeriod) {
            mMappedByteBuffer.force();
            mSyncRemainingCount = 0;
        }
    }
}
