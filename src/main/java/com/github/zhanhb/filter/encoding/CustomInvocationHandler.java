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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author zhanhb
 */
class CustomInvocationHandler implements InvocationHandler {

    private static StringBuilder getMethodDescriptor(StringBuilder buf, Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        buf.append('(');
        for (int i = 0, len = parameters.length; i < len; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        buf.append(')');
        getDescriptor(buf, m.getReturnType());
        return buf;
    }

    private static void getDescriptor(StringBuilder buf, Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */ {
                    car = 'J';
                }
                buf.append(car);
                break;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L').append(d.getName().replace('.', '/')).append(';');
                break;
            }
        }
    }

    private final Object[] parents;

    CustomInvocationHandler(Object... parents) {
        Object[] clone = parents.clone();
        for (Object parent : clone) {
            if (parent == null) {
                throw new NullPointerException();
            }
        }
        this.parents = clone;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        switch (parameterTypes.length) {
            case 0:
                if ("toString".equals(methodName)) {
                    return proxy.getClass().getName() + "@" + Integer.toHexString(proxy.hashCode());
                }
                if ("hashCode".equals(methodName)) {
                    return System.identityHashCode(proxy);
                }
                break;
            case 1:
                if ("equals".equals(methodName) && args[0] == Object.class) {
                    return proxy == args[0];
                }
                break;
        }

        for (int i = 0, len = parents.length; i < len; ++i) {
            Object parent = parents[i];
            Method m;
            try {
                m = parent.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                continue;
            }
            try {
                return m.invoke(parent, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        StringBuilder buf = new StringBuilder(proxy.getClass().getName()).append('.').append(methodName);
        throw new AbstractMethodError(getMethodDescriptor(buf, method).toString());
    }

}
