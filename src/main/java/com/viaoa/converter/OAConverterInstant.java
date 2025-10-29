/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

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

    @Override
    public Instant convert(Class<Instant> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof Instant) return (Instant) fromValue;

        if (fromValue instanceof OADateTime) {
            return ((OADateTime) fromValue).getInstant();
        }

        if (fromValue instanceof OADate) {
            return new OADateTime((OADate) fromValue).getInstant();
        }

        if (fromValue instanceof OATime) {
            return new OADateTime((OATime) fromValue).getInstant();
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return OADateTime.valueOf(s, fmt).getInstant();
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof Timestamp) {
            return ((Timestamp) fromValue).toInstant();
        }

        if (fromValue instanceof java.sql.Time) {
            return new OADateTime((java.sql.Time) fromValue).getInstant();
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

    @Override
    public String convertToString(Instant fromValue, String fmt) {
        if (fromValue == null) return "";
        return new OADateTime(fromValue).toString(fmt);
    }
}

