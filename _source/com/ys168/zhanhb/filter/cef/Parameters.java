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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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
final class Parameters {

    private static final Charset DEFAULT_CHARSET = Constants.DEFAULT_CHARSET;

    private final Map<String, ArrayList<String>> paramHashValues
            = new LinkedHashMap<String, ArrayList<String>>();

    private boolean didQueryParameters = false;

    private String queryString;
    private UDecoder urlDec;

    private String encoding = null;
    private String queryStringEncoding = null;

    private int limit = -1;
    private int parameterCount = 0;

    /**
     * Is set to <code>true</code> if there were failures during parameter
     * parsing.
     */
    private boolean parseFailed = false;

    Parameters setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    Parameters setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    String getEncoding() {
        return encoding;
    }

    Parameters setEncoding(String s) {
        encoding = s;
        return this;
    }

    Parameters setQueryStringEncoding(String s) {
        queryStringEncoding = s;
        return this;
    }

    boolean isParseFailed() {
        return parseFailed;
    }

    Parameters setParseFailed(boolean parseFailed) {
        this.parseFailed = parseFailed;
        return this;
    }

    Parameters recycle() {
        parameterCount = 0;
        paramHashValues.clear();
        didQueryParameters = false;
        encoding = null;
        parseFailed = false;
        return this;
    }

    String[] getParameterValues(String name) {
        handleQueryParameters();
        ArrayList<String> values = paramHashValues.get(name);
        if (values == null) {
            return null;
        }
        return values.toArray(new String[values.size()]);
    }

    Enumeration<String> getParameterNames() {
        handleQueryParameters();
        return Collections.enumeration(paramHashValues.keySet());
    }

    String getParameter(String name) {
        handleQueryParameters();
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

    int size() {
        return paramHashValues.size();
    }

    // -------------------- Processing --------------------
    /**
     * Process the query string into parameters
     */
    Parameters handleQueryParameters() {
        if (didQueryParameters) {
            return this;
        }

        didQueryParameters = true;

        if (queryString == null) {
            return this;
        }

        return processParameters(DEFAULT_CHARSET.encode(queryString), queryStringEncoding);
    }

    Parameters addParameter(String key, String value)
            throws IllegalStateException {

        if (key == null) {
            return this;
        }

        parameterCount++;
        if (limit > -1 && parameterCount > limit) {
            // Processing this parameter will push us over the limit. ISE is
            // what Request.parseParts() uses for requests that are too big
            parseFailed = true;
            throw new IllegalStateException(); // sm.getString("parameters.maxCountFail", Integer.valueOf(limit))
        }

        ArrayList<String> values = paramHashValues.get(key);
        if (values == null) {
            values = new ArrayList<String>(1);
            paramHashValues.put(key, values);
        }
        values.add(value);
        return this;
    }

    Parameters setURLDecoder(UDecoder u) {
        urlDec = u;
        return this;
    }

    Parameters processParameters(byte bytes[], int start, int len) {
        return processParameters(bytes, start, len, getCharset(encoding));
    }

    private Parameters processParameters(byte bytes[], int start, int len,
            Charset charset) {
        int pos = start;
        int end = start + len;

        while (pos < end) {
            int nameStart = pos;
            int nameEnd = -1;
            int valueStart = -1;
            int valueEnd = -1;

            boolean parsingName = true;
            boolean decodeName = false;
            boolean decodeValue = false;
            boolean parameterComplete = false;

            do {
                switch (bytes[pos]) {
                    case '=':
                        if (parsingName) {
                            // Name finished. Value starts from next character
                            nameEnd = pos;
                            parsingName = false;
                            ++pos;
                            valueStart = pos;
                        } else {
                            // Equals character in value
                            pos++;
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
                        pos++;
                        break;
                    case '%':
                    case '+':
                        // Decoding required
                        if (parsingName) {
                            decodeName = true;
                        } else {
                            decodeValue = true;
                        }
                        pos++;
                        break;
                    default:
                        pos++;
                        break;
                }
            } while (!parameterComplete && pos < end);

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

            final ByteBuffer tmpName;
            final ByteBuffer tmpValue;

            tmpName = ByteBuffer.wrap(bytes, nameStart, nameEnd - nameStart);
            if (valueStart >= 0) {
                tmpValue = ByteBuffer.wrap(bytes, valueStart, valueEnd - valueStart);
            } else {
                tmpValue = ByteBuffer.wrap(bytes, 0, 0);
            }

            try {
                String name;
                String value;

                name = charset.decode(decodeName ? urlDecode(tmpName) : tmpName).toString();

                if (valueStart >= 0) {
                    value = charset.decode(decodeValue ? urlDecode(tmpValue) : tmpValue).toString();
                } else {
                    value = "";
                }

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
        return this;
    }

    private ByteBuffer urlDecode(ByteBuffer bc)
            throws IOException {
        if (urlDec == null) {
            urlDec = new UDecoder();
        }
        return urlDec.convert(bc);
    }

    Parameters processParameters(ByteBuffer data, String encoding) {
        if (data == null || !data.hasRemaining()) {
            return this;
        }

        return processParameters(data.array(), data.arrayOffset() + data.position(), data.remaining(), getCharset(encoding));
    }

    private Charset getCharset(String encoding) {
        if (encoding == null) {
            return DEFAULT_CHARSET;
        }
        try {
            return Charset.forName(encoding);
        } catch (UnsupportedCharsetException e) {
            return DEFAULT_CHARSET;
        }
    }
}
