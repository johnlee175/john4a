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
package com.johnsoft.library.util.cache;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-20
 */
public class DiskCachePolicy {
    private static final long MAX_DISK_CACHE_SIZE = 64L * 1024 * 1024;
    private static final String DISK_CACHE_DIRECTORY_NAME = "policy_medias";

    public static DiskLruCache open(Context appContext) {
        try {
            return DiskLruCache.open(getDiskCacheDir(appContext, DISK_CACHE_DIRECTORY_NAME),
                    getAppVersion(appContext), 1, MAX_DISK_CACHE_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath = null;
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

    private static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static InputStream get(DiskLruCache cache, String key) throws IOException {
        if (cache != null) {
            final DiskLruCache.Snapshot snapshot = cache.get(Md5Utils.encryptMD5(key));
            if (snapshot != null) {
                return snapshot.getInputStream(0);
            }
        }
        return null;
    }

    public static RollbackableOutputStream set(DiskLruCache cache, String key) throws IOException {
        if (cache != null) {
            final DiskLruCache.Editor editor = cache.edit(Md5Utils.encryptMD5(key));
            if (editor != null) {
                return new RollbackableOutputStream(editor.newOutputStream(0), cache, editor);
            }
        }
        return null;
    }

    public static boolean remove(DiskLruCache cache, String key) throws IOException {
        if (cache != null) {
            final boolean result = cache.remove(Md5Utils.encryptMD5(key));
            cache.flush();
            return result;
        }
        return false;
    }

    public static void close(DiskLruCache cache) throws IOException {
        if (cache != null) {
            cache.close();
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
