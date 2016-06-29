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
package com.johnsoft.library.util.tasks.graph.test;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import com.johnsoft.library.util.tasks.graph.DependentTask;
import com.johnsoft.library.util.tasks.graph.TaskDependenceGraph;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-05
 */
public class TestGraph {
    public static void main(String[] args) {
        TaskDependenceGraph graph = new TaskDependenceGraph();
        graph.addTaskExecuteListener(new TaskDependenceGraph.TaskExecuteListener() {
            @Override
            public void onTaskPrepared(String name) {
            }
            @Override
            public void onTaskFinished(String name) {
                System.err.println("DUDU Finished " + name);
            }
        });
        DependentTask c0 = createTaskInfo("c0", graph);
        DependentTask c1 = createTaskInfo("c1", graph);
        DependentTask c2 = createTaskInfo("c2", graph);
        DependentTask c3 = createTaskInfo("c3", graph);
        DependentTask c4 = createTaskInfo("c4", graph);
        DependentTask c5 = createTaskInfo("c5", graph);
        DependentTask c6 = createTaskInfo("c6", graph);
        DependentTask c7 = createTaskInfo("c7", graph);
        DependentTask c8 = createTaskInfo("c8", graph);
        graph.bindDependency(c2, c0);
        graph.bindDependency(c2, c1);
        graph.bindDependency(c3, c2);
        graph.bindDependency(c3, c4);
        graph.bindDependency(c4, c1);
        graph.bindDependency(c5, c3);
        graph.bindDependency(c5, c4);
        graph.bindDependency(c6, c0);
        graph.bindDependency(c7, c6);
        graph.bindDependency(c8, c3);
        graph.bindDependency(c8, c7);
        try {
            graph.executeTasks();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        graph = new TaskDependenceGraph();
        DependentTask ct0 = createTaskInfo("0", graph);
        DependentTask ct1 = createTaskInfo("1", graph);
        DependentTask ct2 = createTaskInfo("2", graph);
        DependentTask ct3 = createTaskInfo("3", graph);
        DependentTask ct4 = createTaskInfo("4", graph);
        DependentTask ct5 = createTaskInfo("5", graph);
        DependentTask ct6 = createTaskInfo("6", graph);
        DependentTask ct7 = createTaskInfo("7", graph);
        DependentTask ct8 = createTaskInfo("8", graph);
        DependentTask ct9 = createTaskInfo("9", graph);
        DependentTask ct10 = createTaskInfo("10", graph);
        DependentTask ct11 = createTaskInfo("11", graph);
        DependentTask ct12 = createTaskInfo("12", graph);
        graph.bindDependency(ct0, ct2);
        graph.bindDependency(ct1, ct0);
        graph.bindDependency(ct3, ct2);
        graph.bindDependency(ct4, ct5);
        graph.bindDependency(ct4, ct6);
        graph.bindDependency(ct5, ct0);
        graph.bindDependency(ct5, ct3);
        graph.bindDependency(ct6, ct0);
        graph.bindDependency(ct6, ct7);
        graph.bindDependency(ct7, ct8);
        graph.bindDependency(ct9, ct6);
        graph.bindDependency(ct10, ct9);
        graph.bindDependency(ct11, ct9);
        graph.bindDependency(ct12, ct9);
        graph.bindDependency(ct12, ct11);
        try {
            graph.executeTasks();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        graph.fireTaskStateChanged(ct0, "service down");
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            graph.executeTasks(ct9);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static DependentTask createTaskInfo(final String name, final TaskDependenceGraph graph) {
        DependentTask task = new DependentTask() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void onDependentTaskStateChanged(String name, Object event) {
                System.err.println("%%%%%%%%%%%%" + getName() + " -> " + name + " -> " + event);
                graph.fireTaskStateChanged(this, event);
            }

            @Override
            public void onTaskDependencyChanged(String name, boolean addOrRemove) {
                System.err.println("&&&&&&&&&&&&&" + getName() + " -> " + name + " -> " + addOrRemove);
            }

            @Override
            public void run() {
                try {
                    Thread.sleep(new Random().nextInt(30) * 100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println(">>>>>>>>> " + name);
            }
        };
        graph.addTask(task);
        return task;
    }
}
