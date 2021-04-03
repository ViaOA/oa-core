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
import java.time.ZonedDateTime;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Convert to/from a java.time.Instant value. <br>
 * See OADateTime for format definitions.
 *
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterInstant implements OAConverterInterface {

	/**
	 * Convert to/from a Instant value.
	 *
	 * @return Object of type clazz if conversion can be done, else null.
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(Instant.class)) {
			return convertToInstant(value, fmt);
		}
		if (value != null && value instanceof Instant) {
			return convertFromInstant(clazz, (Instant) value, fmt);
		}
		return null;
	}

	protected Instant convertToInstant(Object value, String fmt) {
		if (value == null) {
			return null;
		}

		if (value instanceof Instant) {
			Instant instant = (Instant) value;
			return instant;
		}
		if (value instanceof OADateTime) {
			OADateTime dt = (OADateTime) value;
			return dt.getInstant();
		}
		if (value instanceof OADate) {
			OADate d = (OADate) value;
			OADateTime dt = new OADateTime(d);
			return dt.getInstant();
		}
		if (value instanceof OATime) {
			OATime t = (OATime) value;
			OADateTime dt = new OADateTime(t);
			return dt.getInstant();
		}
		if (value instanceof String) {
			OADateTime dt = OADateTime.valueOf((String) value, fmt);
			return dt.getInstant();
		}
		if (value instanceof java.sql.Time) {
			OADateTime dt = new OADateTime((java.sql.Time) value);
			return dt.getInstant();
		}
		if (value instanceof Date) {
			OADateTime dt = new OADateTime((Date) value);
			return dt.getInstant();
		}
		if (value instanceof byte[]) {
			OADateTime dt = new OADateTime(new java.math.BigInteger((byte[]) value).longValue());
			return dt.getInstant();
		}
		if (value instanceof Number) {
			OADateTime dt = new OADateTime(((Number) value).longValue());
			return dt.getInstant();
		}

		if (value instanceof LocalDateTime) {
			OADateTime dt = new OADateTime((LocalDateTime) value);
			return dt.getInstant();
		}

		if (value instanceof ZonedDateTime) {
			OADateTime dt = new OADateTime((ZonedDateTime) value);
			return dt.getInstant();
		}

		if (value instanceof LocalDate) {
			OADate d = new OADate((LocalDate) value);
			return d.getInstant();
		}

		if (value instanceof LocalTime) {
			OATime t = new OATime((LocalTime) value);
			return t.getInstant();
		}

		return null;
	}

	protected Object convertFromInstant(Class toClass, Instant instant, String fmt) {
		if (instant == null || toClass == null) {
			return null;
		}
		if (toClass.equals(String.class)) {
			OADateTime dt = new OADateTime(instant);
			return dt.toString(fmt);
		}
		if (Number.class.isAssignableFrom(toClass)) {
			OADateTime dt = new OADateTime(instant);
			long lx = dt.getTime();
			return lx;
		}
		return null;
	}

}
