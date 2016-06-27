package com.johnsoft.library;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

/**
 * 捕获此进程相关的完整的Logcat的服务
 * @author John Kenrinus Lee
 * @version 2016-04-28
 */
public class LogcatRecordService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        LogcatRecorder.singleInstance.start();
        startForeground("LogcatRecordService".hashCode(), createNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogcatRecorder.singleInstance.stop();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; //later
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setAutoCancel(false);
//        builder.setWhen(System.currentTimeMillis());
//        builder.setShowWhen(true);
//        builder.setSmallIcon(R.drawable.ic_launcher);
//        builder.setContentTitle("Logcat-Record");
//        builder.setContentText("Log files stored at /sdcard/" + LogcatRecorder.CHILD_FOLDRE_NAME);
//        builder.setContentIntent(PendingIntent.getActivity(this, "LogcatRecordService".hashCode(),
//                new Intent(this, Launcher.class), PendingIntent.FLAG_UPDATE_CURRENT));
        return builder.build();
    }
}
