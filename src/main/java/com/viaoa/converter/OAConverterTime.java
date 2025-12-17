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

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Converter for transforming values into {@link java.sql.Time} and formatting
 * {@link java.sql.Time} instances into {@link String} values.
 *
 * <h3>Conversion Behavior</h3>
 * The following input types are supported:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link java.sql.Time} — returned directly</li>
 *   <li>{@link String} — parsed using {@link OADateTime#valueOf(String, String)};
 *       if empty or blank, returns {@code null}</li>
 *   <li>{@link OADateTime} — epoch millis preserved</li>
 *   <li>{@link java.util.Date} — epoch millis preserved</li>
 *   <li>{@link LocalTime} — time preserved; date component set to current day</li>
 *   <li>{@link LocalDateTime} — time preserved via system default zone</li>
 *   <li>{@code byte[]} — interpreted as epoch milliseconds (BigInteger)</li>
 * </ul>
 *
 * <p><strong>Time-only Semantics:</strong><br>
 * {@link java.sql.Time} represents only a time-of-day value. Any date component
 * present in the input is ignored except for the purpose of epoch-time conversion.</p>
 *
 * <h3>Formatting Behavior</h3>
 * <ul>
 *   <li>If {@code fmt} is supplied, formatting is delegated to
 *       {@link OATime#toString(String)}</li>
 *   <li>If {@code fromValue} is {@code null}, returns empty string ({@code ""})</li>
 *   <li>Formatting is UI-safe: never returns {@code null}</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterTime conv = new OAConverterTime();
 *
 * Time t = conv.convert(Time.class, "16:30", "HH:mm");
 * // t represents 4:30 PM
 *
 * String s = conv.convertToString(t, "hh:mm a");
 * // "04:30 PM"
 * }</pre>
 *
 * @see OAConverterInterface
 * @see OATime
 * @see OADateTime
 * @see java.sql.Time
 */
public class OAConverterTime implements OAConverterInterface<Time> {

    /**
     * Converts a supplied object into a {@link java.sql.Time} instance.
     *
     * @param thisClass the expected result type (always {@code Time.class})
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt optional parsing mask used for {@link String} input values
     * @return a new {@link java.sql.Time} instance representing the extracted
     *         time-of-day, or {@code null} if conversion is not possible
     */
	@Override
	public Time convert(Class<Time> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) {
			return null;
		}
		if (fromValue instanceof Time) {
			return (Time) fromValue;
		}

		if (fromValue instanceof String) {
			String s = ((String) fromValue).trim();
			if (s.isEmpty()) return null;
			fromValue = OADateTime.valueOf((String) fromValue, fmt);
		}

		if (fromValue instanceof OADateTime) {
			return new Time(((OADateTime) fromValue).getTime());
		}

		if (fromValue instanceof java.util.Date) {
			return new Time(((java.util.Date) fromValue).getTime());
		}
		if (fromValue instanceof byte[]) {
			return new Time(new java.math.BigInteger((byte[]) fromValue).longValue());
		}

		if (fromValue instanceof LocalTime) {
		    LocalTime lt = (LocalTime) fromValue;
		    return Time.valueOf(lt);
		}

		if (fromValue instanceof LocalDateTime) {
		    LocalDateTime ldt = (LocalDateTime) fromValue;
		    return Time.valueOf(ldt.toLocalTime());
		}
		
		if (fromValue instanceof Number) {
		    return new Time(((Number) fromValue).longValue());
		}		
		
		return null;
	}

    /**
     * Converts a {@link java.sql.Time} value into a formatted {@link String}.
     * <p>Returns an empty string ({@code ""}) when {@code fromValue} is {@code null}.</p>
     *
     * @param fromValue the time value to convert; may be {@code null}
     * @param fmt optional time format; if {@code null}, {@link OATime#toString()} determines output
     * @return formatted string, never {@code null}
     */
	@Override
	public String convertToString(Time fromValue, String fmt) {
		if (fromValue == null) {
			return "";
		}
		OATime t = new OATime(fromValue);
		String s = t.toString(fmt);
		return (s == null ? "" : s);
	}

}

