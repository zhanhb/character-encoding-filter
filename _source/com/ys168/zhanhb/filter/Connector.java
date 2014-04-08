/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of a Coyote connector.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */
public class Connector {

    // ----------------------------------------------------- Instance Variables
    /**
     * The string manager for this package.
     */
    private static final ResourceBundle bundle
            = ResourceBundle.getBundle("com.ys168.zhanhb.filter" + ".LocalStrings");

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
     * Maximum size of a POST which will be saved by the container during
     * authentication. 4kB by default
     */
    private int maxSavePostSize = 4 * 1024;

    /**
     * Comma-separated list of HTTP methods that will be parsed according to
     * POST-style rules for application/x-www-form-urlencoded request bodies.
     */
    private String parseBodyMethods = "POST";

    /**
     * A Set of methods determined by {@link #parseBodyMethods}.
     */
    private HashSet<String> parseBodyMethodsSet;

    /**
     * query string encoding.
     */
    private String queryStringEncoding = null;

    /**
     * URI encoding as body.
     */
    private boolean useBodyEncodingForQueryString = false;

    // ------------------------------------------------------------ Constructor
    public Connector() {
    }

    // ------------------------------------------------------------- Properties
    /**
     * Return the maximum number of parameters (GET plus POST) that will be
     * automatically parsed by the container. A value of less than 0 means no
     * limit.
     *
     * @return
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
     */
    public void setMaxParameterCount(int maxParameterCount) {
        this.maxParameterCount = maxParameterCount;
    }

    /**
     * Return the maximum size of a POST which will be automatically parsed by
     * the container.
     *
     * @return
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
     */
    public void setMaxPostSize(int maxPostSize) {
        this.maxPostSize = maxPostSize;
    }

    /**
     * Return the maximum size of a POST which will be saved by the container
     * during authentication.
     *
     * @return
     */
    public int getMaxSavePostSize() {
        return maxSavePostSize;
    }

    /**
     * Set the maximum size of a POST which will be saved by the container
     * during authentication.
     *
     * @param maxSavePostSize The new maximum size in bytes of a POST which will
     * be saved by the container during authentication.
     */
    public void setMaxSavePostSize(int maxSavePostSize) {
        this.maxSavePostSize = maxSavePostSize;
    }

    public String getParseBodyMethods() {
        return this.parseBodyMethods;
    }

    public void setParseBodyMethods(String methods) {
        HashSet<String> methodSet;
        if (null != methods) {
            List<String> asList = Arrays.asList(methods.split("\\s*,\\s*"));
            methodSet = new HashSet<String>(asList.size());
            for (String string : asList) {
                methodSet.add(string.trim().toUpperCase(Locale.ENGLISH));
            }
            if (methodSet.contains("TRACE")) {
                throw new IllegalArgumentException(bundle.getString("coyoteConnector.parseBodyMethodNoTrace"));
            }
        } else {
            methodSet = new HashSet<String>(0);
        }
        this.parseBodyMethods = methods;
        this.parseBodyMethodsSet = methodSet;
    }

    boolean isParseBodyMethod(String method) {
        return parseBodyMethodsSet.contains(method);
    }

    public String getQueryStringEncoding() {
        return queryStringEncoding;
    }

    public void setQueryStringEncoding(String URIEncoding) {
        this.queryStringEncoding = URIEncoding;
    }

    public boolean isUseBodyEncodingForQueryString() {
        return useBodyEncodingForQueryString;
    }

    public void setUseBodyEncodingForQueryString(boolean useBodyEncodingForURI) {
        this.useBodyEncodingForQueryString = useBodyEncodingForURI;
    }

    public Request createRequest(HttpServletRequest req) {
        Request request = new Request(req);
        request.setConnector(this);
        return request;
    }
}
