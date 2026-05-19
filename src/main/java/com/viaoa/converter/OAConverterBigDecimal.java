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

import java.math.BigDecimal;
import java.math.BigInteger;


/*qqqqqqqqqq
CODEX

#7 — OAConverterBigDecimal.convert(...)

  File/class/method: src/main/java/com/viaoa/converter/OAConverterBigDecimal.java, convert

  Concrete bug: string-to-BigDecimal conversion can silently lose precision because it delegates through
  OAConverterNumber.convert(Number.class, ...), which parses with DecimalFormat as a generic Number. Large decimal
  strings can become Double before being wrapped with BigDecimal.valueOf(n.doubleValue()).

  Runtime scenario: converting a datasource/UI/import value like "12345678901234567890.12" to BigDecimal.class can
  produce a rounded decimal instead of the exact value represented by the string.

  Why this violates OA/OG converter semantics: BigDecimal conversion is usually used for money, quantities, keys, and
  persisted values. Silent precision loss corrupts OA property values while appearing successful.

  Minimal fix direction: when target is BigDecimal, parse string input with DecimalFormat.setParseBigDecimal(true) or
  a direct cleaned-string BigDecimal path. Avoid the intermediate Double.



*/

/**
 * Converter for creating and formatting {@link java.math.BigDecimal} values.
 * <p>
 * This class extends {@link OAConverterNumber} to provide consistent numeric
 * parsing and formatting, with BigDecimal as the primary target type.
 *
 * <h3>Conversion Behavior</h3>
 * The {@link #convert(Class, Object, String)} method accepts multiple source
 * types (Strings, Numbers, Booleans, byte[], etc.) and produces a
 * {@link java.math.BigDecimal} when possible by delegating to
 * {@link OAConverterNumber}.
 *
 * <h3>Formatting Behavior</h3>
 * The {@link #convertToString(java.math.BigDecimal, String)} method supports:
 * <ul>
 *   <li>Standard {@link java.text.DecimalFormat} patterns 
 *       (e.g. {@code "#,##0.00"})</li>
 *   <li>Alignment masks via {@link OAStr#format(String, String)}</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterBigDecimal conv = new OAConverterBigDecimal();
 *
 * BigDecimal d1 = conv.convert(BigDecimal.class, "1,234.50", "#,##0.00");
 * // d1 = 1234.50
 *
 * String s = conv.convertToString(new BigDecimal("99.9"), "#,##0.00");
 * // s = "99.90"
 * }</pre>
 *
 * @see OAConverterNumber
 * @see java.math.BigDecimal
 * @see OAStr#format(String, String)
 */
public class OAConverterBigDecimal implements OAConverterInterface<BigDecimal> {

	private final OAConverterNumber numConv = new OAConverterNumber();
	
    /**
     * Converts a value to a {@link java.math.BigDecimal} using formatting rules
     * defined by {@link OAConverterNumber}. Null input produces zero.
     *
     * @param clazz expected type (always {@code BigDecimal.class})
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt   optional {@link java.text.DecimalFormat} pattern or alignment mask
     * @return a {@link java.math.BigDecimal} value or {@code null} if unsupported input
     */
    @Override
    public BigDecimal convert(Class<BigDecimal> clazz, Object fromValue, String fmt) {
        if (fromValue == null) return BigDecimal.ZERO;
        if (fromValue instanceof BigDecimal) return (BigDecimal) fromValue;

        // Delegate to Number converter once
        Number n = numConv.convert(Number.class, fromValue, fmt);
        if (n == null) return null;

        if (n instanceof BigDecimal) return (BigDecimal) n;
        if (n instanceof BigInteger) return new BigDecimal((BigInteger) n);
        if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
            return BigDecimal.valueOf(n.longValue());
        }
        // Float/Double path: use valueOf(double) (string-based) to avoid binary artifacts
        return BigDecimal.valueOf(n.doubleValue());
    }
    
    /**
     * Formats a {@link java.math.BigDecimal} into a {@link String}. Alignment
     * masks and numeric patterns are supported as described in
     * {@link OAConverterNumber}.
     *
     * @param value the decimal value; may be {@code null}
     * @param fmt   optional formatting/alignment mask
     * @return formatted string, never {@code null}
     */
    @Override
    public String convertToString(BigDecimal fromValue, String fmt) {
    	return numConv.convertToString(fromValue, fmt);
    }
}

