package com.johnsoft.library;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.johnsoft.library.util.ConcurrentDateFormat;
import com.johnsoft.library.util.DiskBlockSliceSink;
import com.johnsoft.library.util.SafeGenPathFactory;
import com.johnsoft.library.util.ZipUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Capture log to file, and keep the freshness of the log in the file and the total size of the file
 * @author John Kenrinus Lee
 * @version 2016-04-27
 */
public enum LogcatRecorder implements DiskBlockSliceSink.FileSwitchListener {
    singleInstance;

    private static final String LOG_TAG = "LogcatRecorder";
    public static final String CHILD_FOLDRE_NAME = "logcat_data";

    private final byte[] lock = new byte[0]; // lock status changed

    private OldestLogCleaning cleaningThread;
    private LogcatRecording recordingThread;
    private boolean isStarting;

    private final byte[] waitZipConditionLock = new byte[0]; //lock for zip
    private boolean isZipping;

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

    public void turnToNextFile() {
        if (recordingThread != null && !recordingThread.isInterrupted()
                && recordingThread.writer != null) {
            try {
                recordingThread.writer.turnToNextFile();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Recorder turn to next file failed.");
            }
        }
    }

    public List<File> listZipFiles() {
        final ArrayList<File> fileList = new ArrayList<>();
        final File folder = getFolderFile(null, CHILD_FOLDRE_NAME);
        if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canRead()) {
            Log.w(LOG_TAG, "Logcat data not found.");
            return fileList;
        }
        synchronized(waitZipConditionLock) {
            try {
                while (isZipping) {
                    Log.w(LOG_TAG, "Waiting on LogcatRecorder.listZipFiles()...");
                    waitZipConditionLock.wait(500L);
                }
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, e);
            }
        }
        // use flat, no recursive
        final File[] files = folder.listFiles();
        Arrays.sort(files, Collections.<File>reverseOrder());
        // filter
        for (int i = 1; i < files.length; ++i) {
            final File file = files[i];
            final String fileName = file.getName();
            if (file.isFile() && fileName.matches("^logcat\\.dump\\.\\S+\\.log.*")) {
                if (!fileName.endsWith(".zip")) {
                    fileList.add(zipItAndDelIt(file.getAbsolutePath()));
                } else {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    @Override
    public void onFileClosed(String filePath) {
        File file = new File(filePath);
        if (file.length() > 8) {
            // Why is 8 byte? because of the pre-open file may be stuff some block,
            // and lack of 8 bytes will not cause an obstacle to the semantic understanding.
            zipItAndDelIt(filePath);
        } else { // delete pre-open file no matter whether it's out of date or not.
            try {
                Log.w(LOG_TAG, "Deleting logcat file [" + filePath + "] ...");
                if (!file.delete()) {
                    Log.w(LOG_TAG, "Delete logcat file [" + filePath + "] failed.");
                }
            } catch (Throwable e) {
                Log.e(LOG_TAG, "Delete logcat file [" + filePath + "] failed.", e);
            }
        }
    }

    private class  OldestLogCleaning extends Thread {
        private final Pattern fileNamePattern = Pattern.compile("^logcat\\.dump\\.(\\S+)\\.log.*");
        @Override
        public void run() {
            while (true) {
                if (isInterrupted()) {
                    Log.w(LOG_TAG, "OldestLogCleaning Thread has been interrupted.");
                    break;
                }
                final String timestamp = ConcurrentDateFormat.getUnsigned().format(new Date());
                try {
                    Thread.sleep(30 * 1000L/*30 second*/);
                } catch (InterruptedException e) {
                    Log.w(LOG_TAG, "OldestLogCleaning Thread has been interrupted.");
                    break;
                }
                final File folder = getFolderFile(null, CHILD_FOLDRE_NAME);
                if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canRead()) {
                    Log.w(LOG_TAG, "Nothing clean for logcat data.");
                    continue;
                } else {
                    Log.w(LOG_TAG, "Logcat data stored at " + folder.getAbsolutePath() + ".");
                }
                synchronized(waitZipConditionLock) {
                    try {
                        while (isZipping) {
                            Log.w(LOG_TAG, "Waiting on OldestLogCleaning.run()...");
                            waitZipConditionLock.wait(500L);
                        }
                    } catch (InterruptedException e) {
                        Log.w(LOG_TAG, e);
                    }
                }
                // use flat, no recursive
                final File[] files = folder.listFiles();
                // filter
                final ArrayList<File> fileList = new ArrayList<>();
                for (final File file : files) {
                    if (file.isFile()) {
                        final Matcher matcher = fileNamePattern.matcher(file.getName());
                        if (matcher.matches()) {
                            // Just comment it. Let onFileClosed(String) do it. -- John Kenrinus Lee
//                            if ((timestamp.compareTo(matcher.group(1)) > 0) && file.length() <= 8) {
//                                //delete pre-open file which out of date
//                                final String fileAbsolutePath = file.getAbsolutePath();
//                                try {
//                                    Log.w(LOG_TAG, "Deleting logcat file [" + fileAbsolutePath + "] ...");
//                                    if (!file.delete()) {
//                                        Log.w(LOG_TAG, "Delete logcat file [" + fileAbsolutePath + "] failed.");
//                                    }
//                                } catch (Throwable e) {
//                                    Log.e(LOG_TAG, "Delete logcat file [" + fileAbsolutePath + "] failed.", e);
//                                }
//                            } else {
//                                fileList.add(file);
//                            }
                            fileList.add(file);
                        }
                    }
                }
                // because of more complex when using file length, and due to time constraints, use file count instead.
                int diff = fileList.size() - (100 + 2)/*one may be being edited, one may be pre-opened*/;
                if (diff < 0) {
                    Log.w(LOG_TAG, "Nothing clean for logcat data.");
                } else { // delete old files
                    Log.w(LOG_TAG, "Cleaning logcat data...");
                    Collections.sort(fileList);
                    for (int i = 0; i < diff; ++i) {
                        final File file = fileList.get(i);
                        if (!file.delete()) {
                            Log.w(LOG_TAG, "Delete logcat file [" + file.getAbsolutePath() + "] failed finally.");
                        }
                    }
                }
            }
        }
    }

    private class LogcatRecording extends Thread {
        BufferedReader reader = null;
        DiskBlockSliceSink writer = null;
        @Override
        public void run() {
            try {
                final File folder = getFolderFile(null, CHILD_FOLDRE_NAME);
                if (folder == null) {
                    Log.w(LOG_TAG, "No storage for logcat data.");
                    return;
                }
                // use flat, no recursive
                writer = new DiskBlockSliceSink(2L * 1024 * 1024, 0.15f, 5,
                        new SafeGenPathFactory(folder.getAbsolutePath(), "logcat.dump.", "log"));
                writer.addFileSwitchListener(LogcatRecorder.this);
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
                    writer.write((line + "\n").getBytes("UTF-8"));
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

    private File zipItAndDelIt(String srcPath) {
        synchronized(waitZipConditionLock) {
            File result;
            isZipping = true;
            try {
                File srcFile = new File(srcPath);
                File zipFile = new File(srcPath + ".zip");
                ZipUtils.zipFilesQuietly(zipFile, srcFile);
                if (zipFile.exists()) {
                    Log.w(LOG_TAG, "Had zipped. Deleting logcat file [" + srcPath + "] ... ");
                    if (!srcFile.delete()) {
                        Log.w(LOG_TAG, "Delete logcat file [" + srcPath + "] failed after zipped.");
                    }
                    result = zipFile;
                } else {
                    result = srcFile;
                }
            } finally {
                isZipping = false;
                waitZipConditionLock.notifyAll();
            }
            return result;
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
