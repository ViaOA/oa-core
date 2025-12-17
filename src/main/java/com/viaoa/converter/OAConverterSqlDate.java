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

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Converter for transforming values into {@link java.sql.Date} and formatting
 * {@link java.sql.Date} values into strings.
 *
 * <h3>Conversion to {@code java.sql.Date}</h3>
 * The following input types are supported:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link java.sql.Date} — returned directly</li>
 *   <li>{@link String} — parsed using {@link OADateTime#valueOf(String, String)}
 *       if {@code fmt} supplied, or default parsing rules otherwise</li>
 *   <li>{@link OADateTime} — epoch millis converted to {@code java.sql.Date}</li>
 *   <li>{@link Number} — epoch millis (long value)</li>
 *   <li>{@code byte[]} — byte array interpreted as a signed integer (epoch millis)</li>
 *   <li>{@link Instant} — converted using system default zone</li>
 *   <li>{@link LocalDate} — full precision preserved using {@link java.sql.Date#valueOf(LocalDate)}</li>
 *   <li>{@link LocalDateTime} — time portion dropped; date component preserved</li>
 *   <li>{@link ZonedDateTime} — converted to local date (time-zone retained only to find local date)</li>
 *   <li>{@link java.sql.Timestamp} — epoch millis preserved</li>
 *   <li>{@link java.util.Date} — epoch millis preserved</li>
 * </ul>
 *
 * <p><strong>Date-only semantics:</strong><br>
 * For {@link LocalDateTime} and {@link ZonedDateTime}, the time portion is
 * intentionally discarded because {@code java.sql.Date} does not represent time.</p>
 *
 * <h3>Formatting to {@code String}</h3>
 * <ul>
 *   <li>If {@code fmt} is supplied, {@link OADate#toString(String)} is used</li>
 *   <li>Null {@code fromValue} → empty string ({@code ""})</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterSqlDate conv = new OAConverterSqlDate();
 *
 * Date d = conv.convert(Date.class, "2025-10-30", "yyyy-MM-dd");
 *
 * String s = conv.convertToString(d, "MM/dd/yyyy");  // "10/30/2025"
 * }</pre>
 *
 * @see OAConverterInterface
 * @see OADateTime
 * @see OADate
 */
public class OAConverterSqlDate implements OAConverterInterface<Date> {
	
    /**
     * Converts a supplied object into a {@link java.sql.Date} instance.
     *
     * @param thisClass requested return type (always {@code Date.class})
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt optional parsing mask used for {@link String} input values
     * @return converted {@link java.sql.Date}, or {@code null} if conversion is not possible
     */	
	@Override
	public Date convert(Class<Date> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) {
			return null;
		}
		if (fromValue instanceof Date) {
			return (Date) fromValue;
		}

		if (fromValue instanceof String) {
			fromValue = ((String) fromValue).trim();
		    if (((String) fromValue).isEmpty()) return null;
			OADateTime dt = OADateTime.valueOf((String) fromValue, fmt);
			if (dt == null) {
				return null;
			}
			Date d = new Date(dt.getTime());
			return d;
		}

		if (fromValue instanceof byte[]) {
			return new Date(new java.math.BigInteger((byte[]) fromValue).longValue());
		}

		if (fromValue instanceof OADateTime) {
			Date d = new Date(((OADateTime) fromValue).getTime());
			return d;
		}

		if (fromValue instanceof Number) {
			long x = ((Number) fromValue).longValue();
			return new Date(x);
		}

		if (fromValue instanceof Instant) {
			return new Date(((Instant) fromValue).toEpochMilli());
		}

		if (fromValue instanceof LocalDate) {
			LocalDate ld = (LocalDate) fromValue;
			Date out = java.sql.Date.valueOf(ld);
			return out;
		}

		if (fromValue instanceof LocalDateTime) {
			LocalDateTime ldt = (LocalDateTime) fromValue;
			Date out = java.sql.Date.valueOf(ldt.toLocalDate());
			return out;
		}

		if (fromValue instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) fromValue;
			Date out = java.sql.Date.valueOf(zdt.toLocalDate());
			return out;
		}

		if (fromValue instanceof java.sql.Timestamp) {
		    return new Date(((java.sql.Timestamp) fromValue).getTime());
		}
		
		if (fromValue instanceof java.util.Date) { // after Timestamp branch
		    return new Date(((java.util.Date) fromValue).getTime());
		}
		
		return null;
	}

	
	/**
     * Converts a {@link java.sql.Date} into a formatted {@link String}.
     * <p>Returns {@code ""} when the input value is {@code null}.</p>
     *
     * @param fromValue the date to convert; may be {@code null}
     * @param fmt optional date format; if {@code null}, {@link OADate#toString()} determines output
     * @return formatted string, never {@code null}
     */	
	@Override
	public String convertToString(Date fromValue, String fmt) {
		if (fromValue == null) {
			return "";
		}
		OADate od = new OADate(fromValue);
		String s = od.toString(fmt);
		return (s == null ? "" : s);
	}
	
}
