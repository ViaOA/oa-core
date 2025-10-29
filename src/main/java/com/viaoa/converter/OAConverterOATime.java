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
import java.sql.Time;
import java.time.*;
import java.util.Date;

import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Converter for transforming values into {@link OATime} and formatting
 * {@link OATime} instances into {@link String} values.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types and behavior:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link OATime} — returned directly</li>
 *     <li>{@link String} — parsed via {@link OATime#valueOf(String, String)}, trimmed first</li>
 *     <li>{@link java.sql.Time} — time-of-day extracted</li>
 *     <li>{@link OADateTime} — time-of-day extracted (preserves milliseconds)</li>
 *     <li>{@code byte[]} — milliseconds value extracted from first 8 bytes</li>
 *     <li>{@link Number} — interpreted as milliseconds since Epoch, time extracted</li>
 *     <li>{@link Instant} — converted using {@link OADateTime}, time extracted</li>
 *     <li>{@link LocalTime} — directly mapped to hours/min/sec/millis</li>
 *     <li>{@link LocalDateTime} — timezone assumed {@link ZoneId#systemDefault()}</li>
 *     <li>{@link ZonedDateTime} — zone respected; instant evaluated to extract time</li>
 *     <li>{@link LocalDate} — interpreted as midnight (00:00:00.000)</li>
 * </ul>
 *
 * <p><strong>Time-Only Semantics:</strong><br>
 * OATime represents only a time-of-day value. When converting from a type that
 * includes a date or zone (e.g. {@link LocalDateTime}), only the time portion
 * is preserved.</p>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>Delegates to {@link OATime#toString(String)} when {@code fmt} provided</li>
 *     <li>Returns non-null strings suitable for UI display</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see OATime
 * @see OADateTime
 */
public class OAConverterOATime implements OAConverterInterface<OATime> {

    @Override
    public OATime convert(Class<OATime> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof OATime) return (OATime) fromValue;

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return (OATime) OATime.valueOf(s, fmt);
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof Time) {
            return new OATime((Time) fromValue);
        }

        if (fromValue instanceof OADateTime) {
            return new OATime((OADateTime) fromValue);
        }

        if (fromValue instanceof byte[]) {
            long ms = new BigInteger((byte[]) fromValue).longValue();
            return new OATime(ms);
        }

        if (fromValue instanceof Number) {
            return new OATime(((Number) fromValue).longValue());
        }

        if (fromValue instanceof Date) {
            return new OATime(new OADateTime((Date) fromValue));
        }

        if (fromValue instanceof Instant) {
            return new OATime(new OADateTime((Instant) fromValue));
        }

        if (fromValue instanceof LocalTime) {
            LocalTime lt = (LocalTime) fromValue;
            int ms = lt.getNano() / 1_000_000;
            return new OATime(lt.getHour(), lt.getMinute(), lt.getSecond(), ms);
        }

        if (fromValue instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) fromValue;
            return new OATime(new OADateTime(ldt));
        }

        if (fromValue instanceof ZonedDateTime) {
            ZonedDateTime zdt = (ZonedDateTime) fromValue;
            return new OATime(new OADateTime(zdt));
        }

        if (fromValue instanceof LocalDate) {
            return new OATime(0, 0, 0, 0);
        }

        return null;
    }

    @Override
    public String convertToString(OATime fromValue, String fmt) {
        if (fromValue == null) return "";
        return fromValue.toString(fmt);
    }
}
