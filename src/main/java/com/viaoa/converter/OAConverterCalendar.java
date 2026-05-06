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

import java.util.Calendar;

import com.viaoa.datetime.OADateTime;

/**
 * Converter for transforming values into {@link Calendar} instances and
 * formatting {@link Calendar} values into display-friendly {@link String}
 * representations.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types when converting to {@code Calendar}:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link Calendar} — returned as a defensive clone</li>
 *     <li>Any type supported by {@link OAConverterOADateTime} (e.g., String,
 *         {@link java.util.Date}, {@link java.time.LocalDateTime},
 *         {@link java.time.ZonedDateTime}, etc.)</li>
 * </ul>
 *
 * <p><strong>Semantics:</strong><br>
 * Conversions are delegated to {@link OAConverterOADateTime}, which applies the
 * standard OA date/time parsing and formatting rules. The transfer to
 * {@link Calendar} is performed using {@link OADateTime#getCalendar()}.</p>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 *     <li>Formatting is delegated to {@link OAConverterOADateTime#convertToString}</li>
 *     <li>If {@code fromValue} is {@code null}, returns {@code ""}</li>
 *     <li>If the conversion to {@link OADateTime} fails, returns {@code ""}</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see OAConverterOADateTime
 * @see OADateTime
 * @see Calendar
 */
public class OAConverterCalendar implements OAConverterInterface<Calendar> {

	private final OAConverterOADateTime dtConv = new OAConverterOADateTime(); 
	
    /**
     * Converts a supplied value into a {@link Calendar}.
     * <p>If {@code fromValue} is already a {@link Calendar}, a defensive clone is
     * returned to prevent unintended external modification.</p>
     *
     * <p>Otherwise, the value is converted to an {@link OADateTime} and then
     * converted to a {@link Calendar}.</p>
     *
     * @param thisClass the expected result type (always {@code Calendar.class})
     * @param fromValue source value to convert; may be {@code null}
     * @param fmt optional parsing mask for {@link String} input values
     * @return a cloned or newly created {@link Calendar}, or {@code null} when
     *         conversion is not possible
     */
	@Override
	public Calendar convert(Class<Calendar> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) return null;
		if (fromValue instanceof Calendar) {
			return (Calendar) ((Calendar) fromValue).clone();
		}
		
		OADateTime dt = dtConv.convert(OADateTime.class, fromValue, fmt);
		if (dt == null) return null;
		return dt.getCalendar();
	}

    /**
     * Converts a {@link Calendar} to a formatted {@link String}.
     *
     * @param fromValue the calendar value to convert; may be {@code null}
     * @param fmt optional date/time formatting mask; if {@code null},
     *            OA default formatting is applied
     * @return formatted date/time string, or {@code ""} when {@code fromValue} is {@code null}
     */
	@Override
	public String convertToString(Calendar fromValue, String fmt) {
		if (fromValue == null) return "";

		OADateTime dt = dtConv.convert(OADateTime.class, fromValue, fmt);
		if (dt == null) return "";

		String s = dtConv.convertToString(dt, fmt);
		if (s == null) return "";
		return s;
	}
}

