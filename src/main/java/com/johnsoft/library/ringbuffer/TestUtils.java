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

import java.nio.ByteBuffer;

/**
 * @author John Kenrinus Lee
 * @version 2016-08-08
 */
public class TestUtils {
    public static String dumpAsCharString(byte[] bytes) {
        return dumpAsCharString(bytes, 0, bytes.length);
    }

    public static String dumpAsCharString(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < length; ++i) {
            sb.append((char)bytes[i]);
        }
        return sb.toString();
    }

    public static String dumpAsCharString(ByteBuffer buffer) {
        StringBuilder stringBuilder = new StringBuilder();
        while (buffer.hasRemaining()) {
            stringBuilder.append((char)buffer.get());
        }
        return stringBuilder.toString();
    }

    public static String dumpAsCharString(ByteBuffer buffer, int offset, int length) {
        ByteBuffer byteBuffer = buffer.duplicate();
        final int limit = byteBuffer.limit();
        final int newPos = byteBuffer.position() + offset;
        final int newLimit = newPos + length;
        byteBuffer.position(newPos < limit ? newPos : limit);
        byteBuffer.limit(newLimit < limit ? newLimit : limit);
        StringBuilder stringBuilder = new StringBuilder();
        while (buffer.hasRemaining()) {
            stringBuilder.append((char)buffer.get());
        }
        return stringBuilder.toString();
    }
}
