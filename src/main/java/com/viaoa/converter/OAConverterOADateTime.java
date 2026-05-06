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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

/**
 * Converter for transforming values into {@link OADateTime} instances and
 * formatting them into display-friendly {@link String} values.
 *
 * <h3>Conversion Rules</h3>
 * The following input types are supported when converting to {@code OADateTime}:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link OADateTime} — returned directly</li>
 *   <li>{@link OADate} — date-only converted to date/time container</li>
 *   <li>{@link OATime} — time-only converted to date/time container</li>
 *   <li>{@link String} — parsed via {@link OADateTime#valueOf(String, String)}</li>
 *   <li>{@link java.sql.Time} — time preserved; date inferred by {@link OADateTime}</li>
 *   <li>{@link java.sql.Date} — date preserved; time set to 00:00 in system zone</li>
 *   <li>{@link byte[]} — interpreted as epoch milliseconds</li>
 *   <li>{@link Number} — epoch milliseconds preserved</li>
 *   <li>{@link Instant} — retained as instant on UTC timeline</li>
 *   <li>{@link LocalDate} — interpreted as start-of-day in system default zone</li>
 *   <li>{@link LocalDateTime} — interpreted in system default zone</li>
 *   <li>{@link ZonedDateTime} — exact instant preserved</li>
 * </ul>
 *
 * <p><strong>Semantics:</strong><br>
 * {@link OADateTime} represents a full date and time value. When converting
 * from date-only or time-only types, missing components are filled using OA
 * conventions (e.g., start-of-day for date-only).</p>
 *
 * <h3>Formatting Behavior</h3>
 * <ul>
 *   <li>Formatting uses {@link OADateTime#toString(String)}</li>
 *   <li>If {@code fromValue} is {@code null}, returns {@code ""}</li>
 *   <li>If {@code fmt} is {@code null}, OA default formatting rules apply</li>
 * </ul>
 */
public class OAConverterOADateTime implements OAConverterInterface<OADateTime> {

    /**
     * Converts a source value into an {@link OADateTime}.
     *
     * <p>If {@code fmt} is provided and {@code fromValue} is a {@link String},
     * parsing is delegated to {@link OADateTime#valueOf(String, String)}.</p>
     *
     * @param thisClass expected return type (always {@code OADateTime.class})
     * @param fromValue value to convert; may be {@code null}
     * @param fmt optional parsing mask for {@link String} input values
     * @return converted {@link OADateTime} instance, or {@code null}
     *         when conversion is not possible
     */	
	@Override
	public OADateTime convert(Class<OADateTime> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) {
			return null;
		}
		if (fromValue instanceof OADateTime) {
			return (OADateTime) fromValue;
		}
		if (fromValue instanceof OADate) {
			return new OADateTime((OADate) fromValue);
		}
		if (fromValue instanceof OATime) {
			return new OADateTime((OATime) fromValue);
		}
		if (fromValue instanceof String) {
		    String s = ((String) fromValue).trim();
		    if (s.isEmpty()) return null;
			return OADateTime.valueOf(s, fmt);
		}
		if (fromValue instanceof java.sql.Time) {
			return new OADateTime((java.sql.Time) fromValue);
		}
		if (fromValue instanceof java.sql.Timestamp) {
			return new OADateTime((java.sql.Timestamp) fromValue);
		}
		if (fromValue instanceof java.sql.Date) {
			return new OADateTime((java.sql.Date) fromValue);
		}
		if (fromValue instanceof java.util.Date) {
			return new OADateTime((java.util.Date) fromValue);
		}
		if (fromValue instanceof byte[]) {
			return new OADateTime(new java.math.BigInteger((byte[]) fromValue).longValue());
		}
		if (fromValue instanceof Number) {
			return new OADateTime(((Number) fromValue).longValue());
		}

		if (fromValue instanceof Instant) {
			OADateTime out = new OADateTime((Instant) fromValue);
			return out;
		}

		if (fromValue instanceof LocalDate) {
			OADateTime dt = new OADate((LocalDate) fromValue);
			return dt;
		}

		if (fromValue instanceof LocalDateTime) {
			OADateTime out = new OADateTime((LocalDateTime) fromValue);
			return out;
		}

		if (fromValue instanceof ZonedDateTime) {
			OADateTime out = new OADateTime((ZonedDateTime) fromValue);
			return out;
		}

		return null;
	}

    /**
     * Formats an {@link OADateTime} into a {@link String}.
     *
     * @param fromValue the date/time to convert; may be {@code null}
     * @param fmt optional formatting mask; if {@code null}, OA defaults apply
     * @return formatted value (never {@code null}); empty when {@code fromValue} is null
     */
	@Override
	public String convertToString(OADateTime fromValue, String fmt) {
		if (fromValue == null) return "";
		return fromValue.toString(fmt);
	}
}
