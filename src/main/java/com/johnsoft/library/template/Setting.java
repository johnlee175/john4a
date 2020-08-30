package com.johnsoft.library.template;

import android.content.SharedPreferences;

/**
 * main包下的Setting, 后续多渠道多设备时, 将此类移出src/main分散至具体的
 * @author John Kenrinus Lee
 * @version 2016-06-16
 */
public final class Setting {
    private Setting() {}

    /** server响应返回给调用者(JSON格式) */
    public static final boolean FORWARD_SERVER_RESPONSE = true;

    /** 一般可传入PreferenceManager.getDefaultSharedPreferences(Context) */
    public static void fillSharedPreferences(SharedPreferences preferences) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Configuration.KEY_FORWARD_SERVER_RESPONSE, FORWARD_SERVER_RESPONSE);
        editor.apply();
    }
}
