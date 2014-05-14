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
package com.ys168.zhanhb.filter.cef;

import java.io.CharConversionException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * All parameter decoding happens here. This way we can reuse, review, optimize
 * without adding complexity to the buffers.
 *
 * The conversion will modify the original buffer.
 *
 * @author zhanhb
 */
final class UDecoder {

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

    private static int x2c(int b1, int b2) {
        return (((b1 > 0x40 ? b1 + 9 : b1) & 0xF) << 4) | ((b2 > 0x40 ? 9 + b2 : b2) & 0xF);
    }

    /**
     * URLDecode, will modify the source.
     *
     * @param mb
     * @return a buffer encoded maybe the parameter itself is returned.
     * @throws java.io.IOException
     */
    public ByteBuffer convert(ByteBuffer buff) throws IOException {
        int idx = buff.position();
        int end = buff.limit();
        for (; idx < end; ++idx) {
            int b = buff.get(idx);
            if (b == '+') {
                buff.put(idx, (byte) ' ');
                continue;
            }
            if (b == '%') {
                break;
            }
        }
        // idx will be the smallest positive index of % or end
        for (int j = idx; j < end; j++, idx++) {
            int b = buff.get(j);
            switch (b) {
                case '%':
                    // read next 2 digits
                    if (j + 2 >= end) {
                        throw EXCEPTION_EOF;
                    }
                    byte b1 = buff.get(j + 1);
                    byte b2 = buff.get(j + 2);
                    if (!isHexDigit(b1) || !isHexDigit(b2)) {
                        throw EXCEPTION_NOT_HEX_DIGIT;
                    }

                    j += 2;
                    int res = x2c(b1, b2);
                    buff.put(idx, (byte) res);
                    break;
                case '+':
                    buff.put(idx, (byte) ' ');
                    break;
                default:
                    buff.put(idx, (byte) b);
            }
        }
        buff.limit(idx);
        return buff;
    }

    private static class DecodeException extends CharConversionException {

        private static final long serialVersionUID = 1L;

        DecodeException(String s) {
            super(s);
        }

        @Override
        public Throwable fillInStackTrace() {
            // This class does not provide a stack trace
            return this;
        }
    }
}
