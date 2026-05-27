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
package com.viaoa.converter.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;

/*qqqqqqqqqqq
CODEX

#13 — OAConverterDate.convertToString(...)

  File/class/method: src/main/java/com/viaoa/converter/OAConverterDate.java, convertToString

  Concrete bug: formatting a java.util.Date converts it through new OADate(fromValue), which clears the time portion
  before formatting.

  Runtime scenario: OAConverter.toString(date, "yyyy-MM-dd HH:mm:ss") where date contains 2026-05-18 14:30:45 can
  render as midnight for that date instead of preserving 14:30:45.

  Why this violates OA/OG converter semantics: java.util.Date represents an instant/date-time value, not a date-only
  value. If the caller supplies a time-bearing format, silently clearing time produces wrong UI/report/template/
  serialization output.

  Minimal fix direction: use OADateTime for java.util.Date formatting, or only use OADate when the source type is
  semantically date-only.

  Suggested CODEX comment location: directly above OADate od = new OADate(fromValue); in
  OAConverterDate.convertToString.


*/

/**
 * Converter for transforming values into {@link java.util.Date} instances and
 * formatting {@link java.util.Date} into display-friendly {@link String} values.
 *
 * <h3>Conversion Rules</h3>
 * The following input types are supported when converting to {@code java.util.Date}:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link Date} — returned directly</li>
 *   <li>{@link String} — parsed using {@link OADateTime#valueOf(String, String)};
 *       if blank, returns {@code null}</li>
 *   <li>{@link byte[]} — interpreted as epoch milliseconds</li>
 *   <li>{@link OADateTime} — converted using underlying epoch milliseconds</li>
 *   <li>{@link Number} — epoch milliseconds preserved</li>
 *   <li>{@link Instant} — converted using {@link Date#from(Instant)}</li>
 *   <li>{@link LocalDate} — interpreted at system-default zone start-of-day</li>
 *   <li>{@link LocalDateTime} — system-default zone used to derive instant</li>
 *   <li>{@link ZonedDateTime} — exact instant preserved</li>
 * </ul>
 *
 * <p><strong>Date and Time Semantics:</strong><br>
 * {@link Date} represents a precise moment on the UTC timeline. When converting
 * from a date-only type such as {@link LocalDate}, the system default time zone
 * determines the instant that reflects start-of-day for that date.</p>
 *
 * <h3>Formatting Behavior</h3>
 * <ul>
 *   <li>Formatting is delegated to {@link OADate#toString(String)}</li>
 *   <li>If {@code fromValue} is {@code null}, this method returns {@code ""}</li>
 *   <li>When {@code fmt} is {@code null}, format is determined by OA defaults</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterDate conv = new OAConverterDate();
 *
 * Date d = conv.convert(Date.class, "2025-10-30", "yyyy-MM-dd");
 * String s = conv.convertToString(d, "MM/dd/yyyy");  // "10/30/2025"
 * }</pre>
 *
 * @see OAConverterInterface
 * @see OADateTime
 * @see OADate
 */
public class OAConverterDate implements OAConverterInterface<Date> {

	
	
    /**
     * Converts a supplied value into a {@link java.util.Date}.
     *
     * <p>If {@code fmt} is provided and {@code fromValue} is a {@link String},
     * parsing is delegated to {@link OADateTime#valueOf(String, String)}.</p>
     *
     * @param thisClass the expected result type (always {@code Date.class})
     * @param fromValue the source value to convert; may be {@code null}
     * @param fmt optional format mask when parsing {@link String} values
     * @return a {@link Date} instance representing the input, or {@code null}
     *         when conversion is not possible
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
		    String s = ((String) fromValue).trim();
		    if (s.isEmpty()) return null;
		    OADateTime dt = OADateTime.valueOf(s, fmt);
		    return (dt == null) ? null : new Date(dt.getTime());
		}		

		if (fromValue instanceof byte[]) {
			return new java.util.Date(new java.math.BigInteger((byte[]) fromValue).longValue());
		}

		if (fromValue instanceof OADateTime) {
			return ((OADateTime) fromValue).getDate();
		}

		if (fromValue instanceof Number) {
			long x = ((Number) fromValue).longValue();
			return new Date(x);
		}

		if (fromValue instanceof Instant) {
			Date out = Date.from((Instant) fromValue);
			return out;
		}

		if (fromValue instanceof LocalDate) {
			LocalDate ld = (LocalDate) fromValue;
			return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
		}

		if (fromValue instanceof LocalDateTime) {
			LocalDateTime ldt = (LocalDateTime) fromValue;
			Date out = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			return out;
		}

		if (fromValue instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) fromValue;
			Date out = Date.from(zdt.toInstant());
			return out;
		}

		return null;
	}

    /**
     * Converts a {@link java.util.Date} to a formatted {@link String}.
     *
     * @param fromValue the {@code Date} to convert; may be {@code null}
     * @param fmt optional date formatting mask; if {@code null}, the OA default
     *            date formatting rules are applied
     * @return formatted date text, or {@code ""} when {@code fromValue} is null
     */
	@Override
	public String convertToString(Date fromValue, String fmt) {
		if (fromValue == null) return "";
		OADate od = new OADate(fromValue);
		String s = od.toString(fmt);
		return (s == null ? "" : s);
	}



}
