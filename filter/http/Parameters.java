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
package com.ys168.zhanhb.filter.http;

import com.ys168.zhanhb.filter.buf.ByteChunk;
import com.ys168.zhanhb.filter.buf.MessageBytes;
import com.ys168.zhanhb.filter.buf.UDecoder;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Costin Manolache
 */
public final class Parameters {

    public static final String DEFAULT_ENCODING = "ISO-8859-1";
    private static final Charset DEFAULT_CHARSET = ByteChunk.DEFAULT_CHARSET;

    private final Map<String, ArrayList<String>> paramHashValues
            = new LinkedHashMap<String, ArrayList<String>>();

    private boolean didQueryParameters = false;

    private MessageBytes queryMB;

    private UDecoder urlDec;
    private final MessageBytes decodedQuery = MessageBytes.newInstance();

    private String encoding = null;
    private String queryStringEncoding = null;

    private int limit = -1;
    private int parameterCount = 0;

    /**
     * Is set to <code>true</code> if there were failures during parameter
     * parsing.
     */
    private boolean parseFailed = false;

    // -------------------- Parameter parsing --------------------
    // we are called from a single thread - we can do it the hard way
    // if needed
    private final ByteChunk tmpName = new ByteChunk();
    private final ByteChunk tmpValue = new ByteChunk();

    public Parameters() {
        // NO-OP
    }

    public void setQuery(MessageBytes queryMB) {
        this.queryMB = queryMB;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String s) {
        encoding = s;
    }

    public void setQueryStringEncoding(String s) {
        queryStringEncoding = s;
    }

    public boolean isParseFailed() {
        return parseFailed;
    }

    public void setParseFailed(boolean parseFailed) {
        this.parseFailed = parseFailed;
    }

    public void recycle() {
        parameterCount = 0;
        paramHashValues.clear();
        didQueryParameters = false;
        encoding = null;
        decodedQuery.recycle();
        parseFailed = false;
    }

    public String[] getParameterValues(String name) {
        handleQueryParameters();
        // no "facade"
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

    // -------------------- Processing --------------------
    /**
     * Process the query string into parameters
     */
    public void handleQueryParameters() {
        if (didQueryParameters) {
            return;
        }

        didQueryParameters = true;

        if (queryMB == null || queryMB.isNull()) {
            return;
        }

        try {
            decodedQuery.duplicate(queryMB);
        } catch (IOException impossible) {
        }
        processParameters(decodedQuery, queryStringEncoding);
    }

    public void addParameter(String key, String value)
            throws IllegalStateException {

        if (key == null) {
            return;
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
    }

    public void setURLDecoder(UDecoder u) {
        urlDec = u;
    }

    public void processParameters(byte bytes[], int start, int len) {
        processParameters(bytes, start, len, getCharset(encoding));
    }

    private void processParameters(byte bytes[], int start, int len,
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

            tmpName.setBytes(bytes, nameStart, nameEnd - nameStart);
            if (valueStart >= 0) {
                tmpValue.setBytes(bytes, valueStart, valueEnd - valueStart);
            } else {
                tmpValue.setBytes(bytes, 0, 0);
            }

            try {
                String name;
                String value;

                if (decodeName) {
                    urlDecode(tmpName);
                }
                tmpName.setCharset(charset);
                name = tmpName.toString();

                if (valueStart >= 0) {
                    if (decodeValue) {
                        urlDecode(tmpValue);
                    }
                    tmpValue.setCharset(charset);
                    value = tmpValue.toString();
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

            tmpName.recycle();
            tmpValue.recycle();
        }
    }

    private void urlDecode(ByteChunk bc)
            throws IOException {
        if (urlDec == null) {
            urlDec = new UDecoder();
        }
        urlDec.convert(bc);
    }

    public void processParameters(MessageBytes data, String encoding) {
        if (data == null || data.isNull() || data.getLength() <= 0) {
            return;
        }

        if (data.getType() != MessageBytes.T_BYTES) {
            data.toBytes();
        }
        ByteChunk bc = data.getByteChunk();
        processParameters(bc.getBytes(), bc.getOffset(),
                bc.getLength(), getCharset(encoding));
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
