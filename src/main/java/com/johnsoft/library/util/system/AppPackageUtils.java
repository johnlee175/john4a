package com.johnsoft.library.util.system;

import static android.content.Context.ACTIVITY_SERVICE;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;

public final class AppPackageUtils {
    private AppPackageUtils() {
    }

    public static void upgrade(String apkUri, Context context) {
        Uri uri = Uri.fromFile(new File(apkUri)); //这里是APK路径
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    // like keytool -printcert -file unzipped-apk/META-INF/CERT.RSA -> SHA1
    private static final List<String> validRegisterSignatures = Arrays.asList("XX:XX:XX:XX:XX:XX:XX:XX");
    private static final List<String> validRegisterPackageNames = Arrays.asList("xxx.xxx.xxx.xxx");

    static {
        preProcessRegisterSignatures();
    }

    private static void preProcessRegisterSignatures() {
        final int size = validRegisterSignatures.size();
        for (int i = 0; i < size; ++i) {
            final String signature = validRegisterSignatures.get(i);
            String result = signature;
            if (signature.contains(":")) {
                result = signature.replace(":", "");
            }
            validRegisterSignatures.set(i, result.toUpperCase());
        }
    }

    public static boolean passPermissionSha1CheckForBinder(Context context) {
        return passPermissionSha1Check(context, Binder.getCallingUid());
    }

    public static boolean passPermissionSha1Check(final Context context, final int uid) {
        final PackageManager pm = context.getPackageManager();
        final String officialPackageName = pm.getNameForUid(uid);
        final String[] sharedPackageNames = pm.getPackagesForUid(uid);
        if (sharedPackageNames != null || officialPackageName != null) {
            final ArrayList<String> callingPackageNames = new ArrayList<>();
            if (sharedPackageNames != null) {
                for (String packageName : sharedPackageNames) {
                    callingPackageNames.add(packageName);
                }
            }
            if (officialPackageName != null && !callingPackageNames.contains(officialPackageName)) {
                callingPackageNames.add(officialPackageName);
            }
            final char[] hexDigits = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B', 'C', 'D', 'E', 'F'};
            boolean allPackagesOk = true;
            for (String callingPackageName : callingPackageNames) {
                if (!validRegisterPackageNames.contains(callingPackageName)) {
                    allPackagesOk = false;
                    break;
                }
                boolean singlePackageOk = false;
                try {
                    @SuppressLint("PackageManagerGetSignatures")
                    final PackageInfo packageInfo =
                            pm.getPackageInfo(callingPackageName, PackageManager.GET_SIGNATURES);
                    final Signature[] signatures = packageInfo.signatures;
                    for (Signature signature : signatures) {
                        try {
                            final MessageDigest digest = MessageDigest.getInstance("SHA1");
                            final byte[] sha1 = digest.digest(signature.toByteArray());
                            final char[] resultCharArray = new char[sha1.length << 1];
                            int index = 0;
                            for (byte b : sha1) {
                                resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
                                resultCharArray[index++] = hexDigits[b & 0xf];
                            }
                            singlePackageOk = validRegisterSignatures.contains(new String(resultCharArray));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!singlePackageOk) {
                    allPackagesOk = false;
                    break;
                }
            }
            return allPackagesOk;
        }
        return false;
    }

    /**
     * Just can check self process is foreground or not.
     */
    public static boolean isForegroundProcess(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
        List<String> packageNames = new ArrayList<>();
        if (appProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    packageNames.add(appProcess.processName);
                }
            }
        }
        return packageNames.contains(packageName);
    }

    private static String topActivityPackageName;

    /**
     * Can check top Activity in any process, but this way exists strict limits of permission.
     * 1. First Step:
     * <pre><code> {@code
     * <manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
     *   <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
     *   <uses-permission android:name="android.permission.GET_TASKS" />
     * </manifest>
     * } <code></pre>
     * 2. Second Step:
     * <pre><code> {@code
     * ...
     * if (Build.VERSION.SDK_INT >= 21) {
     *      forwardUsageAccessStat();
     * }
     * ...
     * @TargetApi(21)
     * private void forwardUsageAccessStat() {
     *      Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
     *      startActivity(intent);
     * }
     * } <code></pre>
     *
     */
    public static synchronized boolean isTopActivty(Context context, String packageName) {
        final String name;
        if (Build.VERSION.SDK_INT >= 21) {
            name = getTopActivtyPackageNameFromUsageStatsService(context);
        } else {
            name = getTopActivtyPackageNameFromActivityService(context);
        }
        if (name != null) {
            topActivityPackageName = name;
        }
        return packageName.equals(topActivityPackageName);
    }

    @TargetApi(21)
    private static String getTopActivtyPackageNameFromUsageStatsService(Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
        if (stats != null) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                return mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        return null;
    }

    @TargetApi(14)
    private static String getTopActivtyPackageNameFromActivityService(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && !runningTasks.isEmpty()) {
            ComponentName componentName = runningTasks.get(0).topActivity;
            if (componentName != null) {
                return componentName.getPackageName();
            }
        }
        return null;
    }

    public static void launch(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (packageInfo != null) {
                Intent intent = pm.getLaunchIntentForPackage(packageName);
                context.startActivity(intent);
            }
        } catch (PackageManager.NameNotFoundException e) {
            /* ignore */
        }
    }

    public static boolean isForegroundServiceRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(Integer.MAX_VALUE);
        if (runningServices != null && !runningServices.isEmpty()) {
            for (ActivityManager.RunningServiceInfo runningService : runningServices) {
                if (runningService.foreground) {
                    String processName = runningService.process;
                    ComponentName componentName = runningService.service;
                    return packageName.equals(processName) ||
                            (componentName != null && packageName.equals(componentName.getPackageName()));
                }
            }
        }
        return false;
    }
}
