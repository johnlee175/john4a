package com.johnsoft.library.util.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.johnsoft.library.util.BuildConfigHelper;

import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * 打印日志的类。
 *
 * @author John Kenrinus Lee
 * @version 2016-07-04
 */
public final class LoggerX {
    public static void initialize(String logHome, String appName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
        rollingFileAppender.setName("FILE");
        rollingFileAppender.setContext(context);
        rollingFileAppender.setAppend(true);

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setFileNamePattern(logHome + "/" + appName + "_%d{yyyyMMdd}_%i.log.zip");
        rollingPolicy.setMaxHistory(30);
        SizeAndTimeBasedFNATP<ILoggingEvent> fnatp = new SizeAndTimeBasedFNATP<>();
        fnatp.setMaxFileSize("3MB");
        rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
        rollingPolicy.setParent(rollingFileAppender);
        rollingPolicy.setContext(context);
        rollingPolicy.start();
        rollingFileAppender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%date{MM-dd HH:mm:ss.SSS} %mdc{pid} %mdc{tid} %.-1level %logger{48}: "
                + "[%mdc{uid}][%thread]%msg%n");
        encoder.setContext(context);
        encoder.start();
        rollingFileAppender.setEncoder(encoder);
        rollingFileAppender.start();

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(context);
        asyncAppender.setName("ASYNC");
        asyncAppender.setQueueSize(512);
        asyncAppender.setDiscardingThreshold(0);
        asyncAppender.addAppender(rollingFileAppender);
        asyncAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);
        root.addAppender(asyncAppender);

        prepareCustomConverter();
        // print any status messages (warnings, etc) encountered in logback config
        StatusPrinter.print(context);
    }

    private static final boolean ENABLE_LOG = BuildConfigHelper.getCurrentHelper().isDebug(); // 打印日志的开关
    public static final int LARGE_STRING_LIMIT = 1600; // logcat每条日志的最大字符数

    public static int v(String tag, String msg) {
        return println(Log.VERBOSE, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return println(Log.VERBOSE, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int d(String tag, String msg) {
        return println(Log.DEBUG, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return println(Log.DEBUG, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int i(String tag, String msg) {
        return println(Log.INFO, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return println(Log.INFO, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, String msg) {
        return println(Log.WARN, tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return println(Log.WARN, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int e(String tag, String msg) {
        return println(Log.ERROR, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return println(Log.ERROR, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    private static int println(int priority, String tag, String msg) {
        if ((ENABLE_LOG || Log.isLoggable(tag, priority))
                && (tag != null && msg != null)) {
            final Logger logger = LoggerFactory.getLogger(tag);
            prepareCustomConverter();
            switch (priority) {
                case Log.VERBOSE:
                    if (logger.isTraceEnabled()) {
                        logger.trace(msg);
                    }
                    // Not in use increase priority, use "adb shell setprop log.tag.<LOG_TAG> <LEVEL>" instead
                    //                    priority = Log.INFO;
                    break;
                case Log.DEBUG:
                    if (logger.isDebugEnabled()) {
                        logger.debug(msg);
                    }
                    // Not in use increase priority, use "adb shell setprop log.tag.<LOG_TAG> <LEVEL>" instead
                    //                    priority = Log.INFO;
                    break;
                case Log.INFO:
                    if (logger.isInfoEnabled()) {
                        logger.info(msg);
                    }
                    break;
                case Log.WARN:
                    if (logger.isWarnEnabled()) {
                        logger.warn(msg);
                    }
                    break;
                case Log.ERROR:
                    if (logger.isErrorEnabled()) {
                        logger.error(msg);
                    }
                    break;
                case Log.ASSERT:
                    logger.error(msg); // no same level in org.slf4j.Logger
                    break;
                default:
                    throw new IllegalArgumentException("Unknown log priority!");
            }
            return printlnLargeString(priority, tag, msg);
        }
        return 0;
    }

    // There is a fixed size buffer in logcat for binary logs (/dev/log/events) and this limit is 1024 bytes.
    // For the non-binary logs there is also a limit:
    // #define LOGGER_ENTRY_MAX_LEN        (4*1024)
    // #define LOGGER_ENTRY_MAX_PAYLOAD (LOGGER_ENTRY_MAX_LEN - sizeof(struct logger_entry))
    private static int printlnLargeString(int priority, String tag, String msg) {
        if (msg.length() > LARGE_STRING_LIMIT) {
            final int written = Log.println(priority, tag, msg.substring(0, LARGE_STRING_LIMIT));
            return written + printlnLargeString(priority, tag, msg.substring(LARGE_STRING_LIMIT));
        } else {
            return Log.println(priority, tag, msg);
        }
    }

    private static void prepareCustomConverter() {
        MDC.put("pid", String.valueOf(Process.myPid()));
        MDC.put("tid", String.valueOf(Process.myTid()));
        MDC.put("uid", String.valueOf(Process.myUid()));
    }

    /**
     * 原e级别日志输出, 没有消息主体, 直接打印Throwable堆栈
     */
    public static int error(@NonNull String tag, @Nullable Throwable e) {
        return println(Log.ERROR, tag, Log.getStackTraceString(e));
    }

    /**
     * 为便于声明式编程和避免字符串拼接, 使用参数格式化日志记录方式作为封装, 此处使用String.format实现<br>
     * Example: <br>
     * LoggerX.error("Tag", (Throwable)null, "%s -> %d", "age", 18);<br>
     * // will print: age -> 18
     */
    public static int error(@NonNull String tag, @Nullable Throwable e, @Nullable String msg,
                            @Nullable Object... args) {
        if (msg == null) {
            return 0;
        }
        return println(Log.ERROR, tag, String.format(msg, args) + '\n' + Log.getStackTraceString(e));
    }

    /**
     * 为便于声明式编程和避免字符串拼接, 使用参数格式化日志记录方式作为封装, 此处使用String.format实现<br>
     * Example: <br>
     * LoggerX.warn("Tag", "%s -> %d", "age", 18);<br>
     * // will print: age -> 18
     */
    public static int warn(@NonNull String tag, @Nullable String msg, @Nullable Object... args) {
        if (msg == null) {
            return 0;
        }
        return println(Log.WARN, tag, String.format(msg, args));
    }

    /**
     * 为便于声明式编程和避免字符串拼接, 使用参数格式化日志记录方式作为封装, 此处使用String.format实现<br>
     * Example: <br>
     * LoggerX.info("Tag", "%s -> %d", "age", 18);<br>
     * // will print: age -> 18
     */
    public static int info(@NonNull String tag, @Nullable String msg, @Nullable Object... args) {
        if (msg == null) {
            return 0;
        }
        return println(Log.INFO, tag, String.format(msg, args));
    }
}
