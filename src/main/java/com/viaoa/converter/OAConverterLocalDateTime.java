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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Converter for transforming values into {@link LocalDateTime} and formatting
 * them for display using OA {@link OADateTime} rules.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types when converting to {@link LocalDateTime}:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link LocalDateTime} – returned as-is</li>
 *     <li>{@link OADateTime} – year-month-day-time converted directly,
 *         preserving millisecond precision via nanoseconds</li>
 *     <li>{@link OADate} – normalized to midnight on the same date
 *         (original time information is not preserved)</li>
 *     <li>{@link OATime} – combined with Epoch date (1970-01-01),
 *         as no date information is present in the source</li>
 *     <li>{@link java.sql.Timestamp} – exact conversion via {@code toLocalDateTime()}</li>
 *     <li>{@link java.sql.Time} – combined with Epoch date</li>
 *     <li>{@link LocalDate} – normalized to midnight (time information lost)</li>
 *     <li>{@link LocalTime} – combined with Epoch date</li>
 *     <li>{@link ZonedDateTime} – converted using {@code toLocalDateTime()},
 *         discarding zone and offset information</li>
 *     <li>{@link Instant} – interpreted using the <strong>system default timezone</strong></li>
 *     <li>{@link String} – parsed by {@link OADateTime#valueOf(String)}</li>
 *     <li>{@link Number} – interpreted as epoch milliseconds</li>
 *     <li>{@code byte[]} – first 8 bytes interpreted as epoch milliseconds</li>
 * </ul>
 *
 * <p><strong>Timezone Behavior:</strong><br>
 * When conversion requires timezone context (e.g., for {@link Instant}),
 * the <strong>system default timezone</strong> is always applied for consistency
 * with OA runtime expectations.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty String {@code ""}</li>
 *     <li>Converted using {@link OADateTime#toString(String)}</li>
 *     <li>{@code fmt} parameter is passed directly to the formatter</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see LocalDateTime
 * @see OADateTime
 */
public class OAConverterLocalDateTime implements OAConverterInterface<LocalDateTime> {

    private static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    
    /**
     * Converts the supplied value into a {@link LocalDateTime}.
     *
     * <p>Supported source types include:</p>
     * <ul>
     *     <li>{@code null} → {@code null}</li>
     *     <li>{@link LocalDateTime} – returned directly</li>
     *     <li>{@link com.viaoa.util.OADateTime} – full date/time, millis → nanos</li>
     *     <li>{@link com.viaoa.util.OADate} – midnight on same date</li>
     *     <li>{@link com.viaoa.util.OATime} – combined with Epoch date (1970-01-01)</li>
     *     <li>{@link java.sql.Timestamp} – converted using {@code toLocalDateTime()}</li>
     *     <li>{@link java.sql.Time} – Epoch date + {@code toLocalTime()}</li>
     *     <li>{@link LocalDate} – midnight</li>
     *     <li>{@link LocalTime} – Epoch date + time</li>
     *     <li>{@link ZonedDateTime} – {@code toLocalDateTime()}</li>
     *     <li>{@link Instant} – converted using the system default timezone</li>
     *     <li>{@link String} – parsed with {@link com.viaoa.util.OADateTime#valueOf(String)}</li>
     *     <li>{@link Number} – interpreted as epoch milliseconds</li>
     *     <li>{@code byte[]} – epoch milliseconds extracted from bytes</li>
     * </ul>
     *
     * <p><strong>Timezone behavior:</strong> If timezone interpretation is required
     * (e.g., for {@link Instant}), this converter consistently applies the
     * <strong>system default timezone</strong>.</p>
     *
     * @param thisClass expected type ({@code LocalDateTime.class})
     * @param fromValue value to convert; may be {@code null}
     * @param fmt ignored
     * @return a {@link LocalDateTime}, or {@code null} if unconvertible
     */    
    @Override
    public LocalDateTime convert(Class<LocalDateTime> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof LocalDateTime) return (LocalDateTime) fromValue;

        if (fromValue instanceof OADateTime) {
            OADateTime dt = (OADateTime) fromValue;
            int nanos = dt.getMilliSecond() * 1_000_000;
            return LocalDateTime.of(dt.getYear(), dt.getMonth() + 1, dt.getDay(),
                    dt.getHour(), dt.getMinute(), dt.getSecond(), nanos);
        }

        if (fromValue instanceof OADate) {
            OADate d = (OADate) fromValue;
            return LocalDateTime.of(d.getYear(), d.getMonth() + 1, d.getDay(), 0, 0);
        }

        if (fromValue instanceof OATime) {
            OATime t = (OATime) fromValue;
            return LocalDateTime.of(EPOCH_DATE,
                    LocalTime.of(t.getHour(), t.getMinute(), t.getSecond()));
        }

        if (fromValue instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) fromValue).toLocalDateTime();
        }

        if (fromValue instanceof java.sql.Time) {
            return LocalDateTime.of(EPOCH_DATE,
                    ((java.sql.Time) fromValue).toLocalTime());
        }

        if (fromValue instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) fromValue, ZoneId.systemDefault());
        }

        if (fromValue instanceof LocalDate) {
            return LocalDateTime.of((LocalDate) fromValue, LocalTime.MIDNIGHT);
        }

        if (fromValue instanceof LocalTime) {
            return LocalDateTime.of(EPOCH_DATE, (LocalTime) fromValue);
        }

        if (fromValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) fromValue).toLocalDateTime();
        }

        if (fromValue instanceof String) {
        	String s = ((String) fromValue).trim();
        	if (s.isEmpty()) return null;
            try {
                OADateTime dt = OADateTime.valueOf(s);
                int nanos = dt.getMilliSecond() * 1_000_000;
                return LocalDateTime.of(dt.getYear(), dt.getMonth() + 1, dt.getDay(),
                        dt.getHour(), dt.getMinute(), dt.getSecond(), nanos);
            }
            catch (Throwable e) {
                return null;
            }
        }

        if (fromValue instanceof Number) {
            long tm = ((Number) fromValue).longValue();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(tm), ZoneId.systemDefault());
        }

        if (fromValue instanceof byte[]) {
            ByteBuffer bb = ByteBuffer.wrap((byte[]) fromValue);
            long tm = bb.getLong();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(tm), ZoneId.systemDefault());
        }

        return null;
    }

    /**
     * Converts a {@link LocalDateTime} into a formatted {@link String} using OA
     * {@link com.viaoa.util.OADateTime} formatting rules.
     *
     * <ul>
     *     <li>{@code null} → empty String {@code ""}</li>
     *     <li>Value is wrapped into an {@link com.viaoa.util.OADateTime}, where
     *         conversion to {@link java.util.Date} uses the system default timezone
     *     </li>
     *     <li>{@code fmt} is passed directly to
     *         {@link com.viaoa.util.OADateTime#toString(String)}</li>
     * </ul>
     *
     * @param fromValue {@link LocalDateTime} to convert; may be {@code null}
     * @param fmt optional format string; implementation depends on {@link com.viaoa.util.OADateTime}
     * @return formatted string, never {@code null}
     */    
    @Override
    public String convertToString(LocalDateTime fromValue, String fmt) {
        if (fromValue == null) return "";
        OADateTime dt = new OADateTime(
                Date.from(fromValue.atZone(ZoneId.systemDefault()).toInstant())
        );
        return dt.toString(fmt);
    }
}
