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
package com.github.zhanhb.filter.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Request facade.
 *
 * @author zhanhb
 */
@SuppressWarnings({"ClassWithoutLogger", "FinalClass"})
final class Request extends HttpServletRequestWrapper {

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed = false;
    private final Parameters parameters = new Parameters();
    private Connector connector;
    private final Detector pathDetector = new Detector();
    private final Detector pathInfoDetector = new Detector();
    private boolean useInputStream;

    Request(HttpServletRequest request) {
        super(request);
    }

    /**
     * Return the value of the specified request parameter, if any; otherwise,
     * return <code>null</code>. If there is more than one value defined, return
     * only the first one.
     *
     * @param name Name of the desired request parameter
     * @return the value of the specified request parameter
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
        Map<String, String[]> map = parameterMap;
        if (map == null) {
            map = new HashMap<String, String[]>(parseParameters().size());
            Enumeration<String> parameterNames = getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                String[] values = getParameterValues(name);
                map.put(name, values);
            }
            parameterMap = map;
        }
        return Collections.unmodifiableMap(map);
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
     * @return the defined values for the specified request parameter
     */
    @Override
    public String[] getParameterValues(String name) {
        return parseParameters().getParameterValues(name);
    }

    @Override
    public String getServletPath() {
        return pathDetector.expr(super.getServletPath(), getCharacterEncoding());
    }

    @Override
    public String getPathInfo() {
        return pathInfoDetector.expr(super.getPathInfo(), getCharacterEncoding());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        useInputStream = true;
        return super.getInputStream();
    }

    private Connector getConnector() {
        return connector;
    }

    private ByteBuffer ensureCapacity(ByteBuffer buffer, int addition) {
        if (buffer.remaining() < addition) {
            int oldSize = buffer.limit(), newSize;
            // grow in larger chunks
            if (buffer.position() + addition < (oldSize << 1)) {
                newSize = oldSize << 1;
            } else {
                newSize = (oldSize << 1) + addition;
            }
            buffer.flip();
            return (buffer.isDirect()
                    ? ByteBuffer.allocateDirect(newSize)
                    : ByteBuffer.allocate(newSize)).put(buffer);
        }
        return buffer;
    }

    private int readPostBody(byte[] body, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        ReadableByteChannel channel = getInputChannel();
        if (channel == null) {
            return -1;
        }
        ByteBuffer buff = ByteBuffer.wrap(body, 0, len);
        for (; buff.hasRemaining() && channel.read(buff) >= 0;) {
        }
        return buff.position();
    }

    private ByteBuffer readChunkedPostBody() throws IOException {
        ReadableByteChannel channel = getInputChannel();
        if (channel == null) {
            return null;
        }
        ByteBuffer body = ByteBuffer.allocate(256);

        int len;
        do {
            len = channel.read(body);
            if (connector.getMaxPostSize() > 0 && (body.position()) > connector.getMaxPostSize()) {
                // Too much data
                throw new IOException(); // sm.getString("coyoteRequest.chunkedPostTooLarge")
            }
            if (len > 0) {
                body = ensureCapacity(body, len);
            }
        } while (len > -1);

        if (body.position() == 0) {
            return null;
        }
        body.flip();
        return body;
    }

    private ReadableByteChannel getInputChannel() {
        try {
            if (!useInputStream) {
                ServletInputStream inputStream = super.getInputStream();
                if (inputStream != null) {
                    return Channels.newChannel(inputStream);
                }
            }
        } catch (IllegalStateException ex) {
        } catch (IOException ex) {
        }
        return null;
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private void handleAllQueryStrings(Parameters param) {
        ServletRequest request = this;

        ArrayList<String> queryStrings = new ArrayList<String>();
        for (HashMap<Object, Boolean> dejaVu = new HashMap<Object, Boolean>();
                !dejaVu.containsKey(request);) {
            dejaVu.put(request, Boolean.TRUE);
            if (request instanceof HttpServletRequest) {
                String query = HttpServletRequest.class.cast(request).getQueryString();
                if (query != null && !dejaVu.containsKey(query)) {
                    dejaVu.put(query, Boolean.TRUE);
                    queryStrings.add(query);
                }
            }
            try {
                request = ServletRequestWrapper.class.cast(request).getRequest();
            } catch (ClassCastException ex) {
                break;
            }
        }
        for (int i = queryStrings.size() - 1; i >= 0; --i) {
            param.handleQueryParameters(queryStrings.get(i), i != 0);
        }
    }

    private Parameters parseParameters() {
        Parameters param = this.parameters;
        if (parametersParsed) {
            return param;
        }
        parametersParsed = true;

        // Set this every time in case limit has been changed via JMX
        param.setLimit(getConnector().getMaxParameterCount());

        // getCharacterEncoding() may have been overridden to search for
        // hidden form field containing request encoding
        String enc = getCharacterEncoding();
        handleAllQueryStrings(param.setEncoding(enc).setQueryStringEncoding(enc));

        if (!getConnector().isParseBodyMethod(getMethod())) {
            return param;
        }

        String contentType = getContentType();
        if (contentType != null) {
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
                param.processParameters(ByteBuffer.wrap(formData, 0, len));
            } else if ("chunked".equalsIgnoreCase(getHeader("transfer-encoding"))) {
                ByteBuffer formData;
                try {
                    formData = readChunkedPostBody();
                } catch (IOException e) {
                    // Client disconnect or chunkedPostTooLarge error
                    return param;
                }
                if (formData != null) {
                    param.processParameters(formData);
                }
            }
            success = true;
            return param;
        } finally {
            if (!success) {
                param.setParseFailed(true);
            }
        }
    }

    Request setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }
}
