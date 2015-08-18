/*
 * Copyright 2014-2015 zhanhb.
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

/**
 * Path Detector. detect the contextPath and pathInfo who doesn't have properly
 * character encoding.
 *
 * @author zhanhb
 */
@SuppressWarnings("FinalClass")
final class Detector {

    public static Detector newDetector() {
        return new Detector();
    }

    private String expectPath;
    private String expectEncoding;
    private String result;

    private Detector() {
    }

    private String detect(String path, String encoding) {
        if (path == null || path.length() == 0) {
            return path;
        }
        ByteBuffer bytes;
        try {
            bytes = CharsetFactory.ISO_8859_1.newEncoder().encode(CharBuffer.wrap(path));
        } catch (CharacterCodingException ex) {
            // not latin bytes
            // Strings already parsed by the server
            return path;
        }

        // browsers will transfer URI encoding as UTF-8
        // so we try UTF-8 first, then the character encoding set in request attribute
        for (Charset charset : new Charset[]{
            CharsetFactory.UTF_8,
            CharsetFactory.getCharset(encoding, null)
        }) {
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

    public String expr(String superPath, String characterEncoding) {
        if (superPath == null || superPath.length() == 0) {
            return superPath;
        }
        String path = result;
        // some servers such as glassfish may change the path during the request
        if (path == null || !superPath.equals(expectPath)
                || (characterEncoding == null
                        ? expectEncoding != null
                        : !characterEncoding.equals(expectEncoding))) {
            path = detect(superPath, characterEncoding);
            result = path;
            expectEncoding = characterEncoding;
            expectPath = superPath;
        }
        return path;
    }
}
