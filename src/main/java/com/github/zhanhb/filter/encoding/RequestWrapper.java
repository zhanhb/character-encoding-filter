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
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 *
 * @author zhanhb
 */
class RequestWrapper extends HttpServletRequestWrapper {

    private final Request delegate;

    RequestWrapper(HttpServletRequest request) {
        super(request);
        delegate = new Request(request);
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
        return delegate.getParameter(name);
    }

    /**
     * Returns a <code>Map</code> of the parameters of this super. Request
     * parameters are extra information sent with the super. For HTTP servlets,
     * parameters are contained in the query string or posted form data.
     *
     * @return A <code>Map</code> containing parameter names as keys and
     * parameter values as map values.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return delegate.getParameterMap();
    }

    /**
     *
     * @return the names of all defined request parameters for this super.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return delegate.getParameterNames();
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
        return delegate.getParameterValues(name);
    }

    @Override
    public String getServletPath() {
        return delegate.getServletPath();
    }

    @Override
    public String getPathInfo() {
        return delegate.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return delegate.getPathTranslated();
    }

    @Override
    public String getCharacterEncoding() {
        return delegate.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        delegate.setCharacterEncoding(env);
    }

    @Override
    public void setRequest(ServletRequest request) {
        super.setRequest(request);
        delegate.setRequest(HttpServletRequest.class.cast(request));
    }

}
