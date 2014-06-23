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
package com.ys168.zhanhb.filter.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parameters
 *
 * @author Costin Manolache
 */
@SuppressWarnings("FinalClass")
final class Parameters {

    private static final Charset DEFAULT_CHARSET = CharsetFactory.ISO_8859_1;

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final Map<String, ArrayList<String>> paramHashValues
            = new LinkedHashMap<String, ArrayList<String>>();

    private String encoding = null;
    private String queryStringEncoding = null;

    private int limit = -1;
    private int parameterCount = 0;

    /**
     * Is set to <code>true</code> if there were failures during parameter
     * parsing.
     */
    private boolean parseFailed = false;

    public Parameters setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public String getEncoding() {
        return encoding;
    }

    public Parameters setEncoding(String s) {
        encoding = s;
        return this;
    }

    public Parameters setQueryStringEncoding(String s) {
        queryStringEncoding = s;
        return this;
    }

    public boolean isParseFailed() {
        return parseFailed;
    }

    public Parameters setParseFailed(boolean parseFailed) {
        this.parseFailed = parseFailed;
        return this;
    }

    public Parameters recycle() {
        parameterCount = 0;
        paramHashValues.clear();
        encoding = null;
        parseFailed = false;
        return this;
    }

    public String[] getParameterValues(String name) {
        ArrayList<String> values = paramHashValues.get(name);
        if (values == null) {
            return null;
        }
        return values.toArray(new String[values.size()]);
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(paramHashValues.keySet());
    }

    public String getParameter(String name) {
        ArrayList<String> values = paramHashValues.get(name);
        if (values != null) {
            /*if (values.isEmpty()) { // will never happen
             return "";
             }*/
            return values.get(0);
        } else {
            return null;
        }
    }

    public int size() {
        return paramHashValues.size();
    }

    // -------------------- Processing --------------------
    /**
     * Process the query string into parameters
     *
     * @param queryString query string to handle.
     * @return the parameter itself
     */
    public Parameters handleQueryParameters(String queryString, boolean useQueryStringEncoding) {
        if (queryString == null || queryString.length() == 0) {
            return this;
        }
        ByteBuffer query;
        if (useQueryStringEncoding) {
            query = getCharset(queryStringEncoding).encode(queryString);
        } else {
            try {
                query = DEFAULT_CHARSET.newEncoder().encode(CharBuffer.wrap(queryString));
            } catch (CharacterCodingException ex) {
                query = getCharset(queryStringEncoding).encode(queryString);
            }
        }
        return processParameters(query, getCharset(queryStringEncoding));
    }

    private Parameters addParameter(String key, String value)
            throws IllegalStateException {

        if (key == null) {
            return this;
        }

        ++parameterCount;
        if (limit > -1 && parameterCount > limit) {
            // Processing this parameter will push us over the limit. ISE is
            // what Request.parseParts() uses for requests that are too big
            parseFailed = true;
            throw new IllegalStateException(); // sm.getString("parameters.maxCountFail", Integer.valueOf(limit))
        }

        ArrayList<String> values = paramHashValues.get(key);
        if (values == null) {
            values = new ArrayList<String>(1);
            values.add(value);
            paramHashValues.put(key, values);
        } else {
            values.add(value);
        }
        return this;
    }

    public Parameters processParameters(ByteBuffer buff) {
        return processParameters(buff, getCharset(encoding));
    }

    private ByteBuffer urlDecode(ByteBuffer buff)
            throws IOException {
        return UDecoder.convert(buff);
    }

    private Parameters processParameters(ByteBuffer data, Charset charset) {
        if (data == null || !data.hasRemaining()) {
            return this;
        }
        int pos = data.position();
        int end = data.limit();
        while (pos < end) {
            int nameStart = pos;
            int nameEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;

            boolean parsingName = true;
            boolean decodeName = false;
            boolean decodeValue = false;
            boolean parameterComplete = false;

            for (; !parameterComplete && pos < end; pos++) {
                switch (data.get(pos)) {
                    case '=':
                        if (parsingName) {
                            // Name finished. Value starts from next character
                            nameEnd = pos;
                            parsingName = false;
                            valueStart = pos + 1;
                        } else {
                            // Equals character in value
                        }
                        break;
                    case '&':
                        if (parsingName) {
                            // Name finished. No value.
                            nameEnd = pos;
                        } else {
                            // Value finished
                            valueEnd = pos;
                        }
                        parameterComplete = true;
                        break;
                    case '%':
                    case '+':
                        // Decoding required
                        if (parsingName) {
                            decodeName = true;
                        } else {
                            decodeValue = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            if (pos == end) {
                if (nameEnd == -1) {
                    nameEnd = pos;
                } else if (valueStart > -1 && valueEnd == -1) {
                    valueEnd = pos;
                }
            }

            if (nameEnd <= nameStart) {
                if (valueStart == -1) {
                    // &&
                    // Do not flag as error
                    continue;
                }
                // &=foo&
                parseFailed = true;
                continue;
                // invalid chunk - it's better to ignore
            }

            final ByteBuffer nameBuf = data.duplicate();
            final ByteBuffer valueBuf = data.duplicate();

            nameBuf.position(nameStart).limit(nameEnd);
            if (valueStart >= 0) {
                valueBuf.position(valueStart).limit(valueEnd);
            }

            try {
                String name = charset.decode(decodeName ? urlDecode(nameBuf) : nameBuf).toString();
                String value = valueStart >= 0 ? charset.decode(decodeValue ? urlDecode(valueBuf) : valueBuf).toString() : "";

                try {
                    addParameter(name, value);
                } catch (IllegalStateException ise) {
                    // Hitting limit stops processing further params but does
                    // not cause request to fail.
                    parseFailed = true;
                    break;
                }
            } catch (IOException e) {
                parseFailed = true;
            }
        }
        data.position(end);
        return this;
    }

    private Charset getCharset(String encoding) {
        return CharsetFactory.getCharset(encoding, DEFAULT_CHARSET);
    }
}
