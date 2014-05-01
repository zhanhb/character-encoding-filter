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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author zhanhb
 */
class Response extends HttpServletResponseWrapper {

    private static final char[] hexBytes = "0123456789ABCDEF".toCharArray();

    Response(HttpServletResponse response) {
        super(response);
    }

    @Override
    public String encodeURL(String url) {
        return super.encodeURL(encode(url));
    }

    @Override
    public String encodeRedirectURL(String url) {
        return super.encodeRedirectURL(encode(url));
    }

    @Override
    public String encodeUrl(String url) {
        return super.encodeUrl(encode(url));
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return super.encodeRedirectUrl(encode(url));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(encode(location));
    }

    private CharBuffer ensureCapacity(CharBuffer buff, int addition) {
        if (buff.remaining() < addition) {
            int oldSize = buff.capacity();
            int newSize;
            // grow in larger chunks
            if (buff.position() + addition < (oldSize << 1)) {
                newSize = oldSize << 1;
            } else {
                newSize = (oldSize << 1) + addition;
            }
            buff.flip();
            return CharBuffer.allocate(newSize).put(buff);
        }
        return buff;
    }

    private String encode(String str) {
        int start = 0;
        final int end = str.length();
        for (; start < end; ++start) {
            if (str.charAt(start) >= 128) {
                break;
            }
        }
        if (start == end) {
            return str;
        }

        Charset charset = Constants.UTF8;
        CharBuffer out = CharBuffer.allocate(end + 50).put(str, 0, start);
        CharBuffer in = CharBuffer.wrap(str, start, end);

        while (in.hasRemaining()) {
            char ch = in.get();
            if (ch >= 128) {
                int position = out.position();
                while (true) {
                    out = ensureCapacity(out, 1).put(ch);
                    if (!in.hasRemaining()) {
                        break;
                    }
                    ch = in.get();
                }
                out.flip().position(position);
                ByteBuffer encode = charset.encode(out);
                out.limit(out.capacity()).position(position);
                while (encode.hasRemaining()) {
                    byte b = encode.get();
                    out = ensureCapacity(out, 3).put('%').put(hexBytes[b >> 4 & 0xF]).put(hexBytes[b & 0xF]);
                }
            } else {
                out = ensureCapacity(out, 1).put(ch);
            }
        }
        return out.flip().toString();
    }
}
