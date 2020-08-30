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
package com.johnsoft.library.util.log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.johnsoft.library.annotation.NonNull;
import com.johnsoft.library.annotation.NotThreadSafe;
import com.johnsoft.library.util.StringListFile;
import com.johnsoft.library.util.log.LogFileProcessService.CleanupStrategy;
import com.johnsoft.library.util.log.LogFileProcessService.ScheduleStrategy;
import com.johnsoft.library.util.log.LogFileProcessService.UploadStrategy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * A default strategy factory for LogFileProcessService needed. <br>
 * You can implements more custom strategy for LogFileProcessService. <br>
 * You also can ignore LogFileProcessService, and do upload or cleanup log file manually,
 * for example when receive a upload command, zip all the log files which in the last three days and upload it. <br>
 * <p>The important tendency is that the newer the log files, the greater the value. </p>
 *
 * @author John Kenrinus Lee
 * @version 2015-11-01
 * @see LogFileProcessService
 * @see com.johnsoft.library.util.log.LogFileProcessService.CleanupStrategy
 * @see com.johnsoft.library.util.log.LogFileProcessService.UploadStrategy
 * @see com.johnsoft.library.util.log.LogFileProcessService.ScheduleStrategy
 */
public final class LogFileProcessStrategyFactory {
    private LogFileProcessStrategyFactory() {}

    public enum TimeInterval {
        HOUR(1000L * 60 * 60),
        DAY(1000L * 60 * 60 * 24),
        WEEK(1000L * 60 * 60 * 24 * 7),
        MONTH(1000L * 60 * 60 * 24 * 30);
        private long mInterval;
        TimeInterval(long pInterval) {
            mInterval = pInterval;
        }
        public long getInterval() {
            return mInterval;
        }
    }

    public enum MemSizeUnit {
        KB(1024L),
        MB(1024L * 1024),
        GB(1024L * 1024 * 1024);
        private long mBytes;
        MemSizeUnit(long pBytes) {
            mBytes = pBytes;
        }
        public long getBytes() {
            return mBytes;
        }
    }

    /**
     * Using for {@link UploadStrategy} to indicate what to do after per log file uploaded.
     */
    public enum AfterUploadPolicy {
        /**
         * remove the uploaded file after had uploaded
         */
        REMOVE,
        /**
         * mark had uploaded to record list after had uploaded
         */
        MARK,
        /**
         * do nothing for uploaded file after had uploaded
         */
        IGNORE
    }

    /**
     * Indicate how to upload concretely via client, because of UploadStrategy just offer abstract steps.
     */
    public interface UploadOutline {
        /**
         * Encrypt a text log file.
         * @param src a text log file.
         * @return should return different file, as a example,
         *         if the input file is "a.log.txt", the output file is "a.log.enc".
         */
        File encrypt(File src);

        /**
         * Compress a encrypted log file.
         * @param src a encrypted log file.
         * @return should return different file, as a example,
         *         if the input file is "a.log.enc", the output file is "a.log.gz".
         */
        File compress(File src);

        /**
         * Upload a compressed log file.
         * @param src a compressed log file.
         * @return if upload success, should return true, else return false.
         */
        boolean upload(File src);
    }

    /**
     * A independent cleanup strategy.
     * @return the strategy will delete all log files which in log file directory.
     */
    public static CleanupStrategy getClearCleanupStrategy() {
        return new CleanupStrategy() {
            @Override
            public void cleanup(@NonNull File logFileDir) {
                for (File logFile : logFileDir.listFiles()) {
                    logFile.delete();
                }
            }
        };
    }

    /**
     * A independent cleanup strategy.
     * @return the strategy will delete the oldest log files,
     * to ensure the log files count which in log file directory less than
     * given values of the parameters.
     */
    public static CleanupStrategy getCountCleanupStrategy(int pMaxCount) {
        return new CountCleanupStrategy(pMaxCount);
    }

    /**
     * A independent cleanup strategy.<br>
     * Example: call getSizeCleanupStrategy(100, MemSizeUnit.MB),
     * means if the disk size of log file directory more than 100MB, will trigger cleanup.
     *
     * @return the strategy will delete the oldest log files,
     * to ensure the total log files's disk size less than
     * given values of the parameters.
     */
    public static CleanupStrategy getSizeCleanupStrategy(int pMaxSize, MemSizeUnit pUnit) {
        return new SizeCleanupStrategy(pMaxSize, pUnit);
    }

    /**
     * A independent cleanup strategy.<br>
     * @return the strategy will delete all log files which last modified time older than
     * given values of the parameters(millisecond).
     */
    public static CleanupStrategy getTimeCleanupStrategy(long pMinTimeMillis) {
        return new TimeCleanupStrategy(pMinTimeMillis);
    }

