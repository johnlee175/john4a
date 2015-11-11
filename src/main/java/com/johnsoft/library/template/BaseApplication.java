package com.johnsoft.library.template;

import java.util.HashMap;
import java.util.Stack;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;

public class BaseApplication extends Application
{
	private static BaseApplication instance;
	private Stack<FragmentActivity> activityStack;
	private HashMap<String, Object> map = new HashMap<String, Object>();

	@Override
	public void onCreate()
	{
		super.onCreate();
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
		
		setApplication(this);
		
		TelephonyManager tm = (TelephonyManager) getApplicationContext().getSystemService(
				Context.TELEPHONY_SERVICE);
		map.put("device_id", tm.getDeviceId());
		map.put("os_type", "Android " + Build.VERSION.RELEASE);
		try
		{
			map.put("app_version",
					getApplicationContext().getPackageManager()
							.getPackageInfo(getApplicationContext().getPackageName(),
									PackageManager.GET_ACTIVITIES).versionName);
		}
		catch (NameNotFoundException e)
		{
			map.put("app_version", "1.0");
		}
	}

	public static final BaseApplication getApplication()
	{
		return instance;
	}

	public static final void setApplication(BaseApplication baseApp)
	{
		if (instance == null)
			instance = baseApp;
	}

	public Object getExtra(String key)
	{
		return map.get(key);
	}

	public Object putExtra(String key, Object value)
	{
		return map.put(key, value);
	}

	public Object removeExtra(String key)
	{
		return map.remove(key);
	}

	public void addActivity(FragmentActivity activity)
	{
		if (activityStack == null)
		{
			activityStack = new Stack<FragmentActivity>();
		}
		activityStack.add(activity);
	}

	public FragmentActivity currentActivity()
	{
		FragmentActivity activity = activityStack.lastElement();
		return activity;
	}

	public void finishActivity()
	{
		FragmentActivity activity = activityStack.lastElement();
		finishActivity(activity);
	}

	public void finishActivity(FragmentActivity activity)
	{
		if (activity != null)
		{
			activityStack.remove(activity);
			activity.finish();
			activity = null;
		}
	}

	public void finishActivity(Class<?> cls)
	{
		Stack<FragmentActivity> activitys = new Stack<FragmentActivity>();
		for (FragmentActivity activity : activityStack)
		{
			if (activity.getClass().equals(cls))
			{
				activitys.add(activity);
			}
		}
		for (FragmentActivity activity : activitys)
		{
			finishActivity(activity);
		}
	}

	public void finishAllActivity()
	{
		for (int i = 0, size = activityStack.size(); i < size; i++)
		{
			if (null != activityStack.get(i))
			{
				activityStack.get(i).finish();
			}
		}
		activityStack.clear();
	}

	public void exit()
	{
		finishAllActivity();
		int pid = Process.myPid();
		Process.killProcess(pid);
		System.exit(0);
	}
}
