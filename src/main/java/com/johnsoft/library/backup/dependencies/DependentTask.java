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
package com.johnsoft.library.backup.dependencies;

import com.johnsoft.library.util.SimpleTaskExecutor;

import android.os.Handler;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-23
 */
public abstract class DependentTask {
    private final Handler workHandler = SimpleTaskExecutor.createWorkHandler("DependentTask");
    private volatile boolean isInit;

    public DependentTask(final Dependency dependency) {
        workHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isInit) {
                    return;
                }
                if (dependency.isEnabled()) {
                    try {
                        postTask();
                    } finally {
                        isInit = true;
                    }
                } else {
                    final Dependency self = new AbstractDependency() {
                        @Override
                        protected boolean isSelfEnabled() {
                            return isInit;
                        }

                        @Override
                        protected void handleDependencyStateChanged(Dependency dependency, boolean enable) {
                            if (enable) {
                                try {
                                    postTask();
                                } finally {
                                    isInit = true;
                                    getDirectDependencies().remove(dependency);
                                    dependency.getDirectRelationships().remove(this);
                                }
                            }
                        }
                    };
                    self.dependsOn(dependency);
                }
            }
        });
    }

    private void postTask() {
        workHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    doTask();
                } finally {
                    workHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            workHandler.getLooper().quit();
                        }
                    }, 5_000L);
                }
            }
        });
    }

    protected abstract void doTask();
}
