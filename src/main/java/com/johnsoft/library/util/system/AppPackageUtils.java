package com.johnsoft.library.util.system;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;

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
}
