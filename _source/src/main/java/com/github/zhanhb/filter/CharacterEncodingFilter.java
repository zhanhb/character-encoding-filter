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
package com.github.zhanhb.filter;

import com.github.zhanhb.filter.encoding.Connector;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Character Encoding Filter
 *
 * @author zhanhb
 */
public class CharacterEncodingFilter implements Filter {

    private String characterEncoding = "UTF-8";
    private Connector connector;
    private boolean setResponseCharacterEncoding = false;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Connector conn = new Connector();

        String str = filterConfig.getInitParameter("characterEncoding");
        if (str != null) {
            characterEncoding = str;
        } else {
            Enumeration<String> e = filterConfig.getInitParameterNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                if (name != null) {
                    String upperCase = name.toUpperCase();
                    if (!upperCase.contains("RESPONSE") && upperCase.contains("ENCODING")) {
                        characterEncoding = filterConfig.getInitParameter(name);
                        break;
                    }
                }
            }
        }

        str = filterConfig.getInitParameter("parseBodyMethods");
        if (str == null) {
            str = conn.getParseBodyMethods();
        }
        conn.setParseBodyMethods(str);
        str = filterConfig.getInitParameter("maxPostSize");
        if (str != null) {
            conn.setMaxPostSize(Integer.parseInt(str.trim()));
        }
        str = filterConfig.getInitParameter("maxParameterCount");
        if (str != null) {
            conn.setMaxParameterCount(Integer.parseInt(str.trim()));
        }
        this.connector = conn;
        str = filterConfig.getInitParameter("setResponseEncoding");
        if (str != null) {
            setResponseCharacterEncoding = Boolean.parseBoolean(str.trim());
        } else {
            Enumeration<String> e = filterConfig.getInitParameterNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                if (name != null) {
                    String upperCase = name.toUpperCase();
                    if (upperCase.contains("ENCODING") && upperCase.contains("RESPONSE")) {
                        setResponseCharacterEncoding = Boolean.parseBoolean(upperCase.trim());
                    }
                }
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String enc = this.characterEncoding;
        if (enc != null) {
            request.setCharacterEncoding(enc);
            if (setResponseCharacterEncoding) {
                response.setCharacterEncoding(enc);
            }
        }
        chain.doFilter(connector.createRequest(request), response);
    }

    @Override
    public void destroy() {
        connector = null;
    }
}
