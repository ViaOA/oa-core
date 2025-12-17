/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.converter;

import com.viaoa.util.*;


/**
 * Converter for transforming a value into a {@link Character}, and formatting
 * {@link Character} values as {@link String} text.
 *
 * <h3>Conversion to {@code Character}</h3>
 * The following input types are supported:
 * <ul>
 *   <li>{@link Character} — returned directly</li>
 *   <li>{@link String} — only if length is exactly 1 (first character used)</li>
 *   <li>{@link Boolean} — {@code true → 'T'}, {@code false → 'F'}</li>
 *   <li>{@link Number} — integer value within valid {@code char} range</li>
 * </ul>
 *
 * <p>If the input cannot be converted, {@code null} is returned.</p>
 *
 * <h3>Formatting from {@code Character}</h3>
 * <ul>
 *   <li>{@code toString()} is used to convert to a single-character string</li>
 *   <li>If {@code fmt} contains formatting/alignment rules,
 *       {@link OAStr#format(String, String)} is applied</li>
 *   <li>{@code null} → empty string ({@code ""})</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * OAConverterCharacter conv = new OAConverterCharacter();
 *
 * Character c1 = conv.convert(Character.class, "A", null); // 'A'
 * Character c2 = conv.convert(Character.class, true, null); // 'T'
 *
 * String s = conv.convertToString('Z', "R3"); // "  Z"
 * }</pre>
 *
 * @see com.viaoa.util.OAConverter
 */
public class OAConverterCharacter implements OAConverterInterface<Character> {

    /**
     * Converts the supplied {@code fromValue} into a {@link Character} instance.
     * See class-level Javadoc for supported input types and behaviors.
     *
     * @param thisClass the desired target type (always {@code Character.class})
     * @param fromValue the source value to convert; may be {@code null}
     * @param fmt       optional formatting mask (ignored for conversion)
     * @return a {@link Character} value or {@code null} if not convertible
     */
	@Override
	public Character convert(Class<Character> thisClass, Object fromValue, String fmt) {
        if (fromValue instanceof Character) {
        	return (Character) fromValue;
        }
        if (fromValue instanceof String) {
            String str = (String)fromValue;
            if (str.length() == 1) return str.charAt(0);
            return null;
        }
        
        if (fromValue instanceof Number) {
            int x = ((Number) fromValue).intValue();
            if (x >= Character.MIN_VALUE && x <= Character.MAX_VALUE) return Character.valueOf((char)x);
            return null;
        }
        if (fromValue instanceof Boolean) {
            return Character.valueOf( ((Boolean)fromValue).booleanValue() ? 'T' : 'F' );
        }
        return null;
    }

    /**
     * Converts a {@link Character} into a formatted {@link String}.
     *
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt       optional formatting/alignment mask; may be {@code null}
     * @return formatted string or an empty string if {@code fromValue} is {@code null}
     */
	@Override
	public String convertToString(Character fromValue, String fmt) {
        String s = fromValue == null ? "" : fromValue.toString();
        if (OAStr.isNotEmpty(fmt)) s = OAStr.format(s, fmt);
		return (s == null ? "" : s);
    }
}
