package com.johnsoft.library.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * 此类为执行异步任务提供了可重用设施. <br>
 * 注意: 所有接口方法并未做参数判空, 请务必自行处理; <br>
 * 注意: 任务应完全自行处理所有异常, 此框架未作任何加工; <br>
 * 注意: 此版本并未使用任何daemon thread(守护线程, 其任务代码中的finally块的执行无法得到保证); <br>
 *
 * @author John Kenrinus Lee
 * @version 2016-06-12
 */
public final class SimpleTaskExecutor {
    private SimpleTaskExecutor() {}

    private static final byte[] CREATE_DESTROY_LOCK = new byte[0];
    private static volatile ScheduledExecutorService scheduledTaskService;

    private static void createScheduledTaskService() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (scheduledTaskService == null) {
                final int cpuCount = Runtime.getRuntime().availableProcessors();
                scheduledTaskService = new ScheduledThreadPoolExecutor(cpuCount * 2 + 1,
                        new NameableThreadFactory("SimpleTaskExecutor-ScheduledThread")) {

                    private volatile int mOldCorePoolSize = 0;
                    private volatile boolean mIsFirstGlow = true;

                    @Override
                    protected void beforeExecute(Thread t, Runnable r) {
                        super.beforeExecute(t, r);
                        final int queueSize = getQueue().size();
                        final int corePoolSize = getCorePoolSize();
                        final int activeCount = getActiveCount();
                        System.out.println("before: queueSize: " + queueSize
                                + ", corePoolSize: " + corePoolSize + ", activeCount: " + activeCount);
                        if (queueSize > (corePoolSize << 2) && activeCount == corePoolSize) {
                            if (mIsFirstGlow) {
                                mIsFirstGlow = false;
                                mOldCorePoolSize = corePoolSize;
                            }
                            setCorePoolSize(corePoolSize << 2);
                        }
                    }

                    @Override
                    protected void afterExecute(Runnable r, Throwable t) {
                        super.afterExecute(r, t);
                        final int queueSize = getQueue().size();
                        final int corePoolSize = getCorePoolSize();
                        final int activeCount = getActiveCount();
                        System.out.println("after: queueSize: " + queueSize
                                + ", corePoolSize: " + corePoolSize + ", activeCount: " + activeCount);
                        if (queueSize <= mOldCorePoolSize && corePoolSize != mOldCorePoolSize
                                && mOldCorePoolSize > 0) {
                            setCorePoolSize(mOldCorePoolSize);
                        }
                    }

                };
            }
        }
    }

    /**
     * 销毁方法, 应在非主线程调用, 且轻易不应调用, 直到进程即将结束.
     */
    public static void destroy() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (scheduledTaskService != null) {
                scheduledTaskService.shutdown();
                boolean terminated;
                try {
                    terminated = scheduledTaskService.awaitTermination(4000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    terminated = false;
                }
                if (!terminated) {
                    scheduledTaskService.shutdownNow();
                    try {
                        scheduledTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }
                }
                scheduledTaskService = null;
            }
        }
    }

    /**
     * 创建一个工作者Thread对象并启动, 强制赋值线程名, 强制限制继承Thread类(而是使用Task描述待执行的任务);
     * 注意: 仅应在需要非常高的优先级, 线程中存在死循环的情况下考虑调用此方法, 一般任务应考虑schedule系列方法;
     */
    @Deprecated
    public static Thread startWorkThread(Task task) {
        Thread thread = new Thread(task, task.getName());
        thread.start();
        return thread;
    }

    /** 辅助方法: 创建一个工作者Handler线程, 用于post Runnable或send Message. */
    public static Handler createWorkHandler(String name) {
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    /** 辅助方法: 创建一个工作者Handler线程, 用于post Runnable或send Message. */
    public static Handler createWorkHandler(String name, Handler.Callback callback) {
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        return new Handler(handlerThread.getLooper(), callback);
    }

    /**
     * 在目前为止提交的所有消息处理完或任务执行完后, 以delayMillis毫秒的延迟结束HandlerThread; <br/>
     * Example:<br/>
     * <pre> {@code
     *  Handler handler = createWorkHandler("Send-Three-Request");
     *   handler.post(new RequestTask("One"));
     *   handler.post(new RequestTask("Two"));
     *   handler.sendEmptyMessageDelayed(THREE, 100L);
     *   destroyHandlerAfterAllHandle(handler, 10 * 1000L);
     * } </pre>
     */
    public static void destroyHandqlerAfterAllHandle(Handler handler, long delayMillis) {
        final Looper looper = handler.getLooper();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                looper.quit();
            }
        }, delayMillis);
    }

    /** 让线程池立即异步执行任务. */
    public static ScheduledFuture<?> scheduleNow(@NonNull Task task) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.schedule(task, 0L, TimeUnit.MILLISECONDS);
    }

    /** 让线程池延迟指定毫秒数后异步执行任务, 如为非正数, 将立即请求执行. */
    public static ScheduledFuture<?> scheduleDelayed(@NonNull Task task, long delayMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池经过initialDelayMillis毫秒的延迟后开始异步执行任务, 任务执行完毕后延迟delayMillis毫秒重复执行异步任务. */
    public static ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Task task,
                                                                   long initialDelayMillis, long delayMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.scheduleWithFixedDelay(task, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 任务的基本抽象, 便于封装扩展和获得更佳的控制权.
     */
    public interface Task extends Runnable {
        String getName();
    }

    /**
     * 安全的抽象任务类, 仅供参考.
     * @see com.johnsoft.library.util.DefaultTask
     */
    public static abstract class AbstractSafeTask implements Task {
        @Override
        public final void run() {
            try {
                doTask();
            } catch (Throwable e) {
                onThrowable(e);
            }
        }
        /** 不在实现run(), 而是实现该方法 */
        public abstract void doTask();
        /** 任何异常或错误都可能或可以调用该方法 */
        public abstract void onThrowable(Throwable t);
    }
}