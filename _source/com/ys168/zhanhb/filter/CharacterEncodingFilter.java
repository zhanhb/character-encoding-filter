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
package com.ys168.zhanhb.filter;

import com.ys168.zhanhb.filter.cef.Connector;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author zhanhb
 */
public class CharacterEncodingFilter implements Filter {

    private String characterEncoding = "UTF-8";
    private Connector connector;
    private boolean setResponseCharacterEncoding = true;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Connector conn = new Connector();

        String str = filterConfig.getInitParameter("characterEncoding");
        if (str != null) {
            characterEncoding = str;
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
        str = filterConfig.getInitParameter("maxSavePostSize");
        if (str != null) {
            conn.setMaxSavePostSize(Integer.parseInt(str.trim()));
        }
        this.connector = conn;
        str = filterConfig.getInitParameter("setResponseCharacterEncoding");
        if (str != null) {
            setResponseCharacterEncoding = Boolean.parseBoolean(str.trim());
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (characterEncoding != null) {
            request.setCharacterEncoding(characterEncoding);
            if (setResponseCharacterEncoding) {
                response.setCharacterEncoding(characterEncoding);
            }
        }
        // Create request and do the chain
        chain.doFilter(connector.createRequest(request),
                connector.createResponse(response));
    }

    @Override
    public void destroy() {
        connector = null;
    }
}