    /**
     * A independent cleanup strategy.<br>
     * Example: call getIntervalCleanupStrategy(3, TimeInterval.DAY),
     * means check the last modified time of the log files, if modified before 3 days, just delete it.
     *
     * @return the strategy will delete all log files which last modified time older than
     * given values of the parameters.
     */
    public static CleanupStrategy getIntervalCleanupStrategy(int pMaxCount, TimeInterval pInterval) {
        return new IntervalCleanupStrategy(pMaxCount, pInterval);
    }

    /**
     * A upload strategy, which include cleanup strategy. The strategy means:
     * check the newest log file which not uploaded and using to write every times, and upload it.<br>
     * The parameter named pWorkDir which type of File, indicate which the uploaded.list record file store.
     *
     * @see com.johnsoft.library.util.log.LogFileProcessStrategyFactory.AfterUploadPolicy
     * @see com.johnsoft.library.util.log.LogFileProcessStrategyFactory.UploadOutline
     *
     */
    public static UploadStrategy getNewestUploadStrategy(AfterUploadPolicy pPolicy,
                                                         UploadOutline pOutline, File pWorkDir) {
        return new NewestUploadStrategy(pPolicy, pOutline, pWorkDir);
    }

    /**
     * A upload strategy, which include cleanup strategy. The strategy means:
     * sort current files in log directory by last modified time, then mark the result list, and upload it.
     * So the files which created after sort action will be ignored this time, they will be upload next time as
     * current task.
     * If some files in result list not uploaded this time, they also will be uploaded next time as left list task.
     * <br>
     * The parameter named pWorkDir which type of File, indicate which the uploadable.list and left.list record file
     * store.
     *
     * @see com.johnsoft.library.util.log.LogFileProcessStrategyFactory.AfterUploadPolicy
     * @see com.johnsoft.library.util.log.LogFileProcessStrategyFactory.UploadOutline
     */
    public static UploadStrategy getPointUploadStrategy(AfterUploadPolicy pPolicy,
                                                        UploadOutline pOutline, File pWorkDir) {
        return new PointUploadStrategy(pPolicy, pOutline, pWorkDir);
    }

    /**
     * A independent schedule strategy.
     * @param pContext a non-null Context object, the method will resolved the application context by calling
     *                 getApplicationContext().
     * @param pMaxCount a quantity value, see the parameter which type of TimeInterval.
     * @param pInterval time unit.
     * @return the default implementation of ScheduleStrategy.
     */
    public static ScheduleStrategy getDefaultScheduleStrategy(Context pContext, int pMaxCount, TimeInterval pInterval) {
        return new DefaultScheduleStrategy(pContext, pMaxCount, pInterval);
    }

