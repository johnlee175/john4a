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
package com.johnsoft.library.util.log;

import com.johnsoft.library.annotation.ThreadSafe;

/**
 * A faster formatter for placeholder message.
 *
 * @author John Kenrinus Lee
 * @version 2015-10-30
 */
@ThreadSafe
public final class MessageFormatter {
    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';
    private static final char ESCAPE_CHAR = '\\';

    /**
     * Replace placeholders in the given message with arguments.
     * "{}" will be replaced by the same position arguments as a placeholder.
     * "\\{}" will be escaped as plain "{}".
     *
     * @param message   the message pattern containing placeholders.
     * @param arguments the arguments to be used to replace placeholders.
     *
     * @return the formatted message.
     */
    public static String format(String message, Object... arguments) {
        if (message == null || arguments == null || arguments.length == 0) {
            return message;
        }
        for (int i = 0; i < arguments.length; ++i) {
            arguments[i] = String.valueOf(arguments[i]);
        }
        StringBuilder result = new StringBuilder();
        int escapeCounter = 0;
        int currentArgument = 0;
        for (int i = 0; i < message.length(); ++i) {
            char curChar = message.charAt(i);
            if (curChar == ESCAPE_CHAR) {
                ++escapeCounter;
            } else {
                if ((curChar == DELIM_START)
                        && (i < message.length() - 1)
                        && (message.charAt(i + 1) == DELIM_STOP)) {
                    int escapedEscapes = escapeCounter / 2;
                    for (int j = 0; j < escapedEscapes; ++j) {
                        result.append(ESCAPE_CHAR);
                    }
                    if (escapeCounter % 2 == 1) {
                        result.append(DELIM_START);
                        result.append(DELIM_STOP);
                    } else {
                        if (currentArgument < arguments.length) {
                            result.append(arguments[currentArgument]);
                        } else {
                            result.append(DELIM_START).append(DELIM_STOP);
                        }
                        ++currentArgument;
                    }
                    ++i;
                    escapeCounter = 0;
                    continue;
                }
                if (escapeCounter > 0) {
                    for (int j = 0; j < escapeCounter; ++j) {
                        result.append(ESCAPE_CHAR);
                    }
                    escapeCounter = 0;
                }
                result.append(curChar);
            }
        }
        return result.toString();
    }
}