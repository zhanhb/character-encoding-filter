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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
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
final class Request extends HttpServletRequestWrapper {

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed = false;
    private final Parameters parameters = new Parameters().setURLDecoder(new UDecoder());
    private final PathDetector detector = new PathDetector();
    private Connector connector;
    private String servletPath, expectedServletPath, expectedServletPathEncoding;
    private String pathInfo, expectedPathInfo, expectedPathInfoEncoding;
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
        if (parameterMap == null) {
            HashMap<String, String[]> map = new HashMap<String, String[]>(parseParameters().size());
            Enumeration<String> parameterNames = getParameterNames();
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
     * @return the defined values for the specified request parameter
     */
    @Override
    public String[] getParameterValues(String name) {
        return parseParameters().getParameterValues(name);
    }

    @Override
    public String getServletPath() {
        String superServletPath = super.getServletPath();
        if (superServletPath == null || superServletPath.length() == 0) {
            return superServletPath;
        }
        String characterEncoding = getCharacterEncoding();
        String path = servletPath;
        // some servers such as glassfish may change the path during the request
        if (path == null || !superServletPath.equals(expectedServletPath)
                || (characterEncoding == null
                        ? expectedServletPathEncoding != null
                        : !characterEncoding.equals(expectedServletPathEncoding))) {
            path = detector.detect(superServletPath, characterEncoding);
            servletPath = path;
            expectedServletPathEncoding = characterEncoding;
            expectedServletPath = superServletPath;
        }
        return path;
    }

    @Override
    public String getPathInfo() {
        String path = pathInfo;
        String superPathInfo = super.getPathInfo();
        if (superPathInfo == null || superPathInfo.length() == 0) {
            return superPathInfo;
        }
        String characterEncoding = getCharacterEncoding();
        if (path == null || !superPathInfo.equals(expectedPathInfo)
                || (characterEncoding == null
                        ? expectedPathInfoEncoding != null
                        : !characterEncoding.equals(expectedPathInfoEncoding))) {
            path = detector.detect(superPathInfo, characterEncoding);
            pathInfo = path;
            expectedPathInfoEncoding = characterEncoding;
            expectedPathInfo = superPathInfo;
        }
        return path;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        useInputStream = true;
        return super.getInputStream(); //To change body of generated methods, choose Tools | Templates.
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

    private void handleAllQueryStrings(Parameters param) {
        ServletRequest request = this;

        ArrayList<String> queryStrings = new ArrayList<String>();
        for (Set<Object> dejaVu = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());;) {
            dejaVu.add(request);
            if (request instanceof HttpServletRequest) {
                HttpServletRequest hrequest = (HttpServletRequest) request;
                String query = hrequest.getQueryString();
                if (query != null && !dejaVu.contains(query)) {
                    dejaVu.add(query);
                    queryStrings.add(query);
                }
            }
            try {
                request = ServletRequestWrapper.class.cast(request).getRequest();
            } catch (ClassCastException ex) {
                break;
            }
            if (dejaVu.contains(request)) {
                break;
            }
        }
        for (int i = queryStrings.size() - 1; i >= 0; --i) {
            param.handleQueryParameters(queryStrings.get(i));
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
