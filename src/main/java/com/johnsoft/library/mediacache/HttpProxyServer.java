/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.library.mediacache;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A http proxy server for android.media.MediaPlayer http request which to using for media cache.<br/>
 * NOTICE:<br/>
 * - not support "POST" method http request, not support https;(non essential)<br/>
 * - not support like gzip or like base64 encoding;(non essential)<br/>
 * - not support "Very", "Cookie", "Cache-Control", "Expired" headers for cache;(non essential)<br/>
 * - not support header about Accept-Ranges or Content-Range, so not support MediaPlayer seek while no cached;<br/>
 * - if cache not prepared, will discard the cache temp file and re-create, this not good for large file;<br/>
 *
 * @author John Kenrinus Lee
 * @version 2016-07-21
 */
public class HttpProxyServer extends Thread {
    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 8080;
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String CRLF = "\r\n";
    private static final int MAX_REDIRECT_COUNT = 3;
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    public static String getProxyUrl(String url, boolean forceRefresh/*if true then don't use cache*/) {
        File file;
        try {
            file = DiskCachePolicy.get(url);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            file = null;
        }
        if (file != null && !forceRefresh) {
            return file.getPath();
        } else {
            try {
                return String.format(Locale.getDefault(), "http://%s:%d/%s",
                        PROXY_HOST, PROXY_PORT, URLEncoder.encode(url, DEFAULT_CHARSET));
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    public static String getProxyUrlWithHttpCache(String url) {
        try {
            final String[] token = DiskCachePolicy.getToken(url);
            if (token == null) {
                return getProxyUrl(url, true);
            }
            final HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
            String eTag = token[0];
            String lastModified = token[1];
            if (eTag != null && !eTag.trim().isEmpty()) {
                urlConnection.setRequestProperty("If-None-Match", eTag);
            }
            if (lastModified != null && !lastModified.trim().isEmpty()) {
                urlConnection.setRequestProperty("If-Modified-Since", lastModified);
            }
            urlConnection.connect();
            final int respCode = urlConnection.getResponseCode();
            eTag = urlConnection.getRequestProperty("ETag");
            lastModified = urlConnection.getRequestProperty("Last-Modified");
            urlConnection.disconnect();
            if (respCode == HTTP_NOT_MODIFIED) {
                return getProxyUrl(url, false);
            } else {
                DiskCachePolicy.setToken(url, eTag, lastModified);
                return getProxyUrl(url, true);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return getProxyUrl(url, false);
        }
    }

    @Override
    public void run() {
        try {
            final ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(PROXY_HOST, PROXY_PORT));
            while (!isInterrupted()) {
                new ProxyHandler(serverSocket.accept()).start();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final class ProxyHandler extends Thread {
        private final Socket realSocket;
        private String uri;

        public ProxyHandler(Socket socket) {
            realSocket = socket;
        }

        @Override
        public void run() {
            DiskCachePolicy.RollbackableOutputStream cacheOut = null;
            try {
                final String requestHeader = getRequestHeader();
                parseUri(requestHeader);
                // now the uri is set
                final HttpURLConnection urlConnection = openConnection();
                cacheToken(urlConnection);
                final String responseHeader = makeResponseHeader(urlConnection);
                cacheOut = DiskCachePolicy.set(uri);
                writeResponse(realSocket.getOutputStream(), cacheOut, responseHeader, urlConnection.getInputStream());
                if (cacheOut != null) {
                    cacheOut.commit();
                }
                urlConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                if (cacheOut != null) {
                    try {
                        cacheOut.rollback();
                        cacheOut.close();
                    } catch (IOException ignored) {
                        // do nothing
                    }
                }
            }
        }

        private String getRequestHeader() throws IOException {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(realSocket.getInputStream(),
                    DEFAULT_CHARSET), DEFAULT_BUFFER_SIZE * 10);
            final StringBuilder sb = new StringBuilder();
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null || line.trim().isEmpty()) { // headers ending
                    break;
                }
                sb.append(line).append(CRLF);
            }
            return sb.append(CRLF).toString();
        }

        private void parseUri(String requestHeader) throws IOException {
            final Matcher matcher = Pattern.compile("GET /(.*) HTTP/1\\.1").matcher(requestHeader);
            if (matcher.find()) {
                uri = URLDecoder.decode(matcher.group(1), DEFAULT_CHARSET);
            } else {
                throw new IOException("No uri found by request header");
            }
        }

        private HttpURLConnection openConnection() throws IOException {
            HttpURLConnection urlConnection;
            boolean redirected;
            int redirectCount = 0;
            do {
                urlConnection = (HttpURLConnection) new URL(uri).openConnection();
                urlConnection.setInstanceFollowRedirects(true);
                urlConnection.connect();
                final int code = urlConnection.getResponseCode();
                redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
                if (redirected) {
                    uri = urlConnection.getHeaderField("Location");
                    redirectCount++;
                    urlConnection.disconnect();
                } else if (code != HTTP_OK) {
                    throw new IOException("Proxy request failed with code " + code + "!");
                }
                if (redirectCount > MAX_REDIRECT_COUNT) {
                    throw new IOException("Too many redirects!");
                }
            } while (redirected);
            return urlConnection;
        }

        private void cacheToken(HttpURLConnection urlConnection) throws IOException {
            final String eTag = urlConnection.getRequestProperty("ETag");
            final String lastModified = urlConnection.getRequestProperty("Last-Modified");
            DiskCachePolicy.setToken(uri, eTag, lastModified);
        }

        private String makeResponseHeader(HttpURLConnection urlConnection) throws IOException {
            final String contentType = urlConnection.getContentType();
            final int contentLength = urlConnection.getContentLength();
            final String transferEncoding = urlConnection.getRequestProperty("Transfer-Encoding");
            return new StringBuilder()
                    .append("HTTP/1.1 200 OK").append(CRLF)
                    .append((contentType != null && !contentType.trim().isEmpty())
                            ? ("Content-Type: " + contentType + CRLF) : "")
                    .append((contentLength > 0) ? ("Content-Length: " + contentLength + CRLF) : "")
                    .append((transferEncoding != null && transferEncoding.trim().equals("chunked"))
                            ? ("Transfer-Encoding: " + transferEncoding + CRLF) : "")
                    .append("Connection: keep-alive").append(CRLF)
                    .append(CRLF) // headers ending
                    .toString();
        }

        private void writeResponse(OutputStream realOut, DiskCachePolicy.RollbackableOutputStream cacheOut,
                                   String responseHeader, InputStream responseBody) throws IOException {
            final OutputStream out = new BufferedOutputStream(realOut, DEFAULT_BUFFER_SIZE * 10);
            out.write(responseHeader.getBytes(DEFAULT_CHARSET));
            final OutputStream os;
            if (cacheOut != null) {
                os = new BufferedOutputStream(cacheOut, DEFAULT_BUFFER_SIZE * 10);
            } else {
                os = null;
            }
            final InputStream in = new BufferedInputStream(responseBody, DEFAULT_BUFFER_SIZE * 10);
            final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, len);
                if (os != null) {
                    os.write(buffer, 0, len);
                }
            }
            out.flush();
            if (os != null) {
                os.flush();
            }
        }
    }
}
