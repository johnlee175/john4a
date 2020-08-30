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
package com.johnsoft.library.util.tasks.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.johnsoft.library.util.NameableThreadFactory;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-03
 */
public final class TaskDependenceGraph {
    private final Map<String, TaskInfo> taskInfoContext = new LinkedHashMap<>();
    private final Set<TaskInfo> finishedTaskInfos = new LinkedHashSet<>();
    private final Lock taskContextLock = new ReentrantLock();
    private final Lock taskFinishedLock = new ReentrantLock();
    private final Condition taskFinishedCondition = taskFinishedLock.newCondition();
    private final ExecutorService taskExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(16), new NameableThreadFactory("Dependent-Task-Executor"),
            new ThreadPoolExecutor.CallerRunsPolicy());
    private final Set<TaskExecuteListener> taskExecuteListeners = new LinkedHashSet<>();
    private final ExecutorService listenerExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new NameableThreadFactory("Task-Listener-Executor"),
            new ThreadPoolExecutor.AbortPolicy());
    private final byte[] listenersLock = new byte[0];

    public void addTask(DependentTask task) {
        try {
            taskContextLock.lockInterruptibly();
            String name = task.getName();
            TaskInfo taskInfo = taskInfoContext.get(name);
            if (taskInfo == null) {
                taskInfo = new TaskInfo(task);
                taskInfoContext.put(name, taskInfo);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            taskContextLock.unlock();
        }
    }

    public void bindDependency(DependentTask from, DependentTask to) {
        try {
            taskContextLock.lockInterruptibly();
            TaskInfo taskInfoFrom = taskInfoContext.get(from.getName());
            TaskInfo taskInfoTo = taskInfoContext.get(to.getName());
            taskInfoFrom.dependsOn(taskInfoTo);
            taskInfoTo.dependedBy(taskInfoFrom);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            taskContextLock.unlock();
        }
    }

    public void unbindDependency(DependentTask from, DependentTask to) {
        try {
            taskContextLock.lockInterruptibly();
            TaskInfo taskInfoFrom = taskInfoContext.get(from.getName());
            TaskInfo taskInfoTo = taskInfoContext.get(to.getName());
            taskInfoFrom.noLongerDependsOn(taskInfoTo);
            taskInfoTo.noLongerDependedBy(taskInfoFrom);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            taskContextLock.unlock();
        }
    }

    public void executeTasks() throws ExecutionException {
        try {
            List<TaskInfo> taskInfos = topologicalSort();
            try {
                taskFinishedLock.lockInterruptibly();
                finishedTaskInfos.clear();
            } finally {
                taskFinishedLock.unlock();
            }
            ArrayList<TaskUnit> list = new ArrayList<>();
            for (TaskInfo taskInfo : taskInfos) {
                list.add(new TaskUnit(taskInfo));
            }
            List<Future<Void>> futures= taskExecutor.invokeAll(list);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void executeTasks(DependentTask fromTask) throws ExecutionException {
        try {
            List<TaskInfo> taskInfos = topologicalSort();
            ArrayList<TaskUnit> list = new ArrayList<>();
            boolean isStart = false;
            for (TaskInfo taskInfo : taskInfos) {
                if (taskInfo.getName().equals(fromTask.getName())) {
                    isStart = true;
                }
                if (isStart) {
                    list.add(new TaskUnit(taskInfo));
                    try {
                        taskFinishedLock.lockInterruptibly();
                        finishedTaskInfos.remove(taskInfo);
                    } finally {
                        taskFinishedLock.unlock();
                    }
                }
            }
            List<Future<Void>> futures= taskExecutor.invokeAll(list);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fireTaskStateChanged(DependentTask task, Object event) {
        String name = task.getName();
        TaskInfo taskInfo;
        try {
            taskContextLock.lockInterruptibly();
            taskInfo = taskInfoContext.get(name);
        } catch (InterruptedException e) {
            e.printStackTrace();
            taskInfo = null;
        } finally {
            taskContextLock.unlock();
        }
        if (taskInfo != null) {
            List<TaskInfo> directPaths = taskInfo.getDirectPaths();
            for (TaskInfo taskInfoInPath : directPaths) {
                taskInfoInPath.getTask().onDependentTaskStateChanged(name, event);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        taskExecutor.shutdownNow();
        listenerExecutor.shutdownNow();
        super.finalize();
    }

    private List<TaskInfo> topologicalSort() throws InterruptedException {
        LinkedList<TaskInfo> topologicalSortSequence = new LinkedList<>();
        LinkedList<TaskInfo> queue = new LinkedList<>();
        LinkedList<TaskInfo> taskInfos;
        try {
            taskContextLock.lockInterruptibly();
            taskInfos = new LinkedList<>(taskInfoContext.values());
        } finally {
            taskContextLock.unlock();
        }
        for (TaskInfo taskInfo : taskInfos) {
            taskInfo.inDegree = taskInfo.getDirectDependencies().size();
            if (taskInfo.inDegree == 0) {
                taskInfo.isRemoved = true;
                queue.addLast(taskInfo);
            }
        }
        while (!queue.isEmpty()) {
            TaskInfo taskInfo = queue.removeFirst();
            topologicalSortSequence.add(taskInfo);
            for (TaskInfo node : taskInfos) {
                if (node.isRemoved) {
                    continue;
                }
                if (node.getDirectDependencies().contains(taskInfo)) {
                    if (--node.inDegree == 0) {
                        node.isRemoved = true;
                        queue.addLast(node);
                    }
                }
            }
        }
        boolean noCycle = true;
        for (TaskInfo taskInfo : taskInfos) {
            taskInfo.isRemoved = false;
            if (taskInfo.inDegree > 0) {
                noCycle = false;
            }
        }
        if (!noCycle) {
            throw new RuntimeException("Had cycle in graph");
        }
        System.out.println(topologicalSortSequence);
        return topologicalSortSequence;
    }

    private void prepareTask(final TaskInfo taskInfo) {
        listenerExecutor.submit(new Runnable() {
            @Override
            public void run() {
                synchronized(listenersLock) {
                    for (TaskExecuteListener listener : taskExecuteListeners) {
                        listener.onTaskPrepared(taskInfo.getName());
                    }
                }
            }
        });
    }

    private void finishTask(final TaskInfo taskInfo) {
        listenerExecutor.submit(new Runnable() {
            @Override
            public void run() {
                synchronized(listenersLock) {
                    for (TaskExecuteListener listener : taskExecuteListeners) {
                        listener.onTaskFinished(taskInfo.getName());
                    }
                }
            }
        });
    }

    public void addTaskExecuteListener(TaskExecuteListener listener) {
        synchronized(listenersLock) {
            taskExecuteListeners.add(listener);
        }
    }

    public void removeTaskExecuteListener(TaskExecuteListener listener) {
        synchronized(listenersLock) {
            taskExecuteListeners.remove(listener);
        }
    }

    public interface TaskExecuteListener {
        void onTaskPrepared(String name);
        void onTaskFinished(String name);
    }

    public static final class TaskInfo {
        private final byte[] dependenciesLock = new byte[0];
        private final byte[] pathsLock = new byte[0];
        private final Set<TaskInfo> dependencies = new LinkedHashSet<>();
        private final Set<TaskInfo> paths = new LinkedHashSet<>();
        private final DependentTask task;
        private final String name;
        private int inDegree;
        private boolean isRemoved;

        public TaskInfo(DependentTask task) {
            if (task == null || task.getName() == null || task.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("DependentTask is null or its name is empty");
            }
            this.task = task;
            this.name = task.getName();
        }

        public String getName() {
            return name;
        }

        public DependentTask getTask() {
            return task;
        }

        public List<TaskInfo> getDirectDependencies() {
            synchronized(dependenciesLock) {
                return new LinkedList<>(dependencies);
            }
        }

        public List<TaskInfo> getDirectPaths() {
            synchronized(pathsLock) {
                return new LinkedList<>(paths);
            }
        }

        public void dependsOn(TaskInfo info) {
            synchronized(dependenciesLock) {
                dependencies.add(info);
            }
            task.onTaskDependencyChanged(info.name, true);
        }

        public void dependedBy(TaskInfo info) {
            synchronized(pathsLock) {
                paths.add(info);
            }
        }

        public void noLongerDependsOn(TaskInfo info) {
            synchronized(dependenciesLock) {
                dependencies.remove(info);
            }
            task.onTaskDependencyChanged(info.name, false);
        }

        public void noLongerDependedBy(TaskInfo info) {
            synchronized(pathsLock) {
                paths.remove(info);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TaskInfo taskInfo = (TaskInfo) o;
            return name.equals(taskInfo.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final class TaskUnit implements Callable<Void> {
        private final TaskInfo taskInfo;
        TaskUnit(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }
        @Override
        public Void call() {
            try {
                try {
                    taskFinishedLock.lockInterruptibly();
                    while (!finishedTaskInfos.containsAll(taskInfo.getDirectDependencies())) {
                        taskFinishedCondition.await(2000L, TimeUnit.MILLISECONDS);
                    }
                } finally {
                    taskFinishedLock.unlock();
                }
                prepareTask(taskInfo);
                taskInfo.getTask().run();
                finishTask(taskInfo);
                try {
                    taskFinishedLock.lockInterruptibly();
                    finishedTaskInfos.add(taskInfo);
                    taskFinishedCondition.signalAll();
                } finally {
                    taskFinishedLock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
