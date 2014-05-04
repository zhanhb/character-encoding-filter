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

    private static final Charset DEFAULT_CHARSET = CharsetFactory.ISO_8859_1;

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

    public Parameters setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

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
        didQueryParameters = false;
        encoding = null;
        parseFailed = false;
        return this;
    }

    public String[] getParameterValues(String name) {
        handleQueryParameters();
        ArrayList<String> values = paramHashValues.get(name);
        if (values == null) {
            return null;
        }
        return values.toArray(new String[values.size()]);
    }

    public Enumeration<String> getParameterNames() {
        handleQueryParameters();
        return Collections.enumeration(paramHashValues.keySet());
    }

    public String getParameter(String name) {
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

    public int size() {
        return paramHashValues.size();
    }

    // -------------------- Processing --------------------
    /**
     * Process the query string into parameters
     */
    public Parameters handleQueryParameters() {
        if (didQueryParameters) {
            return this;
        }

        didQueryParameters = true;

        if (queryString == null || queryString.length() == 0) {
            return this;
        }

        return processParameters(DEFAULT_CHARSET.encode(queryString), getCharset(queryStringEncoding));
    }

    private Parameters addParameter(String key, String value)
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

    public Parameters setURLDecoder(UDecoder u) {
        urlDec = u;
        return this;
    }

    public Parameters processParameters(ByteBuffer buff) {
        return processParameters(buff, getCharset(encoding));
    }

    private ByteBuffer urlDecode(ByteBuffer bc)
            throws IOException {
        if (urlDec == null) {
            urlDec = new UDecoder();
        }
        return urlDec.convert(bc);
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

            do {
                switch (data.get(pos)) {
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

            tmpName = data.duplicate();
            tmpName.limit(nameEnd).position(nameStart);
            if (valueStart >= 0) {
                tmpValue = data.duplicate();
                tmpValue.limit(valueEnd).position(valueStart);
            } else {
                tmpValue = null;
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
        data.position(end);
        return this;
    }

    private Charset getCharset(String encoding) {
        return CharsetFactory.getCharset(encoding, DEFAULT_CHARSET);
    }
}
