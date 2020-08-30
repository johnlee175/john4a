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
package com.johnsoft.library.mediacache;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Cache policy holder
 * @author John Kenrinus Lee
 * @version 2016-07-20
 */
public class DiskCachePolicy {
    private DiskCachePolicy() {}

    public static final long MAX_DISK_CACHE_SIZE = 64L * 1024 * 1024;
    public static final String DISK_CACHE_DIRECTORY_NAME = "policy_medias";
    private static DiskLruCache diskLruCache;

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath = null;
        //如果sd卡存在并且没有被移除
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            final File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null) {
                cachePath = externalCacheDir.getPath();
            }
        }
        if (cachePath == null) {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 1;
        }
    }

    /** If call then just call once on application create */
    public static synchronized void open(@NonNull File cacheDiectory, int version, long maxCacheSize) {
        try {
            diskLruCache = DiskLruCache.open(cacheDiectory, version, 2, maxCacheSize);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** If call then just call once on application create */
    public static synchronized void open(@NonNull Context context) {
        final Context appContext = context.getApplicationContext();
        try {
            diskLruCache = DiskLruCache.open(getDiskCacheDir(appContext, DISK_CACHE_DIRECTORY_NAME),
                    getAppVersion(appContext), 2, MAX_DISK_CACHE_SIZE);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    public static synchronized File get(@NonNull String key) throws IOException {
        if (diskLruCache != null) {
            final DiskLruCache.Snapshot snapshot = diskLruCache.get(Md5Utils.encryptMD5(key));
            if (snapshot != null) {
                return snapshot.getFile(0);
            }
        }
        return null;
    }

    @Nullable
    public static synchronized RollbackableOutputStream set(@NonNull String key) throws IOException {
        if (diskLruCache != null) {
            final DiskLruCache.Editor editor = diskLruCache.edit(Md5Utils.encryptMD5(key));
            if (editor != null) {
                return new RollbackableOutputStream(editor.newOutputStream(0), diskLruCache, editor);
            }
        }
        return null;
    }

    /**
     * @return [0] = ETag; [1] = Last-Modified;
     */
    public static synchronized String[] getToken(@NonNull String key) throws IOException {
        if (diskLruCache != null) {
            final DiskLruCache.Snapshot snapshot = diskLruCache.get(Md5Utils.encryptMD5(key));
            if (snapshot != null) {
                String token = snapshot.getString(1);
                if (token != null && !token.trim().isEmpty()) {
                    return token.split("\\r\\n");
                }
            }
        }
        return null;
    }

    /** will auto commit */
    public static synchronized void setToken(@NonNull String key, @Nullable String eTag, @Nullable String lastModified)
            throws IOException {
        if (diskLruCache != null) {
            final DiskLruCache.Editor editor = diskLruCache.edit(Md5Utils.encryptMD5(key));
            if (editor != null) {
                editor.set(1, (eTag == null ? "" : eTag) + "\r\n" + (lastModified == null ? "" : lastModified));
                editor.commit();
            }
        }
    }

    public static synchronized boolean remove(@NonNull String key) throws IOException {
        if (diskLruCache != null) {
            final boolean result = diskLruCache.remove(Md5Utils.encryptMD5(key));
            diskLruCache.flush();
            return result;
        }
        return false;
    }

    /** Call close() if not use DiskCachePolicy any more, should call it just the application will be destoryed. */
    public static synchronized void close() throws IOException {
        if (diskLruCache != null) {
            diskLruCache.close();
            diskLruCache = null;
        }
    }

    public static final class RollbackableOutputStream extends FilterOutputStream {
        private DiskLruCache cache;
        private DiskLruCache.Editor editor;
        private boolean handled;
        private boolean isClosed;

        protected RollbackableOutputStream(OutputStream out, DiskLruCache cache, DiskLruCache.Editor editor) {
            super(out);
            this.cache = cache;
            this.editor = editor;
        }

        public void commit() throws IOException {
            if (!handled) {
                handled = true;
                editor.commit();
                cache.flush();
            }
        }

        public void rollback() throws IOException {
            if (!handled) {
                handled = true;
                editor.abort();
                cache.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (!isClosed) {
                isClosed = true;
                super.close();
                if (!handled) {
                    commit();
                }
                cache = null;
                editor = null;
            }
        }
    }
}
