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

import java.util.*;

import com.viaoa.datetime.OATimeZone;


/**
 * Converter for transforming values into {@link TimeZone} objects and formatting
 * them for UI display.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types when converting to {@link TimeZone}:
 * <ul>
 *   <li>{@code null} → {@code null}</li>
 *   <li>{@link TimeZone} instance → returned directly</li>
 *   <li>{@link String} → matched by {@link OATimeZone#getTimeZone(String)}</li>
 * </ul>
 *
 * <p>String inputs are delegated to {@link OATimeZone}, which resolves the
 * textual identifier into a concrete {@link TimeZone} if recognized. Otherwise,
 * conversion returns {@code null}.</p>
 *
 * <h3>String Formatting Rules</h3>
 * <ul>
 *   <li>{@code null} → empty string {@code ""}</li>
 *   <li>Non-null values formatted using
 *       {@link OATimeZone#getOATimeZone(TimeZone)} and
 *       {@link OATimeZone.TZ#getDisplay()} — suitable for UI display</li>
 *   <li>{@code fmt} parameter currently ignored</li>
 * </ul>
 *
 * <p>Note that the returned display value is not guaranteed to be a stable
 * identifier for round-trip conversion. This converter prioritizes
 * human-friendly representation. Identity-based formatting may be introduced
 * via {@code fmt} in future enhancements.</p>
 *
 * @see OAConverterInterface
 * @see OATimeZone
 * @see TimeZone
 */
public class OAConverterTimeZone implements OAConverterInterface<TimeZone> {

    /**
     * Converts the supplied value into a {@link TimeZone}.
     *
     * @param thisClass expected type ({@code TimeZone.class})
     * @param fromValue value to convert; may be {@code null}
     * @param fmt ignored
     * @return matching {@link TimeZone} or {@code null} if not recognized
     */
	@Override
	public TimeZone convert(Class<TimeZone> thisClass, Object fromValue, String fmt) {
        if (fromValue == null) return null;
        if (fromValue instanceof TimeZone) return (TimeZone) fromValue;
        TimeZone tz = null;
        if (fromValue instanceof String) {
            String s = ((String) fromValue).trim();
            if (!s.isEmpty()) {
                tz = OATimeZone.getTimeZone(s);
            }
        }
        return tz;
    }

	
    /**
     * Converts a {@link TimeZone} into a human-readable string for UI use.
     *
     * @param fromValue source TimeZone; may be {@code null}
     * @param fmt ignored
     * @return display string based on {@link OATimeZone} or {@code ""} if null
     */
	@Override
	public String convertToString(TimeZone fromValue, String fmt) {
		if (fromValue == null) return "";
        OATimeZone.TZ tz = OATimeZone.getOATimeZone(fromValue);
        if (tz == null) return "";
        return tz.getDisplay();
	}
}



