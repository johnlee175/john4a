package com.johnsoft.library.util.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpUtils
{
	public static final int TIMEOUT_CONN = 500000;
	public static final int TIMEOUT_READ = 500000;
	
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";
	
	public static final String ENCODE_UTF8 = "UTF-8";
	public static final String ENCODE_GBK = "GBK";
	
	public static final String CRLF = "\r\n";
	public static final char CR = '\r', LF = '\n';
	
	public static final int BUFFER_SIZE = 4096;
	
	public static final String post(String urlString, Map<String, String> parameters, String encode)
	{
		return requestTextResponse(urlString, METHOD_POST, parameters, encode);
	}
	
	public static final String get(String urlString, String encode)
	{
		return requestTextResponse(urlString, METHOD_GET, null, encode);
	}
	
	public static final String requestTextResponse(String urlString, String method, Map<String, String> parameters,
            String encode)
    {
	 	String responseContent = null;
        HttpURLConnection conn = null;
        byte[] bytes = null;
        try
        {
        	if(parameters != null)
        	{
        		StringBuffer sb = new StringBuffer();
                for (String key : parameters.keySet())
                {
                    sb.append(key);
                    sb.append("=");
                    sb.append(URLEncoder.encode(parameters.get(key), encode));
                    sb.append("&");
                }
                if (sb.length() > 0)
                    sb = sb.deleteCharAt(sb.length() - 1);
                bytes = sb.toString().getBytes();
        	}
            
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_CONN);
            conn.setReadTimeout(TIMEOUT_READ);
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            
            if(bytes != null)
            {
            	 BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream());
                 bos.write(bytes, 0, bytes.length);
                 bos.close();
            }
            
    		BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		bytes = new byte[bis.available()];
    		int len = 0;
    		while((len = bis.read(bytes)) > 0)
    			baos.write(bytes, 0, len);
    		responseContent = new String(baos.toByteArray(), encode);
    		bis.close();
    		baos.close();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        finally
        {
            if (conn != null)
                conn.disconnect();
        }
        return responseContent;
    }
	
	public static final String nPost(String uriString, Map<String, String> parameters, String encode)
	{
		URI uri = URI.create(uriString);
		try
		{
			return parseTextResponse(transmitNIO(uri, createHttpRequest(uri, METHOD_POST, parameters, encode)), encode);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static final String nGet(String uriString, String encode)
	{
		URI uri = URI.create(uriString);
		try
		{
			return parseTextResponse(transmitNIO(uri, createHttpRequest(uri, METHOD_GET, null, encode)), encode);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static final byte[] createHttpRequest(URI uri, String method, Map<String, String> parameters, String encode)
	{
		String header = null;
		String body = null;
		if(parameters != null)
    	{
    		StringBuffer sb = new StringBuffer();
            for (String key : parameters.keySet())
            {
                sb.append(key);
                sb.append("=");
                try
				{
					sb.append(URLEncoder.encode(parameters.get(key), encode));
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
                sb.append("&");
            }
            if (sb.length() > 0)
                sb = sb.deleteCharAt(sb.length() - 1);
            body = sb.toString();
    	}
		
		StringBuffer head = new StringBuffer();
		head.append(method).append(' ').append(uri.getPath()).append(' ').append("HTTP/1.1").append(CRLF);
		head.append("Host: ").append(uri.getHost()).append(":").append(uri.getPort()).append(CRLF);
		head.append("");
		if(body != null)
		{
			int len = body.length();
			head.append("Content-Type: ").append("text/plain; charset=").append(encode).append(CRLF);
			head.append("Content-Length: ").append(len).append(CRLF);
		}
		header = head.toString();
		try
		{
			return ((header != null ? header : "") + (body != null ? body : "") + CRLF).getBytes(encode);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static final String parseTextResponse(byte[] bytes, String encode)
	{
		try
		{
			return new String(bytes, encode);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static final byte[] transmitBIO(URI uri, byte[] requestContent) throws IOException
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		transmitBIO(uri, requestContent, new BIOResponseHandler()
		{
			@Override
			public void handleResponse(Map<String, String> headers, InputStream is) throws IOException
			{
				writeToOutputStream(is, baos, headers);
			}
		});
		byte[] bytes = baos.toByteArray();
		baos.close();
		return bytes;
	}
	
	public static final void transmitBIO(URI uri, byte[] requestContent, BIOResponseHandler handler) throws IOException
	{
		Socket socket = new Socket();
		socket.setSoTimeout(TIMEOUT_READ);
		socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), TIMEOUT_CONN);
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		bos.write(requestContent);
		bos.flush();
		socket.shutdownOutput();
		
		BufferedInputStream is = new BufferedInputStream(socket.getInputStream());
		Map<String, String> headers = new HashMap<String, String>();
		readHeaders(is, headers);
		handler.handleResponse(headers, is);
		socket.shutdownInput();
		
		socket.close();
	}
	
	private static final void readHeaders(InputStream is, Map<String, String> headers) throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		readHeaders(is, baos);
		String lines = new String(baos.toByteArray());
		String[] headerlines = lines.split(CRLF);
		headers.put("Status-Line", headerlines[0]);
		for(int i = 1; i < headerlines.length; ++i)
		{
			String[] keyvalues = headerlines[i].split(":");
			headers.put(keyvalues[0].trim(), keyvalues[1].trim());
		}
	}
	
    private static final void readHeaders(InputStream is, OutputStream os) throws IOException 
    {  
        int b, b1, b2, b3;  
        while((b = is.read()) != CR) 
        	os.write(b);
        os.write(b);
        os.write(b1 = is.read());
        os.write(b2 = is.read());
        os.write(b3 = is.read());
        if(b1 == LF && b2 == CR && b3 == LF)
        	return;  
        readHeaders(is, os);  
    }
    
    public static final void writeToOutputStream(InputStream is, OutputStream os, Map<String, String> headers) throws IOException
    {
    	if(headers.get("Content-Length") != null)
    	{
    		byte[] bytes = new byte[BUFFER_SIZE];
    		int len = 0;
    		while((len = is.read(bytes)) > 0)
    		{
    			os.write(bytes, 0, len);
    			os.flush();
    		}
    		return;
    	}
    	if("chunked".equals(headers.get("Transfer-Encoding")))
    	{
    		byte[] bytes = new byte[BUFFER_SIZE];
    		int len = 0;
    		while((len = readChunkedLength(is)) != 0)
    		{
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			while(len > BUFFER_SIZE)
    			{
    				is.read(bytes);
    				baos.write(bytes);
    				baos.flush();
    				len = len - BUFFER_SIZE;
    			}
    			is.read(bytes, 0, len);
    			baos.write(bytes, 0, len);
    			baos.flush();
    			len = 0;
    			is.read(); //read CR
    			is.read(); //read LF
    			System.out.println(baos.toByteArray().length);
    			System.out.println(baos.toString());
    		}
    		return;
    	}
    }
    
    private static final int readChunkedLength(InputStream is) throws IOException
    {
    	StringBuilder result = new StringBuilder(80);
        while (true) 
        {
            int c = is.read();
            if (c == -1) {
                throw new EOFException();
            } else if (c == LF) {
                break;
            }
            result.append((char) c);
        }
        int length = result.length();
        if (length > 0 && result.charAt(length - 1) == CR)
            result.setLength(length - 1);
        String numExt = result.toString();
        //maybe size of chunked and ";" chunk-extension(name=value;name=value...\r\n) 
        int index = numExt.indexOf(";");
        if (index != -1) {
        	numExt = numExt.substring(0, index);
        }
        try {
            return Integer.parseInt(numExt.trim(), 16);
        } catch (NumberFormatException e) {
            throw new IOException("Expected a hex chunk size, but was " + numExt);
        }
    }
	
	public static final byte[] transmitNIO(URI uri, byte[] requestContent) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final WritableByteChannel wbc = Channels.newChannel(baos);
		transmitNIO(uri, requestContent, new NIOResponseHandler()
		{
			@Override
			public void handleResponse(Map<String, String> headers, SocketChannel socketChannel)
					throws IOException
			{
				writeToChannel(socketChannel, wbc);
			}
		});
		byte[] bytes = baos.toByteArray();
		wbc.close();
		baos.close();
		return bytes;
	}
	
	public static final void transmitNIO(URI uri, byte[] requestContent, NIOResponseHandler handler) throws IOException
	{
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_CONNECT);
		channel.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
		int timeout = TIMEOUT_CONN;
		outter:
		while(true)
		{
			if(selector.select(timeout) <= 0)
			{
				if(timeout == TIMEOUT_CONN)
					throw new IOException("Connect timeout!");
				else
					throw new IOException("Read timeout!");
			}
			Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
			while(selectedKeys.hasNext())
			{
				SelectionKey selectedKey = selectedKeys.next();
				selectedKeys.remove();
				if(selectedKey.isConnectable())
				{
					SocketChannel socketChannel = (SocketChannel)selectedKey.channel();
					if(socketChannel.isConnectionPending())
					{
						socketChannel.finishConnect();
					}
					SelectionKey key = socketChannel.register(selector, SelectionKey.OP_WRITE);
					key.attach(ByteBuffer.wrap(requestContent));
				}
				else if(selectedKey.isReadable())
				{
					SocketChannel socketChannel = (SocketChannel)selectedKey.channel();
					Map<String, String> headers = new HashMap<String, String>();
					readHeaders(socketChannel, headers);
					handler.handleResponse(headers, socketChannel);
					socketChannel.shutdownInput();
					socketChannel.close();
					break outter;
				}
				else if(selectedKey.isWritable())
				{
					SocketChannel socketChannel = (SocketChannel)selectedKey.channel();
					ByteBuffer buffer = (ByteBuffer)selectedKey.attachment();
					while(buffer.hasRemaining())
					{
						socketChannel.write(buffer);
					}
					socketChannel.register(selector, SelectionKey.OP_READ);
					socketChannel.shutdownOutput();
				}
			}
		}
	}
	
	private static final void readHeaders(SocketChannel socketChannel, Map<String, String> headers) throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		WritableByteChannel byteChannel = Channels.newChannel(baos);
		readHeaders(socketChannel, byteChannel);
		String lines = new String(baos.toByteArray());
		String[] headerlines = lines.split(CRLF);
		headers.put("Status-Line", headerlines[0]);
		for(int i = 1; i < headerlines.length; ++i)
		{
			String[] keyvalues = headerlines[i].split(":");
			headers.put(keyvalues[0].trim(), keyvalues[1].trim());
		}
	}
	
    private static final void readHeaders(SocketChannel socketChannel, WritableByteChannel byteChannel) throws IOException 
    {  
        ByteBuffer bb = ByteBuffer.allocate(1);
        ByteBuffer b1 = ByteBuffer.allocate(1);
        ByteBuffer b2 = ByteBuffer.allocate(1);
        ByteBuffer b3 = ByteBuffer.allocate(1);
        while(socketChannel.read(bb) > 0)
        {
        	bb.flip();
        	byteChannel.write(bb);
        	bb.rewind();
        	if(bb.get() == CR)
        		break;
        	else
        		bb.clear();
        }
        socketChannel.read(b1); socketChannel.read(b2); socketChannel.read(b3);
        b1.flip(); b2.flip(); b3.flip();
        byteChannel.write(b1); byteChannel.write(b2); byteChannel.write(b3);
        b1.rewind(); b2.rewind(); b3.rewind();
        if(b1.get() == LF && b2.get() == CR && b3.get() == LF)
        	return;  
        readHeaders(socketChannel, byteChannel);  
    }
    
    public static final void writeToChannel(SocketChannel socketChannel, WritableByteChannel wbc) throws IOException
    {
    	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		while(socketChannel.read(buffer) > 0)
		{
			buffer.flip();
			wbc.write(buffer);
			buffer.compact();
		}
    }
	
	public interface BIOResponseHandler
	{
		public void handleResponse(Map<String, String> headers, InputStream is) throws IOException;
	}
	
	public interface NIOResponseHandler
	{
		public void handleResponse(Map<String, String> headers, SocketChannel socketChannel) throws IOException;
	}
}
