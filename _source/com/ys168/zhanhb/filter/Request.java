package com.ys168.zhanhb.filter;

import com.ys168.zhanhb.filter.buf.ByteChunk;
import com.ys168.zhanhb.filter.buf.MessageBytes;
import com.ys168.zhanhb.filter.buf.UDecoder;
import com.ys168.zhanhb.filter.http.Parameters;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class Request extends HttpServletRequestWrapper {

    public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
    private static final int CACHED_POST_LEN = 8192;
    private static final Pattern pattern = Pattern.compile(buildUTF8Pattern());

    /**
     *
     * @see http://tools.ietf.org/html/rfc3629#section-4
     * @return
     */
    private static String buildUTF8Pattern() {
        final String utf8_1 = "[\u0000-\u007F]";
        final String utf8tail = "[\u0080-\u00BF]";
        final String utf8_2 = "(?:[\u00C2-\u00DF]" + utf8tail + ")";
        final String utf8_3 = "(?:\u00E0[\u00A0-\u00BF]" + utf8tail + ")|"
                + "(?:[\u00E1-\u00EC]" + utf8tail + "{2}" + ")|"
                + "(?:\u00ED[\u0080-\u009F]" + utf8tail + ")|"
                + "(?:[\u00EE-\u00EF]" + utf8tail + "{2}" + ")";
        final String utf8_4 = "(?:\u00F0[\u0090-\u00BF]" + utf8tail + "{2})|"
                + "(?:[\u00F1-\u00F3]" + utf8tail + "{3}" + ")|"
                + "(?:\u00F4[\u0080-\u008F]" + utf8tail + "{2}" + ")";
        final String utf8char = utf8_1 + "|" + utf8_2 + "|" + utf8_3 + "|" + utf8_4;
        final String utf8 = "(?:" + utf8char + ")*";
        return utf8;
    }

    private Map<String, String[]> parameterMap;
    private boolean parametersParsed = false;
    private final Parameters parameters = new Parameters();
    private Connector connector;
    private byte[] postData = null;
    private final MessageBytes queryMB = MessageBytes.newInstance();
    private boolean guessURIEncoding;

    /**
     * URL decoder.
     */
    private final UDecoder urlDecoder = new UDecoder();

    public Request(HttpServletRequest request) {
        super(request);
        parameters.setQuery(queryMB);
        parameters.setURLDecoder(urlDecoder);
    }

    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Return the value of the specified request parameter, if any; otherwise,
     * return <code>null</code>. If there is more than one value defined, return
     * only the first one.
     *
     * @param name Name of the desired request parameter
     * @return
     */
    public String getParameter(String name) {
        if (!parametersParsed) {
            parseParameters();
        }
        return parameters.getParameter(name);
    }

    /**
     * Returns a <code>Map</code> of the parameters of this request. Request
     * parameters are extra information sent with the request. For HTTP
     * servlets, parameters are contained in the query string or posted form
     * data.
     *
     * @return A <code>Map</code> containing parameter names as keys and
     * parameter values as map values.
     */
    public Map<String, String[]> getParameterMap() {
        if (parameterMap == null) {
            HashMap<String, String[]> map = new HashMap<String, String[]>(10);
            Enumeration<String> parameterNames = getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                String[] values = getParameterValues(name);
                map.put(name, values);
            }
            parameterMap = map;
        }
        return Collections.unmodifiableMap(parameterMap);
    }

    /**
     *
     * @return the names of all defined request parameters for this request.
     */
    public Enumeration<String> getParameterNames() {
        if (!parametersParsed) {
            parseParameters();
        }
        return parameters.getParameterNames();
    }

    /**
     * Return the defined values for the specified request parameter, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired request parameter
     * @return
     */
    public String[] getParameterValues(String name) {
        if (!parametersParsed) {
            parseParameters();
        }
        return parameters.getParameterValues(name);
    }

    public String getServletPath() {
        String servletPath = super.getServletPath();
        if (guessURIEncoding) {
            if (pattern.matcher(servletPath).matches()) {  // UTF-8 bytes but recognized as ISO-8859-1
                try {
                    return new String(servletPath.getBytes("ISO-8859-1"), "UTF-8");
                } catch (UnsupportedEncodingException impossible) {
                }
            }
        }
        return servletPath;
    }

    public Connector getConnector() {
        return connector;
    }

    private int readPostBody(InputStream inputStream, byte[] body, int len) throws IOException {
        int offset = 0;
        do {
            int inputLen = inputStream.read(body, offset, len - offset);
            if (inputLen <= 0) {
                return offset;
            }
            offset += inputLen;
        } while ((len - offset) > 0);
        return len;
    }

    private byte[] readChunkedPostBody(InputStream inputStream) throws IOException {
        ByteChunk body = new ByteChunk();

        byte[] buffer = new byte[CACHED_POST_LEN];

        int len = 0;
        while (len > -1) {
            len = inputStream.read(buffer, 0, CACHED_POST_LEN);
            if (connector.getMaxPostSize() > 0 && (body.getLength() + len) > connector.getMaxPostSize()) {
                // Too much data
                throw new IOException(); // sm.getString("coyoteRequest.chunkedPostTooLarge")
            }
            if (len > 0) {
                body.append(buffer, 0, len);
            }
        }
        if (body.getLength() == 0) {
            return null;
        }
        if (body.getLength() < body.getBuffer().length) {
            int length = body.getLength();
            byte[] result = new byte[length];
            System.arraycopy(body.getBuffer(), 0, result, 0, length);
            return result;
        } else {
            return body.getBuffer();
        }
    }

    private void parseParameters() {
        parametersParsed = true;

        queryMB.setString(super.getQueryString());

        // Set this every time in case limit has been changed via JMX
        parameters.setLimit(getConnector().getMaxParameterCount());

        // getCharacterEncoding() may have been overridden to search for
        // hidden form field containing request encoding
        String enc = getCharacterEncoding();

        boolean useBodyEncodingForURI = connector.isUseBodyEncodingForQueryString();
        if (enc != null) {
            parameters.setEncoding(enc);
            if (useBodyEncodingForURI) {
                parameters.setQueryStringEncoding(enc);
            } else {
                parameters.setQueryStringEncoding(connector.getQueryStringEncoding());
            }
        } else {
            parameters.setEncoding(DEFAULT_CHARACTER_ENCODING);
            if (useBodyEncodingForURI) {
                parameters.setQueryStringEncoding(DEFAULT_CHARACTER_ENCODING);
            } else {
                parameters.setQueryStringEncoding(connector.getQueryStringEncoding());
            }
        }

        parameters.handleQueryParameters();

        if (!getConnector().isParseBodyMethod(getMethod())) {
            return;
        }

        String contentType = getContentType();
        if (contentType == null) {
            contentType = "";
        }
        int semicolon = contentType.indexOf(';');
        if (semicolon >= 0) {
            contentType = contentType.substring(0, semicolon);
        }
        contentType = contentType.trim();
        if (!("application/x-www-form-urlencoded".equals(contentType))) {
            return;
        }

        boolean success = false;
        int len = getContentLength();

        InputStream inputStream;
        try {
            inputStream = super.getInputStream();
        } catch (IllegalStateException ignored) { // IOException, IllegalStateException
            return;
        } catch (IOException ignored) { // IOException, IllegalStateException
            return;
        }

        try {
            if (len > 0) {
                int maxPostSize = connector.getMaxPostSize();
                if ((maxPostSize > 0) && (len > maxPostSize)) {
                    return;
                }
                byte[] formData;
                if (len < CACHED_POST_LEN) {
                    if (postData == null) {
                        postData = new byte[CACHED_POST_LEN];
                    }
                    formData = postData;
                } else {
                    formData = new byte[len];
                }
                try {
                    if (readPostBody(inputStream, formData, len) != len) {
                        return;
                    }
                } catch (IOException e) {
                    // Client disconnect
                    return;
                }
                parameters.processParameters(formData, 0, len);
            } else if ("chunked".equalsIgnoreCase(getHeader("transfer-encoding"))) {
                byte[] formData;
                try {
                    formData = readChunkedPostBody(inputStream);
                } catch (IOException e) {
                    // Client disconnect or chunkedPostTooLarge error
                    return;
                }
                if (formData != null) {
                    parameters.processParameters(formData, 0, formData.length);
                }
            }
            success = true;
        } finally {
            if (!success) {
                parameters.setParseFailed(true);
            }
        }
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public UDecoder getURLDecoder() {
        return urlDecoder;
    }

    public void setGuessURIEncoding(boolean guessURIEncoding) {
        this.guessURIEncoding = guessURIEncoding;
    }

    public void recycle() {
        parameterMap = null;
        queryMB.recycle();
        parametersParsed = false;
        parameters.recycle();
    }
}
