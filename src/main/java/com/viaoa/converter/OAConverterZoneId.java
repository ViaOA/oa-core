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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import com.viaoa.util.OADateTime;

/**
 * Converter for transforming values into {@link ZoneId} and formatting them
 * into a {@link String} zone identifier. This enables OA model objects and UI
 * layers to interoperate cleanly with Java's modern timezone system.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types and behavior:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link ZoneId} – returned directly</li>
 *     <li>{@link TimeZone} – {@link TimeZone#toZoneId()}</li>
 *     <li>{@link OADateTime} – extracts and returns its associated timezone</li>
 *     <li>{@link ZonedDateTime} – uses {@link ZonedDateTime#getZone()}</li>
 *     <li>{@link String} – parsed using {@link ZoneId#of(String)}, trimmed first
 *         <ul>
 *             <li>e.g., {@code "America/Chicago"}, {@code "UTC"}, {@code "GMT-05:00"}</li>
 *             <li>returns {@code null} if the identifier is invalid</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p><strong>Unsupported Types:</strong><br>
 * Numeric and date/time values that do not contain timezone information
 * (e.g., {@link java.time.LocalDate}, {@link java.time.LocalTime},
 * {@link java.time.Instant}) will return {@code null}, since this converter
 * never attempts to invent arbitrary timezone behavior.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>{@link ZoneId#getId()} returned unchanged (e.g. {@code "UTC"}, {@code "Europe/Berlin"})</li>
 *     <li>{@code fmt} parameter is ignored</li>
 * </ul>
 *
 * <p><strong>Developer Guidance:</strong><br>
 * This converter preserves explicit timezone semantics at all times. If a
 * source object does not contain enough information to determine a timezone
 * reliably, the result is {@code null} instead of falling back to defaults.
 * This prevents accidental mixing of date/time and timezone semantics.</p>
 *
 * @see OAConverterInterface
 * @see ZoneId
 * @see TimeZone
 * @see OADateTime
 */
public class OAConverterZoneId implements OAConverterInterface<ZoneId> {

    @Override
    public ZoneId convert(Class<ZoneId> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof ZoneId) return (ZoneId) fromValue;

        if (fromValue instanceof TimeZone) {
            return ((TimeZone) fromValue).toZoneId();
        }

        if (fromValue instanceof OADateTime) {
            return ((OADateTime) fromValue).getTimeZone().toZoneId();
        }

        if (fromValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) fromValue).getZone();
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                return ZoneId.of(s);
            }
            catch (Throwable t) {
                return null;
            }
        }

        return null;
    }

    @Override
    public String convertToString(ZoneId fromValue, String fmt) {
        if (fromValue == null) return "";
        return fromValue.getId();
    }
}
