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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of a character encoding connector.
 *
 * @author zhanhb
 */
public final class Connector {

    // ----------------------------------------------------- Instance Variables
    /**
     * The maximum number of parameters (GET plus POST) which will be
     * automatically parsed by the container. 10000 by default. A value of less
     * than 0 means no limit.
     */
    private int maxParameterCount = 10000;

    /**
     * Maximum size of a POST which will be automatically parsed by the
     * container. 2MB by default.
     */
    private int maxPostSize = 2 * 1024 * 1024;

    /**
     * Comma-separated list of HTTP methods that will be parsed according to
     * POST-style rules for application/x-www-form-urlencoded request bodies.
     */
    private String parseBodyMethods = "POST";

    /**
     * A Set of methods determined by {@link #parseBodyMethods}.
     */
    private Set<String> parseBodyMethodsSet;

    // ------------------------------------------------------------- Properties
    /**
     * Return the maximum number of parameters (GET plus POST) that will be
     * automatically parsed by the container. A value of less than 0 means no
     * limit.
     *
     * @return the maximum number of parameters (GET plus POST) that will be
     * automatically parsed by the container.
     */
    public int getMaxParameterCount() {
        return maxParameterCount;
    }

    /**
     * Set the maximum number of parameters (GET plus POST) that will be
     * automatically parsed by the container. A value of less than 0 means no
     * limit.
     *
     * @param maxParameterCount The new setting
     * @return this
     */
    public Connector setMaxParameterCount(int maxParameterCount) {
        this.maxParameterCount = maxParameterCount;
        return this;
    }

    /**
     * Return the maximum size of a POST which will be automatically parsed by
     * the container.
     *
     * @return the maximum size of a POST which will be automatically parsed by
     * the container.
     */
    public int getMaxPostSize() {
        return maxPostSize;
    }

    /**
     * Set the maximum size of a POST which will be automatically parsed by the
     * container.
     *
     * @param maxPostSize The new maximum size in bytes of a POST which will be
     * automatically parsed by the container
     * @return this
     */
    public Connector setMaxPostSize(int maxPostSize) {
        this.maxPostSize = maxPostSize;
        return this;
    }

    public String getParseBodyMethods() {
        return this.parseBodyMethods;
    }

    public Connector setParseBodyMethods(String methods) {
        Set<String> methodSet;
        methodSet = new HashSet<String>(
                Arrays.asList(
                        (methods != null ? methods : "")
                        .trim()
                        .toUpperCase(Locale.ENGLISH)
                        .split("\\s*,\\s*")
                )
        );
        if (methodSet.contains("TRACE")) {
            throw new IllegalArgumentException("TRACE method MUST NOT include an entity (see RFC 2616 Section 9.6)");
        }
        this.parseBodyMethods = methods;
        this.parseBodyMethodsSet = methodSet;
        return this;
    }

    public boolean isParseBodyMethod(String method) {
        if (parseBodyMethodsSet == null) {
            setParseBodyMethods(getParseBodyMethods());
        }
        return parseBodyMethodsSet.contains(method);
    }

    public ServletRequest createRequest(ServletRequest request) {
        return request instanceof HttpServletRequest
                ? new Request((HttpServletRequest) request).setConnector(this)
                : request;
    }
}
