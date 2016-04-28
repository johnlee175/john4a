package com.johnsoft.library.util.ui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.Toast;

/**
 * 简易封装Toast, 不同类型的提示使用不同的实例
 * @author John Kenrinus Lee
 * @version 2016-03-04
 */
public final class TipCenter {
    private TipCenter() {}

    public static final String TYPE_DEFAULT = "default";

    private static Context appCtx;
    private static final Map<String, Toast> toastMap = new ConcurrentHashMap<>();

    /** 首先要调用此方法以初始化 */
    public static void setApplicationContext(@NonNull Context context) {
        appCtx = context.getApplicationContext();
    }

    public static void showInstanceToast(String type, CharSequence textStr, boolean shortDuration) {
        checkContext();
        if (emptyText(textStr)) {
            return;
        }
        if (emptyText(type)) {
            type = TYPE_DEFAULT;
        }
        Toast toast = toastMap.get(type);
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(appCtx, textStr, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toastMap.put(type, toast);
        toast.show();
    }

    public static void showInstanceToast(String type, int textResId, boolean shortDuration) {
        checkContext();
        showInstanceToast(type, appCtx.getResources().getText(textResId), shortDuration);
    }

    @Deprecated
    public static void showStaticToast(CharSequence textStr, boolean shortDuration) {
        checkContext();
        if (emptyText(textStr)) return;
        Toast toast = Toast.makeText(appCtx, textStr, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, -100);
        toast.show();
    }

    @Deprecated
    public static void showStaticToast(int textResId, boolean shortDuration) {
        checkContext();
        showStaticToast(appCtx.getResources().getText(textResId), shortDuration);
    }

    private static void checkContext() {
        if (appCtx == null) {
            throw new NullPointerException("Please pre-call TipCenter#setApplicationContext to initialize.");
        }
    }

    private static boolean emptyText(CharSequence textStr) {
        return (textStr == null || textStr.toString().trim().isEmpty());
    }
}
