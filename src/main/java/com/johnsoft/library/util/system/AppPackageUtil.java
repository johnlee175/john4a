package com.johnsoft.library.util.system;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AppPackageUtil
{
	public static final void upgrade(String apkUri, Context context)
	{
		Uri uri = Uri.fromFile(new File(apkUri)); //这里是APK路径
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri,"application/vnd.android.package-archive");
        context.startActivity(intent); 
	}
}
