package com.johnsoft.library.util.thread;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.johnsoft.library.annotation.OutMainThread;
import com.johnsoft.library.annotation.Singleton;
import com.johnsoft.library.annotation.ThreadSafe;
import com.johnsoft.library.util.Resource;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * 此类是同步异步线程任务执行器线程安全单实例. <br>
 * 此类便于投递操作到主线程, 并为执行异步任务提供的可重用设施, 便于线程间通信. <br>
 * 其中: <br>
 * runOnMainThread*系列方法将异步投递任务到主线程执行; <br>
 * runOnWorkerThread*系列方法将异步投递任务到使用新的HandlerThread创建的工作线程上执行; <br>
 * runOnScheduledThread*系列方法将异步投递任务到线程池中执行; <br>
 * 也有其他方法为那些非时间单位为优先级依据的任务, 可以被多个线程并发的任务, 相互依赖的任务和AsyncTask提供了支持; <br>
 * FAQ: runOnScheduledThread*系列方法相对于runOnWorkerThread*系列方法都是非主线程异步, 其区别主要有两个: <br>
 * 一是对于时间精度要求高的任务在runOnWorkerThread*可能无法得到满足, 而runOnScheduledThread*将由多个工作线程共同工作, <br>
 * 但runOnWorkerThread*相对于runOnScheduledThread*更可控和简单; <br>
 * 二是runOnScheduledThread*自带了定时循环执行周期性任务的能力, 而runOnWorkerThread*需要由调用方在逻辑最后重新投递任务来实现相同功能, <br>
 * 但显然runOnWorkerThread*不如runOnScheduledThread*精度高和灵活性高; <br>
 * 注意: 首要的, 所有接口方法并未做参数判空, 请务必自行处理; <br>
 * 注意: 任务边界应该独立清晰并尤其做好<b>阻塞超时</b>, 由于目前非runOnScheduledThread*系列方法都是采用单线程机制完成,
 * 不应向其前后投递互有依赖的任务以造成死锁, 也不应投递不可中断的阻塞任务; <br>
 * 注意: 任务应完全自行处理所有异常, 此框架未作任何加工; <br>
 * 注意: 线程池的创建并未采用"用时创建"这样的懒创建方式, 而是饥饿创建并改成不可变类以尽可能避免线程安全问题
 * TODO 目前查询任务执行情况的机制未加入, 可能需要自定义ThreadPoolExecutor, ScheduledThreadPoolExecutor子类的afterExecute并用map管理任务ID, 但因为销毁资源时比较复杂, 未来版本可能提供实现
 *
 * @author John Kenrinus Lee
 * @version 2015-08-07
 */
@Singleton
@ThreadSafe
public enum TaskExecutor implements Resource {
    singleInstance;

