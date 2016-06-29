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

import com.johnsoft.library.util.tasks.graph.Action;
import com.johnsoft.library.util.tasks.graph.DefaultDependentTask;
import com.johnsoft.library.util.tasks.graph.TaskDependenceGraph;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-07
 */
public class TestDefaultTask {
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
        DefaultDependentTask c0 = new DefaultDependentTask("c0", graph, createAction("c0"));
        DefaultDependentTask c1 = new DefaultDependentTask("c1", graph, createAction("c1"));
        DefaultDependentTask c2 = new DefaultDependentTask("c2", graph, createAction("c2"));
        DefaultDependentTask c3 = new DefaultDependentTask("c3", graph, createAction("c3"));
        DefaultDependentTask c4 = new DefaultDependentTask("c4", graph, createAction("c4"));
        DefaultDependentTask c5 = new DefaultDependentTask("c5", graph, createAction("c5"));
        DefaultDependentTask c6 = new DefaultDependentTask("c6", graph, createAction("c6"));
        DefaultDependentTask c7 = new DefaultDependentTask("c7", graph, createAction("c7"));
        DefaultDependentTask c8 = new DefaultDependentTask("c8", graph, createAction("c8"));
        c2.dependsOn(c0);
        c2.dependsOn(c1);
        c3.dependsOn(c2);
        c3.dependsOn(c4);
        c4.dependsOn(c1);
        c5.dependsOn(c3);
        c5.dependsOn(c4);
        c6.dependsOn(c0);
        c7.dependsOn(c6);
        c8.dependsOn(c3);
        c8.dependsOn(c7);
        try {
            graph.executeTasks();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        graph = new TaskDependenceGraph();
        DefaultDependentTask ct0 = new DefaultDependentTask("0", graph, createAction("0"));
        DefaultDependentTask ct1 = new DefaultDependentTask("1", graph, createAction("1"));
        DefaultDependentTask ct2 = new DefaultDependentTask("2", graph, createAction("2"));
        DefaultDependentTask ct3 = new DefaultDependentTask("3", graph, createAction("3"));
        DefaultDependentTask ct4 = new DefaultDependentTask("4", graph, createAction("4"));
        DefaultDependentTask ct5 = new DefaultDependentTask("5", graph, createAction("5"));
        DefaultDependentTask ct6 = new DefaultDependentTask("6", graph, createAction("6"));
        DefaultDependentTask ct7 = new DefaultDependentTask("7", graph, createAction("7"));
        DefaultDependentTask ct8 = new DefaultDependentTask("8", graph, createAction("8"));
        DefaultDependentTask ct9 = new DefaultDependentTask("9", graph, createAction("9"));
        DefaultDependentTask ct10 = new DefaultDependentTask("10", graph, createAction("10"));
        DefaultDependentTask ct11 = new DefaultDependentTask("11", graph, createAction("11"));
        DefaultDependentTask ct12 = new DefaultDependentTask("12", graph, createAction("12"));
        ct0.dependsOn(ct2);
        ct1.dependsOn(ct0);
        ct3.dependsOn(ct2);
        ct4.dependsOn(ct5);
        ct4.dependsOn(ct6);
        ct5.dependsOn(ct0);
        ct5.dependsOn(ct3);
        ct6.dependsOn(ct0);
        ct6.dependsOn(ct7);
        ct7.dependsOn(ct8);
        ct9.dependsOn(ct6);
        ct10.dependsOn(ct9);
        ct11.dependsOn(ct9);
        ct12.dependsOn(ct9);
        ct12.dependsOn(ct11);
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
        ct0.fireTaskStateChanged("service down");
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

    private static Action createAction(final String name) {
        return new Action<DefaultDependentTask>() {
            @Override
            public void execute(DefaultDependentTask task) {
                try {
                    Thread.sleep(new Random().nextInt(30) * 100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println(">>>>>>>>> " + name);
            }
        };
    }
}
