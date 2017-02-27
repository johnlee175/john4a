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
package com.johnsoft.library.util.net;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import com.johnsoft.library.template.BaseApplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * @author John Kenrinus Lee
 * @version 2017-02-27
 */
public class NetUtils {
    public static String getWifiIp() {
        Context appContext = BaseApplication.getApplication();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isAvailable() && networkInfo.isConnected()) {
            WifiManager wifiManager = (WifiManager) appContext.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int ip = wifiInfo.getIpAddress();
                if (ip != 0) {
                    byte[] addressBytes = {
                            (byte)(0xff & ip),
                            (byte)(0xff & (ip >> 8)),
                            (byte)(0xff & (ip >> 16)),
                            (byte)(0xff & (ip >> 24))
                    };
                    try {
                        return InetAddress.getByAddress(addressBytes).getHostAddress();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static String getIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface != null) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress()) {
                            String ip = address.getHostAddress();
                            if (ip.indexOf(':') < 0 /* IPv4 */) {
                                return ip;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isNetworkConnected() {
        Context appContext = BaseApplication.getApplication();
        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }
}
