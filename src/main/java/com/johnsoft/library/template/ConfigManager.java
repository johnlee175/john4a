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
package com.johnsoft.library.template;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-16
 */
public final class ConfigManager {
    private ConfigManager() {
    }

    private static final byte[] DEFAULT_CONFIGURATION_LOCK = new byte[0];
    private static final byte[] CONFIGURATION_LISTENER_LOCK = new byte[0];
    private static Configuration defaultConfiguration;
    private static SharedPreferences sharedPreferences;
    private static final Handler workHandler = createWorkHandler("ConfigChangeCallback");
    private static final SharedPreferenceChangeListener listener = new SharedPreferenceChangeListener();
    private static final Object contentStub = new Object();
    private static final WeakHashMap<OnConfigurationChangedListener, Object> listeners = new WeakHashMap<>();

    private static Handler createWorkHandler(String name) {
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    public static void setDefaultConfiguration(Configuration config) {
        synchronized(DEFAULT_CONFIGURATION_LOCK) {
            defaultConfiguration = config;
        }
        notifyAllObserver(config);
    }

    public static Configuration getDefaultConfiguration() {
        synchronized(DEFAULT_CONFIGURATION_LOCK) {
            return defaultConfiguration;
        }
    }

    /** 一般可传入PreferenceManager.getDefaultSharedPreferences(Context) */
    private static Configuration createDefaultConfiguration(SharedPreferences sharedPreferences) {
        // 从SharedPreferences提取过去存储的配置值到Configuration对象上并返回
        //        return new Configuration.Builder(sharedPreferences.getAll()).build();
        final Configuration.Builder builder;
        synchronized(DEFAULT_CONFIGURATION_LOCK) {
            if (defaultConfiguration == null) {
                builder = new Configuration.Builder();
            } else {
                builder = defaultConfiguration.newBuilder();
            }
        }
        builder.setForwardServerResponse(sharedPreferences
                .getBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE, Setting.FORWARD_SERVER_RESPONSE));
        return builder.build();
    }

    // 只接收SharedPreferences, 如果配置从外部程序中传来Bundle, 或从网络中下载配置并使用gson转换,
    // 或者本地文件/数据库等其他存储, 都先填充成SharedPreferences
    public static void bindSharedPreferences(SharedPreferences preferences) {
        sharedPreferences = preferences;
        final Configuration configuration = createDefaultConfiguration(preferences);
        preferences.registerOnSharedPreferenceChangeListener(listener);
        setDefaultConfiguration(configuration);
    }

    public static void registerOnConfigurationChangedListener(OnConfigurationChangedListener l) {
        synchronized(CONFIGURATION_LISTENER_LOCK) {
            listeners.put(l, contentStub);
        }
    }

    public static void unregisterOnConfigurationChangedListener(OnConfigurationChangedListener l) {
        synchronized(CONFIGURATION_LISTENER_LOCK) {
            listeners.remove(l);
        }
    }

    private static void notifyAllObserver(final Configuration config) {
        // 广播通知所有监听者
        workHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized(CONFIGURATION_LISTENER_LOCK) {
                    for (OnConfigurationChangedListener l : listeners.keySet()) {
                        l.onConfigurationChanged(config);
                    }
                }
            }
        });
    }

    private static final class SharedPreferenceChangeListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences preferences, final String key) {
            workHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (sharedPreferences == preferences) {
                        final Configuration configuration = createDefaultConfiguration(preferences);
                        setDefaultConfiguration(configuration);
                    }
                }
            });
        }
    }

    public interface OnConfigurationChangedListener {
        void onConfigurationChanged(Configuration newConfiguration);
    }

    public static final class ConvertUtils {
        public static void fromBundle(Bundle bundle, SharedPreferences preferences) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                    bundle.getBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE));
            editor.apply();
        }

        public static void fromMap(Map<String, Object> netGsonMap, SharedPreferences preferences) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                    (boolean) netGsonMap.get(Configuration.KEY_FORWARD_SERVER_RESPONSE));
            editor.apply();
        }

        public static void fromJSON(JSONObject jsonObject, SharedPreferences preferences)
                throws JSONException {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                    jsonObject.getBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE));
            editor.apply();
        }

        public static void fromProperties(Properties properties, SharedPreferences preferences) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                    Boolean.valueOf(properties.getProperty(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                            String.valueOf(Setting.FORWARD_SERVER_RESPONSE))));
            editor.apply();
        }

        public static void fromSqliteCursor(Cursor cursor, SharedPreferences preferences) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE,
                    cursor.getInt(cursor.getColumnIndex(Configuration.KEY_FORWARD_SERVER_RESPONSE)) != 0);
            editor.apply();
        }
    }
}
