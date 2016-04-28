package com.johnsoft.library;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import com.johnsoft.library.util.DiskBlockSliceSink;
import com.johnsoft.library.util.SafeGenPathFactory;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Capture log to file, and keep the freshness of the log in the file and the total size of the file
 * @author John Kenrinus Lee
 * @version 2016-04-27
 */
public enum LogcatRecorder {
    singleInstance;

    private static final String LOG_TAG = "LogcatRecorder";

    private final byte[] lock = new byte[0];
    private OldestLogCleaning cleaningThread;
    private LogcatRecording recordingThread;
    private boolean isStarting;

    public void start() {
        synchronized(lock) {
            if (!isStarting) {
                cleaningThread = new OldestLogCleaning();
                recordingThread = new LogcatRecording();
                cleaningThread.start();
                recordingThread.start();
                isStarting = true;
            }
        }
    }

    public void stop() {
        synchronized(lock) {
            if (isStarting) {
                if (!cleaningThread.isInterrupted()) {
                    cleaningThread.interrupt();
                }
                if (!recordingThread.isInterrupted()) {
                    recordingThread.interrupt();
                }
                isStarting = false;
            }
        }
    }

    public boolean isStarting() {
        synchronized(lock) {
            return isStarting;
        }
    }

    private class OldestLogCleaning extends Thread {
        @Override
        public void run() {
            while (true) {
                if (isInterrupted()) {
                    Log.w(LOG_TAG, "OldestLogCleaning Thread has been interrupted.");
                    break;
                }
                try {
                    Thread.sleep(60 * 1000L/*1 minute*/);
                } catch (InterruptedException e) {
                    Log.w(LOG_TAG, "OldestLogCleaning Thread has been interrupted.");
                    break;
                }
                final File folder = getFolderFile(null, "logcat_data");
                if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canRead()) {
                    Log.w(LOG_TAG, "Nothing clean for logcat data.");
                    continue;
                } else {
                    Log.w(LOG_TAG, "Logcat data stored at " + folder.getAbsolutePath() + ".");
                }
                // use flat, no recursive
                final File[] files = folder.listFiles();
                // filter
                final ArrayList<File> fileList = new ArrayList<>();
                for (final File file : files) {
                    if (file.isFile() && file.getName().matches("^logcat\\.dump\\.\\S+\\.log$")) {
                        fileList.add(file);
                    }
                }
                int diff = fileList.size() - (20 + 2)/*one may be being edited, one may be pre-opened*/;
                if (diff < 0) {
                    Log.w(LOG_TAG, "Nothing clean for logcat data.");
                } else { // delete old files
                    Log.w(LOG_TAG, "Cleaning logcat data...");
                    Collections.sort(fileList);
                    for (int i = 0; i < diff; ++i) {
                        final File file = fileList.get(i);
                        if (!file.delete()) {
                            Log.w(LOG_TAG, "Delete logcat file " + file.getAbsolutePath() + " failed.");
                        }
                    }
                }
            }
        }
    }

    private class LogcatRecording extends Thread {
        @Override
        public void run() {
            BufferedReader reader = null;
            DiskBlockSliceSink writer = null;
            try {
                final File folder = getFolderFile(null, "logcat_data");
                if (folder == null) {
                    Log.w(LOG_TAG, "No storage for logcat data.");
                    return;
                }
                // use flat, no recursive
                writer = new DiskBlockSliceSink(5L * 1024 * 1024, 0.15f, 5,
                        new SafeGenPathFactory(folder.getAbsolutePath(), "logcat.dump.", "log"));
                final Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c",
                "logcat -v threadtime"});
                final InputStream inputStream = process.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isInterrupted()) {
                        Log.w(LOG_TAG, "LogcatRecording Thread has been interrupted.");
                        break;
                    }
                    writer.write(line.getBytes("UTF-8"));
                }
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    private static File getFolderFile(Context context, String folderName) {
        final File folder;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            folder = Environment.getExternalStorageDirectory();
        } else {
            if (context != null) {
                folder = context.getFilesDir();
            } else {
                folder = null;
            }
        }
        if (folder != null && folderName != null && !folderName.trim().isEmpty()) {
            return new File(folder, folderName);
        } else {
            return null;
        }
    }
}
