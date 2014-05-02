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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Response facade.
 *
 * @author zhanhb
 */
class Response extends HttpServletResponseWrapper {

    private Request request;
    private final UEncoder urlEncoder;

    Response(HttpServletResponse response) {
        super(response);
        this.urlEncoder = new UEncoder();
    }

    @Override
    public String encodeURL(String url) {
        return super.encodeURL(urlEncoder.encode(url));
    }

    @Override
    public String encodeRedirectURL(String url) {
        return super.encodeRedirectURL(urlEncoder.encode(url));
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return super.encodeUrl(urlEncoder.encode(url));
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return super.encodeRedirectUrl(urlEncoder.encode(url));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(urlEncoder.encode(location));
    }

    public Request getRequest() {
        if (this.request == null) {
            throw new IllegalStateException("Request not connected");
        }
        return request;
    }

    public void connect(Request request) {
        if (request == null) {
            throw new NullPointerException();
        } else if (this.request != null) {
            throw new IllegalStateException("Already connected");
        }
        this.request = request;
    }
}
