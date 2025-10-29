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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Converter for transforming values into {@link LocalTime} and formatting them
 * using OA {@link OATime} rules.
 *
 * <h3>Conversion Rules</h3>
 * Supported source types and behavior:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link LocalTime} – returned directly</li>
 *     <li>{@link OATime} – full time preserved including milliseconds</li>
 *     <li>{@link OADate} – normalized to {@link LocalTime#MIDNIGHT}</li>
 *     <li>{@link String} – parsed using {@link OATime#valueOf(String, String)},
 *         input is trimmed prior to parsing</li>
 *     <li>{@link java.sql.Time} – converted via {@link OATime} utilities</li>
 *     <li>{@link Number} – interpreted as epoch milliseconds</li>
 *     <li>{@code byte[]} – interpreted as epoch milliseconds</li>
 *     <li>{@link Instant} – converted using the system default timezone,
 *         preserving the clock time visible in that zone</li>
 *     <li>{@link ZonedDateTime} – {@code toLocalTime()}, zone and offset discarded</li>
 * </ul>
 *
 * <p><strong>Timezone behavior:</strong><br>
 * When conversion requires timezone context (e.g., {@link Instant}),
 * the <strong>system default timezone</strong> is applied to determine the
 * resulting {@link LocalTime}.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>Formatted using {@link OATime#toString(String)}</li>
 *     <li>{@code fmt} passed directly to formatter</li>
 * </ul>
 *
 * <p>Note: {@link LocalTime} does not contain date information. Round-trip
 * conversions from sources with date parts (e.g., {@link Instant},
 * {@link OADateTime}) will lose the date component by design.</p>
 *
 * @see OAConverterInterface
 * @see OATime
 * @see LocalTime
 */
public class OAConverterLocalTime implements OAConverterInterface<LocalTime> {

    @Override
    public LocalTime convert(Class<LocalTime> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof LocalTime) return (LocalTime) fromValue;

        if (fromValue instanceof OADate) {
            return LocalTime.MIDNIGHT;
        }

        if (fromValue instanceof OATime) {
            return convertOATime((OATime) fromValue);
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return convertOATime((OATime) OATime.valueOf(s, fmt));
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof java.sql.Time) {
            return convertOATime(new OATime((java.sql.Time) fromValue));
        }

        if (fromValue instanceof byte[]) {
            long tm = new BigInteger((byte[]) fromValue).longValue();
            return convertOATime(new OATime(tm));
        }

        if (fromValue instanceof Number) {
            return convertOATime(new OATime(((Number) fromValue).longValue()));
        }

        if (fromValue instanceof Instant) {
            LocalDateTime ldt = LocalDateTime.ofInstant((Instant) fromValue, ZoneId.systemDefault());
            return ldt.toLocalTime();
        }

        if (fromValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) fromValue).toLocalTime();
        }

        return null;
    }

    private LocalTime convertOATime(OATime t) {
        int nanos = t.getMilliSecond() * 1_000_000;
        return LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), nanos);
    }

    @Override
    public String convertToString(LocalTime fromValue, String fmt) {
        if (fromValue == null) return "";
        int millis = fromValue.getNano() / 1_000_000;
        OATime t = new OATime(fromValue.getHour(), fromValue.getMinute(),
                fromValue.getSecond(), millis);
        return t.toString(fmt);
    }
}
