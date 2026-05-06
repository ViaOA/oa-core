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

import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;

/**
 * Converter for transforming values into {@link OADate} instances and
 * formatting {@link OADate} values into display-friendly {@link String}
 * representations.
 *
 * <h3>Conversion Rules</h3>
 * The following input types are supported when converting to {@code OADate}:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link OADate} — returned directly</li>
 *   <li>{@link String} — parsed using {@link OADate#valueOf(String, String)};
 *       blank values return {@code null}</li>
 *   <li>{@link Time} — converts using date component of {@link OADate}</li>
 *   <li>{@link java.sql.Date} — date preserved; time discarded</li>
 *   <li>{@link OADateTime} — date portion extracted</li>
 *   <li>{@link byte[]} — interpreted as epoch milliseconds</li>
 *   <li>{@link Number} — interpreted as epoch milliseconds</li>
 *   <li>{@link Instant} — converted using {@link OADateTime} rules</li>
 *   <li>{@link LocalDate} — preserved directly</li>
 *   <li>{@link LocalDateTime} — instant derived from system default zone</li>
 *   <li>{@link ZonedDateTime} — exact instant preserved then converted to date-only</li>
 * </ul>
 *
 * <p><strong>Date-only semantics:</strong><br>
 * {@link OADate} represents a calendar date without a time component.
 * When converting from types that include a time or time zone, the time
 * portion is intentionally discarded.</p>
 *
 * <p><strong>Input validation:</strong><br>
 * To comply with system constraints, dates where the year exceeds four
 * digits (year &gt; 9999) are rejected, returning {@code null}.</p>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 *   <li>Formatting uses {@link OADate#toString(String)}</li>
 *   <li>If {@code fromValue} is {@code null}, returns empty string ({@code ""})</li>
 *   <li>If {@code fmt} is {@code null}, OA default date formatting rules apply</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see OADate
 * @see OADateTime
 */
public class OAConverterOADate implements OAConverterInterface<OADate> {

    /**
     * Converts a supplied value into an {@link OADate}.
     *
     * <p>If {@code fmt} is provided and {@code fromValue} is a {@link String},
     * parsing is delegated to {@link OADate#valueOf(String, String)}.</p>
     *
     * @param thisClass the expected result type (always {@code OADate.class})
     * @param fromValue value to convert; may be {@code null}
     * @param fmt optional input format mask for {@link String} values
     * @return converted {@link OADate}, or {@code null} when conversion is not possible
     */
	@Override
	public OADate convert(Class<OADate> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) {
			return null;
		}
		if (fromValue instanceof OADate) {
			return (OADate) fromValue;
		}
		OADate d = null;
		if (fromValue instanceof String) {
		    String s = ((String) fromValue).trim();
		    if (s.isEmpty()) return null;
			d = (OADate) OADate.valueOf(s, fmt);
		} else if (fromValue instanceof Time) {
			d = new OADate((Time) fromValue);
		} else if (fromValue instanceof Date) {
			d = new OADate((Date) fromValue);
		} else if (fromValue instanceof OADateTime) {
			d = new OADate((OADateTime) fromValue);
		} else if (fromValue instanceof byte[]) {
			d = new OADate(new BigInteger((byte[]) fromValue).longValue());
		} else if (fromValue instanceof Number) {
			d = new OADate(((Number) fromValue).longValue());
		} else if (fromValue instanceof Instant) {
			d = new OADate(new OADateTime((Instant) fromValue));
		}
		else if (fromValue instanceof LocalDate) {
			d = new OADate((LocalDate) fromValue);
		}
		else if (fromValue instanceof LocalDateTime) {
			d =new OADate(new OADateTime((LocalDateTime) fromValue));
		}
		else if (fromValue instanceof ZonedDateTime) {
			d = new OADate(new OADateTime((ZonedDateTime) fromValue));
		}
		return d;
	}

    /**
     * Converts an {@link OADate} value into a formatted {@link String}.
     *
     * @param fromValue the date to convert; may be {@code null}
     * @param fmt optional format mask; if {@code null}, OA default formatting applies
     * @return formatted text, or {@code ""} when {@code fromValue} is null
     */
	@Override
	public String convertToString(OADate fromValue, String fmt) {
		if (fromValue == null) return "";
		return (fromValue).toString(fmt);
	}
}
