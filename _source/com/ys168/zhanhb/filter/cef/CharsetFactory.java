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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author zhanhb
 */
final class CharsetFactory {

    private static final Map<String, Charset> cache = Collections.synchronizedMap(new WeakHashMap<String, Charset>());
    public static final Charset ISO_8859_1 = lookup("ISO-8859-1");
    public static final Charset UTF_8 = lookup("UTF-8");

    public static Charset getCharset(String enc, Charset defaultValue) {
        Charset charset = lookup(enc);
        return charset != null ? charset : defaultValue;
    }

    // we don't want to serve too much memory.
    private static Charset lookup(String enc) {
        Charset lookup = cache.get(enc);
        if (lookup != null) {
            return lookup;
        }
        try {
            if (enc != null && Charset.isSupported(enc)) {
                Charset charset = Charset.forName(enc);
                cache.put(enc, charset);
                return charset;
            }
        } catch (IllegalArgumentException ex) {
            // illegal charset name
            // unsupport charset
        }
        return null;
    }

    public static CharsetDecoder newDecoder(Charset charset) {
        return charset.newDecoder();
    }

    public static CharsetEncoder newEncoder(Charset charset) {
        return charset.newEncoder();
    }

    public static ByteBuffer encode(Charset charset, String str) {
        try {
            return newEncoder(charset)
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .encode(CharBuffer.wrap(str));
        } catch (CharacterCodingException ex) {
            throw new Error(ex);
        }
    }

    public static CharBuffer decode(Charset charset, ByteBuffer byteBuffer) {
        try {
            return newDecoder(charset)
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .decode(byteBuffer);
        } catch (CharacterCodingException ex) {
            throw new Error(ex);
        }
    }

    private CharsetFactory() {
    }
}
