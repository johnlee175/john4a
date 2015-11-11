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
import java.util.concurrent.atomic.AtomicBoolean;

import com.johnsoft.library.annotation.NoInstanceResolved;
import com.johnsoft.library.annotation.NonNull;
import com.johnsoft.library.annotation.OneShotInApplication;
import com.johnsoft.library.annotation.OutMainThread;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

/**
 * Log files process service. A shell.<br>
 * As a Service, should be started on Application onCreate(),
 * and it will cleanup some old expired log files,
 * it also will upload the newest log files to server,
 * then it maybe schedule the next task,
 * you can determine the cleanup task and upload task, which one run first,
 * you also can empty implement one task, to just do cleanup or upload,
 * after all task done, it will stop self.<br>
 * Why using IntentService, not AsyncTask, just because of it is independent
 * and maybe take a long time when upload log files,
 * so it can run on independent process, not interact with UI component like Activity
 * or show progress bar Dialog.<br>
 *
 * <p>NOTICE: you need config it on AndroidManifest.xml.</p>
 *
 * @author John Kenrinus Lee
 * @version 2015-10-31
 * @see LogFileProcessStrategyFactory
 */
public final class LogFileProcessService extends IntentService {
    private static final String LOG_TAG = "LogFileProcessService";
    private static final AtomicBoolean IS_START = new AtomicBoolean(false);
    private static File sLogFilesDir;
    private static CleanupStrategy sCleanupStrategy;
    private static UploadStrategy sUploadStrategy;
    private static boolean sCleanup1Upload2;
    private static ScheduleStrategy sScheduleStrategy;

    /**
     *
     * @param context just a Context instance, not null, the instance not be a member of LogFileProcessService.
     * @param logFilesDir where the log files locate, assume that all log files in one directory, no sub-folder,
     *                    the directory should not be null, and had read-write-execute permission for it.
     * @param cleanup the cleanup strategy, see {@link com.johnsoft.library.util.log.LogFileProcessService.CleanupStrategy}.
     * @param upload the upload strategy, see {@link com.johnsoft.library.util.log.LogFileProcessService.UploadStrategy}.
     * @param cleanup1Upload2 determine the run order of cleanup task and upload task,
     *                        if true, it will run cleanup task first, after that will run upload task,
     *                        if false, it will run upload task first, after that will run cleanup task.
     * @param schedule the schedule strategy, see {@link com.johnsoft.library.util.log.LogFileProcessService.ScheduleStrategy}.
     */
    @OneShotInApplication
    public static void start(@NoInstanceResolved Context context, File logFilesDir,
                             CleanupStrategy cleanup, UploadStrategy upload, boolean cleanup1Upload2,
                             ScheduleStrategy schedule) {
        if (IS_START.compareAndSet(false, true)) {
            final Context appContext = context.getApplicationContext();
            appContext.startService(new Intent(appContext, LogFileProcessService.class));
            sLogFilesDir = logFilesDir;
            sCleanupStrategy = cleanup;
            sUploadStrategy = upload;
            sCleanup1Upload2 = cleanup1Upload2;
            sScheduleStrategy = schedule;
        }
    }

    private LogFileProcessService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (sLogFilesDir == null || !sLogFilesDir.isDirectory() || !sLogFilesDir.exists()
                || !(sLogFilesDir.canRead() && sLogFilesDir.canWrite() && sLogFilesDir.canExecute())) {
            return;
        }
        if (sCleanup1Upload2) {
            try {
                if (sCleanupStrategy != null) {
                    sCleanupStrategy.cleanup(sLogFilesDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (sUploadStrategy != null) {
                    sUploadStrategy.upload(sLogFilesDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (sUploadStrategy != null) {
                    sUploadStrategy.upload(sLogFilesDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (sCleanupStrategy != null) {
                    sCleanupStrategy.cleanup(sLogFilesDir);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            if (sScheduleStrategy != null) {
                sScheduleStrategy.schedule();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup old expired log files strategy interface.<br>
     * You can remove all log files when some conditions are satisfied,
     * you can remove those log files which create before a mouth,
     * you can remove oldest files to guarantee the total log files count,
     * you can remove log files which had upload,
     * you can remove log files according to period, like a week cleanup all. <br>
     * Some much strategy you can choose, just implement the interface. <br>
     *
     * @see LogFileProcessStrategyFactory
     */
    public interface CleanupStrategy {
        @OutMainThread
        void cleanup(@NonNull File logFileDir);
    }

    /**
     * Upload newest log files strategy interface.<br>
     * You can upload log files which create at today, or in a week,
     * you can upload all log files which not removed, as long as the network allows,
     * you can remember which one upload last time, and the one is upload successful or not,
     * and upload next older one continue this time, and when there is no network, just mark it. <br>
     * Some much strategy you can choose, just implement the interface. <br>
     *
     * @see LogFileProcessStrategyFactory
     */
    public interface UploadStrategy {
        @OutMainThread
        void upload(@NonNull File logFileDir);
    }

    /**
     * Schedule next task strategy interface.<br>
     * You can schedule cleanup and upload task after a hour,
     * you can just ignore it, and start this service on application create. <br>
     * @see LogFileProcessStrategyFactory
     */
    public interface ScheduleStrategy {
        @OutMainThread
        void schedule();
    }
}
