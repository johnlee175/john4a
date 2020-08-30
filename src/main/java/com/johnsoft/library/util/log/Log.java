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

import com.johnsoft.library.annotation.NoInstanceResolved;
import com.johnsoft.library.annotation.OneShotInApplication;
import com.johnsoft.library.util.ConcurrentDateFormat;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

/**
 * The proxy of the class {@link android.util.Log}. <br>
 * It and android.util.Log is interface compatible,
 * these method will both print log to logcat and to file. <br>
 * These methods which name based on android.util.Log but start with 'n'(like e -> ne),
 * will just print log to logcat, not to file. <br>
 * These methods which name based on android.util.Log but end with 's'(like e -> es),
 * support binding parameters for log message. <br>
 * <br>EXAMPLES:<br>
 * <code><pre>
 * public static void main(String[] args) {
 *      int code = 200; String msg = "OK";
 *
 *      List list = new ArrayList();
 *      list.add("zhangsan");
 *      list.add("lisi");
 *
 *      Map map = new HashMap();
 *      map.put("timeout", 30);
 *      map.put("offset", 10293558321L);
 *
 *      // print: code=200, msg=OK, list=["zhangsan","lisi"], map={"timeout":30,"offset":10293558321}, escape={}
 *      Log.es("MyTag", "code={}, msg={}, list={}, map={}, escape=\\{}", code, msg, list, map);
 *
 *      // if you need more custom format pattern, and/or performance isn't the most important,
 *      // the following code will be good:
 *      Log.e("MyTag", String.format("name %s, age %d", "zhangsan", 33));
 *      Log.e("MyTag", MessageFormat.format("name {0}, age {1}", "zhangsan", 33));
 *
 *      // if not using String.format or MessageFormat.format,
 *      // then using Log.{v|d|i|w|e}s is good choice,
 *      // it will shorter and simpler than Log.{v|d|i|w|e} without format,
 *      // on the other hand, using format string can avoid some mistakes like:
 *      final String pig = "length: 10";
 *      final String dog = "length: " + pig.length();
 *      Log.e("MyTag", "Animals are equal: " + pig==dog); // print: false
 *      Log.es("MyTag", "Animals are equal: {}", pig==dog); // print: Animals are equal: false
 * }
 * </pre></code>
 * <br>PERFORMANCE:<br>
 * No matter print to logcat or write to log file, it isn't asynchronous.
 * But it uses NIO way to avoid blocking as far as possible(It may cause a thread switch).
 * It uses SimpleDateFormat to format date of log message, uses custom MessageFormatter to format binding args message,
 * but they have been optimized for fast enough.
 *
 * @author John Kenrinus Lee
 * @version 2015-10-30
 */
public final class Log {
    /**
     * the name of this class
     */
    private static final String CLASS_NAME = "Log";
    /**
     * the default TAG for null TAG
     */
    public static final String TAG = "System.out";

    /**
     * Priority constant for the println method; use Log.v.
     */
    public static final int VERBOSE = android.util.Log.VERBOSE;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int DEBUG = android.util.Log.DEBUG;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int INFO = android.util.Log.INFO;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int WARN = android.util.Log.WARN;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int ERROR = android.util.Log.ERROR;

    /**
     * if the log level used bigger than this value cause logging
     */
    private static int START_LOG_LEVEL = 0;

    /**
     * if log to file, the max size of the file
     */
    private static int LOG_FILE_SIZE = 1024 * 1024;

    /**
     * a synchronized lock object for write to log file
     */
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
    private static int sOffset;

    private Log() {
    }

    /*****************************************************************
     *                      THE BASIC INTERFACE                      *
     *****************************************************************/

    /**
     * Send a {@link #VERBOSE} log message using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void v(String msg) {
        println(VERBOSE, TAG, msg, null, true);
    }

    /**
     * Send a {@link #VERBOSE} log message.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
        println(VERBOSE, tag, msg, null, true);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void v(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr, true);
    }

    /**
     * Send a {@link #DEBUG} log message using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void d(String msg) {
        println(DEBUG, TAG, msg, null, true);
    }

    /**
     * Send a {@link #DEBUG} log message.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
        println(DEBUG, tag, msg, null, true);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr, true);
    }

    /**
     * Send an {@link #INFO} log message using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void i(String msg) {
        println(INFO, TAG, msg, null, true);
    }

    /**
     * Send an {@link #INFO} log message.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
        println(INFO, tag, msg, null, true);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr, true);
    }

    /**
     * Send a {@link #WARN} log message using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void w(String msg) {
        println(WARN, TAG, msg, null, true);
    }

    /**
     * Send a {@link #WARN} log message.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
        println(WARN, tag, msg, null, true);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void w(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr, true);
    }

    /**
     * Send a {@link #WARN} empty log message and log the exception using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param tr An exception to log
     */
    public static void w(Throwable tr) {
        println(WARN, TAG, "", tr, true);
    }

