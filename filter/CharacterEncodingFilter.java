package com.ys168.zhanhb.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class CharacterEncodingFilter implements Filter {

    private String characterEncoding = "UTF-8";
    private Connector connector;
    private boolean guessURIEncoding = true;

    public void init(FilterConfig filterConfig) throws ServletException {
        Connector conn = new Connector();

        String str = filterConfig.getInitParameter("characterEncoding");
        if (str != null) {
            characterEncoding = str;
        }
        str = filterConfig.getInitParameter("queryStringEncoding");
        if (str == null) {
            str = "UTF-8";
        }
        conn.setQueryStringEncoding(str);
        str = filterConfig.getInitParameter("parseBodyMethods");
        if (str == null) {
            str = conn.getParseBodyMethods();
        }
        conn.setParseBodyMethods(str);
        str = filterConfig.getInitParameter("useBodyEncodingForQueryString");
        if (str != null) {
            conn.setUseBodyEncodingForQueryString(Boolean.parseBoolean(str.trim()));
        }
        str = filterConfig.getInitParameter("maxPostSize");
        if (str != null) {
            conn.setMaxPostSize(Integer.parseInt(str.trim()));
        }
        str = filterConfig.getInitParameter("maxParameterCount");
        if (str != null) {
            conn.setMaxParameterCount(Integer.parseInt(str.trim()));
        }
        str = filterConfig.getInitParameter("guessURIEncoding");
        if (str != null) {
            guessURIEncoding = Boolean.parseBoolean(str.trim());
        }
        this.connector = conn;
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (characterEncoding != null) {
            req.setCharacterEncoding(characterEncoding);
            resp.setCharacterEncoding(characterEncoding);

            HttpServletRequest request;
            try {
                request = (HttpServletRequest) req;
            } catch (ClassCastException ex) {
                chain.doFilter(req, resp);
                return;
            }
            // Create objects
            Request wrapper = connector.createRequest(request);
            wrapper.setGuessURIEncoding(guessURIEncoding);
            // Set query string encoding
            wrapper.getParameters().setQueryStringEncoding(
                    connector.getQueryStringEncoding());
            chain.doFilter(wrapper, resp);
            wrapper.recycle();
        } else {
            chain.doFilter(req, resp);
        }
    }

    public void destroy() {
        connector = null;
    }
}
