package com.johnsoft.library.util;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 128位AES加解密辅助类
 * 注:此种方式是快速粗糙的方式, 如果需要高效, 前沿, 安全的方式, 请使用openssl本地库
 * @author John Kenrinus Lee
 * @version 2015-06-08
 */
public final class AesUtils {

    private AesUtils() {}

    /**
     * 用于加解密都在Java层(不需要与openssl本地库互操作, 不需要与非Java服务端交互).<br>
     * 使用默认的AES/ECB/PKCSPadding5方式, 使用了随机码加强
     *
     * @param content  要加密的字符串
     * @param password 加密密码
     *
     * @return 加密后内容字节
     */
    public static final byte[] encryptInJavaLocal(String content, String password) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(password.getBytes("utf-8")));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(byteContent);
            return result;
        } catch (Exception e) {
            processThrowable(e);
            return null;
        }
    }

    /**
     * 用于加解密都在Java层(不需要与openssl本地库互操作, 不需要与非Java服务端交互).<br>
     * 使用默认的AES/ECB/PKCSPadding5方式, 使用了随机码加强
     *
     * @param content  加密后的内容字节
     * @param password 加密密码
     *
     * @return 解密后的原字符串
     */
    public static final String decryptInJavaLocal(byte[] content, String password) {
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(password.getBytes("utf-8")));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return new String(result, "utf-8");
        } catch (Exception e) {
            processThrowable(e);
            return null;
        }
    }

    /**
     * 由于采用ECB模式和NoPadding填充方案, 此方法不应用于极其敏感的数据, 但一般来说是安全的.<br>
     * 可与任何其他AES库互操作
     *
     * @param content  要加密的字符串
     * @param password 加密密码
     *
     * @return 加密后内容字节
     *
     */
    public static final byte[] encryptNoPadding(String content, String password) {
        try {
            if (password == null) {
                return null;
            }
            if (password.length() != 16) {
                return null;
            }
            String padStr = paddingString(content);
            byte[] raw = password.getBytes("utf-8");
            SecretKeySpec key = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(padStr.getBytes("utf-8"));
            return result;
        } catch (Exception e) {
            processThrowable(e);
            return null;
        }
    }

    /**
     * 由于采用ECB模式和NoPadding填充方案, 此方法不应用于极其敏感的数据, 但一般来说是安全的.<br>
     * 可与任何其他AES库互操作
     *
     * @param content  加密后的内容字节
     * @param password 加密密码
     *
     * @return 解密后的原字符串
     */
    public static final String decryptNoPadding(byte[] content, String password) {
        try {
            if (password == null) {
                return null;
            }
            if (password.length() != 16) {
                return null;
            }
            byte[] raw = password.getBytes("utf-8");
            SecretKeySpec key = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] original = cipher.doFinal(content);
            return dePaddingString(new String(original, "utf-8"));
        } catch (Exception e) {
            processThrowable(e);
            return null;
        }
    }

    /**
     * 添加填充字符串, 总长为16的倍数, AES加密算法要求
     */
    private static String paddingString(final String str) throws UnsupportedEncodingException {
        final byte[] strbytes = str.getBytes("utf-8");
        final int len = strbytes.length;
        if (0 == len % 16) {
            return str;
        }
        final byte[] bytes = new byte[(16 * (len / 16)) + 16];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = '\0';
        }
        for (int i = 0; i < len; ++i) {
            bytes[i] = strbytes[i];
        }
        return new String(bytes, "utf-8");
    }

    /**
     * 去除填充字符串, 总长为16的倍数, AES加密算法要求
     */
    private static String dePaddingString(final String str) throws UnsupportedEncodingException {
        final byte[] strbytes = str.getBytes("utf-8");
        final int len = strbytes.length;
        int pos = 0;
        for (int i = 0; i < len; ++i) {
            if ((strbytes[i]) == '\0') {
                pos = i;
                break;
            }
        }
        if (pos != 0) {
            final byte[] destByte = new byte[pos];
            System.arraycopy(strbytes, 0, destByte, 0, pos);
            return new String(destByte, "utf-8");
        }
        return str;
    }

    private static void processThrowable(Throwable throwable) {
        throwable.printStackTrace();
    }
}
