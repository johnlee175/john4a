package com.johnsoft.library.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * 当前所有线程信息的采集类
 * 配合/data/anr/traces.txt和DropBoxManager分析
 * @author John Kenrinus Lee
 * @version 2016-03-25
 */
public final class ThreadStackTracer extends Throwable {
    private Thread thread;

    private ThreadStackTracer() {}

    private void setThread(Thread thread) {
        this.thread = thread;
    }

    @Override
    public String toString() {
        if (thread == null) return "";
        return thread.getId() + " " + thread.getName() + " " + thread.getState().name();
    }

    /** 打印出所有的Java线程信息 */
    public static void dumpJavaThreadsInfo() {
        final Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        final ThreadStackTracer tracer = new ThreadStackTracer();
        for (final Thread thread : allStackTraces.keySet()) {
            tracer.setThread(thread);
            tracer.setStackTrace(allStackTraces.get(thread));
            tracer.printStackTrace();
        }
    }

    /** 以字符串形式给出所有的Java线程信息 */
    public static String getJavaThreadsInfo() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        final ThreadStackTracer tracer = new ThreadStackTracer();
        for (final Thread thread : allStackTraces.keySet()) {
            tracer.setThread(thread);
            tracer.setStackTrace(allStackTraces.get(thread));
            tracer.printStackTrace(pw);
        }
        pw.flush();
        return sw.toString();
    }

    /** 通过终端top命令打印出进程可能的所有线程信息 */
    public static void dumpThreadsInfoFromTopCmd(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c",
                    "top -t -n 1 -d 0 | grep '" + packageName + "'"});
            String message = FileUtils.toString(process.getInputStream(), FileUtils.UTF_8);
            if (process.waitFor() == 0) {
                System.err.println(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 通过终端top命令给出进程可能的所有线程信息的字符串 */
    public static String getThreadsInfoFromTopCmd(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c",
                    "top -t -n 1 -d 0 | grep '" + packageName + "'"});
            String message = FileUtils.toString(process.getInputStream(), FileUtils.UTF_8);
            if (process.waitFor() == 0) {
                return message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
