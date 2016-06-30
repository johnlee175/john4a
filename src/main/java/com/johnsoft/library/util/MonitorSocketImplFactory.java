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
package com.johnsoft.library.util;

import java.io.FileDescriptor;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-29
 */
public final class MonitorSocketImplFactory implements SocketImplFactory {
    private static final String LOG_TAG = "SocketImplFactory";
    private static final String REAL_SOCKET_IMPL_CLASS = "java.net.PlainSocketImpl";

    public static void startMonitoring() {
        try {
            Socket.setSocketImplFactory(new MonitorSocketImplFactory());
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
        }
    }

    public static void stopMonitoring() {
        try {
            Socket.setSocketImplFactory(null);
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
        }
    }

    @Override
    public SocketImpl createSocketImpl() {
        try {
            return new ForwardingSocketImpl((SocketImpl) Class.forName(REAL_SOCKET_IMPL_CLASS).newInstance());
        } catch (Exception e) {
            Log.e(LOG_TAG, "", e);
            return null;
        }
    }

    private static final class ForwardingSocketImpl extends SocketImpl {
        private static final String LOG_TAG = "SocketImpl";
        private static final Map<String, Field> fieldMap = createFieldMap();
        private static final Map<String, Method> methodMap = createMethodMap();

        private static Map<String, Field> createFieldMap() {
            final Map<String, Field> fieldMap = new HashMap<>();
            final Field[] declaredFields = SocketImpl.class.getDeclaredFields();
            int fieldModifiers;
            String fieldName;
            for (Field field : declaredFields) {
                fieldModifiers = field.getModifiers();
                if (Modifier.isProtected(fieldModifiers) && !Modifier.isStatic(fieldModifiers)
                        && !Modifier.isFinal(fieldModifiers)) {
                    field.setAccessible(true);
                    fieldName = field.getName();
                    fieldMap.put(fieldName, field);
                }
            }
            return Collections.unmodifiableMap(fieldMap);
        }

        private static Map<String, Method> createMethodMap() {
            try {
                final Map<String, Method> methodMap = new HashMap<>();
                final Class<?> clazz = Class.forName(REAL_SOCKET_IMPL_CLASS);
                methodMap.put("create(Z)V", findMethod(clazz, "create", boolean.class));
                methodMap.put("bind(L/java/net/InetAddress;I)V", findMethod(clazz, "bind",
                        InetAddress.class, int.class));
                methodMap.put("listen(I)V", findMethod(clazz, "listen", int.class));
                methodMap.put("accept(L/java/net/SocketImpl;)V", findMethod(clazz, "accept", SocketImpl.class));
                methodMap.put("connect(Ljava/lang/String;I)V", findMethod(clazz, "connect", String.class, int.class));
                methodMap.put("connect(Ljava/net/InetAddress;I)V", findMethod(clazz, "connect",
                        InetAddress.class, int.class));
                methodMap.put("connect(Ljava/net/SocketAddress;I)V", findMethod(clazz, "connect",
                        SocketAddress.class, int.class));
                methodMap.put("close()V", findMethod(clazz, "close"));
                methodMap.put("available()I", findMethod(clazz, "available"));
                methodMap.put("getInputStream()Ljava/io/InputStream;", findMethod(clazz, "getInputStream"));
                methodMap.put("getOutputStream()Ljava/io/OutputStream;", findMethod(clazz, "getOutputStream"));
                methodMap.put("sendUrgentData(I)V", findMethod(clazz, "sendUrgentData", int.class));
                methodMap.put("getOption(I)Ljava/lang/Object;", findMethod(clazz, "getOption", int.class));
                methodMap.put("setOption(ILjava/lang/Object;)V", findMethod(clazz, "setOption",
                        int.class, Object.class));
                // Override methods
                methodMap.put("getFileDescriptor()Ljava/io/FileDescriptor;", findMethod(clazz, "getFileDescriptor"));
                methodMap.put("getInetAddress()Ljava/net/InetAddress;", findMethod(clazz, "getInetAddress"));
                methodMap.put("getLocalPort()I", findMethod(clazz, "getLocalPort"));
                methodMap.put("getPort()I", findMethod(clazz, "getPort"));
                methodMap.put("shutdownInput()V", findMethod(clazz, "shutdownInput"));
                methodMap.put("shutdownOutput()V", findMethod(clazz, "shutdownOutput"));
                methodMap.put("supportsUrgentData()Z", findMethod(clazz, "supportsUrgentData"));
                methodMap.put("setPerformancePreferences(III)V", findMethod(clazz, "setPerformancePreferences",
                        int.class, int.class, int.class));
                return Collections.unmodifiableMap(methodMap);
            } catch (Exception e) {
                Log.e(LOG_TAG, "", e);
                return null;
            }
        }

