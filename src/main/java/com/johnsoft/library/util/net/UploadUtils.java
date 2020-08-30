package com.johnsoft.library.util.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

public class UploadUtils
{
	private static final String BOUNDARY = "---------------------------7da2137580612"; // 数据分隔线
	private static final String CRLF = "\r\n";

	/**
	 * socket流实现,直接通过传输原生HTTP协议post请求的提交数据到服务器,类似如下面表单提交功能: <br>
	 * 《FORM METHOD="POST" ACTION="http://192.168.0.100:8080/fileload/test.do"
	 * enctype="multipart/form-data"》<br>
	 * 《INPUT TYPE="text" NAME="id"》《INPUT TYPE="text" NAME="name"》<br>
	 * 《INPUT TYPE="file" NAME="image"》《INPUT TYPE="file" NAME="gzip"》 <br>
	 * 《/FORM》<br>
	 * <br>
	 * 
	 * @param urlPath
	 *            上传路径(注：避免使用localhost或127.0.0.1这样的路径测试，因为它会指向手机模拟器，你可以使用http://
	 *            www.xxx.cn或http://192.168.1.10:8080这样的路径测试)
	 * @param params
	 *            请求参数 key为参数名,类似上述表单中的id,name;value为参数值,类似上述表单中NAME为id,
	 *            name的INPUT的输入值
	 * @param files
	 *            上传文件集,FileItem的parameterName属性指上述表单中的image,gzip
	 * @see FileItem
	 */
	public static final String postRawHttp(String urlPath, Map<String, String> params,
			FileItem... files) throws Exception
	{
		String endline = "--" + BOUNDARY + "--" + CRLF; // 数据结束标志
		int fileEntityLength = 0;
		for (FileItem uploadFile : files)
		{ // 得到文件类型实体数据的总长度
			StringBuffer fileExplain = new StringBuffer();
			fileExplain.append("--");
			fileExplain.append(BOUNDARY);
			fileExplain.append(CRLF);
			fileExplain.append("Content-Disposition: form-data;name=\"")
					.append(uploadFile.getParameterName()).append("\";filename=\"")
					.append(uploadFile.getFileName()).append("\"").append(CRLF);
			fileExplain.append("Content-Type: ").append(uploadFile.getContentType()).append(CRLF)
					.append(CRLF);
			fileExplain.append(CRLF);
			fileEntityLength += fileExplain.length();
			if (uploadFile.getInputStream() != null)
			{
				fileEntityLength += uploadFile.getFile().length();
			}
			else
			{
				fileEntityLength += uploadFile.getData().length;
			}
		}
		StringBuffer textEntity = new StringBuffer();
		if (params != null)
		{
			for (Map.Entry<String, String> entry : params.entrySet())
			{ // 得到文本类型实体数据的总长度
				textEntity.append("--");
				textEntity.append(BOUNDARY);
				textEntity.append(CRLF);
				textEntity.append("Content-Disposition: form-data; name=\"").append(entry.getKey())
						.append("\"").append(CRLF).append(CRLF);
				textEntity.append(entry.getValue());
				textEntity.append(CRLF);
			}
		}
		// 计算传输给服务器的实体数据总长度
		int dataLength = textEntity.toString().getBytes().length + fileEntityLength
				+ endline.getBytes().length;

		URL url = new URL(urlPath);
		int port = url.getPort() == -1 ? 80 : url.getPort();
		Socket socket = new Socket(InetAddress.getByName(url.getHost()), port);
		OutputStream outStream = socket.getOutputStream();
		// 下面完成HTTP请求头的发送
		String requestmethod = "POST " + url.getPath() + " HTTP/1.1" + CRLF;
		outStream.write(requestmethod.getBytes());
		String accept = "Accept: image/gif, image/jpeg, image/pjpeg, image/pjpeg, "
				+ "application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, "
				+ "application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, "
				+ "application/vnd.ms-powerpoint, application/msword, */*"
				+ CRLF;
		outStream.write(accept.getBytes());
		String language = "Accept-Language: zh-CN" + CRLF;
		outStream.write(language.getBytes());
		String contenttype = "Content-Type: multipart/form-data; boundary=" + BOUNDARY + CRLF;
		outStream.write(contenttype.getBytes());
		String contentlength = "Content-Length: " + dataLength + CRLF;
		outStream.write(contentlength.getBytes());
		String alive = "Connection: Keep-Alive" + CRLF;
		outStream.write(alive.getBytes());
		String host = "Host: " + url.getHost() + ":" + port + CRLF;
		outStream.write(host.getBytes());
		// 写完HTTP请求头后根据HTTP协议再写一个回车换行
		outStream.write(CRLF.getBytes());
		// 把所有文本类型的实体数据发送出来
		outStream.write(textEntity.toString().getBytes());
		// 把所有文件类型的实体数据发送出来
		for (FileItem uploadFile : files)
		{
			StringBuffer fileEntity = new StringBuffer();
			fileEntity.append("--");
			fileEntity.append(BOUNDARY);
			fileEntity.append(CRLF);
			fileEntity.append("Content-Disposition: form-data;name=\"")
					.append(uploadFile.getParameterName()).append("\";filename=\"")
					.append(uploadFile.getFileName()).append("\"").append(CRLF);
			fileEntity.append("Content-Type: ").append(uploadFile.getContentType()).append(CRLF)
					.append(CRLF);
			outStream.write(fileEntity.toString().getBytes());
			InputStream is = uploadFile.getInputStream();
			if (is != null)
			{
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) > 0)
				{
					outStream.write(buffer, 0, len);
				}
				is.close();
			}
			else
			{
				outStream.write(uploadFile.getData(), 0, uploadFile.getData().length);
			}
			outStream.write(CRLF.getBytes());
		}
		// 下面发送数据结束标志，表示数据已经结束
		outStream.write(endline.getBytes());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = socket.getInputStream();
		byte[] bytes = new byte[1024];
		int len;
		while ((len = is.read(bytes)) > 0)
		{
			baos.write(bytes, 0, len);
		}
		bytes = baos.toByteArray();
		baos.close();
		socket.close();
		return new String(bytes);
	}

	/**
	 * 上传的文件,目前不应大于2M
	 */
	public static class FileItem
	{
		// -------------------上传文件的数据开始-----------------
		private File file;
		private byte[] data;
		private InputStream inputStream;
		private String fileName;
		// -------------------上传文件的数据结束-----------------

		// 请求参数名称
		private String parameterName;
		// 内容类型
		private String contentType;

		/**
		 * @param file
		 *            上传的文件
		 * @param parameterName
		 *            参数
		 * @param contentType
		 *            内容内容类型 <br>
		 *            注:参数皆不能为null
		 */
		public FileItem(File file, String parameterName, String contentType)
		{
			try
			{
				this.inputStream = new FileInputStream(file);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			this.file = file;
			this.fileName = file.getName();
			this.parameterName = parameterName;
			this.contentType = contentType;
		}

		/**
		 * @param fileName
		 *            文件名称
		 * @param data
		 *            上传的文件数据
		 * @param parameterName
		 *            参数
		 * @param contentType
		 *            内容类型 <br>
		 *            注:参数皆不能为null
		 */
		public FileItem(String fileName, byte[] data, String parameterName, String contentType)
		{
			this.data = data;
			this.fileName = fileName;
			this.parameterName = parameterName;
			this.contentType = contentType;
		}

		public String getFileName()
		{
			return fileName;
		}

		public File getFile()
		{
			return file;
		}

		public InputStream getInputStream()
		{
			return inputStream;
		}

		public byte[] getData()
		{
			return data;
		}

		public String getParameterName()
		{
			return parameterName;
		}

		public String getContentType()
		{
			return contentType;
		}
	}
}
