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

import java.util.Set;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-23
 */
public abstract class AbstractDependency implements Dependency {
    private final DependencySupport support = new DependencySupport(this);

    @Override
    public final Set<Dependency> getDirectDependencies() {
        return support.getDirectDependencies();
    }

    @Override
    public final Set<Dependency> getDirectRelationships() {
        return support.getDirectRelationships();
    }

    @Override
    public final void dependsOn(Dependency dependency) {
        support.dependsOn(dependency);
    }

    @Override
    public final boolean isEnabled() {
        return isSelfEnabled() && isDependenciesEnabled();
    }

    @Override
    public final boolean isDependenciesEnabled() {
        return support.isDependenciesEnabled();
    }

    @Override
    public final void fireSelfStateChanged(boolean enable) {
        support.fireSelfStateChanged(enable);
    }

    @Override
    public final void onDependencyStateChanged(Dependency dependency, boolean enable) {
        support.onDependencyStateChanged(dependency, enable);
        handleDependencyStateChanged(dependency, enable);
    }

    protected abstract boolean isSelfEnabled();
    protected abstract void handleDependencyStateChanged(Dependency dependency, boolean enable);
}
