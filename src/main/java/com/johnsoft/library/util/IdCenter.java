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

/**
 * ID generate center
 * @author John Kenrinus Lee
 * @version 2016-12-27
 */
public final class IdCenter {
    /** generate 'A' - 'Z' and 'a' - 'Z' sequence */
    public static final int ALL_CASE = 0;
    /** generate 'A' - 'Z' sequence */
    public static final int UPPER_CASE = 1;
    /** generate 'a' - 'z' sequence */
    public static final int LOWER_CASE = -1;

    private static final char[] UPPER_CHARS = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    private static final char[] LOWER_CHARS = new char[] {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static final char[] ALL_CHARS = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z','a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static int calcLetterOffset(char[] where, String caseLetter) {
        final int length = where.length;
        char[] chars = caseLetter.toCharArray();
        final int size = chars.length;
        int result = 0, index = -1;
        for (int i = 0; i < size; ++i) {
            index = indexOf(where, chars[i]);
            if (index == -1) {
                throw new IllegalArgumentException("case letter should match letter type, and in [A-Za-z]");
            }
            result += (index + 1) * Math.pow(length, size - 1 - i);
        }
        return result - 1;
    }

    private static int indexOf(char[] where, char target) {
        final int size = where.length;
        for (int i = 0; i < size; ++i) {
            if (where[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Example:
     * (1) if you create with "new IdCenter(IdCenter.UPPER_CASE, 0, 0, 99), will got sequence like:
     * A0 A1 ... A99 B0 B1 ... B99 C0 ... ... Z99 AA0 AA1 ... AA99 AB0 ...
     * (2) if you create with "new IdCenter(IdCenter.UPPER_CASE, 0, 100, 999), will got sequence like:
     * A100 A101 ... A999 B100 B101 ... B999 C100 ... ... Z999 AA100 AA101 ... AA999 AB100 ...
     *
     * @param letterType see {@link IdCenter#ALL_CASE}, {@link IdCenter#UPPER_CASE}, {@link IdCenter#LOWER_CASE};
     *                   others same as {@link IdCenter#ALL_CASE}
     * @param letterOffset include, must be make sure digitFrom >= 0;
     *                     indicate the start letter width,
     *                     if set to 0, start from A in upper case type,
     *                     if set to 26^1, start from A in upper case type,
     *                     if set to 26^2 + 26^1, start from AAA in upper case type,
     *                     if set to 26^3 + 26^2 + 26^1, start from AAAA in upper case type,
     *                     26^3 = 26 * 26 * 26 and 26^2 = 26 * 26 in here;
     * @param digitFrom include, must be make sure digitFrom >= 0;
     *                  if set digitFrom >= digitTo, will be ignore digit part, like "AA", "az";
     * @param digitTo exclude, must be make sure digitTo >= 0;
     *                if set digitFrom >= digitTo, will be ignore digit part, like "AA", "az";
     */
    public IdCenter(int letterType, int letterOffset, int digitFrom, int digitTo) {
        if (digitFrom < 0 || digitTo < 0 || digitFrom >= digitTo) {
            throw new IllegalArgumentException("digitFrom < 0 || digitTo < 0 || digitFrom >= digitTo");
        }
        this.letters = letterType == UPPER_CASE ? UPPER_CHARS : letterType == LOWER_CASE ? LOWER_CHARS: ALL_CHARS;
        this.digitFrom = digitFrom;
        this.digitTo = digitTo;
        this.stepLetter = letterOffset;
        this.stepDigit = this.digitFrom;
    }

    /**
     * Example:
     * (1) if you create with "new IdCenter(IdCenter.UPPER_CASE, "A", 0, 99), will got sequence like:
     * A0 A1 ... A99 B0 B1 ... B99 C0 ... ... Z99 AA0 AA1 ... AA99 AB0 ...
     * (2) if you create with "new IdCenter(IdCenter.UPPER_CASE, "A", 100, 999), will got sequence like:
     * A100 A101 ... A999 B100 B101 ... B999 C100 ... ... Z999 AA100 AA101 ... AA999 AB100 ...
     *
     * @param letterType see {@link IdCenter#ALL_CASE}, {@link IdCenter#UPPER_CASE}, {@link IdCenter#LOWER_CASE};
     *                   others same as {@link IdCenter#ALL_CASE}
     * @param startChars if ALL_CASE, [A-Za-z] can be used, like "tCx", sequence will start tCx tCy tCz tDA
     *                   if UPPER_CASE, [A-Z] can be used, like "AX", sequence will start AX AY AZ BA
     *                   if LOWER_CASE, [a-z] can be used, like "mx", sequence will start mx my mz na;
     * @param digitFrom include, must be make sure digitFrom >= 0;
     *                  if set digitFrom >= digitTo, will be ignore digit part, like "AA", "az";
     * @param digitTo exclude, must be make sure digitTo >= 0;
     *                if set digitFrom >= digitTo, will be ignore digit part, like "AA", "az";
     */
    public IdCenter(int letterType, String startChars, int digitFrom, int digitTo) {
        if (digitFrom < 0 || digitTo < 0 || digitFrom >= digitTo) {
            throw new IllegalArgumentException("digitFrom < 0 || digitTo < 0 || digitFrom >= digitTo");
        }
        this.letters = letterType == UPPER_CASE ? UPPER_CHARS : letterType == LOWER_CASE ? LOWER_CHARS: ALL_CHARS;
        this.digitFrom = digitFrom;
        this.digitTo = digitTo;
        this.stepLetter = calcLetterOffset(this.letters, startChars);
        this.stepDigit = this.digitFrom;
    }

    private final char[] letters;
    private final int digitFrom;
    private final int digitTo;
    private int stepLetter;
    private int stepDigit;

    /** generate next ID String */
    public synchronized String generateNextId() {
        if (stepDigit >= digitTo) {
            stepDigit = digitFrom;
            ++stepLetter;
        }
        String prefix = "";
        final int length = letters.length;
        int dividend = stepLetter;
        int quotient;
        while ((quotient = dividend / length) > 0) {
            prefix = letters[(quotient - 1) % length] + prefix;
            dividend = quotient - 1;
        }
        prefix += letters[stepLetter % length];
        final String result = prefix + stepDigit++;
        return result;
    }
}
