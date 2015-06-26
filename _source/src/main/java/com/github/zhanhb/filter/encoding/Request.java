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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Request facade.
 *
 * @author zhanhb
 */
final class Request {

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed = false;
    private final Parameters parameters = new Parameters();
    private final Detector pathDetector =  Detector.newDetector();
    private final Detector pathInfoDetector = Detector.newDetector();
    private final Detector pathTranslated = Detector.newDetector();
    private final HttpServletRequest request;
    private String characterEncoding = CharsetFactory.ISO_8859_1.name();

    Request(HttpServletRequest request) {
        if (request == null) {
            throw new NullPointerException();
        }
        this.request = request;
    }

    /**
     * Return the value of the specified request parameter, if any; otherwise,
     * return <code>null</code>. If there is more than one value defined, return
     * only the first one.
     *
     * @param name Name of the desired request parameter
     * @return the value of the specified request parameter
     */
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
    public String[] getParameterValues(String name) {
        return parseParameters().getParameterValues(name);
    }

    public String getServletPath() {
        return pathDetector.expr(request.getServletPath(), characterEncoding);
    }

    public String getPathInfo() {
        return pathInfoDetector.expr(request.getPathInfo(), characterEncoding);
    }

    public String getPathTranslated() {
        return pathTranslated.expr(request.getPathTranslated(), characterEncoding);
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        request.setCharacterEncoding(CharsetFactory.ISO_8859_1.name());
        try {
            characterEncoding = Charset.forName(env).name();
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedEncodingException(env);
        }
    }

    private Parameters parseParameters() {
        Parameters param = this.parameters;
        if (parametersParsed) {
            return param;
        }
        parametersParsed = true;

        Enumeration<?> e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = parse(e.nextElement().toString());
            String[] parameterValues = request.getParameterValues(name);
            if (parameterValues != null) {
                for (String value : parameterValues) {
                    param.addParameter(name, parse(value));
                }
            }
        }
        return param;
    }

    private String parse(String name) {
        try {
            ByteBuffer encode = CharsetFactory.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(name));
            return CharsetFactory.getCharset(characterEncoding, CharsetFactory.UTF_8).decode(encode).toString();
        } catch (CharacterCodingException ex) {
        }
        return name;
    }

}
