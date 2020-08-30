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
package com.johnsoft.library.util.cache;

import java.security.MessageDigest;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-20
 */
public final class Md5Utils {
    private Md5Utils() {}

    public static String encryptMD5(String message) {
        try {
            return byte2hex(encryptMD5(message.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] encryptMD5(byte[] message) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(message);
        MessageDigest mdc = (MessageDigest) md.clone();
        return mdc.digest();
    }

    public static String byte2hex(byte[] bytes) {
        String hs = "";
        String stmp = "";
        for (int i = 0; i < bytes.length; i++) {
            stmp = Integer.toHexString(bytes[i] & 0XFF);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }
}
