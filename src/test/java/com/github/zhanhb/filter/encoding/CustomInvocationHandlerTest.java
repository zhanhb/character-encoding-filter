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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author zhanhb
 */
public class CustomInvocationHandlerTest {

    private static InvocationHandler of(Object... args) {
        return new CustomInvocationHandler(args);
    }

    private static Class<?>[] of(Class<?>... args) {
        return args;
    }

    private final ClassLoader load = CustomInvocationHandlerTest.class.getClassLoader();

    private <T> T build(Class<T> cl, Object... obj) {
        return cl.cast(Proxy.newProxyInstance(load, of(cl), of(obj)));
    }

    @Test(expected = UnknownError.class)
    public void testError() throws Exception {
        build(Callable.class, new Callable<Void>() {

            @Override
            public Void call() {
                throw new UnknownError("ex");
            }
        }).call();
    }

    @Test(expected = AbstractMethodError.class)
    public void testNoMethodDef() {
        build(Runnable.class).run();
    }

    @Test
    public void testMultiple() {
        Runnable exception = new Runnable() {

            @Override
            public void run() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, ok = new Runnable() {

            @Override
            public void run() {
            }
        };
        try {
            build(Runnable.class, exception, ok).run();
            fail();
        } catch (UnsupportedOperationException ex) {
            // ok
        }
        build(Runnable.class, ok, exception).run();
    }

    @Test
    public void testWithoutInterface() {
        Object exception = new Object() {

            public void run() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, ok = new Object() {

            public void run() {
            }
        };
        try {
            build(Runnable.class, exception, ok).run();
            fail();
        } catch (UnsupportedOperationException ex) {
            // ok
        }
        build(Runnable.class, ok, exception).run();
    }

    @Test
    public void testNotImplement() {
        build(Runnable.class, new Object(), new Runnable() {

            @Override
            public void run() {
            }
        }).run();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIae() {
        build(Runnable.class, new Object(), new Runnable() {

            @Override
            public void run() {
                throw new IllegalArgumentException();
            }
        }).run();
    }

}
