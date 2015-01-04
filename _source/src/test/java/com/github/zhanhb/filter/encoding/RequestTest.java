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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.junit.Test;

public class RequestTest {

    public static final Charset UTF_8 = CharsetFactory.UTF_8;
    public static final Charset ISO_8859_1 = CharsetFactory.ISO_8859_1;

    private String gg(String servletPath) throws IOException {
        try {
            String enc = "utf8";
            Charset charset = CharsetFactory.getCharset(enc, ISO_8859_1);
            if (!charset.equals(UTF_8) && !charset.equals(ISO_8859_1)) {
                ByteBuffer bytes = ISO_8859_1.encode(servletPath);
                try {
                    return charset.newDecoder().decode(bytes).toString();
                } catch (CharacterCodingException ex) {
                }
            }
        } catch (UnsupportedCharsetException ex) {
        }
        return servletPath;
    }

    @Test
    public void testGetServletPath() throws IOException {
        // this getBytes of this char as GBK you can get a UTF-8 like bytes.
        byte[] bytes = "通通".getBytes("GBK");
        System.out.println(gg(new String(bytes, "ISO-8859-1")));
        ByteBuffer wrap = ByteBuffer.wrap(bytes);
        System.out.println(ISO_8859_1.newDecoder().decode(wrap));
    }
}
