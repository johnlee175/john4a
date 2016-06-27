package com.johnsoft.library.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A common sink which using nio and will pre-open a new file async so that swap file smooth.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public final class DiskBlockSliceSink implements Closeable {
    private final byte[] LOCK = new byte[0];

    private final long mFileFixSize;
    private final float mPreOpenThreshold;
    private final int mSyncPeriod;
    private final FilePathFactory mPathFactory;
    private final ExecutorService mSingleAsyncThread;

    private PathFileChannel mFileChannel;
    private PathFileChannel mFileChannelBak;
    private Future<?> mFuture;
    private volatile boolean mIsPreOpenCalled;
    private volatile boolean mHadPreOpened;
    private volatile boolean mIsSinkOpen;
    private int mOffset;
    private int mSyncRemainingCount;

    private final List<FileSwitchListener> mListeners;
    private final ExecutorService mFireListenersThread;

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
        mSingleAsyncThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameableThreadFactory("DiskBlockSliceSink-SwapFile"));
        mIsSinkOpen = true;

        mListeners = Collections.synchronizedList(new ArrayList<FileSwitchListener>());
        mFireListenersThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameableThreadFactory("DiskBlockSliceSink-FireListeners"));
    }

    public final void close() {
        synchronized(LOCK) {
            if (mIsSinkOpen) {
                lastClose(true);
                mSingleAsyncThread.shutdownNow();
                mFireListenersThread.shutdownNow();
                mIsSinkOpen = false;
            }
        }
    }

    public final void write(byte[] bytesSrc) throws IOException {
        write(ByteBuffer.wrap(bytesSrc, 0, bytesSrc.length));
    }

    public final void write(byte[] bytesSrc, int byteOffset, int byteCount)
            throws IOException {
        write(ByteBuffer.wrap(bytesSrc, byteOffset, byteCount));
    }

    // the core method
    public final void write(ByteBuffer byteBufferSrc) throws IOException {
        synchronized(LOCK) {
            checkSinkOpen();
            if (mFileChannel == null) {
                firstOpen();
                if (mFileChannel == null) {
                    throw new IOException("DiskBlockSliceSink.mFileChannel is still null.");
                }
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
    public final void turnToNextFile() throws IOException {
        synchronized(LOCK) {
            checkSinkOpen();
            if (mHadPreOpened) {
                close0();
                open0();
            } else {
                lastClose(false);
                firstOpen();
            }
        }
    }

    private void write0(ByteBuffer byteBuffer, int remaining) throws IOException {
        mFileChannel.write(byteBuffer);
        mOffset += remaining;
    }

    private PathFileChannel openFileChannel() throws IOException {
        //9025.487 ± 53.311  ns/op
        //106592.926 ± 4086.916  ops/s
//        return new FileOutputStream(mPathFactory.getNextFilePath()).getChannel();
        //5074.877 ± 27.325  ns/op
        //199157.991 ± 8375.167  ops/s
        final String filePath = mPathFactory.getNextFilePath();
        return new PathFileChannel(new RandomAccessFile(filePath, "rwd").getChannel(), filePath);
    }

    private void firstOpen() throws IOException {
        mFileChannel = openFileChannel();
        mOffset = 0;
        mSyncRemainingCount = 0;
    }

    private void preOpen() throws IOException {
        mFuture = mSingleAsyncThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mFileChannelBak = openFileChannel();
                    mHadPreOpened = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void open0() throws IOException {
        if (mFuture != null) {
            try {
                mFuture.get(5000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                Throwable tr = e;
                while (tr.getCause() != null) {
                    tr = tr.getCause();
                }
                throw new IOException(tr);
            }
        }
        if (!mHadPreOpened) {
            throw new IllegalStateException("Pre-open file failed.");
        }
//        while (!mHadPreOpened) {
//            Thread.yield();
//        }
        mHadPreOpened = false;
        mFileChannel = mFileChannelBak;
        mFileChannelBak = null;
        mOffset = 0;
        mSyncRemainingCount = 0;
        mIsPreOpenCalled = false;
    }

    private void close0() {
        final PathFileChannel tempChannel = mFileChannel;
        mSingleAsyncThread.submit(new Runnable() {
            @Override
            public void run() {
                if (tempChannel != null && tempChannel.isOpen()) {
                    try {
                        tempChannel.close();
                        fireFileClosed(tempChannel.toString());
                    } catch (IOException ignored) {
                    }
                }
            }
        });
    }

    private void lastClose(boolean cleanBak) {
        if (mFileChannel != null && mFileChannel.isOpen()) {
            try {
                mFileChannel.close();
                fireFileClosed(mFileChannel.toString());
            } catch (IOException ignored) {
            }
            mFileChannel = null;
        }
        if (cleanBak && mFileChannelBak != null && mFileChannelBak.isOpen()) {
            try {
                mFileChannelBak.close();
            } catch (IOException ignored) {
            }
            mFileChannelBak = null;
        }
    }

    private void checkSinkOpen() throws IOException {
        if (!mIsSinkOpen) {
            throw new IOException("DiskBlockSliceSink is closed");
        }
    }

    private void forceFlushSync() throws IOException {
        ++mSyncRemainingCount;
        if (mSyncRemainingCount >= mSyncPeriod) {
            mFileChannel.force(true);
            mSyncRemainingCount = 0;
        }
    }

    private void fireFileClosed(final String filePath) {
        mFireListenersThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (FileSwitchListener l : mListeners) {
                        l.onFileClosed(filePath);
                    }
                } catch (Exception e) {
                    System.err.println("throw Exception when fire file closed event to listeners");
                }
            }
        });
    }

    public boolean addFileSwitchListener(FileSwitchListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeFileSwitchListener(FileSwitchListener listener) {
        return mListeners.remove(listener);
    }

    public interface FileSwitchListener {
        void onFileClosed(String filePath);
    }

    private static final class PathFileChannel {
        private final FileChannel fileChannel;
        private final String filePath;

        PathFileChannel(FileChannel channel, String path) {
            fileChannel = channel;
            filePath = path;
        }

        @Override
        public String toString() {
            return filePath;
        }

        public boolean isOpen() {
            return fileChannel.isOpen();
        }

        public void close() throws IOException {
            fileChannel.close();
        }

        public void write(ByteBuffer src) throws IOException {
            fileChannel.write(src);
        }

        public void force(boolean metadata) throws IOException {
            fileChannel.force(metadata);
        }
    }
}
