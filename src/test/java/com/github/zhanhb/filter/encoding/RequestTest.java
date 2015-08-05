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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RequestTest {

    public static final Charset UTF_8 = CharsetFactory.UTF_8;
    public static final Charset ISO_8859_1 = CharsetFactory.ISO_8859_1;

    @Test
    public void testProxy() {
        System.out.println("testProxy");
        HttpServletRequest proxy = (HttpServletRequest) Proxy.newProxyInstance(RequestTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                        throw new IllegalStateException();
                    }
                });
        assertTrue(Proxy.class.isAssignableFrom(proxy.getClass()));
    }
}