    /**
     * Send a {@link #WARN} empty log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static void w(String tag, Throwable tr) {
        println(WARN, tag, "", tr, true);
    }

    /**
     * Send an {@link #ERROR} log message using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void e(String msg) {
        println(ERROR, TAG, msg, null, true);
    }

    /**
     * Send an {@link #ERROR} empty log message and log the exception using tag {@link #TAG}.
     * NOTICE: it will log to file.
     *
     * @param tr An exception to log
     */
    public static void e(Throwable tr) {
        println(ERROR, TAG, "", tr, true);
    }

    /**
     * Send an {@link #ERROR} log message.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        println(ERROR, tag, msg, null, true);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr, true);
    }

    /**
     * Send a {@link #ERROR} empty log message and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static void e(String tag, Throwable tr) {
        println(ERROR, tag, "", tr, true);
    }

    /**
     * Send a {@link #VERBOSE} log message with variable parameters.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void vs(String tag, String msg, Object... args) {
        println(VERBOSE, tag, bindArgs(msg, args), null, true);
    }

    /**
     * Send a {@link #DEBUG} log message with variable parameters.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void ds(String tag, String msg, Object... args) {
        println(DEBUG, tag, bindArgs(msg, args), null, true);
    }

    /**
     * Send a {@link #INFO} log message with variable parameters.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void is(String tag, String msg, Object... args) {
        println(INFO, tag, bindArgs(msg, args), null, true);
    }

    /**
     * Send a {@link #WARN} log message with variable parameters and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     * @param args the real value binding on parameter named msg.
     */
    public static void ws(String tag, String msg, Throwable tr, Object... args) {
        println(WARN, tag, bindArgs(msg, args), tr, true);
    }

    /**
     * Send a {@link #ERROR} log message with variable parameters and log the exception.
     * NOTICE: it will log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     * @param args the real value binding on parameter named msg.
     */
    public static void es(String tag, String msg, Throwable tr, Object...args) {
        println(ERROR, tag, bindArgs(msg, args), tr, true);
    }

    /*****************************************************************
     *              THE VERSION OF NON LOG TO FILE                   *
     *****************************************************************/

    /**
     * Send a {@link #VERBOSE} log message using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void nv(String msg) {
        println(VERBOSE, TAG, msg, null, false);
    }

    /**
     * Send a {@link #VERBOSE} log message.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void nv(String tag, String msg) {
        println(VERBOSE, tag, msg, null, false);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void nv(String tag, String msg, Throwable tr) {
        println(VERBOSE, tag, msg, tr, false);
    }

    /**
     * Send a {@link #DEBUG} log message using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void nd(String msg) {
        println(DEBUG, TAG, msg, null, false);
    }

    /**
     * Send a {@link #DEBUG} log message.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void nd(String tag, String msg) {
        println(DEBUG, tag, msg, null, false);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void nd(String tag, String msg, Throwable tr) {
        println(DEBUG, tag, msg, tr, false);
    }

    /**
     * Send an {@link #INFO} log message using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void ni(String msg) {
        println(INFO, TAG, msg, null, false);
    }

    /**
     * Send an {@link #INFO} log message.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void ni(String tag, String msg) {
        println(INFO, tag, msg, null, false);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void ni(String tag, String msg, Throwable tr) {
        println(INFO, tag, msg, tr, false);
    }

    /**
     * Send a {@link #WARN} log message using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void nw(String msg) {
        println(WARN, TAG, msg, null, false);
    }

    /**
     * Send a {@link #WARN} log message.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void nw(String tag, String msg) {
        println(WARN, tag, msg, null, false);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void nw(String tag, String msg, Throwable tr) {
        println(WARN, tag, msg, tr, false);
    }

    /**
     * Send a {@link #WARN} empty log message and log the exception using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param tr An exception to log
     */
    public static void nw(Throwable tr) {
        println(WARN, TAG, "", tr, false);
    }

