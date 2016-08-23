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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-23
 */
public class DependencySupport {
    private final Set<Dependency> dependencies = new LinkedHashSet<>();
    private final Set<Dependency> relationships = new LinkedHashSet<>();
    private final Dependency self;

    public DependencySupport(Dependency self) {
        this.self = self;
    }

    public Set<Dependency> getDirectDependencies() {
        return dependencies;
    }

    public Set<Dependency> getDirectRelationships() {
        return relationships;
    }

    public boolean isDependenciesEnabled() {
        for (Dependency dependency : getDirectDependencies()) {
            if (!dependency.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    public void dependsOn(Dependency dependency) {
        getDirectDependencies().add(dependency);
        dependency.getDirectRelationships().add(self);
    }

    public void fireSelfStateChanged(boolean enable) {
        for (Dependency dependency : getDirectRelationships()) {
            dependency.onDependencyStateChanged(self, enable);
        }
    }

    public void onDependencyStateChanged(Dependency dependency, boolean enable) {
        fireSelfStateChanged(enable);
    }
}
