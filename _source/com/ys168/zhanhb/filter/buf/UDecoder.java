/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ys168.zhanhb.filter.buf;

import java.io.CharConversionException;
import java.io.IOException;

/**
 * All URL decoding happens here. This way we can reuse, review, optimize
 * without adding complexity to the buffers.
 *
 * The conversion will modify the original buffer.
 *
 * @author Costin Manolache
 */
public final class UDecoder {

    /**
     * Unexpected end of data.
     */
    private static final IOException EXCEPTION_EOF = new DecodeException("EOF");

    /**
     * %xx with not-hex digit
     */
    private static final IOException EXCEPTION_NOT_HEX_DIGIT = new DecodeException(
            "isHexDigit");


    private static boolean isHexDigit(int c) {
        return ((c - '0' | '9' - c)
                & (c - 'A' | 'F' - c)
                & (c - 'a' | 'f' - c)) >= 0;
    }

    private static int x2c(byte b1, byte b2) {
        int digit = (b1 >= 'A') ? ((b1 & 0xDF) - 'A') + 10
                : (b1 - '0');
        digit <<= 4;
        digit |= (b2 >= 'A') ? ((b2 & 0xDF) - 'A') + 10
                : (b2 - '0');
        return digit;
    }

    public UDecoder() {
    }

    /**
     * URLDecode, will modify the source.
     *
     * @param mb
     * @throws java.io.IOException
     */
    public void convert(ByteChunk mb) throws IOException {
        int start = mb.getOffset();
        byte buff[] = mb.getBytes();
        int end = mb.getEnd();

        int idx = ByteChunk.findByte(buff, start, end, (byte) '%');
        int idx2 = ByteChunk.findByte(buff, start, (idx >= 0 ? idx : end), (byte) '+');

        if (idx < 0 && idx2 < 0) {
            return;
        }

        // idx will be the smallest positive index ( first % or + )
        if ((idx2 >= 0 && idx2 < idx) || idx < 0) {
            idx = idx2;
        }

        for (int j = idx; j < end; j++, idx++) {
            if (buff[j] == '+' && true) {
                buff[idx] = ' ';
            } else if (buff[j] != '%') {
                buff[idx] = buff[j];
            } else {
                // read next 2 digits
                if (j + 2 >= end) {
                    throw EXCEPTION_EOF;
                }
                byte b1 = buff[j + 1];
                byte b2 = buff[j + 2];
                if (!isHexDigit(b1) || !isHexDigit(b2)) {
                    throw EXCEPTION_NOT_HEX_DIGIT;
                }

                j += 2;
                int res = x2c(b1, b2);
                buff[idx] = (byte) res;
            }
        }

        mb.setEnd(idx);
    }

    private static class DecodeException extends CharConversionException {

        private static final long serialVersionUID = 1L;

        DecodeException(String s) {
            super(s);
        }

        public Throwable fillInStackTrace() {
            // This class does not provide a stack trace
            return this;
        }
    }
}
