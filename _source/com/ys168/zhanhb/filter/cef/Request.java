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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request facade.
 *
 * @author zhanhb
 */
class Request extends HttpServletRequestWrapper {

    private static final int CACHED_POST_LEN = 8192;

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed;
    private final Parameters parameters;
    private final PathDetector detector;
    private Connector connector;

    Request(HttpServletRequest request) {
        super(request);
        this.parametersParsed = false;
        this.parameters = new Parameters().setURLDecoder(new UDecoder());
        this.detector = new PathDetector();
    }

    /**
     * Return the value of the specified request parameter, if any; otherwise,
     * return <code>null</code>. If there is more than one value defined, return
     * only the first one.
     *
     * @param name Name of the desired request parameter
     * @return
     */
    @Override
    public String getParameter(String name) {
        return parseParameters().getParameter(name);
    }

    /**
     * Returns a <code>Map</code> of the parameters of this request. Request
     * parameters are extra information sent with the request. For HTTP
     * servlets, parameters are contained in the query string or posted form
     * data.
     *
     * @return A <code>Map</code> containing parameter names as keys and
     * parameter values as map values.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        if (parameterMap == null) {
            Enumeration<String> parameterNames = getParameterNames();
            // after statement getParameterNames the parameter will be parsed
            // parameters.size() will be exactly the parameter size.
            HashMap<String, String[]> map = new HashMap<String, String[]>(parameters.size());
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                String[] values = getParameterValues(name);
                map.put(name, values);
            }
            parameterMap = map;
        }
        return Collections.unmodifiableMap(parameterMap);
    }

    /**
     *
     * @return the names of all defined request parameters for this request.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return parseParameters().getParameterNames();
    }

    /**
     * Return the defined values for the specified request parameter, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired request parameter
     * @return
     */
    @Override
    public String[] getParameterValues(String name) {
        return parseParameters().getParameterValues(name);
    }

    @Override
    public String getServletPath() {
        return detector.detect(super.getServletPath(), getCharacterEncoding());
    }

    @Override
    public String getPathInfo() {
        return detector.detect(super.getPathInfo(), getCharacterEncoding());
    }

    private Connector getConnector() {
        return connector;
    }

    private ByteBuffer ensureCapacity(ByteBuffer buff, int addition) {
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
                    ? ByteBuffer.allocateDirect(newSize)
                    : ByteBuffer.allocate(newSize)).put(buff);
        }
        return buff;
    }

    private int readPostBody(byte[] body, int len) throws IOException {
        InputStream inputStream = getInputStream0();
        if (inputStream == null) {
            return -1;
        }
        int offset = 0;
        do {
            int inputLen = inputStream.read(body, offset, len - offset);
            if (inputLen < 0) {
                return offset;
            }
            offset += inputLen;
        } while ((len - offset) > 0);
        return len;
    }

    private byte[] readChunkedPostBody() throws IOException {
        InputStream inputStream = getInputStream0();
        if (inputStream == null) {
            return null;
        }
        ByteBuffer body = ByteBuffer.allocate(256);

        byte[] buffer = new byte[CACHED_POST_LEN];

        int len;
        do {
            len = inputStream.read(buffer, 0, CACHED_POST_LEN);
            if (connector.getMaxPostSize() > 0 && (body.position() + len) > connector.getMaxPostSize()) {
                // Too much data
                throw new IOException(); // sm.getString("coyoteRequest.chunkedPostTooLarge")
            }
            if (len > 0) {
                body = ensureCapacity(body, len).put(buffer, 0, len);
            }
        } while (len > -1);

        if (body.position() == 0) {
            return null;
        }
        if (body.position() < body.limit()) {
            int length = body.position();
            byte[] result = new byte[length];
            body.flip();
            body.get(result, 0, length);
            return result;
        } else {
            return body.array();
        }
    }

    private InputStream getInputStream0() {
        try {
            return getInputStream();
        } catch (IllegalStateException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private Parameters parseParameters() {
        Parameters param = this.parameters;
        if (parametersParsed) {
            return param;
        }
        parametersParsed = true;

        param.setQueryString(getQueryString())
                // Set this every time in case limit has been changed via JMX
                .setLimit(getConnector().getMaxParameterCount());

        // getCharacterEncoding() may have been overridden to search for
        // hidden form field containing request encoding
        String enc = getCharacterEncoding();

        param.setEncoding(enc)
                .setQueryStringEncoding(enc)
                .handleQueryParameters();

        if (!getConnector().isParseBodyMethod(getMethod())) {
            return param;
        }

        String contentType = getContentType();
        if (contentType == null) {
            contentType = "";
        } else {
            int semicolon = contentType.indexOf(';');
            if (semicolon >= 0) {
                contentType = contentType.substring(0, semicolon);
            }
            contentType = contentType.trim();
        }
        if (!("application/x-www-form-urlencoded".equals(contentType))) {
            return param;
        }

        boolean success = false;
        int len = getContentLength();

        try {
            if (len > 0) {
                int maxPostSize = connector.getMaxPostSize();
                if ((maxPostSize > 0) && (len > maxPostSize)) {
                    return param;
                }
                byte[] formData = new byte[len];

                try {
                    if (readPostBody(formData, len) != len) {
                        return param;
                    }
                } catch (IOException e) {
                    // Client disconnect
                    return param;
                }
                param.processParameters(formData, 0, len);
            } else if ("chunked".equalsIgnoreCase(getHeader("transfer-encoding"))) {
                byte[] formData;
                try {
                    formData = readChunkedPostBody();
                } catch (IOException e) {
                    // Client disconnect or chunkedPostTooLarge error
                    return param;
                }
                if (formData != null) {
                    param.processParameters(formData, 0, formData.length);
                }
            }
            success = true;
        } finally {
            if (!success) {
                param.setParseFailed(true);
            }
        }
        return param;
    }

    Request setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }

}
