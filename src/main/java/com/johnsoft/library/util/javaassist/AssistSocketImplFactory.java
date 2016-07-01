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
package com.johnsoft.library.util.javaassist;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import dalvik.system.DexClassLoader;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.android.DexFile;

/**
 * @author John Kenrinus Lee
 * @version 2016-06-30
 */
public class AssistSocketImplFactory implements SocketImplFactory {
    private static final String LOG_TAG = "SocketImplFactory";

    public static void startMonitoring() {
        try {
            Socket.setSocketImplFactory(new AssistSocketImplFactory());
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

    @NonNull
    private static Context getAppContext() {
        throw new UnsupportedOperationException(); // return app context yourself
    }

    private static final Class<?> generatedSocketImplClass = generateProxySocketImpl();
    private static Class generateProxySocketImpl() {
        final File outputDir = getAppContext().getFilesDir();
        final File dexFile = new File(outputDir, "custom-classes.dex");
        if (!dexFile.exists()) {
            try {
                final ClassPool cp = ClassPool.getDefault(getAppContext());
                final CtClass sup = cp.get("java.net.PlainSocketImpl");
                final CtClass cls = cp.makeClass("GenerateSocketImpl", sup);

                final CtConstructor constructor = new CtConstructor(null, cls);
                constructor.setBody("{ super(); }");
                cls.addConstructor(constructor);
                final CtMethod identifer = CtMethod.make(
                        "private final java.lang.String identifer() { "
                                + "return this.toString() + \" id: \" + this.hashCode();"
                        + "}", cls);
                cls.addMethod(identifer);
                final CtMethod getInputStream = CtMethod.make(
                        "protected java.io.InputStream getInputStream() throws java.io.IOException { "
                                + "java.io.InputStream input = super.getInputStream(); "
                                + "java.io.InputStream filterInput = new " + MonitorInputStream.class.getName()
                                + "(input); "
                                + "return filterInput; "
                        + "}", cls);
                cls.addMethod(getInputStream);
                final CtMethod getOutputStream = CtMethod.make(
                        "protected java.io.OutputStream getOutputStream() throws java.io.IOException { "
                                + "java.io.OutputStream output = super.getOutputStream(); "
                                + "java.io.OutputStream filterOutput = new " + MonitorOutputStream.class.getName()
                                + "(output); "
                                + "return filterOutput; "
                        + "}", cls);
                cls.addMethod(getOutputStream);
                final CtMethod create = CtMethod.make(
                        "protected void create(boolean isStreaming) throws java.io.IOException { "
                                + "super.create(isStreaming); "
                                + "java.lang.System.out.println(\"create SocketImpl \" + identifer()); "
                        + "}", cls);
                cls.addMethod(create);
                final CtMethod close = CtMethod.make(
                        "protected void close() throws java.io.IOException { "
                                + "super.close(); "
                                + "java.lang.System.out.println(\"close SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(close);
                final CtMethod shutdownInput = CtMethod.make(
                        "protected void shutdownInput() throws java.io.IOException { "
                                + "super.shutdownInput(); "
                                + "java.lang.System.out.println(\"shutdownInput SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(shutdownInput);
                final CtMethod shutdownOutput = CtMethod.make(
                        "protected void shutdownOutput() throws java.io.IOException { "
                                + "super.shutdownOutput(); "
                                + "java.lang.System.out.println(\"shutdownOutput SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(shutdownOutput);
                final CtMethod connectString = CtMethod.make(
                        "protected void connect(java.lang.String host, int port) throws java.io.IOException { "
                                + "super.connect(host, port); "
                                + "java.lang.System.out.println(\"connect String SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(connectString);
                final CtMethod connectInetAddress = CtMethod.make(
                        "protected void connect(java.net.InetAddress address, int port) throws java.io.IOException { "
                                + "super.connect(address, port); "
                                + "java.lang.System.out.println(\"connect InetAddress SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(connectInetAddress);
                final CtMethod connectSocketAddress = CtMethod.make(
                        "protected void connect(java.net.SocketAddress remoteAddr, int timeout) "
                                                                + "throws java.io.IOException { "
                                + "super.connect(remoteAddr, timeout); "
                                + "java.lang.System.out.println(\"connect SocketAddress SocketImpl \" + identifer()); "
                        + "}" , cls);
                cls.addMethod(connectSocketAddress);

                cls.writeFile(outputDir.getAbsolutePath());
                final DexFile df = new DexFile();
                df.addClass(new File(outputDir, "GenerateSocketImpl.class"));
                df.writeFile(dexFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(LOG_TAG, "", e);
            }
        }
        if (dexFile.exists()) {
            try {
                final DexClassLoader classLoader = new DexClassLoader(
                        dexFile.getAbsolutePath(),
                        getAppContext().getCacheDir().getAbsolutePath(),
                        getAppContext().getApplicationInfo().nativeLibraryDir,
                        AssistSocketImplFactory.class.getClassLoader());
                return classLoader.loadClass("GenerateSocketImpl");
            } catch (Exception e) {
                Log.e(LOG_TAG, "", e);
            }
        }
        return null;
    }

    @Override
    public SocketImpl createSocketImpl() {
        try {
            return (SocketImpl)generatedSocketImplClass.newInstance();
        } catch (Exception e) {
            Log.e(LOG_TAG, "", e);
            return null;
        }
    }
}