    /**
     * Send a {@link #WARN} empty log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static void nw(String tag, Throwable tr) {
        println(WARN, tag, "", tr, false);
    }

    /**
     * Send an {@link #ERROR} log message using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param msg The message you would like logged.
     */
    public static void ne(String msg) {
        println(ERROR, TAG, msg, null, false);
    }

    /**
     * Send an {@link #ERROR} empty log message and log the exception using tag {@link #TAG}.
     * NOTICE: it will NOT log to file.
     *
     * @param tr An exception to log
     */
    public static void ne(Throwable tr) {
        println(ERROR, TAG, "", tr, false);
    }

    /**
     * Send an {@link #ERROR} log message.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void ne(String tag, String msg) {
        println(ERROR, tag, msg, null, false);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void ne(String tag, String msg, Throwable tr) {
        println(ERROR, tag, msg, tr, false);
    }

    /**
     * Send a {@link #ERROR} empty log message and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static void ne(String tag, Throwable tr) {
        println(ERROR, tag, "", tr, false);
    }

    /**
     * Send a {@link #VERBOSE} log message with variable parameters.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void nvs(String tag, String msg, Object... args) {
        println(VERBOSE, tag, bindArgs(msg, args), null, false);
    }

    /**
     * Send a {@link #DEBUG} log message with variable parameters.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void nds(String tag, String msg, Object... args) {
        println(DEBUG, tag, bindArgs(msg, args), null, false);
    }

    /**
     * Send a {@link #INFO} log message with variable parameters.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param args the real value binding on parameter named msg.
     */
    public static void nis(String tag, String msg, Object... args) {
        println(INFO, tag, bindArgs(msg, args), null, false);
    }

    /**
     * Send a {@link #WARN} log message with variable parameters and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     * @param args the real value binding on parameter named msg.
     */
    public static void nws(String tag, String msg, Throwable tr, Object... args) {
        println(WARN, tag, bindArgs(msg, args), tr, false);
    }

    /**
     * Send a {@link #ERROR} log message with variable parameters and log the exception.
     * NOTICE: it will NOT log to file.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     * @param args the real value binding on parameter named msg.
     */
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

    private static void open() throws IOException {
        sRandomAccessFile = new RandomAccessFile(getNextFilePath(), "rwd");
        sFileChannel = sRandomAccessFile.getChannel();
        sMappedByteBuffer = sFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, LOG_FILE_SIZE);
        sMappedByteBuffer.order(ByteOrder.nativeOrder());
        sOffset = 0;
    }

    private static void close() throws IOException {
        if (sFileChannel != null && sFileChannel.isOpen()) {
            sRandomAccessFile.setLength(sOffset);
            sFileChannel.close();
            sFileChannel = null;
        }
    }

    private static void write(byte[] bytes) throws IOException {
        synchronized(LOCK) {
            if (sFileChannel == null) {
                open();
                String description = sDescription.getLogFileDescription();
                if (description != null) {
                    write(description.getBytes("UTF-8"));
                }
            }
            final int length = bytes.length;
            if (length < sMappedByteBuffer.remaining()) {
                sMappedByteBuffer.put(bytes, 0, length);
                sOffset += length;
            } else {
                close();
                write(bytes);
            }
        }
    }

    /*****************************************************************
     *                     THE OTHER INTERFACE                       *
     *****************************************************************/

    /**
     * Initialize the log system.
     * It should be call once, called on Application's onCreate() is best.
     * @param context a Context instance, this class will not hold the instance to member.
     * @param startLogLevel example, a VERBOSE log message is base on 2,
     *                      if input 1 will print the message or write to file, else not print or write.
     * @param pCallback a callback method will be called when check start log level and after each print or write.
     * @param logFileSize the max size of a log file if writing to file perhaps.
     * @param pPathFactory it will indicate how to define the log file name if writing to file perhaps.
     * @param pDescription A callback interface, the methods will be called when the new log file opened,
     *                      it is obtained from the description of the log file.
     *
     * @see LogCallback
     * @see LogFilePathFactory
     * @see LogFileDescription
     */
    @OneShotInApplication
    public static void initialize(@NoInstanceResolved Context context, int startLogLevel, LogCallback pCallback,
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
    }

    /**
     * Destroy the log system, example close stream.
     * It should be call once when the app will be exiting.
     */
    @OneShotInApplication
    public static void destroy() {
        try {
            synchronized(LOCK) {
                close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method will close the current log file and open new one,
     * so that the current log file can be upload soon.
     * If you don't use log file, shouldn't call it.
     */
    public static void turnToNextLogFile() {
        try {
            synchronized(LOCK) {
                close();
                open();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
