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
package com.johnsoft.library.util.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import com.johnsoft.library.annotation.OneShotInApplication;
import com.johnsoft.library.util.ConcurrentDateFormat;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

/**
 * All the same as com.johnsoft.library.util.log.Log
 * but it makes open new log file and close old log file parallel execution possible.
 *
 * @author John Kenrinus Lee
 * @version 2015-10-30
 * @see com.johnsoft.library.util.log.Log
 */
public final class Log2 {
    private static final String CLASS_NAME = "Log2";
    public static final String TAG = "System.out";

    public static final int VERBOSE = android.util.Log.VERBOSE;
    public static final int DEBUG = android.util.Log.DEBUG;
    public static final int INFO = android.util.Log.INFO;
    public static final int WARN = android.util.Log.WARN;
    public static final int ERROR = android.util.Log.ERROR;

    private static int START_LOG_LEVEL = 0;
    private static int LOG_FILE_SIZE = 1024 * 1024;
    private static final byte[] LOCK = new byte[0];

    private static int sUid;
    private static int sPid;
    private static String sPackage;
    private static LogCallback sCallback;
    private static LogFilePathFactory sPathFactory;
    private static LogFileDescription sDescription;
    private static RandomAccessFile sRandomAccessFile;
    private static FileChannel sFileChannel;
    private static MappedByteBuffer sMappedByteBuffer;
    private static RandomAccessFile sRandomAccessFileBak;
    private static FileChannel sFileChannelBak;
    private static MappedByteBuffer sMappedByteBufferBak;
    private static HandlerThread sThread;
    private static Handler sHandler;
    private static boolean sIsPreOpenCalled;
    private static boolean sHadPreOpened;
    private static int sOffset;

    private Log2() {
    }

    /*****************************************************************
     *                      THE BASIC INTERFACE                      *
     *****************************************************************/

    public static void v(String msg) {
        println(VERBOSE, TAG, msg, null, true);
    }

    public static void v(String tag, String msg) {
        println(VERBOSE, tag, msg, null, true);
    }

