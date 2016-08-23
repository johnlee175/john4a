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

import com.johnsoft.library.template.BaseApplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-23
 */
public class ConnectivityDependency extends AbstractDependency {
    private static final ConnectivityDependency dependency = new ConnectivityDependency();

    public static ConnectivityDependency self() {
        return dependency;
    }

    @Override
    protected boolean isSelfEnabled() {
        return getConnectivityInfo().available;
    }

    @Override
    protected void handleDependencyStateChanged(Dependency dependency, boolean enable) {
        // Do nothing
    }

    public static final class ConnectivityInfo {
        public boolean available; // default: disable
        public int type = -1; // default: ConnectivityManager.TYPE_NONE
    }

    public static ConnectivityInfo getConnectivityInfo() {
        final Context appContext = BaseApplication.getApplication();
        final ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();
        final ConnectivityInfo info = new ConnectivityInfo();
        info.available = netInfo != null && netInfo.isAvailable() && netInfo.isConnectedOrConnecting();
        if (info.available) {
            System.out.println("NOTICE: The current network type is " + netInfo.getTypeName());
            info.type = netInfo.getType();
        }
        return info;
    }
}
