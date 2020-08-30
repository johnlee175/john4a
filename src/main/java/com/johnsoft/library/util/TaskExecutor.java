package com.johnsoft.library.util;

import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * 此类是同步异步线程任务执行器线程安全单实例. <br>
 * 此类为执行异步任务提供了可重用设施, 便于线程间通信. <br>
 * 其中: <br>
 * runOnMainThread*系列方法将异步投递任务到主线程执行; <br>
 * runOnWorkerThread*系列方法将异步投递任务到使用新的HandlerThread创建的工作线程上执行; <br>
 * runOnScheduledThread*系列方法将异步投递任务到线程池中执行; <br>
 * 也有其他方法为那些非时间单位为优先级依据的任务, 可以被多个线程并发的任务, 相互依赖的任务和AsyncTask提供了支持; <br>
 * FAQ: <br>
 * runOnScheduledThread*系列方法相对于runOnWorkerThread*系列方法都是非主线程异步, 其区别主要有两个: <br>
 * 一是对于时间精度要求高的任务在runOnWorkerThread*可能无法得到满足, 而runOnScheduledThread*将由多个工作线程共同工作, <br>
 * 但runOnWorkerThread*相对于runOnScheduledThread*更可控和简单; <br>
 * 二是runOnScheduledThread*自带了定时循环执行周期性任务的能力, 而runOnWorkerThread*需要由调用方在逻辑最后重新投递任务来实现相同功能, <br>
 * 但显然runOnWorkerThread*不如runOnScheduledThread*精度高和灵活性高; <br>
 * 注意: 首要的, 所有接口方法并未做参数判空, 请务必自行处理; <br>
 * 注意: 任务边界应该独立清晰并尤其做好<b>阻塞超时</b>, 由于目前非runOnScheduledThread*系列方法都是采用单线程机制完成,
 * 不应向其前后投递互有依赖的任务以造成死锁, 也不应投递不可中断的阻塞任务; <br>
 * 注意: 任务应完全自行处理所有异常, 此框架未作任何加工; <br>
 * 注意: 此版本并未使用任何daemon thread(守护线程, 其任务代码中的finally块的执行无法得到保证); <br>
 * TODO 目前查询任务执行情况的机制未加入, 可能需要自定义ThreadPoolExecutor, ScheduledThreadPoolExecutor子类的afterExecute并用map管理任务ID, 但因为销毁资源时比较复杂, 未来版本可能提供实现
 *
 * @author John Kenrinus Lee
 * @version 2015-08-07
 */
public enum TaskExecutor {
    singleInstance;

    private final Object CREATE_DESTROY_LOCK = new byte[0];
    private Handler mainThreadHandler;
    private Handler workerThreadHandler;
    private HandlerThread workerThread;
    private ScheduledExecutorService scheduledTaskService;
    private ExecutorService priorityTaskService;
    private ExecutorService asyncTaskService;
    private ExecutorService concurrentTaskService;