    //in descending order according to the file was last modified,
    //the lower index for the newer file
    private static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
        @Override
         public int compare(File lhs, File rhs) {
            long lv = lhs.lastModified();
            long rv = rhs.lastModified();
            return lv == rv ? 0 : lv > rv ? -1 : 1;
        }
    };

    private static class CountCleanupStrategy implements CleanupStrategy {
        private int mMaxCount;
        public CountCleanupStrategy(int pMaxCount) {
            mMaxCount = pMaxCount;
        }
        @Override
        public void cleanup(@NonNull File logFileDir) {
            final File[] logFiles = logFileDir.listFiles();
            final int length = logFiles.length;
            if (length <= mMaxCount) {
                return;
            }
            Arrays.sort(logFiles, FILE_COMPARATOR);
            for (int i = mMaxCount; i < length; ++i) {
                logFiles[i].delete();
            }
        }
    }

    private static class SizeCleanupStrategy implements CleanupStrategy {
        private long mTotalBytes;
        public SizeCleanupStrategy(int pMaxSize, MemSizeUnit pUnit) {
            mTotalBytes = pMaxSize * pUnit.getBytes();
        }
        @Override
        public void cleanup(@NonNull File logFileDir) {
            final File[] logFiles = logFileDir.listFiles();
            final int length = logFiles.length;
            Arrays.sort(logFiles, FILE_COMPARATOR);
            long totalUsageSpace = 0L;
            int pos = -1;
            for (int i = 0; i < length; ++i) {
                totalUsageSpace += logFiles[i].length();
                if (totalUsageSpace > mTotalBytes) {
                    pos = i;
                    break;
                }
            }
            if (pos == -1) {
                return;
            }
            for (int i = pos; i < length; ++i) {
                logFiles[i].delete();
            }
        }
    }

    private static class TimeCleanupStrategy implements CleanupStrategy {
        private long mMinTimeMillis;
        public TimeCleanupStrategy(long pMinTimeMillis) {
            mMinTimeMillis = pMinTimeMillis;
        }
        public long getMinTimeMillis() {
            return mMinTimeMillis;
        }
        @Override
        public void cleanup(@NonNull File logFileDir) {
            final File[] logFiles = logFileDir.listFiles();
            long timeMillis = getMinTimeMillis();
            for (File logFile: logFiles) {
                if (logFile.lastModified() < timeMillis) {
                    logFile.delete();
                }
            }
        }
    }

    private static class IntervalCleanupStrategy extends TimeCleanupStrategy {
        private int mMaxCount;
        private TimeInterval mInterval;
        public IntervalCleanupStrategy(int pMaxCount, TimeInterval pInterval) {
            super(0);
            mMaxCount = pMaxCount;
            mInterval = pInterval;
        }
        @Override
        public long getMinTimeMillis() {
            return System.currentTimeMillis() - mMaxCount * mInterval.getInterval();
        }
    }

    private static class NewestUploadStrategy implements UploadStrategy {
        private final ArrayBlockingQueue<File> newestLogFiles = new ArrayBlockingQueue<>(5);
        private volatile boolean isNoUploadableFile;
        private AfterUploadPolicy mPolicy;
        private UploadOutline mOutline;
        private File mWorkDir;
        private StringListFile mUploadedRecord;
        private File mUploadedListFile;
        private Thread mNewestLogFileProducer;
        private Thread mNewestLogFileConsumer;
        private ExecutorService mExecutorService;

        public NewestUploadStrategy(AfterUploadPolicy pPolicy, UploadOutline pOutline, File pWorkDir) {
            mPolicy = pPolicy;
            mOutline = pOutline;
            mWorkDir = pWorkDir;
        }

        private File getNewestLogFile(File logFileDir) {
            long lastModifiedCache = 0L;
            File newestLogFile = null;
            final File[] files =logFileDir.listFiles();
            for (File file : files) {
                final long lastModified = file.lastModified();
                if (lastModified > lastModifiedCache) {
                    boolean usingFile = false, hadUploaded = false;
                    for (String path : LogFileManager.getCurrentPaths()) {
                        if (path.equals(file.getAbsolutePath())) {
                            usingFile = true;
                        }
                    }
                    synchronized(mUploadedRecord) {
                        hadUploaded = mUploadedRecord.contains(file.getAbsolutePath());
                    }
                    if (!usingFile && !hadUploaded) {
                        lastModifiedCache = lastModified;
                        newestLogFile = file;
                    }
                }
            }
            return newestLogFile;
        }

        @Override
        public void upload(@NonNull final File logFileDir) {
            if (mPolicy == AfterUploadPolicy.MARK) {
                mUploadedListFile = new File(mWorkDir, "uploaded.list");
                mUploadedRecord = StringListFile.getSingleInstanceFor(mUploadedListFile);
            }
            isNoUploadableFile = false;
            mExecutorService = Executors.newFixedThreadPool(3);
            mNewestLogFileProducer = new Thread("NewestLogFile-Producer") {
                @Override
                public void run() {
                    while (!isInterrupted()) {
                        try {
                            File newestLogFile = getNewestLogFile(logFileDir);
                            if (newestLogFile == null) {
                                isNoUploadableFile = true;
                                break;
                            }
                            newestLogFiles.put(newestLogFile);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            };
            mNewestLogFileProducer.start();
            mNewestLogFileConsumer = new Thread("NewestLogFile-Consumer") {
                private ArrayList<FutureTask<Void>> tasks = new ArrayList<>(3);
                @Override
                public void run() {
                    while (!isInterrupted() && !isNoUploadableFile) {
                        try {
                            final File file = newestLogFiles.poll(2, TimeUnit.MINUTES);
                            if (file == null) {
                                continue;
                            }
                            Runnable mark = new Runnable() {
                                @Override
                                public void run() {
                                    StringListFile uploadedRecord = StringListFile
                                            .getSingleInstanceFor(mUploadedListFile);
                                    synchronized(uploadedRecord) {
                                        uploadedRecord.add(file.getAbsolutePath());
                                    }
                                }
                            };
                            UploadTask uploadTask = new UploadTask(mPolicy, mOutline, file, mark);
                            FutureTask<Void> task = new FutureTask<>(uploadTask, null);
                            tasks.add(task);
                            mExecutorService.execute(task);
                            int size = tasks.size();
                            if (size >= 3) {
                                for (int i = size - 1; i >= 0; --i) {
                                    try {
                                        tasks.remove(i).get();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    StringListFile.destroy(mUploadedListFile);
                }
            };
            mNewestLogFileConsumer.start();
        }
    }

    @NotThreadSafe
    private static class PointUploadStrategy implements UploadStrategy {
        private AfterUploadPolicy mPolicy;
        private UploadOutline mOutline;
        private File mWorkDir;
        private Thread mLeftThread;
        private Thread mUploadableThread;
        private StringListFile mLeftList;
        private StringListFile mUploadableList;
        private File mLeftListFile;
        private File mUploadableListFile;

        public PointUploadStrategy(AfterUploadPolicy pPolicy, UploadOutline pOutline, File pWorkDir) {
            mPolicy = pPolicy;
            mOutline = pOutline;
            mWorkDir = pWorkDir;
        }

        @Override
        public void upload(@NonNull File logFileDir) {
            mLeftListFile = new File(mWorkDir, "left.list");
            mLeftList = StringListFile.getSingleInstanceFor(mLeftListFile);
            mUploadableListFile = new File(mWorkDir, "uploadable.list");
            mUploadableList = StringListFile.getSingleInstanceFor(mUploadableListFile);

            // use mUploadableList and mLeftList in sequential, doesn't need synchronized
            if (!mUploadableList.isEmpty()) {
                mLeftList.addAll(0, mUploadableList.getList());
                mUploadableList.clear();
            }
            File[] logFiles = logFileDir.listFiles();
            Arrays.sort(logFiles, FILE_COMPARATOR);
            for (File logFile: logFiles) {
                mUploadableList.add(logFile.getAbsolutePath());
            }

            // just use mLeftList in mLeftThread, doesn't need synchronized
            mLeftThread = new Thread("Left-Thread") {
                @Override
                public void run() {
                    for (int i = mLeftList.size() - 1; i >= 0; --i) {
                        final int index = i;
                        String path = mLeftList.get(i);
                        File uploadFile = new File(path);
                        Runnable mark = new Runnable() {
                            @Override
                            public void run() {
                                mLeftList.remove(index);
                            }
                        };
                        new UploadTask(mPolicy, mOutline, uploadFile, mark).run();
                    }
                    StringListFile.destroy(mLeftListFile);
                }
            };
            mLeftThread.start();

            // just use mUploadableList in mUploadableThread, doesn't need synchronized
            mUploadableThread = new Thread("Uploadable-Thread") {
                @Override
                public void run() {
                    for (int i = mUploadableList.size() - 1; i >= 0; --i) {
                        final int index = i;
                        String path = mUploadableList.get(i);
                        File uploadFile = new File(path);
                        Runnable mark = new Runnable() {
                            @Override
                            public void run() {
                                mUploadableList.remove(index);
                            }
                        };
                        new UploadTask(mPolicy, mOutline, uploadFile, mark).run();
                    }
                    StringListFile.destroy(mUploadableListFile);
                }
            };
            mUploadableThread.start();
        }
    }

    private static class DefaultScheduleStrategy implements ScheduleStrategy {
        private Context mContext;
        private int mMaxCount;
        private TimeInterval mInterval;
        public DefaultScheduleStrategy(Context pContext, int pMaxCount, TimeInterval pInterval) {
            mContext = pContext.getApplicationContext();
            mMaxCount = pMaxCount;
            mInterval = pInterval;
        }
        @Override
        public void schedule() {
            AlarmManager alarm = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            long triggerAtMillis = SystemClock.elapsedRealtime() + mMaxCount * mInterval.getInterval();
            Intent intent = new Intent(mContext, LogFileProcessService.class);
            PendingIntent pi = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarm.set(AlarmManager.ELAPSED_REALTIME, triggerAtMillis, pi);
        }
    }

    private static class UploadTask implements Runnable {
        private AfterUploadPolicy mPolicy;
        private UploadOutline mOutline;
        private File mUploadFile;
        private Runnable mMark;
        public UploadTask(AfterUploadPolicy pPolicy, UploadOutline pOutline, File pUploadFile, Runnable pMark) {
            mPolicy = pPolicy;
            mOutline = pOutline;
            mUploadFile = pUploadFile;
            mMark = pMark;
        }
        @Override
        public void run() {
            File encryptFile = mOutline.encrypt(mUploadFile);
            File compressFile = mOutline.compress(encryptFile);
            boolean uploadSuccess = mOutline.upload(compressFile);
            if (!encryptFile.equals(mUploadFile)) {
                encryptFile.delete();
            }
            if (!compressFile.equals(mUploadFile)) {
                compressFile.delete();
            }
            if (uploadSuccess) {
                switch (mPolicy) {
                    case REMOVE:
                        mUploadFile.delete();
                        break;
                    case MARK:
                        if (mMark != null) {
                            mMark.run();
                        }
                        break;
                    case IGNORE:
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
