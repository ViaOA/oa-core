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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.viaoa.datetime.OADateTime;

/**
 * Converter for transforming values into {@link java.sql.Timestamp} and
 * producing formatted {@link String} values from timestamp instances.
 *
 * <h3>Conversion Behavior</h3>
 * The following input types are supported:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link Timestamp} — returned directly</li>
 *   <li>{@link String} — parsed using {@link OADateTime#valueOf(String, String)};
 *       if blank, returns {@code null}</li>
 *   <li>{@link OADateTime} — epoch milliseconds preserved</li>
 *   <li>{@link java.util.Date} — epoch milliseconds preserved</li>
 *   <li>{@link Instant} — preserved as absolute point in time</li>
 *   <li>{@link LocalDate} — interpreted as start-of-day in the system default zone</li>
 *   <li>{@link LocalDateTime} — interpreted in the system default zone</li>
 *   <li>{@link ZonedDateTime} — exact instant preserved</li>
 *   <li>{@code byte[]} — interpreted as a long value containing epoch milliseconds</li>
 *   <li>{@link Number} — interpreted as epoch milliseconds</li>
 * </ul>
 *
 * <p><strong>Zone and Epoch Notes:</strong><br>
 * {@link java.sql.Timestamp} stores a precise moment on the UTC timeline.
 * When converting from {@code LocalDate}/{@code LocalDateTime}, the value is
 * evaluated using {@link ZoneId#systemDefault()}. Applications running across
 * multiple regions should ensure this matches the expected business rules.</p>
 *
 * <h3>Formatting Behavior</h3>
 * <ul>
 *   <li>If {@code fmt} is supplied, formatting is performed via {@link OADateTime#toString(String)}</li>
 *   <li>If value is {@code null}, returns {@code ""} for UI-safe output</li>
 *   <li>Formatting defaults are governed by global OA date/time settings</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterTimestamp conv = new OAConverterTimestamp();
 *
 * Timestamp ts = conv.convert(Timestamp.class, "2025-10-28 14:20", "yyyy-MM-dd HH:mm");
 * String s = conv.convertToString(ts, "MM/dd/yyyy HH:mm:ss");
 * }</pre>
 *
 * @see OAConverterInterface
 * @see OADateTime
 * @see Timestamp
 */
public class OAConverterTimestamp implements OAConverterInterface<Timestamp> {
	

	/**
	 * Converter for transforming values into {@link java.sql.Timestamp} and
	 * producing formatted {@link String} values from timestamp instances.
	 *
	 * <h3>Conversion Behavior</h3>
	 * The following input types are supported:
	 * <ul>
	 *   <li>{@code null} → {@code null}</li>
	 *   <li>{@link Timestamp} — returned directly</li>
	 *   <li>{@link String} — parsed using {@link OADateTime#valueOf(String, String)};
	 *       if blank, returns {@code null}</li>
	 *   <li>{@link OADateTime} — converted using
	 *       {@link OADateTime#toTimestamp()}</li>
	 *   <li>{@link java.util.Date} — epoch milliseconds preserved</li>
	 *   <li>{@link Instant} — preserved as an exact instant</li>
	 *   <li>{@link LocalDate} — interpreted as start-of-day in the system default zone</li>
	 *   <li>{@link LocalDateTime} — interpreted in the system default zone</li>
	 *   <li>{@link ZonedDateTime} — exact instant preserved</li>
	 *   <li>{@code byte[]} — interpreted as a long value containing epoch milliseconds</li>
	 *   <li>{@link Number} — interpreted as epoch milliseconds</li>
	 * </ul>
	 *
	 * <p><strong>OADateTime Conversion Notes:</strong><br>
	 * {@link OADateTime} owns all date/time semantic rules.
	 * Conversion to {@link Timestamp} is delegated to
	 * {@link OADateTime#toTimestamp()}.
	 * </p>
	 *
	 * <ul>
	 *   <li>{@code Instant} values preserve the represented instant.</li>
	 *   <li>{@code ZonedInstant} values preserve the represented instant.</li>
	 *   <li>{@code Floating} values are not semantically compatible with
	 *       {@link Timestamp}, because {@code Timestamp} represents an instant.
	 *       Floating values therefore follow the conversion rules defined by
	 *       {@code OADateTime.toTimestamp()}.</li>
	 * </ul>
	 *
	 * <p><strong>Zone and Epoch Notes:</strong><br>
	 * {@link java.sql.Timestamp} represents an instant on the UTC timeline.
	 * When converting from {@link LocalDate} or {@link LocalDateTime},
	 * the value is interpreted using {@link ZoneId#systemDefault()} unless
	 * the source type explicitly supplies zone information.
	 * </p>
	 *
	 * <h3>Formatting Behavior</h3>
	 * <ul>
	 *   <li>If {@code fmt} is supplied, formatting is performed via
	 *       {@link OADateTime#toString(String)}</li>
	 *   <li>If value is {@code null}, returns {@code ""} for UI-safe output</li>
	 *   <li>Formatting defaults are governed by global OA date/time settings</li>
	 * </ul>
	 */	
	@Override
	public Timestamp convert(Class<Timestamp> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) {
			return null;
		}
		if (fromValue instanceof Timestamp) {
			return (Timestamp) fromValue;
		}

		if (fromValue instanceof String) {
			String s = ((String) fromValue).trim();
			if (s.isEmpty()) return null;
			fromValue = OADateTime.valueOf((String) fromValue, fmt);
		}

		if (fromValue instanceof OADateTime) {
			OADateTime dt = (OADateTime) fromValue;
			return dt.toTimestamp();
		}

		if (fromValue instanceof java.util.Date) {
			return new Timestamp(((java.util.Date) fromValue).getTime());
		}

		if (fromValue instanceof byte[]) {
			return new Timestamp(new java.math.BigInteger((byte[]) fromValue).longValue());
		}

		if (fromValue instanceof Instant) {
			Timestamp out = Timestamp.from((Instant) fromValue);
			return out;
		}

		if (fromValue instanceof LocalDate) {
			LocalDate ld = (LocalDate) fromValue;
			Instant inst = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
			return Timestamp.from(inst);
		}

		if (fromValue instanceof LocalDateTime) {
			LocalDateTime ldt = (LocalDateTime) fromValue;
			Timestamp out = Timestamp.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			return out;
		}

		if (fromValue instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) fromValue;
			Timestamp out = Timestamp.from(zdt.toInstant());
			return out;
		}

		if (fromValue instanceof Number) {
		    return new Timestamp(((Number) fromValue).longValue());
		}		
		
		return null;
	}

    /**
     * Converts a {@link Timestamp} to a formatted {@link String}.
     * <p>
     * If {@code fmt} is {@code null}, output format is determined by global OA
     * date/time settings through {@link OADateTime#toString(String)}.
     * </p>
     *
     * @param fromValue the timestamp to convert; may be {@code null}
     * @param fmt optional format mask; if {@code null}, the OA default is used
     * @return formatted timestamp string, or {@code ""} when {@code fromValue} is null
     */	
	@Override
	public String convertToString(Timestamp fromValue, String fmt) {
		if (fromValue == null) {
			return "";
		}
		OADateTime od = new OADateTime(fromValue);
		String s = od.toString(fmt);
		return (s == null ? "" : s);
	}
}
