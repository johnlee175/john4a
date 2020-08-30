package com.johnsoft.library.template;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.EditText;
import android.widget.ProgressBar;

public class DefaultWebViewHolder
{
	protected WebView wv;
	protected WebSettings ws;
	protected WebChromeClient wcc;
	protected WebViewClient wvc;
	protected Context ctx;
	protected ProgressBar pb;
	protected View.OnKeyListener okl;

	public DefaultWebViewHolder(WebView wv, ProgressBar pb)
	{
		this.wv = wv;
		this.pb = pb;
		this.ctx = wv.getContext();
		this.ws = wv.getSettings();
		this.wcc = new DefaultWebChromeClient(ctx);
		this.wvc = new DefaultWebViewClient(wv, pb);
		this.okl = new DefaultKeyListener(wv);
	}

	public WebView getWebView()
	{
		return wv;
	}

	public WebSettings getWebSettings()
	{
		return ws;
	}

	public WebChromeClient getWebChromeClient()
	{
		return wcc;
	}

	public WebViewClient getWebViewClient()
	{
		return wvc;
	}

	public Context getContext()
	{
		return ctx;
	}

	public ProgressBar getProgressBar()
	{
		return pb;
	}

	public View.OnKeyListener getOnKeyListener()
	{
		return okl;
	}

	public void finishCreate()
	{
		initWebSettings();
		initClient();
		initListeners();
	}

	/** <uses-permission android:name="android.permission.INTERNET"/> */
	public void loadWebHttpUrl(String url)
	{
		wv.loadUrl(url);
	}
	
	public void loadAssetsFile(String path)
	{
		String basePath = "file:///android_asset";
		if(!path.startsWith("/"))
		{
			basePath += "/";
		}
		wv.loadUrl(basePath + path);
	}
	
	public void loadLocalFile(String path)
	{
		wv.loadUrl("file:///" + path);
	}
	
	public void loadHtmlText(String data)
	{
		wv.loadData(data, "text/html", "UTF-8");
	}
	
	public void loadJavascript(String js)
	{
		wv.loadData(js, "text/javascript", "UTF-8");
	}
	
	//复杂情况可以使用json字符串
	public void callJavascript(String functionName, String...args)
	{
		StringBuffer sb = new StringBuffer("javascript:");
		sb.append(functionName);
		if(args.length > 0)
		{
			sb.append("(");
			for(String arg : args)
			{
				sb.append("'").append(arg).append("'");
			}
			sb.append(")");
		}else{
			sb.append("()");
		}
		wv.loadUrl(sb.toString());
	}
	
	public void addDefaultJavascriptInterface(String json, Runnable run)
	{
		wv.addJavascriptInterface(new DefaultAttachment(run, json, wv), DefaultWebViewHolder.class.getSimpleName());
	}

	protected void initListeners()
	{
		wv.setOnKeyListener(okl);
	}

	protected void initClient()
	{
		wv.setWebChromeClient(wcc);
		// 如果希望点击链接继续在当前browser中响应，而不是新开Android的系统browser中响应该链接，必须覆盖
		// webview的WebViewClient对象
		wv.setWebViewClient(wvc);
	}