    TaskExecutor() {
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    private void createWorkerThread() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (workerThread == null) {
                workerThread = new HandlerThread("TaskExecutor-WorkerThread");
                workerThread.start();
                workerThreadHandler = new Handler(workerThread.getLooper());
            }
        }
    }

    private void createScheduledTaskService() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (scheduledTaskService == null) {
                final int cpuCount = Runtime.getRuntime().availableProcessors();
                scheduledTaskService = new ScheduledThreadPoolExecutor(cpuCount,
                        new NameableThreadFactory("TaskExecutor-ScheduledThread"));
            }
        }
    }

    private void createPriorityTaskService() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (priorityTaskService == null) {
                priorityTaskService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.NANOSECONDS,
                        new PriorityBlockingQueue<Runnable>(),
                        new NameableThreadFactory("TaskExecutor-PriorityTask"));
            }
        }
    }

    private void createAsyncTaskService() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (asyncTaskService == null) {
                asyncTaskService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.NANOSECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        new NameableThreadFactory("TaskExecutor-AsyncTask"));
            }
        }
    }

    private void createConcurrentTaskService() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (concurrentTaskService == null) {
                concurrentTaskService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        new NameableThreadFactory("TaskExecutor-ConcurrentTask"));
            }
        }
    }

    /**
     * 销毁方法, 应在非主线程调用, 且轻易不应调用, 直到进程即将结束. 此方法将销毁空闲线程, 并不再接受新任务,
     * 如无意外, 将等待所有池中已有的任务完成后退出. 此方法较为保守, 相对于{@link #destroyNow()}应首先考虑.
     */
    public final void destroySafely() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (workerThread != null) {
                workerThread.quit();
                workerThread = null;
            }
            try {
                if (scheduledTaskService != null) {
                    scheduledTaskService.shutdown();
                    if (!scheduledTaskService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                        scheduledTaskService.shutdownNow();
                        scheduledTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    }
                    scheduledTaskService = null;
                }
                if (priorityTaskService != null) {
                    priorityTaskService.shutdown();
                    if (!priorityTaskService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                        priorityTaskService.shutdownNow();
                        priorityTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    }
                    priorityTaskService = null;
                }
                if (asyncTaskService != null) {
                    asyncTaskService.shutdown();
                    if (!asyncTaskService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                        asyncTaskService.shutdownNow();
                        asyncTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    }
                    asyncTaskService = null;
                }
            } catch (InterruptedException e) {
                if (scheduledTaskService != null) {
                    scheduledTaskService.shutdownNow();
                    scheduledTaskService = null;
                }
                if (priorityTaskService != null) {
                    priorityTaskService.shutdownNow();
                    priorityTaskService = null;
                }
                if (asyncTaskService != null) {
                    asyncTaskService.shutdownNow();
                    asyncTaskService = null;
                }
            } finally {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 销毁方法, 应在非主线程调用, 且轻易不应调用, 直到进程即将结束. 此方法将企图销毁任何任务(使用interrupt方式),
     * 包括正在执行的任务, 并不再接受新任务, 此后尽可能快的抛弃任何工作线程. 此方法较为激进, 应慎重考虑.
     */
    public final void destroyNow() {
        synchronized(CREATE_DESTROY_LOCK) {
            if (workerThread != null) {
                workerThread.quit();
                workerThread = null;
            }
            if (scheduledTaskService != null) {
                scheduledTaskService.shutdownNow();
                scheduledTaskService = null;
            }
            if (priorityTaskService != null) {
                priorityTaskService.shutdownNow();
                priorityTaskService = null;
            }
            if (asyncTaskService != null) {
                asyncTaskService.shutdownNow();
                asyncTaskService = null;
            }
        }
    }

    /** 将任务投递到主线程队列队尾等待执行. */
    public boolean runOnMainThread(@NonNull Task task) {
        return mainThreadHandler.post(task);
    }

    /** 检查当前是否在主线程, 如果是则直接调用run方法执行, 否则投递到主线程任务队列队尾执行. */
    public boolean runOnMainThreadWithCheck(@NonNull Task task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
            return true;
        } else {
            return mainThreadHandler.post(task);
        }
    }

    /** 将任务投递到主线程队列队头期望优先执行, 此任务会在当前任务执行完毕后执行, 主线程队列也可能忽略该优先待遇请求, 慎重考虑使用. */
    public boolean runOnMainThreadNow(@NonNull Task task) {
        return mainThreadHandler.postAtFrontOfQueue(task);
    }

    /** 将任务投递到主线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并返回任务的Future对象. */
    public Future<?> runOnMainThreadWithFuture(@NonNull Task task, boolean frontOfQueue) {
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!mainThreadHandler.postAtFrontOfQueue(futureTask)) {
                return null;
            }
        } else {
            if (!mainThreadHandler.post(futureTask)) {
                return null;
            }
        }
        return futureTask;
    }

    /** 将任务投递到主线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回. */
    public boolean runOnMainThreadBlocking(@NonNull Task task, boolean frontOfQueue)
            throws ExecutionException, InterruptedException, CancellationException {
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!mainThreadHandler.postAtFrontOfQueue(futureTask)) {
                return false;
            }
        } else {
            if (!mainThreadHandler.post(futureTask)) {
                return false;
            }
        }
        futureTask.get();
        return true;
    }

    /**
     * 将任务投递到主线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public boolean runOnMainThreadBlocking(@NonNull Task task, boolean frontOfQueue, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!mainThreadHandler.postAtFrontOfQueue(futureTask)) {
                return false;
            }
        } else {
            if (!mainThreadHandler.post(futureTask)) {
                return false;
            }
        }
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
        return true;
    }

    /** 使任务在经过指定毫秒的延迟后得以在主线程执行, 如为非正数, 将立即请求执行. */
    public boolean runOnMainThreadDelayed(@NonNull Task task, long delayMillis) {
        return mainThreadHandler.postDelayed(task, delayMillis);
    }

    /** 使任务在指定的时间点(化成毫秒数)得以在主线程执行, 如为非正数, 将立即请求执行. */
    public boolean runOnMainThreadAtTime(@NonNull Task task, long uptimeMillis) {
        return mainThreadHandler.postAtTime(task, uptimeMillis);
    }

    /** 将任务投递到工作线程队列队尾等待执行. */
    public boolean runOnWorkerThread(@NonNull Task task) {
        if (workerThread == null) {
            createWorkerThread();
        }
        return workerThreadHandler.post(task);
    }

    /** 检查当前是否在工作线程, 如果是则直接调用run方法执行, 否则投递到工作线程任务队列队尾执行. */
    public boolean runOnWorkerThreadWithCheck(@NonNull Task task) {
        if (workerThread == null) {
            createWorkerThread();
        }
        if (Looper.myLooper() == workerThread.getLooper()) {
            task.run();
            return true;
        } else {
            return workerThreadHandler.post(task);
        }
    }

    /** 将任务投递到工作线程队列队头期望优先执行, 此任务会在当前任务执行完毕后执行, 工作线程队列也可能忽略该优先级, 慎重考虑使用. */
    public boolean runOnWorkerThreadNow(@NonNull Task task) {
        if (workerThread == null) {
            createWorkerThread();
        }
        return workerThreadHandler.postAtFrontOfQueue(task);
    }

    /** 将任务投递到工作线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并返回任务的Future对象. */
    public Future<?> runOnWorkerThreadWithFuture(@NonNull Task task, boolean frontOfQueue) {
        if (workerThread == null) {
            createWorkerThread();
        }
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!workerThreadHandler.postAtFrontOfQueue(futureTask)) {
                return null;
            }
        } else {
            if (!workerThreadHandler.post(futureTask)) {
                return null;
            }
        }
        return futureTask;
    }

    /** 将任务投递到工作线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回. */
    public boolean runOnWorkerThreadBlocking(@NonNull Task task, boolean frontOfQueue)
            throws ExecutionException, InterruptedException, CancellationException {
        if (workerThread == null) {
            createWorkerThread();
        }
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!workerThreadHandler.postAtFrontOfQueue(futureTask)) {
                return false;
            }
        } else {
            if (!workerThreadHandler.post(futureTask)) {
                return false;
            }
        }
        futureTask.get();
        return true;
    }

    /**
     * 将任务投递到工作线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public boolean runOnWorkerThreadBlocking(@NonNull Task task, boolean frontOfQueue, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        if (workerThread == null) {
            createWorkerThread();
        }
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            if (!workerThreadHandler.postAtFrontOfQueue(futureTask)) {
                return false;
            }
        } else {
            if (!workerThreadHandler.post(futureTask)) {
                return false;
            }
        }
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
        return true;
    }

    /** 使任务在经过指定毫秒的延迟后得以在工作线程执行, 如为非正数, 将立即请求执行. */
    public boolean runOnWorkerThreadDelayed(@NonNull Task task, long delayMillis) {
        if (workerThread == null) {
            createWorkerThread();
        }
        return workerThreadHandler.postDelayed(task, delayMillis);
    }

    /** 使任务在指定的时间点(化成毫秒数)得以在工作线程执行, 如为非正数, 将立即请求执行. */
    public boolean runOnWorkerThreadAtTime(@NonNull Task task, long uptimeMillis) {
        if (workerThread == null) {
            createWorkerThread();
        }
        return workerThreadHandler.postAtTime(task, uptimeMillis);
    }

    /** 使用线程池异步执行任务. */
    public ScheduledFuture<?> runOnScheduledThreadNow(@NonNull Task task) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    /** 使用线程池同步执行任务. */
    public void runOnScheduledThreadBlocking(@NonNull Task task)
            throws ExecutionException, InterruptedException, CancellationException {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        ScheduledFuture<?> future = scheduledTaskService.schedule(task, 0, TimeUnit.MILLISECONDS);
        future.get();
    }

    /**
     * 使用线程池同步执行任务.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runOnScheduledThreadBlocking(@NonNull Task task, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        ScheduledFuture<?> future = scheduledTaskService.schedule(task, 0, TimeUnit.MILLISECONDS);
        future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池延迟指定毫秒数后异步执行任务, 如为非正数, 将立即请求执行. */
    public ScheduledFuture<?> runOnScheduledThreadDelayed(@NonNull Task task, long delayMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池在指定时间点(毫秒数表示)异步执行任务, 如为非正数, 将立即请求执行. */
    public ScheduledFuture<?> runOnScheduledThreadAtTime(@NonNull Task task, long uptimeMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.schedule(task, (uptimeMillis - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    /** 让线程池经过initialDelayMillis毫秒的延迟后开始以每隔periodMillis毫秒的周期频率重复触发执行异步任务, 而不关心正在执行的任务是否完成. */
    public ScheduledFuture<?> runOnScheduledThreadAtFixedRate(@NonNull Task task,
                                                              long initialDelayMillis, long periodMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池经过initialDelayMillis毫秒的延迟后开始异步执行任务, 任务执行完毕后延迟delayMillis毫秒重复执行异步任务. */
    public ScheduledFuture<?> runOnScheduledThreadAtWithFixedDelay(@NonNull Task task,
                                                                   long initialDelayMillis, long delayMillis) {
        if (scheduledTaskService == null) {
            createScheduledTaskService();
        }
        return scheduledTaskService.scheduleWithFixedDelay(task, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 异步投递一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法. */
    public Future<?> runPriorityTask(@NonNull PriorityTask task) {
        if (priorityTaskService == null) {
            createPriorityTaskService();
        }
        return priorityTaskService.submit(task);
    }

    /**
     * 同步执行一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法.
     * 由于投递的任务可能优先级颇低, 可能会导致阻塞很久, 慎用.
     */
    public void runPriorityTaskBlocking(@NonNull PriorityTask task)
            throws ExecutionException, InterruptedException, CancellationException {
        if (priorityTaskService == null) {
            createPriorityTaskService();
        }
        Future<?> future = priorityTaskService.submit(task);
        future.get();
    }

    /**
     * 同步执行一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法.
     * @param timeoutMillis 投递优先级任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runPriorityTaskBlocking(@NonNull PriorityTask task, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        if (priorityTaskService == null) {
            createPriorityTaskService();
        }
        Future<?> future = priorityTaskService.submit(task);
        future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 将一个任务并发执行, 并返回任务句柄. 用于那些单个可以被并发执行(即做好了线程安全策略), 以及相互依赖的任务 */
    public Future<?> runConcurrentTask(@NonNull Task task) {
        if (concurrentTaskService == null) {
            createConcurrentTaskService();
        }
        return concurrentTaskService.submit(task);
    }

    /** 鉴于更好的管理和避免Android实现变迁带来的不确定性,
     * 使用此类封装的单线程线程池执行AsyncTask的doInBackground方法, 不应再调用AsyncTask的execute方法. */
    @SafeVarargs
    public final <Params, Progress, Result> AsyncTask<Params, Progress, Result> runAsyncTask(
            @NonNull AsyncTask<Params, Progress, Result> task, Params... params) {
        if (asyncTaskService == null) {
            createAsyncTaskService();
        }
        return task.executeOnExecutor(asyncTaskService, params);
    }

    /**
     * 任务的基本抽象, 便于封装扩展和获得更佳的控制权, 并提供了getName和getId两个额外接口以供查询.
     */
    public interface Task extends Runnable {
        String ID_NOT_ASSIGNED = "IdNotAssigned";
        String getName();
        String getId();
    }

    /**
     *  优先级任务的抽象, 下面是可能的实现用例范本, 请参考:
     *  <pre>{@code
     *  public class MyPriorityTask implements PriorityTask {
     *      private static final String TAG = "MyPriorityTask";
     *      private final int mPriority;
     *      private final String mId;
     *      public MyPriorityTask(String pId, int pPriority) {
     *          mId = pId;
     *          mPriority = pPriority;
     *      }
     *      @Override
     *      public String getName() {
     *          return "HospitalRegistration";
     *      }
     *      @Override
     *      public String getId() {
     *          return mId;
     *      }
     *      @Override
     *      public int getPriority() {
     *          return mPriority;
     *      }
     *      @Override
     *      public int compareTo(@NonNull PriorityTask that) {
     *          return this.getPriority() > that.getPriority() ? 1
     *          : this.getPriority() < that.getPriority() ? -1 : 0;
     *      }
     *      @Override
     *      public void run() {
     *          // Your Runnable logical intent code ...
     *      }
     *  }
     *  }</pre>
     */
    public interface PriorityTask extends Task, Comparable<PriorityTask> {
        int getPriority();
    }

    /**
     * 安全的抽象任务类, 仅供参考
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

    /**
     * ID生成器. <br>
     * 注意: 其提供保证的接口方法有且仅有create方法, 如有其他公开方法仅应在了解其实现的情况下考虑.
     */
    public static final class IdCreator {
        private IdCreator() {}
        public static String create() {
            return UUID.randomUUID().toString();
        }
    }
}