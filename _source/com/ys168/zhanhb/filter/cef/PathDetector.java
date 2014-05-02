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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashSet;
import java.util.Set;

/**
 * Path Detector. detect the contextPath and pathInfo who doesn't have properly
 * character encoding.
 *
 * @author zhanhb
 */
class PathDetector {

    String detect(String path, String encoding) {
        if (path == null || path.length() == 0) {
            return path;
        }
        ByteBuffer bytes;
        try {
            bytes = Constants.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(path));
        } catch (CharacterCodingException ex) {
            // not latin bytes
            // Strings already parsed by the server
            return path;
        }
        if (bytes.position() == path.length()) {
            // ASCII bytes, just return
            return path;
        }

        // browsers will transfer URI encoding as UTF-8
        // so we try UTF-8 first, then the character encoding set in request attribute
        // if failed we will try the system default encoding
        Set<Charset> set = new HashSet<Charset>(4);
        for (Object object : new Object[]{Constants.UTF8, encoding, Charset.defaultCharset()}) {
            final Charset charset;
            if (object instanceof Charset) {
                charset = (Charset) object;
            } else if (object instanceof String) {
                try {
                    charset = Charset.forName(encoding);
                } catch (UnsupportedCharsetException ex) {
                    continue;
                }
            } else {
                // often null
                continue;
            }
            if (set.contains(charset)) {
                continue;
            }
            set.add(charset);
            int position = bytes.position();
            try {
                return charset.newDecoder().decode(bytes).toString();
            } catch (CharacterCodingException ex) {
                // rollback
                bytes.position(position);
            }
        }
        return path;
    }
}
