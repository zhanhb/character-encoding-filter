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

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public final class ByteChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    // --------------------
    /**
     * Default encoding used to convert to strings. It should be UTF8, as most
     * standards seem to converge, but the servlet API requires 8859_1, and this
     * object is used mostly for servlets.
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    /**
     * Returns the first instance of the given byte in the byte array between
     * the specified start and end.
     *
     * @param bytes The byte array to search
     * @param start The point to start searching from in the byte array
     * @param end The point to stop searching in the byte array
     * @param b The byte to search for
     * @return The position of the first instance of the byte or -1 if the byte
     * is not found.
     */
    public static int findByte(byte bytes[], int start, int end, byte b) {
        int offset = start;
        while (offset < end) {
            if (bytes[offset] == b) {
                return offset;
            }
            offset++;
        }
        return -1;
    }

    // byte[]
    private byte[] buff;

    private int start = 0;
    private int end;

    private Charset charset;

    private boolean isSet = false; // XXX

    /**
     * Creates a new, uninitialized ByteChunk object.
     */
    public ByteChunk() {
        // NO-OP
    }

    public boolean isNull() {
        return !isSet; // buff==null;
    }

    /**
     * Resets the message buff to an uninitialized state.
     */
    public void recycle() {
        //        buff = null;
        charset = null;
        start = 0;
        end = 0;
        isSet = false;
    }

    public void reset() {
        buff = null;
    }

    // -------------------- Setup --------------------
    public void allocate(int initial) {
        if (buff == null || buff.length < initial) {
            buff = new byte[initial];
        }
        start = 0;
        end = 0;
        isSet = true;
    }

    /**
     * Sets the message bytes to the specified subarray of bytes.
     *
     * @param b the ascii bytes
     * @param off the start offset of the bytes
     * @param len the length of the bytes
     */
    public void setBytes(byte[] b, int off, int len) {
        buff = b;
        start = off;
        end = start + len;
        isSet = true;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        return charset;
    }

    /**
     * Returns the message bytes.
     *
     * @return
     */
    public byte[] getBytes() {
        return getBuffer();
    }

    /**
     * Returns the message bytes.
     *
     * @return
     */
    public byte[] getBuffer() {
        return buff;
    }

    /**
     * Returns the start offset of the bytes. For output this is the end of the
     * buffer.
     *
     * @return
     */
    public int getStart() {
        return start;
    }

    public int getOffset() {
        return start;
    }

    /**
     * Returns the length of the bytes. XXX need to clean this up
     *
     * @return
     */
    public int getLength() {
        return end - start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int i) {
        end = i;
    }

    public void append(ByteChunk src) throws IOException {
        append(src.getBytes(), src.getStart(), src.getLength());
    }

    /**
     * Add data to the buffer
     *
     * @param src
     * @param off
     * @param len
     * @throws java.io.IOException
     */
    public void append(byte src[], int off, int len) throws IOException {
        // will grow, up to limit
        makeSpace(len);

        // if we don't have limit: makeSpace can grow as it wants
        // assert: makeSpace made enough space
        System.arraycopy(src, off, buff, end, len);
        end += len;
    }

    /**
     * Make space for len chars. If len is small, allocate a reserve space too.
     * Never grow bigger than limit.
     *
     * @param count
     */
    public void makeSpace(int count) {
        byte[] tmp;

        int newSize;
        int desiredSize = end + count;

        if (buff == null) {
            if (desiredSize < 256) {
                desiredSize = 256; // take a minimum
            }
            buff = new byte[desiredSize];
        }

        // limit < buf.length ( the buffer is already big )
        // or we already have space XXX
        if (desiredSize <= buff.length) {
            return;
        }
        // grow in larger chunks
        if (desiredSize < (buff.length << 1)) {
            newSize = buff.length << 1;
            tmp = new byte[newSize];
        } else {
            newSize = (buff.length << 1) + count;
            tmp = new byte[newSize];
        }

        System.arraycopy(buff, start, tmp, 0, end - start);
        buff = tmp;
        end -= start;
        start = 0;
    }

    // -------------------- Conversion and getters --------------------
    public String toString() {
        if (null == buff) {
            return null;
        } else if (end - start == 0) {
            return "";
        }
        return toStringInternal();
    }

    public String toStringInternal() {
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        // new String(byte[], int, int, Charset) takes a defensive copy of the
        // entire byte array. This is expensive if only a small subset of the
        // bytes will be used. The code below is from Apache Harmony.
        CharBuffer cb;
        cb = charset.decode(ByteBuffer.wrap(buff, start, end - start));
        return new String(cb.array(), cb.arrayOffset(), cb.length());
    }
}
