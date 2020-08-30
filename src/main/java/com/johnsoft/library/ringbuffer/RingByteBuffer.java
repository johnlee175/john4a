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
package com.johnsoft.library.ringbuffer;

import static com.johnsoft.library.ringbuffer.TestUtils.dumpAsCharString;

import java.util.Arrays;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-02
 */
public class RingByteBuffer {
    private final byte[] buffer;
    private final int size;
    private int readPointer;
    private int writerPointer;
    private int round;
    private boolean isEmpty;

    public RingByteBuffer(int size) {
        this.buffer = new byte[size];
        this.size = size;
    }

    public synchronized void write(byte[] x) {
        write(x, 0, x.length);
    }

    /** no parameters bounds check */
    public synchronized void write(byte[] x, int offset, int length) {
        for (int i = 0; i < length; ++i) {
            write(x[offset + i]);
        }
    }

    public synchronized int read(byte[] x) {
        return read(x, 0, x.length);
    }

    /** no parameters bounds check */
    public synchronized int read(byte[] x, int offset, int length) {
        int readCount = 0;
        while (readCount < length) {
            final byte b = read();
            if (!isEmpty) {
                x[offset++] = b;
                ++readCount;
            } else {
                break;
            }
        }
        return readCount;
    }

    public synchronized void write(byte x) {
        if (writerPointer == readPointer && round != 0L) { // is full
            if (++readPointer >= size) { // ++readPointer
                --round;
                readPointer = readPointer % size;
            }
        }
        buffer[writerPointer] = x;
        if (++writerPointer >= size) {
            ++round;
            writerPointer = writerPointer % size;
        }
    }

    /** please check isEmpty() to avoid read invalid data. */
    public synchronized byte read() {
        isEmpty = false;
        if (readPointer == writerPointer && round == 0L) { // is empty
            isEmpty = true;
            return 0;
        }
        byte result = buffer[readPointer];
        if (++readPointer >= size) {
            --round;
            readPointer = readPointer % size;
        }
        return result;
    }

    public synchronized boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public synchronized String toString() {
        return Arrays.toString(buffer);
    }

    public synchronized String dump() {
        return dumpAsCharString(buffer);
    }

    public static void main(String[] args) {
        RingByteBuffer ringBuffer = new RingByteBuffer(7);
        System.out.println(ringBuffer.read() == Integer.MIN_VALUE);
        ringBuffer.write((byte)'a');
        ringBuffer.write((byte)'b');
        ringBuffer.write((byte)'c');
        ringBuffer.write((byte)'d');
        System.out.println((char)ringBuffer.read());
        System.out.println((char)ringBuffer.read());
        ringBuffer.write((byte)'e');
        ringBuffer.write((byte)'f');
        ringBuffer.write((byte)'g');
        ringBuffer.write((byte)'h');
        ringBuffer.write((byte)'i');
        System.out.println(ringBuffer.dump());
        ringBuffer.write((byte)'j');
        ringBuffer.write((byte)'k');
        System.out.println(ringBuffer.dump());
        System.out.println((char)ringBuffer.read());
        ringBuffer.write((byte)'l');
        ringBuffer.write((byte)'m');
        ringBuffer.write((byte)'n');
        ringBuffer.write((byte)'o');
        ringBuffer.write((byte)'p');
        ringBuffer.write((byte)'q');
        ringBuffer.write((byte)'r');
        System.out.println(ringBuffer.dump());
        System.out.println((char)ringBuffer.read());

        ringBuffer = new RingByteBuffer(7);
        for (int i = 0; i < 102; ++i) {
            ringBuffer.write((byte)i);
        }
        System.out.println(ringBuffer.toString());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
        System.out.println(ringBuffer.read());
    }
}
