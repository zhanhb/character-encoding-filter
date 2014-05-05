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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Action Context.
 *
 * @author zhanhb
 */
public class ActionContext {

    private final ServletRequest request;
    private final ServletResponse response;

    ActionContext(ServletRequest request, ServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public ServletRequest getRequest() {
        return request;
    }

    public ServletResponse getResponse() {
        return response;
    }

    public void recycle() {
    }
}
