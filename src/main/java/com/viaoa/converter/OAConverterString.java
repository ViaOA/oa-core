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

import java.nio.charset.StandardCharsets;
import java.sql.*;

import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;


/*qqqqqqqqqqq
CODEX

#12 — OAConverterString.convert(...)

  File/class/method: src/main/java/com/viaoa/converter/OAConverterString.java, convert

  Concrete bug: Blob bytes are converted with the platform default charset, while raw byte[] uses UTF-8.

  Runtime scenario: OA stores or receives UTF-8 text bytes in a Blob; conversion output depends on the JVM/platform
  default charset instead of OA’s explicit UTF-8 byte convention.

  Why this violates OA/OG converter semantics: string conversion should be deterministic across servers, clients,
  datasource drivers, and replication nodes. Platform-dependent text decoding can silently produce wrong text.

  Minimal fix direction: decode Blob bytes with StandardCharsets.UTF_8, or define and document a specific Blob text
  charset contract.


*/

/**
 * Converter used to convert various object types into {@link String} values,
 * including handling of {@code null} and SQL large-object types.
 * <p>
 * Behavior for null values:
 * <ul>
 *   <li>If {@code fmt} is null or empty → returns an empty string {@code ""}</li>
 *   <li>If {@code fmt} contains semicolon-separated formatting rules → the
 *       third field (if present) is treated as the null replacement value</li>
 * </ul>
 *
 * <p>Formatting Behavior:</p>
 * If the input is already a string and a non-empty format mask is provided,
 * {@link OAString#fmt(String, String)} is used to apply formatting rules.
 *
 * <p>Support Matrix:</p>
 * <ul>
 *   <li>{@code String} — returned directly (with optional formatting)</li>
 *   <li>{@code Blob} — extracted to string</li>
 *   <li>{@code Clob} — extracted to string</li>
 *   <li>{@code byte[]} — converted via UTF-8 encoding</li>
 *   <li>{@code char[]} — converted directly</li>
 *   <li>Other types — delegated to associated converter or {@code toString()}</li>
 * </ul>
 *
 * @see com.viaoa.converter.OAConverter
 * @see OAString
 */
public class OAConverterString implements OAConverterInterface<String> {


	/**
     * Converts the supplied value into a {@link String}.
     *
     * @param clazz     target class, always {@code String.class}
     * @param value     the value to convert; if {@code null} behavior
     *                  depends on {@code fmt}
     * @param fmt       optional formatting rules; may be {@code null}
     * @return a non-null string result representing {@code value}
     */	
	@Override
    public String convert(final Class<String> clazz, Object value, final String fmt) {
		
        if (value == null) {
            if (fmt == null || fmt.length() == 0) return "";
            value = OAString.field(fmt,";",3);
            if (value != null) return (String) value;
            value = "";
        }
        
        if (value instanceof String) {
            if (fmt != null && fmt.length() > 0) value = OAString.fmt((String)value, fmt);
            return (String) value;
        }
        
        
        // Attempt indirect conversion using another converter
        OAConverterInterface conv = OAConverter.getConverter(value.getClass());
        if (conv != null) { 
            Object obj = conv.convertToString(value, fmt);
            if (obj instanceof String) return (String) obj;
        }
        
        // other possibilities not covered by other OAConverters
        if (value instanceof Blob) {
        	try {
        		Blob blob = (Blob) value;
        		return new String(blob.getBytes(1, (int) blob.length()));
        	}
        	catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }        
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        if (value instanceof char[]) return new String((char[]) value);

        if (value instanceof Clob) {
        	try {
        		Clob clob = (Clob) value;
        		return clob.getSubString(1, (int) clob.length());
        	}
        	catch (Exception e) {
        		throw new RuntimeException(e);
        	}
        }
        value = value.toString();
        if (fmt != null) value = OAString.fmt((String)value, fmt);
        return (String) value;
    }


	/**
	 * Applies optional formatting to an existing {@link String} value.
	 *
	 * @param fromValue the string value to format
	 * @param fmt optional formatting rules
	 * @return the formatted string, or an empty string if the result is {@code null}
	 */
	@Override
	public String convertToString(String fromValue, String fmt) {
		String s = fromValue;
		if (OAStr.isNotEmpty(fmt)) s = OAStr.format(s, fmt);
		return (s == null ? "" : s);
	}
}



