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
 * Converter for transforming values into {@link LocalDate} and formatting them
 * using OA {@link OADate} rules.
 *
 * <h3>Conversion Rules</h3>
 * Supported source types and behavior:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link LocalDate} – returned directly</li>
 *     <li>{@link OADate} – full date preserved</li>
 *     <li>{@link OATime} – converted to Epoch date (1970-01-01),
 *         as no date information exists</li>
 *     <li>{@link String} – parsed using {@link OADate#valueOf(String, String)},
 *         value trimmed prior to parsing</li>
 *     <li>{@link java.sql.Date} – converted via {@link OADateTime} and system timezone</li>
 *     <li>{@link java.sql.Time} – converted to Epoch date (1970-01-01)</li>
 *     <li>{@link Number} – interpreted as epoch milliseconds</li>
 *     <li>{@code byte[]} – interpreted as epoch milliseconds (first 8 bytes)</li>
 *     <li>{@link Instant} – converted using the <strong>system default timezone</strong></li>
 *     <li>{@link LocalTime} – Epoch date + time discarded</li>
 *     <li>{@link ZonedDateTime} – {@code toLocalDate()}, zone/offset discarded</li>
 * </ul>
 *
 * <p><strong>Timezone behavior:</strong><br>
 * When required (e.g., {@link Instant}, numeric types, SQL types), conversion
 * uses the system default timezone to determine the resulting LocalDate.</p>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>Formatted using {@link OADate#toString(String)}</li>
 *     <li>{@code fmt} is passed directly to the formatter</li>
 * </ul>
 *
 * <p>This converter standardizes multiple date representations into
 * {@link LocalDate}, preserving only the calendar date.</p>
 *
 * @see OAConverterInterface
 * @see OADate
 * @see LocalDate
 */
public class OAConverterLocalDate implements OAConverterInterface<LocalDate> {

	/**
	 * Constant representing the epoch date (1970-01-01) used when no date
	 * information is available.
	 */
    private static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    /**
     * Converts the supplied value into a {@link LocalDate}.
     * <p>
     * The conversion behavior depends on the runtime type of {@code fromValue}
     * and may apply system default timezone rules when required.
     *
     * @param thisClass the target class, {@link LocalDate}
     * @param fromValue the value to convert
     * @param fmt the format string used when parsing string values
     * @return the converted {@link LocalDate}, or {@code null} if conversion fails
     */
    @Override
    public LocalDate convert(Class<LocalDate> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof LocalDate) return (LocalDate) fromValue;

        if (fromValue instanceof OADate) {
            OADate d = (OADate) fromValue;
            return LocalDate.of(d.getYear(), d.getMonth() + 1, d.getDay());
        }

        if (fromValue instanceof OATime) {
            return EPOCH_DATE;
        }

        if (fromValue instanceof String) {
            try {
                String s = ((String) fromValue).trim();
                if (s.isEmpty()) return null;
                OADate d = (OADate) OADate.valueOf(s, fmt);
                return LocalDate.of(d.getYear(), d.getMonth() + 1, d.getDay());
            }
            catch (Throwable t) {
                return null;
            }
        }

        if (fromValue instanceof java.sql.Date) {
            OADateTime dt = new OADateTime(new Date(((java.sql.Date) fromValue).getTime()));
            return LocalDate.of(dt.getYear(), dt.getMonth() + 1, dt.getDay());
        }

        if (fromValue instanceof java.sql.Time) {
            return EPOCH_DATE;
        }

        if (fromValue instanceof byte[]) {
            long tm = new java.math.BigInteger((byte[]) fromValue).longValue();
            OADateTime dt = new OADateTime(tm);
            return LocalDate.of(dt.getYear(), dt.getMonth() + 1, dt.getDay());
        }

        if (fromValue instanceof Number) {
            OADateTime dt = new OADateTime(((Number) fromValue).longValue());
            return LocalDate.of(dt.getYear(), dt.getMonth() + 1, dt.getDay());
        }

        if (fromValue instanceof Instant) {
            LocalDateTime ldt =
                    LocalDateTime.ofInstant((Instant) fromValue, ZoneId.systemDefault());
            return ldt.toLocalDate();
        }

        if (fromValue instanceof LocalTime) {
            return EPOCH_DATE;
        }

        if (fromValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) fromValue).toLocalDate();
        }

        return null;
    }

    /**
     * Converts a {@link LocalDate} into a formatted string.
     *
     * @param fromValue the {@link LocalDate} value to convert
     * @param fmt the format string to use
     * @return the formatted string, or an empty string if the value is {@code null}
     */
    @Override
    public String convertToString(LocalDate fromValue, String fmt) {
        if (fromValue == null) return "";
        OADate d = new OADate(fromValue.getYear(),
                              fromValue.getMonthValue() - 1,
                              fromValue.getDayOfMonth());
        return d.toString(fmt);
    }
}
