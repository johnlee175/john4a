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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-07
 */
public class DefaultDependentTask implements DependentTask {
    private final String name;
    private final List<Action<DefaultDependentTask>> actions;
    private final TaskDependenceGraph graph;

    public DefaultDependentTask(String taskName, TaskDependenceGraph taskGraph,
                                List<Action<DefaultDependentTask>> actionList) {
        name = taskName;
        graph = taskGraph;
        actions = actionList;
        graph.addTask(this);
    }

    public DefaultDependentTask(String taskName, TaskDependenceGraph taskGraph,
                                Action<DefaultDependentTask>...actionList) {
        name = taskName;
        graph = taskGraph;
        actions = Arrays.asList(actionList);
        graph.addTask(this);
    }

    public final void fireTaskStateChanged(Object event) {
        graph.fireTaskStateChanged(this, event);
    }

    public final void dependsOn(DependentTask task) {
        graph.bindDependency(this, task);
    }

    public final void noLongerDependsOn(DependentTask task) {
        graph.unbindDependency(this, task);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onDependentTaskStateChanged(String name, Object event) {
        System.err.println("Task" + name + " state changed with event: " + event + ". "
                + "Now callback " + getName() + "#onDependentTaskStateChanged(String, Object).");
        graph.fireTaskStateChanged(this, event);
    }

    @Override
    public void onTaskDependencyChanged(String name, boolean addOrRemove) {
        System.err.println("Task" + name + (addOrRemove ? " added to " : " removed from ") + "dependency. "
                + "Now callback " + getName() + "#onTaskDependencyChanged(String, boolean).");
    }

    @Override
    public void run() {
        List<Action<DefaultDependentTask>> actionList = Collections.unmodifiableList(this.actions);
        for (Action<DefaultDependentTask> action : actionList) {
            action.execute(this);
        }
    }

    @Override
    public String toString() {
        return "Task-" + name;
    }
}
