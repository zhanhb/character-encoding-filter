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
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Implementation of a character encoding connector.
 *
 * @author zhanhb
 */
@SuppressWarnings({"FinalClass", "ClassWithoutLogger"})
public final class Connector {

    private boolean useProxy;

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public ServletRequest createRequest(ServletRequest request) {
        if (request == null) {
            throw new NullPointerException();
        }
        HttpServletRequest h;
        try {
            h = (HttpServletRequest) request;
        } catch (ClassCastException ex) {
            return request;
        }
        Request req = new Request(h);
        return useProxy ? createProxy(req, h) : createWrapper(req, h);
    }

    private ServletRequest createProxy(Object... arr) {
        return (ServletRequest) Proxy.newProxyInstance(Connector.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new CustomInvocationHandler(arr));
    }

    private ServletRequest createWrapper(final Request req, final HttpServletRequest request) {
        class RequestWrapper extends HttpServletRequestWrapper {

            RequestWrapper() {
                super(request);
            }

            @Override
            public String getParameter(String name) {
                return req.getParameter(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return req.getParameterMap();
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return req.getParameterNames();
            }

            @Override
            public String[] getParameterValues(String name) {
                return req.getParameterValues(name);
            }

            @Override
            public String getServletPath() {
                return req.getServletPath();
            }

            @Override
            public String getPathInfo() {
                return req.getPathInfo();
            }

            @Override
            public String getPathTranslated() {
                return req.getPathTranslated();
            }

            @Override
            public String getCharacterEncoding() {
                return req.getCharacterEncoding();
            }

            @Override
            public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
                req.setCharacterEncoding(env);
            }

        }

        return new RequestWrapper();
    }

}
