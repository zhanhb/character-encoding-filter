/*
 * Copyright 2015 zhanhb.
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 *
 * @author zhanhb
 */
class RequestWrapper extends HttpServletRequestWrapper {

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed = false;
    private final Parameters parameters = new Parameters();
    private final Detector pathDetector = Detector.newDetector();
    private final Detector pathInfoDetector = Detector.newDetector();
    private final Detector pathTranslated = Detector.newDetector();
    private String characterEncoding = CharsetFactory.ISO_8859_1.name();

    RequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletRequest getRequest() {
        return super.getRequest();
    }

    @Override
    public void setRequest(ServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (!(request instanceof HttpServletRequest)) {
            throw new ClassCastException();
        }
        super.setRequest(request);
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
     * Returns a <code>Map</code> of the parameters of this super. Request
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
        return pathDetector.expr(super.getServletPath(), characterEncoding);
    }

    @Override
    public String getPathInfo() {
        return pathInfoDetector.expr(super.getPathInfo(), characterEncoding);
    }

    @Override
    public String getPathTranslated() {
        return pathTranslated.expr(super.getPathTranslated(), characterEncoding);
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        try {
            Charset.forName(env).name();
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedEncodingException(env);
        }
        super.setCharacterEncoding(CharsetFactory.ISO_8859_1.name());
        characterEncoding = env;
    }

    private Parameters parseParameters() {
        Parameters param = this.parameters;
        if (parametersParsed) {
            return param;
        }
        parametersParsed = true;

        Enumeration<?> e = super.getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement().toString();
            String parsedName = parse(name);
            String[] parameterValues = super.getParameterValues(name);
            if (parameterValues != null) {
                for (String value : parameterValues) {
                    param.addParameter(parsedName, parse(value));
                }
            }
        }
        return param;
    }

    private String parse(String name) {
        try {
            Charset targetCharset = CharsetFactory.getCharset(characterEncoding, CharsetFactory.ISO_8859_1);
            if (!CharsetFactory.ISO_8859_1.equals(targetCharset)) {
                ByteBuffer encode = CharsetFactory.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(name));
                return targetCharset.newDecoder().decode(encode).toString();
            }
        } catch (CharacterCodingException ex) {
            // keep orign field if character coding is not correct.
        }
        return name;
    }

}