    TaskExecutor() {
        mainThreadHandler = new Handler(Looper.getMainLooper());
        workerThread = new HandlerThread("TaskExecutor-WorkerThread");
        workerThread.start();
        workerThreadHandler = new Handler(workerThread.getLooper());
        final int cpuCount = Runtime.getRuntime().availableProcessors();
        scheduledExecutorService = new ScheduledThreadPoolExecutor(cpuCount,
                new TaskThreadFactory("TaskExecutor-ScheduledThread"));
        priorityTaskService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.NANOSECONDS,
                new PriorityBlockingQueue<Runnable>(),
                new TaskThreadFactory("TaskExecutor-PriorityTask"));
        asyncTaskService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new TaskThreadFactory("TaskExecutor-AsyncTask"));
        concurrentTaskService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new TaskThreadFactory("TaskExecutor-ConcurrentTask"));
    }

    /** 接口方法, 初始化一些环节, 应在非主线程调用, 目前空实现. */
    @OutMainThread
    @Override
    public final void initialize() {
        // Do nothing
    }

    /** 接口方法, 销毁方法, 应在非主线程调用, 目前使用destroySafely实现. */
    @OutMainThread
    @Override
    public final void destroy() {
        destroySafely();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void quitSafely() {
        workerThread.quitSafely();
    }

    /**
     * 销毁方法, 应在非主线程调用, 且轻易不应调用, 直到进程即将结束. 此方法将销毁空闲线程, 并不再接受新任务,
     * 如无意外, 将等待所有池中已有的任务完成后退出. 此方法较为保守, 相对于{@link #destroyNow()}应首先考虑.
     * @see #destroy()
     */
    @OutMainThread
    public final void destroySafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            quitSafely();
        } else {
            workerThread.quit();
        }
        try {
            scheduledExecutorService.shutdown();
            if (!scheduledExecutorService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                scheduledExecutorService.shutdownNow();
                scheduledExecutorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            }
            priorityTaskService.shutdown();
            if (!priorityTaskService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                priorityTaskService.shutdownNow();
                priorityTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            }
            asyncTaskService.shutdown();
            if (!asyncTaskService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                asyncTaskService.shutdownNow();
                asyncTaskService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
            priorityTaskService.shutdownNow();
            asyncTaskService.shutdownNow();
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 销毁方法, 应在非主线程调用, 且轻易不应调用, 直到进程即将结束. 此方法将企图销毁任何任务(使用interrupt方式), 包括正在执行的任务,
     * 并不再接受新任务, 此后尽可能快的抛弃任何工作线程. 此方法较为激进, 应慎重考虑.
     * @see #destroy()
     */
    @OutMainThread
    public final void destroyNow() {
        workerThread.quit();
        scheduledExecutorService.shutdownNow();
        priorityTaskService.shutdownNow();
        asyncTaskService.shutdownNow();
    }

    /** 将任务投递到主线程队列队尾等待执行. */
    public void runOnMainThread(@NonNull Task task) {
        mainThreadHandler.post(task);
    }

    /** 检查当前是否在主线程, 如果是则直接调用run方法执行, 否则投递到主线程任务队列队尾执行. */
    public void runOnMainThreadWithCheck(@NonNull Task task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainThreadHandler.post(task);
        }
    }

    /** 将任务投递到主线程队列队头期望优先执行, 此任务会在当前任务执行完毕后执行, 主线程队列也可能忽略该优先待遇请求, 所以应慎重考虑后使用. */
    public void runOnMainThreadNow(@NonNull Task task) {
        mainThreadHandler.postAtFrontOfQueue(task);
    }

    /** 将任务投递到主线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回. */
    public void runOnMainThreadBlocking(@NonNull Task task, boolean frontOfQueue)
            throws ExecutionException, InterruptedException, CancellationException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            mainThreadHandler.postAtFrontOfQueue(futureTask);
        } else {
            mainThreadHandler.post(futureTask);
        }
        futureTask.get();
    }

    /**
     * 将任务投递到主线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runOnMainThreadBlocking(@NonNull Task task, boolean frontOfQueue, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            mainThreadHandler.postAtFrontOfQueue(futureTask);
        } else {
            mainThreadHandler.post(futureTask);
        }
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 使任务在经过指定毫秒的延迟后得以在主线程执行, 如为非正数, 将立即请求执行. */
    public void runOnMainThreadDelayed(@NonNull Task task, long delayMillis) {
        mainThreadHandler.postDelayed(task, delayMillis);
    }

    /** 使任务在指定的时间点(化成毫秒数)得以在主线程执行, 如为非正数, 将立即请求执行. */
    public void runOnMainThreadAtTime(@NonNull Task task, long uptimeMillis) {
        mainThreadHandler.postAtTime(task, uptimeMillis);
    }

    /** 将任务投递到工作线程队列队尾等待执行. */
    public void runOnWorkerThread(@NonNull Task task) {
        workerThreadHandler.post(task);
    }

    /** 检查当前是否在工作线程, 如果是则直接调用run方法执行, 否则投递到工作线程任务队列队尾执行. */
    public void runOnWorkerThreadWithCheck(@NonNull Task task) {
        if (Looper.myLooper() == workerThread.getLooper()) {
            task.run();
        } else {
            workerThreadHandler.post(task);
        }
    }

    /** 将任务投递到工作线程队列队头期望优先执行, 此任务会在当前任务执行完毕后执行, 工作线程队列也可能忽略该优先待遇请求, 所以应慎重考虑后使用. */
    public void runOnWorkerThreadNow(@NonNull Task task) {
        workerThreadHandler.postAtFrontOfQueue(task);
    }

    /** 将任务投递到工作线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回. */
    public void runOnWorkerThreadBlocking(@NonNull Task task, boolean frontOfQueue)
            throws ExecutionException, InterruptedException, CancellationException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            workerThreadHandler.postAtFrontOfQueue(futureTask);
        } else {
            workerThreadHandler.post(futureTask);
        }
        futureTask.get();
    }

    /**
     * 将任务投递到工作线程队列对头或队尾(取决于第二个参数frontOfQueue)等待执行, 并等待任务执行完成后返回.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runOnWorkerThreadBlocking(@NonNull Task task, boolean frontOfQueue, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        if (frontOfQueue) {
            workerThreadHandler.postAtFrontOfQueue(futureTask);
        } else {
            workerThreadHandler.post(futureTask);
        }
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 使任务在经过指定毫秒的延迟后得以在工作线程执行, 如为非正数, 将立即请求执行. */
    public void runOnWorkerThreadDelayed(@NonNull Task task, long delayMillis) {
        workerThreadHandler.postDelayed(task, delayMillis);
    }

    /** 使任务在指定的时间点(化成毫秒数)得以在工作线程执行, 如为非正数, 将立即请求执行. */
    public void runOnWorkerThreadAtTime(@NonNull Task task, long uptimeMillis) {
        workerThreadHandler.postAtTime(task, uptimeMillis);
    }

    /** 使用线程池异步执行任务. */
    public void runOnScheduledThreadNow(@NonNull Task task) {
        scheduledExecutorService.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    /** 使用线程池同步执行任务. */
    public void runOnScheduledThreadBlocking(@NonNull Task task)
            throws ExecutionException, InterruptedException, CancellationException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        scheduledExecutorService.schedule(futureTask, 0, TimeUnit.MILLISECONDS);
        futureTask.get();
    }

    /**
     * 使用线程池同步执行任务.
     * @param timeoutMillis 投递任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runOnScheduledThreadBlocking(@NonNull Task task, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        scheduledExecutorService.schedule(futureTask, 0, TimeUnit.MILLISECONDS);
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池延迟指定毫秒数后异步执行任务, 如为非正数, 将立即请求执行. */
    public void runOnScheduledThreadDelayed(@NonNull Task task, long delayMillis) {
        scheduledExecutorService.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池在指定时间点(毫秒数表示)异步执行任务, 如为非正数, 将立即请求执行. */
    public void runOnScheduledThreadAtTime(@NonNull Task task, long uptimeMillis) {
        scheduledExecutorService.schedule(task, (uptimeMillis - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    /** 让线程池经过initialDelayMillis毫秒的延迟后开始以每隔periodMillis毫秒的周期频率重复触发执行异步任务, 而不关心正在执行的任务是否完成. */
    public void runOnScheduledThreadAtFixedRate(@NonNull Task task, long initialDelayMillis, long periodMillis) {
        scheduledExecutorService.scheduleAtFixedRate(task, initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    /** 让线程池经过initialDelayMillis毫秒的延迟后开始异步执行任务, 任务执行完毕后延迟delayMillis毫秒重复执行异步任务. */
    public void runOnScheduledThreadAtWithFixedDelay(@NonNull Task task, long initialDelayMillis, long delayMillis) {
        scheduledExecutorService.scheduleWithFixedDelay(task, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 异步投递一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法. */
    public void runPriorityTask(@NonNull PriorityTask task) {
        priorityTaskService.execute(task);
    }

    /**
     * 同步执行一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法.
     * 由于投递的任务可能优先级颇低, 可能会导致阻塞很久, 慎用.
     */
    public void runPriorityTaskBlocking(@NonNull PriorityTask task)
            throws ExecutionException, InterruptedException, CancellationException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        priorityTaskService.execute(futureTask);
        futureTask.get();
    }

    /**
     * 同步执行一个带优先级的任务, 需要自己约定优先级规则, 但这些任务不能无视getPriority和compareTo接口方法.
     * @param timeoutMillis 投递优先级任务并等待其完成, 如果超过此毫秒数则不再等待, 此时任务可能还在执行, 但当前线程流程会往下走.
     */
    public void runPriorityTaskBlocking(@NonNull PriorityTask task, long timeoutMillis)
            throws ExecutionException, InterruptedException, CancellationException, TimeoutException {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        priorityTaskService.execute(futureTask);
        futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /** 将一个任务并发执行, 并返回任务句柄. 主要用于那些单个可以被并发执行(即做好了线程安全策略), 以及相互依赖的任务(依赖大容量主线程的特性) */
    public FutureTask<Void> runConcurrentTask(@NonNull Task task) {
        FutureTask<Void> futureTask = new FutureTask<>(task, null);
        concurrentTaskService.execute(futureTask);
        return futureTask;
    }

    /** 鉴于更好的管理和避免Android实现变迁带来的不确定性, 使用此类封装的单线程线程池执行AsyncTask的doInBackground方法, 不应再调用AsyncTask的execute方法. */
    public <Params, Progress, Result> void runAsyncTask(@NonNull AsyncTask<Params, Progress, Result> task, Params... params) {
        task.executeOnExecutor(asyncTaskService, params);
    }

    private final Handler mainThreadHandler;
    private final Handler workerThreadHandler;
    private final HandlerThread workerThread;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService priorityTaskService;
    private final ExecutorService asyncTaskService;
    private final ExecutorService concurrentTaskService;

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
     * 可组织依赖关系的任务的抽象, 一般应实现为组合模式, 如果一个任务依赖另一些任务, 应设计为等待这些依赖任务执行完毕后执行此任务.
     * 注: 与优先级任务的区别在于, 依赖任务可以不与被依赖任务在同一线程队列上得到执行(也可以将依赖关系实现为优先级关系在同一个线程队列上安排任务).
     * 比如:
     * 有两个线程队列X和Y, X上有两个任务A和B, A依赖Y队列上的C任务, B没有依赖任务,
     * 此时可以挂起A的状态, 启动Y队列上的C任务, 然后直接执行B任务, 当C任务执行完毕回调修改A任务状态为可执行, 在适当时机安排A任务于队列X上执行.
     */
    public interface DependenciesTask extends Task {
        List<DependenciesTask> getDependencies();
    }

    /**
     * 可取消的任务, 其实现设计应考虑:
     * (1)添加cancel方法以便可以取消任务, 一般设置标志为已取消(此时isCancelled返回true);
     * (2)添加getState方法以便查看任务是否已停止, 从收到取消命令(此时isCancelled返回true)到任务真正停止可能有时间间隔;
     * (3)某种情况下, 也可以在任务完全停止后使isCancelled返回true, 但实现应给与充分说明;
     */
    public interface CancelableTask extends Task {
        boolean isCancelled();
    }

    /**
     * 安全的抽象任务类, 仅供参考, 重要意图是想表明任务应自捕获异常
     */
    public static abstract class AbstractSafeTask implements Task {
        @Override
        public final void run() {
            TaskManager.singleInstance.setTask(this);
            try {
                doTask();
            } catch (Exception e) {
                onThrowable(e);
            } finally {
                TaskManager.singleInstance.removeTask(getId());
            }
        }
        /** 不再实现run(), 而是实现该方法 */
        public abstract void doTask();
        /** 任何异常或错误都可能或可以调用该方法 */
        public abstract void onThrowable(Throwable t);
    }

    /**
     * 任务管理器, 目前只公开查询接口, 任务的索引添加和索引删除由任务类本身机制实现, 仅供参考.
     * 注: 这里在任务开始前添加索引, 任务执行完毕后删除索引, 也可以在任务创建处, 或者提交处添加索引, 也可以不删除索引以供查询.
     * 这里由{@link com.johnsoft.library.util.thread.TaskExecutor.AbstractSafeTask}实现.
     */
    public enum TaskManager {
         singleInstance;

        private final Map<String, Task> taskPool = new ConcurrentHashMap<>();

        private void setTask(@NonNull Task task) {
            taskPool.put(task.getId(), task);
        }

        private void removeTask(@NonNull String id) {
            taskPool.remove(id);
        }

        private Task getTask(@NonNull String id) {
            return taskPool.get(id);
        }

        /** 以任务ID查询一个任务是否正在执行. */
        public Task searchTask(String id) {
            return id == null ? null : getTask(id);
        }
    }

    /**
     * ID生成器. <br>
     * 注意: 其提供保证的接口方法有且仅有create方法, 如有其他公开方法仅应在了解其实现的情况下考虑.
     */
    public static final class IdCreator {
        private IdCreator() {}
        public static final String create() {
            return UUID.randomUUID().toString();
        }
    }

    /** 线程池工厂类的一个实现, 一来增大后期的控制权, 而来为每个创建的线程起一个特定意义的名字. */
    private static class TaskThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final String namePrefix;

        public TaskThreadFactory(@NonNull String namePrefix) {
            this.namePrefix = namePrefix;
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(group, r, (namePrefix + threadNumber.getAndIncrement()), 0) {
                // nothing override
            };
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
