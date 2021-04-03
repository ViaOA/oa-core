/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.converter;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Convert to/from a LocalDateDateTime value. <br>
 * See OADateTime for format definitions.
 *
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterLocalDate implements OAConverterInterface {

	/**
	 * Convert to/from a LocalDate value.
	 *
	 * @return Object of type clazz if conversion can be done, else null.
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(LocalDate.class)) {
			return convertToLocalDate(value, fmt);
		}
		if (value != null && value instanceof LocalDate) {
			return convertFromLocalDate(clazz, (LocalDate) value, fmt);
		}
		return null;
	}

	protected LocalDate convertToLocalDate(Object value, String fmt) {
		if (value == null) {
			return null;
		}

		if (value instanceof LocalDate) {
			LocalDate ld = (LocalDate) value;
		}
		if (value instanceof OADate) {
			OADate d = (OADate) value;
			LocalDate ld = LocalDate.of(d.getYear(), d.getMonth() + 1, d.getDay());
			return ld;
		}
		if (value instanceof OATime) {
			OATime t = (OATime) value;
			LocalDate ld = LocalDate.of(0, Month.JANUARY, 0);
			return ld;
		}
		if (value instanceof String) {
			OADate dt = (OADate) OADate.valueOf((String) value, fmt);
			LocalDate ld = LocalDate.of(dt.getYear(), Month.of(dt.getMonth() + 1), dt.getDay());
			return ld;
		}
		if (value instanceof java.sql.Time) {
			OADateTime dt = new OADateTime((java.sql.Time) value);
			LocalDate ld = LocalDate.of(0, Month.JANUARY, 0);
			return ld;
		}
		if (value instanceof Date) {
			OADateTime dt = new OADateTime((Date) value);
			LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonth() + 1, dt.getDay());
			return ld;
		}
		if (value instanceof byte[]) {
			OADateTime dt = new OADateTime(new java.math.BigInteger((byte[]) value).longValue());
			LocalDate ld = LocalDate.of(dt.getYear(), Month.of(dt.getMonth() + 1), dt.getDay());
			return ld;
		}
		if (value instanceof Number) {
			OADateTime dt = new OADateTime(((Number) value).longValue());
			LocalDate ld = LocalDate.of(dt.getYear(), Month.of(dt.getMonth() + 1), dt.getDay());
			return ld;
		}

		if (value instanceof Instant) {
			LocalDateTime ldt = LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
			LocalDate ld = LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth());
			return ld;
		}

		if (value instanceof LocalTime) {
			LocalTime lt = (LocalTime) value;
			LocalDate ld = LocalDate.of(0, Month.JANUARY, 1);
			return ld;
		}

		if (value instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) value;
			return zdt.toLocalDate();
		}

		return null;
	}

	protected Object convertFromLocalDate(Class toClass, LocalDate ld, String fmt) {
		if (ld == null || toClass == null) {
			return null;
		}
		if (toClass.equals(String.class)) {
			OADate d = new OADate(ld.getYear(), ld.getMonth().getValue() - 1, ld.getDayOfMonth());
			return d.toString(fmt);
		}
		if (Number.class.isAssignableFrom(toClass)) {
			OADate d = new OADate(ld.getYear(), ld.getMonth().getValue() - 1, ld.getDayOfMonth());
			long lx = d.getTime();
			return lx;
		}
		return null;
	}

}
