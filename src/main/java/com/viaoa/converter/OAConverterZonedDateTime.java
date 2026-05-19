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
import java.time.*;
import java.util.Date;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

/*qqqqqqqqqqqqqqqqqq
CODEX

#5 — OAConverterZonedDateTime.convertToString(...)

  Concrete bug: formatting discards the original ZoneId/offset.

  Runtime scenario: a ZonedDateTime in America/New_York is converted through Date.from(fromValue.toInstant()) into
  OADateTime, then formatted using the OADateTime timezone/default behavior instead of the source zone.

  Why this violates converter semantics: ZonedDateTime includes zone semantics. Formatting it through an instant-only
  conversion can silently output a different local date/time on systems with different default timezones.

  Minimal fix direction: format the source ZonedDateTime directly, or preserve its zone when constructing the OA date/
  time representation.



*/


/**
 * Converter for transforming values into {@link ZonedDateTime} and formatting
 * them using OA {@link OADateTime} rules.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types include:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link ZonedDateTime} – returned directly</li>
 *     <li>{@link OADateTime} – full date, time, and timezone preserved</li>
 *     <li>{@link OADate} – converted using midnight in the date's timezone</li>
 *     <li>{@link OATime} – combined with Epoch date (1970-01-01),
 *         preserving its configured timezone</li>
 *     <li>{@link String} – parsed using {@link OADateTime#valueOf(String, String)},
 *         trimmed before parsing</li>
 *     <li>{@link java.sql.Time} – defaults to system timezone</li>
 *     <li>{@link java.sql.Date} – interpreted via {@link OADateTime},
 *         timezone from system default</li>
 *     <li>{@link Number} – interpreted as epoch milliseconds</li>
 *     <li>{@code byte[]} – interpreted as epoch milliseconds</li>
 *     <li>{@link Instant} – converted using the <strong>system default timezone</strong></li>
 *     <li>{@link LocalDateTime} – zone assumed to be system default</li>
 *     <li>{@link LocalDate} – midnight, system default timezone</li>
 *     <li>{@link LocalTime} – Epoch date (1970-01-01), system default timezone</li>
 * </ul>
 *
 * <p><strong>Timezone Strategy:</strong><br>
 * If a source type does not supply its own timezone, or it cannot be
 * determined reliably, the <strong>system default timezone</strong> is applied
 * to maintain consistent behavior across the OA platform.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>Formatted using {@link OADateTime#toString(String)}</li>
 *     <li>{@code fmt} is passed directly to the formatter</li>
 * </ul>
 *
 * <p><strong>Developer Guidance:</strong><br>
 * Round-trip conversions between date-only or time-only types (e.g.,
 * {@link LocalDate}, {@link LocalTime}, {@link java.sql.Time}, {@link Instant})
 * will lose some original information by design, since those types do not
 * contain complete date+time+zone fields.</p>
 *
 * @see OAConverterInterface
 * @see ZonedDateTime
 * @see OADateTime
 */
public class OAConverterZonedDateTime implements OAConverterInterface<ZonedDateTime> {

	/**
	 * Constant representing the epoch date (1970-01-01) used when only
	 * time-of-day values are available.
	 */
    private static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    /**
     * Converts the supplied value into a {@link ZonedDateTime}.
     * <p>
     * The conversion behavior depends on the runtime type of {@code fromValue}
     * and applies the system default timezone when no explicit timezone
     * information is available.
     *
     * @param thisClass the target class, {@link ZonedDateTime}
     * @param fromValue the value to convert
     * @param fmt the format string used when parsing string values
     * @return the converted {@link ZonedDateTime}, or {@code null} if conversion fails
     */
    @Override
    public ZonedDateTime convert(Class<ZonedDateTime> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof ZonedDateTime) return (ZonedDateTime) fromValue;

        if (fromValue instanceof OADateTime) {
            return toZDT((OADateTime) fromValue);
        }

        if (fromValue instanceof OADate) {
            OADate d = (OADate) fromValue;
            return ZonedDateTime.of(
                    LocalDate.of(d.getYear(), d.getMonth() + 1, d.getDay()),
                    LocalTime.MIDNIGHT,
                    d.getTimeZone().toZoneId());
        }

        if (fromValue instanceof OATime) {
            OATime t = (OATime) fromValue;
            return ZonedDateTime.of(EPOCH_DATE,
                    toLocalTime(t),
                    t.getTimeZone().toZoneId());
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return toZDT(OADateTime.valueOf(s, fmt));
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof java.sql.Time) {
            return toZDT(new OADateTime((java.sql.Time) fromValue));
        }

        if (fromValue instanceof java.sql.Date) {
            return toZDT(new OADateTime((java.sql.Date) fromValue));
        }

        if (fromValue instanceof byte[]) {
            long tm = new BigInteger((byte[]) fromValue).longValue();
            return toZDT(new OADateTime(tm));
        }

        if (fromValue instanceof Number) {
            return toZDT(new OADateTime(((Number) fromValue).longValue()));
        }

        if (fromValue instanceof Instant) {
            return ZonedDateTime.ofInstant((Instant) fromValue,
                    ZoneId.systemDefault());
        }

        if (fromValue instanceof LocalDateTime) {
            return ((LocalDateTime) fromValue).atZone(ZoneId.systemDefault());
        }

        if (fromValue instanceof LocalDate) {
            return ((LocalDate) fromValue)
                    .atStartOfDay(ZoneId.systemDefault());
        }

        if (fromValue instanceof LocalTime) {
            return ZonedDateTime.of(EPOCH_DATE,
                    (LocalTime) fromValue,
                    ZoneId.systemDefault());
        }

        return null;
    }

    /**
     * Converts an {@link OADateTime} instance into a {@link ZonedDateTime},
     * preserving date, time, and timezone information.
     *
     * @param dt the {@link OADateTime} value to convert
     * @return the corresponding {@link ZonedDateTime}
     */
    private ZonedDateTime toZDT(OADateTime dt) {
        LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonth() + 1, dt.getDay());
        LocalTime lt = toLocalTime(dt);
        return ZonedDateTime.of(ld, lt, dt.getTimeZone().toZoneId());
    }

    /**
     * Converts an {@link OATime} instance into a {@link LocalTime},
     * preserving hour, minute, second, and millisecond precision.
     *
     * @param t the {@link OATime} value to convert
     * @return the corresponding {@link LocalTime}
     */
    private LocalTime toLocalTime(OATime t) {
        int nanos = t.getMilliSecond() * 1_000_000;
        return LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), nanos);
    }

    /**
     * Converts an {@link OADateTime} instance into a {@link LocalTime},
     * preserving hour, minute, second, and millisecond precision.
     *
     * @param dt the {@link OADateTime} value to convert
     * @return the corresponding {@link LocalTime}
     */
    private LocalTime toLocalTime(OADateTime dt) {
        int nanos = dt.getMilliSecond() * 1_000_000;
        return LocalTime.of(dt.get24Hour(), dt.getMinute(), dt.getSecond(), nanos);
    }

    /**
     * Converts a {@link ZonedDateTime} value into a formatted string.
     *
     * @param fromValue the {@link ZonedDateTime} value to convert
     * @param fmt the format string to use
     * @return the formatted string, or an empty string if the value is {@code null}
     */
    @Override
    public String convertToString(ZonedDateTime fromValue, String fmt) {
        if (fromValue == null) return "";
        OADateTime dt = new OADateTime(Date.from(fromValue.toInstant()));
        return dt.toString(fmt);
    }
}

