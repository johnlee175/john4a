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
import java.util.Random;

/**
 * Not Thread Safe
 * @author John Kenrinus Lee
 * @version 2016-08-02
 */
public class DefaultRingBuffer implements RingBuffer {
    private static final long serialVersionUID = -1L;

    private final byte[] buffer;
    private final int size;
    private int readPointer;
    private int writerPointer;
    private int round;

    public DefaultRingBuffer(int size) {
        this.buffer = new byte[size];
        this.size = size;
    }

    /** just share data */
    public DefaultRingBuffer(DefaultRingBuffer ringBuffer) {
        this.buffer = ringBuffer.buffer;
        this.size = ringBuffer.size;
    }

    /** share data, copy status */
    @Override
    public DefaultRingBuffer duplicate() {
        final DefaultRingBuffer ringBuffer = new DefaultRingBuffer(this);
        ringBuffer.readPointer = readPointer;
        ringBuffer.writerPointer = writerPointer;
        ringBuffer.round = round;
        return ringBuffer;
    }

    /** just copy data */
    @Override
    public DefaultRingBuffer clone() {
        final DefaultRingBuffer ringBuffer = new DefaultRingBuffer(size);
        System.arraycopy(buffer, 0, ringBuffer.buffer, 0, size);
        return ringBuffer;
    }

    /** copy data, copy status */
    @Override
    public DefaultRingBuffer copy() {
        final DefaultRingBuffer ringBuffer = clone();
        ringBuffer.readPointer = readPointer;
        ringBuffer.writerPointer = writerPointer;
        ringBuffer.round = round;
        return ringBuffer;
    }

    @Override
    public void close() {
        /* java.util.Arrays.fill(buffer, (byte)0); */
        readPointer = 0;
        writerPointer = 0;
        round = 0;
    }

    @Override
    public void write(int oneByte) {
        write(new byte[]{ (byte)oneByte });
    }

