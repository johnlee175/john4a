package com.johnsoft.library.util.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.view.ViewConfiguration;
import dalvik.system.DexClassLoader;

public class SystemUtil
{
	public static final String CPU_INFO = "cpuinfo";
	public static final String MEMORY_INFO = "meminfo";
	
	/**
	 * 该方法目前仍有问题,仅给出思路,如果有两个处理器将导致第一个被覆盖
	 * @param fileInfo
	 * @return
	 * @throws IOException
	 */
	public static final Map<String, String> getProcInfo(String fileInfo) throws IOException
	{
		StringBuffer sb = new StringBuffer("");
		String[] args = { "/system/bin/cat", "/proc/" + fileInfo };
		ProcessBuilder cmd = new ProcessBuilder(args);
		Process process = cmd.start();
		InputStream in = process.getInputStream();
		byte[] bytes = new byte[1024];
		int len = 0;
		while ((len = in.read(bytes)) > 0)
		{
			sb.append(new String(bytes, 0, len, "UTF-8"));
		}
		in.close();
		String result = sb.toString();
		Map<String, String> map = new HashMap<String, String>();
		String[] lines = result.split("\n");
		for(String line : lines)
		{
			String[] keyValue = line.split(":");
			if(keyValue.length == 2)
			{
				map.put(keyValue[0].trim(), keyValue[1].trim());
			}
		}
		return map;
	}
	
	public static final Map<String, String> getProcessEnv()
	{
		return new ProcessBuilder("").environment();
	}
	
	public static final int getCPUCounts()
	{
		return Runtime.getRuntime().availableProcessors();
	}
	
	public static final long[] getRuntimeMemory()
	{
		Runtime run = Runtime.getRuntime();
		return new long[]{run.maxMemory(), run.totalMemory(), run.freeMemory()};
	}
	
	
	
	
	Object getRes(Context context, Intent intent, String type, String name)
	{
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolves = pm.queryIntentActivities(intent, 0);
//		List<ResolveInfo> resolves = pm.queryIntentServices(intent, 0);
		try
		{
			String packageName = resolves.get(0).resolvePackageName;
			Resources res = pm.getResourcesForApplication(packageName);
			int id = res.getIdentifier(name, type, packageName);
			if("string".equals(type))
			{
				return res.getString(id);
			}
			else if("drawable".equals(type))
			{
				return res.getDrawable(id);
			}
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	@TargetApi(14)
	Class<?> loadClass(ResolveInfo resolve, String classNameRelativePackageName)
	{
		ActivityInfo activityInfo = resolve.activityInfo;
		ApplicationInfo applicationInfo = activityInfo.applicationInfo;
		String packageName = applicationInfo.packageName;
		String dexPath = applicationInfo.sourceDir;
		String optimizedDirectory = applicationInfo.dataDir;
		String libraryPath = applicationInfo.nativeLibraryDir;
		DexClassLoader loader = new DexClassLoader(dexPath, optimizedDirectory, libraryPath, this.getClass().getClassLoader());
		try
		{
			return loader.loadClass(packageName + classNameRelativePackageName);
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean hasSoftOrHardOfMenuKey(Context context)
	{
		return ViewConfiguration.get(context).hasPermanentMenuKey();
	}
}