    public static void v(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr, true);
    }

    public static void d(String msg) {
        println(DEBUG, TAG, msg, null, true);
    }

    public static void d(String tag, String msg) {
        println(DEBUG, tag, msg, null, true);
    }

    public static void d(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr, true);
    }

    public static void i(String msg) {
        println(INFO, TAG, msg, null, true);
    }

    public static void i(String tag, String msg) {
        println(INFO, tag, msg, null, true);
    }

    public static void i(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr, true);
    }

    public static void w(String msg) {
        println(WARN, TAG, msg, null, true);
    }

    public static void w(String tag, String msg) {
        println(WARN, tag, msg, null, true);
    }

    public static void w(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr, true);
    }

    public static void w(Throwable tr) {
        println(WARN, TAG, "", tr, true);
    }

    public static void w(String tag, Throwable tr) {
        println(WARN, tag, "", tr, true);
    }

    public static void e(String msg) {
        println(ERROR, TAG, msg, null, true);
    }

    public static void e(Throwable tr) {
        println(ERROR, TAG, "", tr, true);
    }

    public static void e(String tag, String msg) {
        println(ERROR, tag, msg, null, true);
    }

    public static void e(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr, true);
    }

    public static void e(String tag, Throwable tr) {
        println(ERROR, tag, "", tr, true);
    }

    public static void vs(String tag, String msg, Object... args) {
        println(VERBOSE, tag, bindArgs(msg, args), null, true);
    }

    public static void ds(String tag, String msg, Object... args) {
        println(DEBUG, tag, bindArgs(msg, args), null, true);
    }

    public static void is(String tag, String msg, Object... args) {
        println(INFO, tag, bindArgs(msg, args), null, true);
    }

    public static void ws(String tag, String msg, Throwable tr, Object... args) {
        println(WARN, tag, bindArgs(msg, args), tr, true);
    }

    public static void es(String tag, String msg, Throwable tr, Object...args) {
        println(ERROR, tag, bindArgs(msg, args), tr, true);
    }

    /*****************************************************************
     *              THE VERSION OF NON LOG TO FILE                   *
     *****************************************************************/

    public static void nv(String msg) {
        println(VERBOSE, TAG, msg, null, false);
    }

    public static void nv(String tag, String msg) {
        println(VERBOSE, tag, msg, null, false);
    }

    public static void nv(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr, false);
    }

    public static void nd(String msg) {
        println(DEBUG, TAG, msg, null, false);
    }

    public static void nd(String tag, String msg) {
        println(DEBUG, tag, msg, null, false);
    }

    public static void nd(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr, false);
    }

    public static void ni(String msg) {
        println(INFO, TAG, msg, null, false);
    }

    public static void ni(String tag, String msg) {
        println(INFO, tag, msg, null, false);
    }

    public static void ni(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr, false);
    }

    public static void nw(String msg) {
        println(WARN, TAG, msg, null, false);
    }

    public static void nw(String tag, String msg) {
        println(WARN, tag, msg, null, false);
    }

    public static void nw(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr, false);
    }

    public static void nw(Throwable tr) {
        println(WARN, TAG, "", tr, false);
    }

    public static void nw(String tag, Throwable tr) {
        println(WARN, tag, "", tr, false);
    }

    public static void ne(String msg) {
        println(ERROR, TAG, msg, null, false);
    }

    public static void ne(Throwable tr) {
        println(ERROR, TAG, "", tr, false);
    }

    public static void ne(String tag, String msg) {
        println(ERROR, tag, msg, null, false);
    }

    public static void ne(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr, false);
    }

    public static void ne(String tag, Throwable tr) {
        println(ERROR, tag, "", tr, false);
    }

    public static void nvs(String tag, String msg, Object... args) {
        println(VERBOSE, tag, bindArgs(msg, args), null, false);
    }

    public static void nds(String tag, String msg, Object... args) {
        println(DEBUG, tag, bindArgs(msg, args), null, false);
    }

    public static void nis(String tag, String msg, Object... args) {
        println(INFO, tag, bindArgs(msg, args), null, false);
    }

    public static void nws(String tag, String msg, Throwable tr, Object... args) {
        println(WARN, tag, bindArgs(msg, args), tr, false);
    }

    public static void nes(String tag, String msg, Throwable tr, Object...args) {
        println(ERROR, tag, bindArgs(msg, args), tr, false);
    }

    /*****************************************************************
     *                     THE REAL IMPLEMENTS                       *
     *****************************************************************/

    private static void println(int level, String tag, String msg, Throwable tr, boolean logToFile) {
        final String message = msg + "\n" + android.util.Log.getStackTraceString(tr);
        if (sCallback.handleCondition(START_LOG_LEVEL <= level,
                level, tag, msg, tr)) {
            android.util.Log.println(level, tag, message);
        }
        if (logToFile) {
            output(level, tag, message);
        }
        sCallback.handleError(level, tag, msg, tr);
    }

    private static void output(int level, String tag, String msg) {
        try {
            write(format(level, tag, msg).getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * yyyy-MM-dd HH:mm:ss.SSS UID-PID-TID/PACKAGE LEVEL/TAG: MESSAGE
     */
    private static String format(int level, String tag, String msg) {
        String datetime = ConcurrentDateFormat.getWide().format(new Date());
        int tid = Process.myTid();
        char priority;
        switch (level) {
            case VERBOSE: priority = 'V'; break;
            case DEBUG: priority = 'D'; break;
            case INFO: priority = 'I'; break;
            case WARN: priority = 'W'; break;
            case ERROR: priority = 'E'; break;
            default: priority = 'I'; break;
        }
        StringBuilder stringBuilder = new StringBuilder(datetime);
        stringBuilder.append(" ").append(sUid).append("-").append(sPid).append("-").append(tid);
        stringBuilder.append("/").append(sPackage).append(" ");
        stringBuilder.append(priority).append("/").append(tag).append(": ").append(msg).append("\n");
        return stringBuilder.toString();
    }

    private static String bindArgs(String msg, Object[] args) {
//        return String.format(msg, args); // a little slow
        return MessageFormatter.format(msg, args);
    }

    private static String getNextFilePath() {
        String path = sPathFactory.getNextFilePath();
        LogFileManager.putTypePath(CLASS_NAME, path);
        return path;
    }

    private static void firstOpen() {
        try {
            sRandomAccessFile = new RandomAccessFile(getNextFilePath(), "rwd");
            sFileChannel = sRandomAccessFile.getChannel();
            sMappedByteBuffer = sFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, LOG_FILE_SIZE);
            sMappedByteBuffer.order(ByteOrder.nativeOrder());
            sOffset = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void preOpen() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    sRandomAccessFileBak = new RandomAccessFile(getNextFilePath(), "rwd");
                    sFileChannelBak = sRandomAccessFileBak.getChannel();
                    sMappedByteBufferBak = sFileChannelBak.map(FileChannel.MapMode.READ_WRITE, 0, LOG_FILE_SIZE);
                    sMappedByteBufferBak.order(ByteOrder.nativeOrder());
                    sHadPreOpened = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void open() {
        while (!sHadPreOpened) {
            Thread.yield();
        }
        sHadPreOpened = false;
        sRandomAccessFile = sRandomAccessFileBak;
        sFileChannel = sFileChannelBak;
        sMappedByteBuffer = sMappedByteBufferBak;
        sRandomAccessFileBak = null;
        sFileChannelBak = null;
        sMappedByteBufferBak = null;
        sOffset = 0;
        sIsPreOpenCalled = false;
    }

    private static void close() {
        final RandomAccessFile tempFile = sRandomAccessFile;
        final FileChannel tempChannel = sFileChannel;
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tempChannel != null && tempChannel.isOpen()) {
                        tempFile.setLength(sOffset);
                        tempChannel.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void lastClose() {
        try {
            if (sFileChannel != null && sFileChannel.isOpen()) {
                sRandomAccessFile.setLength(sOffset);
                sFileChannel.close();
                sFileChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDescription() {
        try {
            String description = sDescription.getLogFileDescription();
            if (description != null) {
                write(description.getBytes("UTF-8"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(byte[] bytes) {
        synchronized(LOCK) {
            if (sFileChannel == null) {
                firstOpen();
                writeDescription();
            }
            final int length = bytes.length;
            final int size = sMappedByteBuffer.remaining();
            if (size / (float)LOG_FILE_SIZE <= 0.3f && !sIsPreOpenCalled) {
                preOpen();
                sIsPreOpenCalled = true;
            }
            if (length < size) {
                sMappedByteBuffer.put(bytes, 0, length);
                sOffset += length;
            } else {
                close();
                open();
                writeDescription();
                write(bytes);
            }
        }
    }

    /*****************************************************************
     *                     THE OTHER INTERFACE                       *
     *****************************************************************/

    @OneShotInApplication
    public static void initialize(Context context, int startLogLevel, LogCallback pCallback,
                                  int logFileSize, LogFilePathFactory pPathFactory,
                                  LogFileDescription pDescription) {
        sUid = Process.myUid();
        sPid = Process.myPid();
        sPackage = context.getApplicationContext().getPackageName();

        if (startLogLevel >= START_LOG_LEVEL) {
            START_LOG_LEVEL = startLogLevel;
        }
        if (pCallback == null) {
            sCallback = new DefaultLogCallback();
        } else {
            sCallback = pCallback;
        }
        if (logFileSize >= 1024) {
            LOG_FILE_SIZE = logFileSize;
        }
        if (pPathFactory == null) {
            sPathFactory = new DefaultLogFilePathFactory(Environment.getExternalStorageDirectory(),
                    "temp.log");
        } else {
            sPathFactory = pPathFactory;
        }
        if (pDescription == null) {
            sDescription = new DefaultLogFileDescription(context.getApplicationContext());
        } else {
            sDescription = pDescription;
        }

        sThread = new HandlerThread("Log2-Background-Task");
        sThread.start();
        sHandler = new Handler(sThread.getLooper());
    }

    @OneShotInApplication
    public static void destroy() {
        synchronized(LOCK) {
            lastClose();
        }
        sThread.quit();
    }

    public static void turnToNextLogFile() {
        synchronized(LOCK) {
            if (sHadPreOpened) {
                close();
                open();
            } else {
                lastClose();
                firstOpen();
            }
        }
    }
}
