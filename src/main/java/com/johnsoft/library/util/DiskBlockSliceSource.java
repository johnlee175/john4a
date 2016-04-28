package com.johnsoft.library.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.util.Log;

/**
 * A common source which using nio and will pre-open a new file async so that swap file smooth.
 * @author John Kenrinus Lee
 * @version 2016-02-18
 */
public final class DiskBlockSliceSource implements Closeable {
    public static final int ERROR_BAD_READ = -1;
    private final Object LOCK = new byte[0];

    private final float mPreOpenThreshold;
    private final FilePathFactory mPathFactory;
    private final ExecutorService mSingleAsyncThread;

    private FileChannel mFileChannel;
    private FileChannel mFileChannelBak;
    private Future<?> mFuture;
    private volatile boolean mIsPreOpenCalled;
    private volatile boolean mHadPreOpened;
    private volatile boolean mIsSourceOpen;

    /**
     * @param pPreOpenThreshold a value between 0.0f(exclude) and 1.0f(exclude)
     *                          which indicate pre-open a new one file
     *                          when current file's remaining rate smaller than the threshold,
     *                          the default value is 0.2f;
     * @param pPathFactory see {@link FilePathFactory}, must not be null;
     */
    public DiskBlockSliceSource(float pPreOpenThreshold, FilePathFactory pPathFactory) {
        if (pPreOpenThreshold > 0.0F && pPreOpenThreshold < 1.0F) {
            mPreOpenThreshold = pPreOpenThreshold;
        } else {
            mPreOpenThreshold = 0.2F;
            Log.w("System.err", "Using default PreOpenThreshold.");
        }
        if (pPathFactory == null) {
            throw new NullPointerException("DiskBlockSliceSource constructor need a FilePathFactory instance, "
                    + "but got null.");
        }
        mPathFactory = pPathFactory;
        mSingleAsyncThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameableThreadFactory("DiskBlockSliceSource-SwapFile"));
        mIsSourceOpen = true;
    }

    public final void close() {
        synchronized(LOCK) {
            if (mIsSourceOpen) {
                lastClose(true);
                mSingleAsyncThread.shutdownNow();
                mIsSourceOpen = false;
            }
        }
    }

    public final int read(byte[] bytesDst) throws IOException {
        return read(ByteBuffer.wrap(bytesDst, 0, bytesDst.length));
    }

    public final int read(byte[] bytesDst, int byteOffset, int byteCount)
            throws IOException {
        return read(ByteBuffer.wrap(bytesDst, byteOffset, byteCount));
    }

    // the core method
    public final int read(ByteBuffer byteBufferDst) throws IOException {
        synchronized(LOCK) {
            checkSourceOpen();
            if (mFileChannel == null) {
                firstOpen();
                if (mFileChannel == null) {
                    return ERROR_BAD_READ;
                }
            }
            final long fileSize = mFileChannel.size();
            final long fileRemaining = fileSize - mFileChannel.position();
            final int bufferRemaining = byteBufferDst.remaining();
            if (((fileRemaining - bufferRemaining) / (float) fileSize <= mPreOpenThreshold)
                    && !mIsPreOpenCalled) {
                preOpen();
                mIsPreOpenCalled = true;
            }
            if (fileRemaining > bufferRemaining) {
                return read0(byteBufferDst);
            } else {
                final int firstReadCount = read0(byteBufferDst);
                if (firstReadCount >= 0) {
                    close0();
                    open0();
                    if (mFileChannel == null) {
                        return firstReadCount;
                    }
                    final int secondReadCount = read(byteBufferDst);
                    if (secondReadCount >= 0) {
                        return firstReadCount + secondReadCount;
                    }
                }
                return ERROR_BAD_READ;
            }
        }
    }

    /** request close current read file, and swap to a new one */
    public final void turnToNextFile() throws IOException {
        synchronized(LOCK) {
            checkSourceOpen();
            if (mHadPreOpened) {
                close0();
                open0();
            } else {
                lastClose(false);
                firstOpen();
            }
        }
    }

    private int read0(ByteBuffer byteBuffer) throws IOException {
        return mFileChannel.read(byteBuffer);
    }

    private FileChannel openFileChannel() throws IOException {
        //9025.487 ± 53.311  ns/op
        //106592.926 ± 4086.916  ops/s
//        return new FileInputStream(mPathFactory.getNextFilePath()).getChannel();
        //5074.877 ± 27.325  ns/op
        //199157.991 ± 8375.167  ops/s
        final String nextFilePath = mPathFactory.getNextFilePath();
        if (nextFilePath != null && !nextFilePath.trim().isEmpty()) {
            return new RandomAccessFile(nextFilePath, "rwd").getChannel();
        }
        return null;
    }

    private void firstOpen() throws IOException {
        mFileChannel = openFileChannel();
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
                Log.w("System.err", e);
            } catch (TimeoutException e) {
                Log.w("System.err", e);
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
        mIsPreOpenCalled = false;
    }

    private void close0() {
        final FileChannel tempChannel = mFileChannel;
        mSingleAsyncThread.submit(new Runnable() {
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

    private void lastClose(boolean cleanBak) {
        if (mFileChannel != null && mFileChannel.isOpen()) {
            try {
                mFileChannel.close();
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

    private void checkSourceOpen() throws IOException {
        if (!mIsSourceOpen) {
            throw new IOException("DiskBlockSliceSource is closed");
        }
    }
}
