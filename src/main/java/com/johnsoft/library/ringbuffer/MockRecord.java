package com.johnsoft.library.ringbuffer;

import java.util.Random;

/**
 * @author John Kenrinus Lee
 * @version 2016-02-26
 */
public class MockRecord {
    private int counter;
    private Random random = new Random();
    private int offset;
    private char[] data = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public int readRandom(byte[] bytes) {
        int i = 0;
        for (char c : String.valueOf(counter).toCharArray()) {
            bytes[i] = (byte)c;
            i++;
        }
        while (i < bytes.length) {
            bytes[i] = (byte)(65 + random.nextInt(26));
            i++;
        }
        counter++;
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return bytes.length;
    }

    public int readSequence(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i, ++offset) {
            if (offset >= data.length) {
                offset = 0;
            }
            bytes[i] = (byte)data[offset];
        }
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return bytes.length;
    }
}