	@SuppressLint("SetJavaScriptEnabled")
	protected void initWebSettings()
	{
		ws.setJavaScriptEnabled(true);
		ws.setUseWideViewPort(true);
		ws.setSupportZoom(true);
		int screenDensity = ctx.getResources().getDisplayMetrics().densityDpi;
		WebSettings.ZoomDensity zoomDensity = WebSettings.ZoomDensity.MEDIUM;
		switch (screenDensity)
		{
			case DisplayMetrics.DENSITY_LOW:
				zoomDensity = WebSettings.ZoomDensity.CLOSE;
				break;
			case DisplayMetrics.DENSITY_MEDIUM:
				zoomDensity = WebSettings.ZoomDensity.MEDIUM;
				break;
			case DisplayMetrics.DENSITY_HIGH:
				zoomDensity = WebSettings.ZoomDensity.FAR;
				break;
		}
		ws.setDefaultZoom(zoomDensity);
		ws.setJavaScriptCanOpenWindowsAutomatically(true);
		ws.setLoadsImagesAutomatically(true);
//		ws.setBuiltInZoomControls(true);
		ws.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS); // fit screen or fit page
		// ws.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN); //force fit
		// screen
	}

	public static class DefaultWebViewClient extends WebViewClient
	{
		protected WebView wv;
		protected ProgressBar pb;

		public DefaultWebViewClient(WebView wv, ProgressBar pb)
		{
			this.wv = wv;
			this.pb = pb;
		}

		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			// if(url.indexOf("tel:")<0)
			// {//页面上有数字会导致连接电话
			// view.loadUrl(url);
			// }
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			pb.setVisibility(View.VISIBLE);
			wv.setVisibility(View.GONE);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			pb.setVisibility(View.GONE);
			wv.setVisibility(View.VISIBLE);
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
		{ // webView默认是不处理https请求的，页面显示空白
			handler.proceed();
			// handler.cancel();
			// handler.handleMessage(null);
		}
	}

	public static class DefaultWebChromeClient extends WebChromeClient
	{
		protected Context ctx;

		public DefaultWebChromeClient(Context ctx)
		{
			this.ctx = ctx;
		}

		// 获得网页的标题，作为应用程序的标题进行显示
		public void onReceivedTitle(WebView view, String title)
		{
			if (ctx instanceof Activity)
				((Activity) ctx).setTitle(title);
		}

		public boolean onJsAlert(WebView view, String url, String message, JsResult result)
		{
			final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

			builder.setTitle("对话框").setMessage(message).setPositiveButton("确定", null);

			// 不需要绑定按键事件
			// 屏蔽keycode等于84之类的按键
			builder.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
				{
					Log.v("onJsAlert", "keyCode==" + keyCode + "event=" + event);
					return true;
				}
			});
			// 禁止响应按back键的事件
			// builder.setCancelable(false);
			AlertDialog dialog = builder.create();
			dialog.show();
			result.confirm();// 因为没有绑定事件，需要强行confirm,否则页面会变黑显示不了内容。
			return true;
		}

		public boolean onJsConfirm(WebView view, String url, String message, final JsResult result)
		{
			final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
			builder.setTitle("对话框").setMessage(message)
					.setPositiveButton("确定", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							result.confirm();
						}
					}).setNeutralButton("取消", new OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							result.cancel();
						}
					});
			builder.setOnCancelListener(new OnCancelListener()
			{
				@Override
				public void onCancel(DialogInterface dialog)
				{
					result.cancel();
				}
			});
			// 屏蔽keycode等于84之类的按键，避免按键后导致对话框消息而页面无法再弹出对话框的问题
			builder.setOnKeyListener(new OnKeyListener()
			{
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
				{
					Log.v("onJsConfirm", "keyCode==" + keyCode + "event=" + event);
					return true;
				}
			});
			// 禁止响应按back键的事件
			// builder.setCancelable(false);
			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		}

		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
				final JsPromptResult result)
		{
			final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

			builder.setTitle("对话框").setMessage(message);

			final EditText et = new EditText(view.getContext());
			et.setSingleLine();
			et.setText(defaultValue);
			builder.setView(et).setPositiveButton("确定", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					result.confirm(et.getText().toString());
				}

			}).setNeutralButton("取消", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					result.cancel();
				}
			});

			// 屏蔽keycode等于84之类的按键，避免按键后导致对话框消息而页面无法再弹出对话框的问题
			builder.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
				{
					Log.v("onJsPrompt", "keyCode==" + keyCode + "event=" + event);
					return true;
				}
			});

			// 禁止响应按back键的事件
			// builder.setCancelable(false);
			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		}
	}

	public static class DefaultKeyListener implements View.OnKeyListener
	{
		protected WebView wv;

		public DefaultKeyListener(WebView wv)
		{
			this.wv = wv;
		}

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			// 如果不做任何处理，浏览网页，点击系统“Back”键，整个Browser会调用finish()而结束自身，如果希望浏览的网
			// 页回退而不是推出浏览器，需要在当前Activity中处理并消费掉该Back事件
			if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack())
			{
				wv.goBack();
				return true;
			}
			return false;
		}
	}
	
	public static class DefaultAttachment
	{
		private Runnable runFunction;
		private String jsonData;
		private WebView wv;
		
		public DefaultAttachment(Runnable runFunction, String jsonData, WebView wv)
		{
			super();
			this.runFunction = runFunction;
			this.jsonData = jsonData;
			this.wv = wv;
		}
		
		@JavascriptInterface
		public String getJsonData()
		{
			return jsonData;
		}
		
		@JavascriptInterface
		public Runnable getRunFunction()
		{
			return runFunction;
		}
		
		@JavascriptInterface
		public WebView getWebView()
		{
			return wv;
		}
	}
}
