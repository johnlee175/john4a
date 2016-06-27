package com.johnsoft.library.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名线程的创建工厂
 * @author John Kenrinus Lee
 * @version 2015-10-12
 */
public class NameableThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final Class<? extends Thread> customThreadClass;
    private final ThreadGroup group;
    private final String namePrefix;
    private final boolean daemon;
    private final int priority;

    public NameableThreadFactory(String namePrefix) {
        this(namePrefix, null, false, Thread.NORM_PRIORITY);
    }

    public NameableThreadFactory(String namePrefix, Class<? extends Thread> customThreadClass) {
        this(namePrefix, customThreadClass, false, Thread.NORM_PRIORITY);
    }

    public NameableThreadFactory(String namePrefix, boolean daemon, int priority) {
        this(namePrefix, null, daemon, priority);
    }

    public NameableThreadFactory(String namePrefix, Class<? extends Thread> customThreadClass,
                                 boolean daemon, int priority) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.customThreadClass = customThreadClass;
        this.namePrefix = namePrefix + "-";
        this.daemon = daemon;
        this.priority = priority;
    }

    public Thread newThread(final Runnable r) {
        Thread t = null;
        if (customThreadClass != null) {
            try {
                t = customThreadClass.getConstructor(ThreadGroup.class, Runnable.class, String.class, long.class)
                        .newInstance(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Create Thread failed from " + customThreadClass.getSimpleName()
                        + ", the default Thread class had apply. "
                        + "please see stacktrace printing of Throwable above, "
                        + "and consider that whether there's a constructor "
                        + "with current parameters type and count in the custom class or not.");
            }
        }
        if (t == null) {
            t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        }
        t.setDaemon(daemon);
        t.setPriority(priority);
        return t;
    }
}