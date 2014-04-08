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
import java.nio.charset.Charset;

public final class MessageBytes implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int T_NULL = 0;

    /**
     * getType() is T_STR if the the object used to create the MessageBytes was
     * a String
     */
    public static final int T_STR = 1;

    /**
     * getType() is T_STR if the the object used to create the MessageBytes was
     * a byte[]
     */
    public static final int T_BYTES = 2;

    /**
     * Construct a new MessageBytes instance
     *
     * @return
     */
    public static MessageBytes newInstance() {
        return new MessageBytes();
    }

    // primary type ( whatever is set as original value )
    private int type = T_NULL;

    // Internal objects to represent array + offset, and specific methods
    private final ByteChunk byteC = new ByteChunk();

    // String
    private String strValue;

    /**
     * Creates a new, uninitialized MessageBytes object. Use static
     * newInstance() in order to allow future hooks.
     */
    private MessageBytes() {
    }

    public boolean isNull() {
        // should we check also hasStrValue ???
        return byteC.isNull() && strValue == null;
        // bytes==null && strValue==null;
    }

    /**
     * Resets the message bytes to an uninitialized (NULL) state.
     */
    public void recycle() {
        type = T_NULL;
        byteC.recycle();

        strValue = null;
    }

    /**
     * Set the content to be a string
     *
     * @param s
     */
    public void setString(String s) {
        strValue = s;
        if (s == null) {
            type = T_NULL;
        } else {
            type = T_STR;
        }
    }

    // -------------------- Conversion and getters --------------------
    /**
     * Compute the string value
     */
    public String toString() {
        if (strValue != null) {
            return strValue;
        }

        if (type == T_BYTES) {
            strValue = byteC.toString();
            return strValue;
        }
        return null;
    }

    //----------------------------------------
    /**
     * Return the type of the original content. Can be T_STR, T_BYTES, T_CHARS
     * or T_NULL
     *
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the byte chunk, representing the byte[] and offset/length. Valid
     * only if T_BYTES or after a conversion was made.
     *
     * @return
     */
    public ByteChunk getByteChunk() {
        return byteC;
    }

    /**
     * Returns the string value. Valid only if T_STR or after a conversion was
     * made.
     *
     * @return
     */
    public String getString() {
        return strValue;
    }

    /**
     * Do a char->byte conversion.
     */
    public void toBytes() {
        if (!byteC.isNull()) {
            type = T_BYTES;
            return;
        }
        toString();
        type = T_BYTES;
        byte bb[] = strValue.getBytes(Charset.defaultCharset());
        byteC.setBytes(bb, 0, bb.length);
    }

    /**
     * Returns the length of the original buffer. Note that the length in bytes
     * may be different from the length in chars.
     *
     * @return
     */
    public int getLength() {
        if (type == T_BYTES) {
            return byteC.getLength();
        }
        if (type == T_STR) {
            return strValue.length();
        }
        toString();
        if (strValue == null) {
            return 0;
        }
        return strValue.length();
    }

    /**
     * Copy the src into this MessageBytes, allocating more space if needed
     *
     * @param src
     * @throws java.io.IOException
     */
    public void duplicate(MessageBytes src) throws IOException {
        switch (src.getType()) {
            case MessageBytes.T_BYTES:
                type = T_BYTES;
                ByteChunk bc = src.getByteChunk();
                byteC.allocate(bc.getLength() << 1);
                byteC.append(bc);
                break;
            case MessageBytes.T_STR:
                type = T_STR;
                String sc = src.getString();
                this.setString(sc);
                break;
        }
    }
}
