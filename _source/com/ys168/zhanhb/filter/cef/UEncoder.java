/*
 * Copyright 2014 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ys168.zhanhb.filter.cef;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author zhanhb
 */
public class UEncoder {

    private static final char[] hexBytes = "0123456789ABCDEF".toCharArray();

    private CharBuffer ensureCapacity(CharBuffer buff, int addition) {
        if (buff.remaining() < addition) {
            int oldSize = buff.limit();
            int newSize;
            // grow in larger chunks
            if (buff.position() + addition < (oldSize << 1)) {
                newSize = oldSize << 1;
            } else {
                newSize = (oldSize << 1) + addition;
            }
            buff.flip();
            return (buff.isDirect()
                    ? ByteBuffer.allocateDirect(newSize << 1).asCharBuffer()
                    : CharBuffer.allocate(newSize)).put(buff);
        }
        return buff;
    }

    String encode(String str) {
        int off = 0;
        final int end = str.length();
        for (; off < end; ++off) {
            if (str.charAt(off) >= 128) {
                break;
            }
        }
        if (off == end) {
            return str;
        }

        Charset charset = Constants.UTF8;
        CharBuffer out = ensureCapacity(CharBuffer.allocate(end + 50), off).put(str, 0, off);

        while (off < end) {
            int pos = off;
            while (pos < end && str.charAt(pos) >= 128) {
                ++pos;
            }
            if (off < pos) {
                int position = out.position();
                out = ensureCapacity(out, pos - off).put(str, off, pos);
                out.flip().position(position);
                ByteBuffer encode = charset.encode(out);
                out.limit(out.capacity()).position(position);
                out = ensureCapacity(out, encode.remaining() * 3);
                while (encode.hasRemaining()) {
                    byte b = encode.get();
                    out = out.put(new char[]{'%', hexBytes[b >> 4 & 0xF], hexBytes[b & 0xF]});
                }
            }
            off = pos;
            while (pos < end && str.charAt(pos) < 128) {
                ++pos;
            }
            out = ensureCapacity(out, pos - off).put(str, off, pos);
            off = pos;
        }
        return out.flip().toString();
    }
}
