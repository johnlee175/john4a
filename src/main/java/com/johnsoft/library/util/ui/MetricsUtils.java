package com.johnsoft.library.util.ui;

import android.content.Context;

/**
 * 单位转换辅助类
 * @author John Kenrinus Lee 
 * @date 2014-7-21
 */
public class MetricsUtils
{
	public static final int px2dp(Context context, float pxValue)
	{
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	public static final int dp2px(Context context, float dpValue)
	{
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
	
    public static final int px2sp(Context context, float pxValue) 
    {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (pxValue / fontScale + 0.5f);  
    }  
  
    public static final int sp2px(Context context, float spValue) 
    {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (spValue * fontScale + 0.5f);  
    }  
}
