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

import java.lang.reflect.Proxy;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

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
        return useProxy ? createProxy(new Request(h), h) : createWrapper(h);
    }

    private ServletRequest createProxy(Object... arr) {
        return (ServletRequest) Proxy.newProxyInstance(Connector.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new CustomInvocationHandler(arr));
    }

    private ServletRequest createWrapper(HttpServletRequest request) {
        return new RequestWrapper(request);
    }

}
