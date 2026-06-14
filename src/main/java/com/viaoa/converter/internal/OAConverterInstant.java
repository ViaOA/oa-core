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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

/**
 * Converter for transforming values into {@link Instant} and formatting them
 * using OA {@link OADateTime} rules.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types and behavior:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link Instant} – returned directly</li>
 *     <li>{@link OADateTime} – uses {@link OADateTime#getInstant()}</li>
 *     <li>{@link OADate} – date interpreted using system default timezone</li>
 *     <li>{@link OATime} – combined with Epoch date (1970-01-01),
 *         interpreted using system default timezone</li>
 *     <li>{@link String} – parsed using
 *         {@link OADateTime#valueOf(String, String)}, trimmed before parsing</li>
 *     <li>{@link java.sql.Timestamp} – {@code toInstant()},
 *         preserving millisecond and nanosecond precision</li>
 *     <li>{@link java.sql.Time} – system default timezone applied</li>
 *     <li>{@link java.util.Date} – {@code toInstant()}</li>
 *     <li>{@link Number} – interpreted as epoch milliseconds</li>
 *     <li>{@code byte[]} – epoch milliseconds from first eight bytes</li>
 *     <li>{@link LocalDateTime} – system default timezone assumed</li>
 *     <li>{@link ZonedDateTime} – {@code toInstant()}, zone information preserved</li>
 *     <li>{@link LocalDate} – midnight assumed, system default timezone</li>
 *     <li>{@link LocalTime} – Epoch date (1970-01-01), system default timezone</li>
 * </ul>
 *
 * <p><strong>Timezone Strategy:</strong><br>
 * {@link Instant} does not contain timezone information. When converting
 * from types that do not contain full date+time+zone, this converter applies
 * the <strong>system default timezone</strong> to produce a stable and
 * deterministic result.</p>
 *
 * <p><strong>Precision Notes:</strong><br>
 * Epoch millisecond conversions (e.g., {@link Number}, {@code byte[]}) may
 * truncate sub-millisecond precision that might exist in {@link Instant} values.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>Converted to {@link OADateTime} and formatted using {@code fmt}</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see Instant
 * @see OADateTime
 */
public class OAConverterInstant implements OAConverterInterface<Instant> {

	/**
	 * Converts the supplied value into an {@link Instant}.
	 * <p>
	 * The conversion behavior depends on the runtime type of {@code fromValue}
	 * and may apply system default timezone rules when necessary.
	 *
	 * @param thisClass the target class, {@link Instant}
	 * @param fromValue the value to convert
	 * @param fmt the format string used when parsing string values
	 * @return the converted {@link Instant}, or {@code null} if conversion fails
	 */
    @Override
    public Instant convert(Class<Instant> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof Instant) return (Instant) fromValue;

        if (fromValue instanceof OADateTime) {
            return ((OADateTime) fromValue).toInstant();
        }

        if (fromValue instanceof OADate) {
            return new OADateTime((OADate) fromValue).toInstant();
        }

        if (fromValue instanceof OATime) {
            return new OADateTime((OATime) fromValue).toInstant();
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return OADateTime.valueOf(s, fmt).toInstant();
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof Timestamp) {
            return ((Timestamp) fromValue).toInstant();
        }

        if (fromValue instanceof java.sql.Time) {
            return new OADateTime((java.sql.Time) fromValue).toInstant();
        }

        if (fromValue instanceof java.util.Date) {
            return ((java.util.Date) fromValue).toInstant();
        }

        if (fromValue instanceof byte[]) {
            long tm = new BigInteger((byte[]) fromValue).longValue();
            return Instant.ofEpochMilli(tm);
        }

        if (fromValue instanceof Number) {
            long tm = ((Number) fromValue).longValue();
            return Instant.ofEpochMilli(tm);
        }

        if (fromValue instanceof LocalDateTime) {
            return ((LocalDateTime) fromValue)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }

        if (fromValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) fromValue).toInstant();
        }

        if (fromValue instanceof LocalDate) {
            return ((LocalDate) fromValue)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        }

        if (fromValue instanceof LocalTime) {
            return LocalDate.of(1970, 1, 1)
                    .atTime((LocalTime) fromValue)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }

        return null;
    }

    /**
     * Converts an {@link Instant} into a formatted string.
     *
     * @param fromValue the {@link Instant} value to convert
     * @param fmt the format string to use
     * @return the formatted string, or an empty string if the value is {@code null}
     */
    @Override
    public String convertToString(Instant fromValue, String fmt) {
        if (fromValue == null) return "";
        return new OADateTime(fromValue).toString(fmt);
    }
}

