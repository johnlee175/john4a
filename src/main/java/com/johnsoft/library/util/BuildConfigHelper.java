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
package com.johnsoft.library.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * 获取Context对应包名的BuildConfig类的常量信息
 * @author John Kenrinus Lee
 * @version 2016-09-05
 */
public final class BuildConfigHelper {
    private static final Map<String, BuildConfigHelper> sBuildConfigHelpers = new HashMap<>();
    private static BuildConfigHelper currentHelper;
    private boolean debug;
    private String applicationId;
    private String buildType;
    private String flavor;
    private String versionName;
    private int versionCode;
    private Map<String, Object> customBuildConfigFields = new HashMap<>();

    public static synchronized BuildConfigHelper initWithContext(@NonNull Context appContext,
                                                                 String...customBuildConfigFields) {
        String packageName = appContext.getPackageName();
        BuildConfigHelper helper = sBuildConfigHelpers.get(packageName);
        if (helper != null) {
            return helper;
        }
        helper = new BuildConfigHelper();
        helper.debug = (boolean) getBuildConfigValue(appContext, "DEBUG");
        helper.applicationId = (String) getBuildConfigValue(appContext, "APPLICATION_ID");
        helper.buildType = (String) getBuildConfigValue(appContext, "BUILD_TYPE");
        helper.flavor = (String) getBuildConfigValue(appContext, "FLAVOR");
        helper.versionName = (String) getBuildConfigValue(appContext, "VERSION_NAME");
        helper.versionCode = (int) getBuildConfigValue(appContext, "VERSION_CODE");
        for (String field: customBuildConfigFields) {
            helper.customBuildConfigFields.put(field, getBuildConfigValue(appContext, field));
        }
        sBuildConfigHelpers.put(packageName, helper);
        return helper;
    }

    public static synchronized BuildConfigHelper getWithContext(@NonNull Context appContext) {
        return sBuildConfigHelpers.get(appContext.getPackageName());
    }

    public static synchronized void setCurrentHelper(BuildConfigHelper currentHelper) {
        BuildConfigHelper.currentHelper = currentHelper;
    }

    public static synchronized BuildConfigHelper getCurrentHelper() {
        return currentHelper;
    }

    private static Object getBuildConfigValue(Context context, String fieldName) {
        try {
            Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private BuildConfigHelper() {}

    public boolean isDebug() {
        return debug;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getBuildType() {
        return buildType;
    }

    public String getFlavor() {
        return flavor;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public Object getCustomBuildConfigField(String fieldName) {
        return customBuildConfigFields.get(fieldName);
    }
}
