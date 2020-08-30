/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.johnsoft.library;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * 上传log的服务
 * @author John Kenrinus Lee
 * @version 2016-05-06
 */
public abstract class UploadLogService extends IntentService {
    private static final String TAG = "UploadLogService";

    public UploadLogService() {
        super("Log-Uploader");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        uploadLogcatLogs();
        if (BuildConfig.FLAVOR.contains("system")) {
            uploadAnrTracesTxt();
        }
        Log.i(TAG, "UploadLogService.onHandleIntent(Intent): All files uploaded!");
    }

    private void uploadLogcatLogs() {
        try {
            LogcatRecorder.singleInstance.turnToNextFile();
            final List<File> newFiles = LogcatRecorder.singleInstance.listZipFiles();
            Collections.sort(newFiles, Collections.<File>reverseOrder());
            for (final File file : newFiles) {
                if (file == null || !file.exists() && !file.canRead()) {
                    continue;
                }
                doUploadSync(file, file.getName());
            }
        } catch (Exception e) {
            Log.w("System.err", "", e);
        }
    }

    private void uploadAnrTracesTxt() {
        try {
            final File file = new File("/data/anr/traces.txt");
            if (file.exists() && file.canRead()) {
                doUploadSync(file, file.getName());
            }
        } catch (Exception e) {
            Log.w("System.err", "", e);
        }
    }

    protected abstract void doUploadSync(final File file, String logName) throws IOException;
}