    @Override
    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        for (int i = offset; i < length; ++i) {
            if (writerPointer == readPointer && round != 0L) { // is full
                if (++readPointer >= size) {
                    --round;
                    readPointer = readPointer % size;
                }
            }
            buffer[writerPointer] = bytes[i];
            if (++writerPointer >= size) {
                ++round;
                writerPointer = writerPointer % size;
            }
        }
    }

    @Override
    public int read() {
        final byte[] result = new byte[1];
        if (read(result) == 1) {
            return result[0];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        int readCount = 0;
        for (int i = offset; i < length; ++i) {
            if (readPointer == writerPointer && round == 0L) { // is empty
                return readCount;
            }
            bytes[i] = buffer[readPointer];
            ++readCount;
            if (++readPointer >= size) {
                --round;
                readPointer = readPointer % size;
            }
        }
        return readCount;
    }

    @Override
    public void writeOrWait(int oneByte) throws InterruptedException {
        writeOrWait(new byte[]{ (byte)oneByte });
    }

    @Override
    public void writeOrWait(byte[] bytes) throws InterruptedException {
        writeOrWait(bytes, 0, bytes.length);
    }

    @Override
    public void writeOrWait(byte[] bytes, int offset, int length) throws InterruptedException {
        for (int i = offset; i < length; ++i) {
            synchronized(buffer) {
                while (writerPointer == readPointer && round != 0L) { // is full
                    buffer.wait();
                }
            }
            buffer[writerPointer] = bytes[i];
            if (++writerPointer >= size) {
                ++round;
                writerPointer = writerPointer % size;
            }
            synchronized(buffer) {
                buffer.notifyAll();
            }
        }
    }

    @Override
    public int readOrWait() throws InterruptedException {
        final byte[] result = new byte[1];
        if (readOrWait(result) == 1) {
            return result[0];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public int readOrWait(byte[] bytes) throws InterruptedException {
        return readOrWait(bytes, 0, bytes.length);
    }

    @Override
    public int readOrWait(byte[] bytes, int offset, int length) throws InterruptedException {
        int readCount = 0;
        for (int i = offset; i < length; ++i) {
            synchronized(buffer) {
                while (readPointer == writerPointer && round == 0L) { // is empty
                    buffer.wait();
                }
            }
            bytes[i] = buffer[readPointer];
            ++readCount;
            if (++readPointer >= size) {
                --round;
                readPointer = readPointer % size;
            }
            synchronized(buffer) {
                buffer.notifyAll();
            }
        }
        return readCount;
    }

    @Override
    public String toString() {
        return Arrays.toString(buffer);
    }

    @Override
    public String dump() {
        return dumpAsCharString(buffer);
    }

    public static final RingBuffer synchronizedRingBuffer(final RingBuffer ringBuffer) {
        return new RingBuffer() {
            @Override
            public synchronized RingBuffer duplicate() {
                return ringBuffer.duplicate();
            }
            @Override
            public synchronized RingBuffer copy() {
                return ringBuffer.copy();
            }
            @Override
            public synchronized RingBuffer clone() {
                return ringBuffer.clone();
            }
            @Override
            public synchronized void close() {
                ringBuffer.close();
            }
            @Override
            public synchronized void write(int oneByte) {
                ringBuffer.write(oneByte);
            }
            @Override
            public synchronized void write(byte[] bytes) {
                ringBuffer.write(bytes);
            }
            @Override
            public synchronized void write(byte[] bytes, int offset, int length) {
                ringBuffer.write(bytes, offset, length);
            }
            @Override
            public synchronized void writeOrWait(int oneByte) throws InterruptedException {
                ringBuffer.writeOrWait(oneByte);
            }
            @Override
            public synchronized void writeOrWait(byte[] bytes) throws InterruptedException {
                ringBuffer.writeOrWait(bytes);
            }
            @Override
            public synchronized void writeOrWait(byte[] bytes, int offset, int length) throws InterruptedException {
                ringBuffer.writeOrWait(bytes, offset, length);
            }
            @Override
            public synchronized int read() {
                return ringBuffer.read();
            }
            @Override
            public synchronized int read(byte[] bytes) {
                return ringBuffer.read(bytes);
            }
            @Override
            public synchronized int read(byte[] bytes, int offset, int length) {
                return ringBuffer.read(bytes, offset, length);
            }
            @Override
            public synchronized int readOrWait() throws InterruptedException {
                return ringBuffer.readOrWait();
            }
            @Override
            public synchronized int readOrWait(byte[] bytes) throws InterruptedException {
                return ringBuffer.readOrWait(bytes);
            }
            @Override
            public synchronized int readOrWait(byte[] bytes, int offset, int length) throws InterruptedException {
                return ringBuffer.readOrWait(bytes, offset, length);
            }
            @Override
            public String toString() {
                return ringBuffer.toString();
            }
            @Override
            public synchronized String dump() {
                return ringBuffer.dump();
            }
        };
    }

    public static void main(String[] args) {
        DefaultRingBuffer ringBuffer = new DefaultRingBuffer(7);
        System.out.println(ringBuffer.read(new byte[1]) == 0);
        ringBuffer.write(new byte[]{'a', 'b', 'c', 'd'});
        System.out.println(ringBuffer.dump());
        byte[] bytes = new byte[2];
        ringBuffer.read(bytes);
        System.out.println(dumpAsCharString(bytes));
        ringBuffer.write(new byte[]{'e', 'f', 'g', 'h', 'i'});
        System.out.println(ringBuffer.dump());
        ringBuffer.write(new byte[]{'j', 'k'});
        System.out.println(ringBuffer.dump());
        bytes = new byte[2];
        ringBuffer.read(bytes);
        System.out.println(dumpAsCharString(bytes));
        ringBuffer.write(new byte[]{'l', 'm', 'n', 'o', 'p', 'q', 'r'});
        System.out.println(ringBuffer.dump());
        bytes = new byte[1];
        ringBuffer.read(bytes);
        System.out.println(dumpAsCharString(bytes));

        Random random = new Random();
        ringBuffer = new DefaultRingBuffer(7);
        MockRecord record = new MockRecord();
        bytes = new byte[20]; // change it
        byte[] readed;
        for (int i = 0; i < 5; ++i) {
            record.readSequence(bytes);
            System.out.println(">>>>>>>>>>>" + dumpAsCharString(bytes));
            ringBuffer.write(bytes);
            System.out.println(ringBuffer.dump());
            readed = new byte[1 + random.nextInt(8)];
            System.out.println(ringBuffer.read(readed));
            System.out.println(dumpAsCharString(readed));
        }

        System.out.println("===========================================");
        ringBuffer = new DefaultRingBuffer(7);
        bytes = new byte[4];
        readed = new byte[2];
        for (int i = 0; i < 10; ++i) {
            record.readSequence(bytes);
            System.out.println(">>>>>>>>>>>" + dumpAsCharString(bytes));
            ringBuffer.write(bytes);
            System.out.println("<<<<<<<<<<<" + ringBuffer.dump());
            System.out.println(ringBuffer.read(readed));
            System.out.println(dumpAsCharString(readed));
        }

        System.out.println("===========================================");
        ringBuffer = new DefaultRingBuffer(7);
        bytes = new byte[2];
        readed = new byte[4];
        for (int i = 0; i < 5; ++i) {
            record.readSequence(bytes);
            ringBuffer.write(bytes);
            System.out.println(">>>>>>>>>>>" + dumpAsCharString(bytes));
            System.out.println("<<<<<<<<<<<" + ringBuffer.dump());
            System.out.println(ringBuffer.read(readed));
            System.out.println(dumpAsCharString(readed));
        }
    }
}
