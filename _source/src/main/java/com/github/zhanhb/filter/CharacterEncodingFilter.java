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
import java.util.Locale;
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
    private boolean setResponseCharacterEncoding = false;
    private Connector connector;

    public CharacterEncodingFilter() {
        this("UTF-8");
    }

    public CharacterEncodingFilter(String characterEncoding) {
        this.connector = new Connector();
        this.characterEncoding = characterEncoding;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletRequest createRequest = connector.createRequest(request);
        createRequest.setCharacterEncoding(characterEncoding);
        if (setResponseCharacterEncoding) {
            response.setCharacterEncoding(characterEncoding);
        }
        chain.doFilter(createRequest, response);
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Enumeration<?> e = filterConfig.getInitParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement().toString();
            String upperCase = name.toUpperCase(Locale.US);
            if (upperCase.contains("ENCODING")) {
                String value = filterConfig.getInitParameter(name);
                if (value != null) {
                    if (upperCase.contains("RESPONSE")) {
                        setResponseCharacterEncoding = Boolean.parseBoolean(value);
                    } else {
                        setCharacterEncoding(value);
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
    }
}