        private static Method findMethod(Class<?> clazz, String methodName, Class<?>...parameterTypes)
                throws NoSuchMethodException {
            if (clazz == null) {
                throw new NoSuchMethodException();
            } else {
                try {
                    return clazz.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException e) {
                    return findMethod(clazz.getSuperclass(), methodName, parameterTypes);
                }
            }
        }

        private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
            if (clazz == null) {
                throw new NoSuchFieldException();
            } else {
                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    return findField(clazz.getSuperclass(), fieldName);
                }
            }
        }

        private static SocketException createSocketException(Throwable e) {
            try {
                return SocketException.class.getConstructor(Throwable.class).newInstance(e);
            } catch (Exception ex) {
                Log.e(LOG_TAG, "", e);
                return new SocketException(e.getMessage());
            }
        }

        private final SocketImpl socketImpl;

        public ForwardingSocketImpl(SocketImpl socketImpl) {
            this.socketImpl = socketImpl;
        }

        private Object doInvokeVirtual(String methodSignature, String methodDescription,
                                       Object defaultReturnValue, Object...args) throws InvocationTargetException {
            Log.i(LOG_TAG, "Calling " + methodDescription + " ... ");
            if (methodMap != null && socketImpl != null) {
                Method method = methodMap.get(methodSignature);
                if (method != null) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(socketImpl, args);
                    } catch (IllegalAccessException e) {
                        Log.e(LOG_TAG, "", e);
                    }
                }
            }
            Log.e(LOG_TAG, "Call " + methodDescription + " failed!");
            return defaultReturnValue;
        }

        private void copyAllFields() {
            if (fieldMap == null || fieldMap.isEmpty() || methodMap == null || methodMap.isEmpty()
                    || socketImpl == null) { // state illegal
                Log.e(LOG_TAG, "Copy all real delegate fields value to proxy failed!");
                return;
            }
            Log.i(LOG_TAG, "Copying all real delegate fields value to proxy ...");
            Field field;
            for (String fieldName : fieldMap.keySet()) {
                field = fieldMap.get(fieldName);
                try {
                    synchronized(this) {
                        field.set(this, field.get(socketImpl));
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "", e);
                }
            }
        }

        @Override
        protected void create(boolean isStreaming) throws IOException {
            try {
                doInvokeVirtual("create(Z)V", "void create(boolean)", null, isStreaming);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void bind(InetAddress address, int port) throws IOException {
            try {
                doInvokeVirtual("bind(L/java/net/InetAddress;I)V", "void bind(InetAddress, int)", null, address, port);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void listen(int backlog) throws IOException {
            try {
                doInvokeVirtual("listen(I)V", "void listen(int)", null, backlog);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void accept(SocketImpl newSocket) throws IOException {
            try {
                doInvokeVirtual("accept(L/java/net/SocketImpl;)V", "void accept(SocketImpl)", null, newSocket);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void connect(String host, int port) throws IOException {
            try {
                doInvokeVirtual("connect(Ljava/lang/String;I)V", "void connect(String, int)", null, host, port);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
            try {
                doInvokeVirtual("connect(Ljava/net/InetAddress;I)V", "void connect(InetAddress, int)",
                        null, address, port);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            try {
                doInvokeVirtual("connect(Ljava/net/SocketAddress;I)V", "void connect(SocketAddress, int)",
                        null, remoteAddr, timeout);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void close() throws IOException {
            try {
                doInvokeVirtual("close()V", "void close()", null);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected int available() throws IOException {
            int returnValue = -1;
            try {
                returnValue = (int) doInvokeVirtual("available()I", "int available()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
            return returnValue;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            InputStream returnValue = null;
            try {
                returnValue = (InputStream) doInvokeVirtual("getInputStream()Ljava/io/InputStream;",
                        "InputStream getInputStream()", returnValue);
                copyAllFields();
                returnValue = new MonitorInputStream(returnValue);
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
            return returnValue;
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            OutputStream returnValue = null;
            try {
                returnValue = (OutputStream) doInvokeVirtual("getOutputStream()Ljava/io/OutputStream;",
                        "OutputStream getOutputStream()", returnValue);
                copyAllFields();
                returnValue = new MonitorOutputStream(returnValue);
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
            return returnValue;
        }

        @Override
        protected void sendUrgentData(int value) throws IOException {
            try {
                doInvokeVirtual("sendUrgentData(I)V", "void sendUrgentData(int)", null, value);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            Object returnValue = null;
            try {
                returnValue = doInvokeVirtual("getOption(I)Ljava/lang/Object;",
                        "Object getOption(int)", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw createSocketException(e);
            }
            return returnValue;
        }

        @Override
        public void setOption(int optID, Object val) throws SocketException {
            try {
                doInvokeVirtual("setOption(ILjava/lang/Object;)V", "void setOption(int, Object)", null, optID, val);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw createSocketException(e);
            }
        }

        // Override methods

        @Override
        protected FileDescriptor getFileDescriptor() {
            FileDescriptor returnValue = null;
            try {
                returnValue = (FileDescriptor) doInvokeVirtual("getFileDescriptor()Ljava/io/FileDescriptor;",
                        "FileDescriptor getFileDescriptor()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return returnValue;
        }

        @Override
        protected InetAddress getInetAddress() {
            InetAddress returnValue = null;
            try {
                returnValue = (InetAddress) doInvokeVirtual("getInetAddress()Ljava/net/InetAddress;",
                        "InetAddress getInetAddress()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return returnValue;
        }

        @Override
        protected int getLocalPort() {
            int returnValue = -1;
            try {
                returnValue = (int) doInvokeVirtual("getLocalPort()I",
                        "int getLocalPort()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return returnValue;
        }

        @Override
        protected int getPort() {
            int returnValue = -1;
            try {
                returnValue = (int) doInvokeVirtual("getPort()I",
                        "int getPort()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return returnValue;
        }

        @Override
        protected void shutdownInput() throws IOException {
            try {
                doInvokeVirtual("shutdownInput()V", "void shutdownInput()", null);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void shutdownOutput() throws IOException {
            try {
                doInvokeVirtual("shutdownOutput()V", "void shutdownOutput()", null);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected boolean supportsUrgentData() {
            boolean returnValue = false;
            try {
                returnValue = (boolean) doInvokeVirtual("supportsUrgentData()Z",
                        "boolean supportsUrgentData()", returnValue);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return returnValue;
        }

        @Override
        protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            try {
                doInvokeVirtual("setPerformancePreferences(III)V", "void setPerformancePreferences(int, int, int)",
                        null, connectionTime, latency, bandwidth);
                copyAllFields();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class MonitorInputStream extends FilterInputStream {
        private static final String LOG_TAG = "SocketImplStream";

        public MonitorInputStream(InputStream in) {
            super(in);
            Log.i(LOG_TAG, "MonitorInputStream created!");
        }

        @Override
        public int available() throws IOException {
            return super.available();
        }

        @Override
        public void close() throws IOException {
            super.close();
            Log.i(LOG_TAG, "MonitorInputStream closed!");
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return super.markSupported();
        }

        @Override
        public int read() throws IOException {
//            System.err.println("read()");
            int result = super.read();
            if (result <= 0) {
                Log.i(LOG_TAG, "Socket inputStream EOF!");
            }
            return result;
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
//            System.err.println("read(byte[], int, int)");
            int result = super.read(buffer, byteOffset, byteCount);
            if (result <= 0) {
                Log.i(LOG_TAG, "Socket inputStream EOF!");
            }
            return result;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
//            System.err.println("read(byte[])");
            int result = super.read(buffer);
            if (result <= 0) {
                Log.i(LOG_TAG, "Socket inputStream EOF!");
            }
            return result;
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return super.skip(byteCount);
        }
    }

    private static final class MonitorOutputStream extends FilterOutputStream {
        private static final String LOG_TAG = "SocketImplStream";

        public MonitorOutputStream(OutputStream in) {
            super(in);
            Log.i(LOG_TAG, "MonitorOutputStream created!");
        }

        @Override
        public void close() throws IOException {
            super.close();
            Log.i(LOG_TAG, "MonitorOutputStream closed!");
        }

        @Override
        public void flush() throws IOException {
            super.flush();
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
//            System.err.println("write(byte[], int, int)");
            super.write(buffer, offset, length);
        }

        @Override
        public void write(int oneByte) throws IOException {
//            System.err.println("write(int)");
            super.write(oneByte);
        }

        @Override
        public void write(byte[] buffer) throws IOException {
//            System.err.println("write(byte[])");
            super.write(buffer);
        }
    }
}
