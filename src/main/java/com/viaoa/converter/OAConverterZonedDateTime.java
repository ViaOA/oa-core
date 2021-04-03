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
 * Convert to/from a ZonedDateDateTime value. <br>
 * See OADateTime for format definitions.
 *
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterZonedDateTime implements OAConverterInterface {

	/**
	 * Convert to/from a LocalDateTime value.
	 *
	 * @return Object of type clazz if conversion can be done, else null.
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(ZonedDateTime.class)) {
			return convertToZonedDateTime(value, fmt);
		}
		if (value != null && value instanceof ZonedDateTime) {
			return convertFromZonedDateTime(clazz, (ZonedDateTime) value, fmt);
		}
		return null;
	}

	protected ZonedDateTime convertToZonedDateTime(Object value, String fmt) {
		if (value == null) {
			return null;
		}

		if (value instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) value;
			return zdt;
		}
		if (value instanceof OADateTime) {
			OADateTime dt = (OADateTime) value;
			ZonedDateTime zdt = ZonedDateTime.of(	dt.getYear(), dt.getMonth() + 1, dt.getDay(), dt.get24Hour(), dt.getMinute(),
													dt.getSecond(),
													(int) (dt.getMilliSecond() / Math.pow(10, 6)),
													dt.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof OADate) {
			OADate d = (OADate) value;
			ZonedDateTime zdt = ZonedDateTime.of(d.getYear(), d.getMonth() + 1, d.getDay(), 0, 0, 0, 0, d.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof OATime) {
			OATime t = (OATime) value;
			ZonedDateTime zdt = ZonedDateTime.of(	0, Month.JANUARY.getValue(), 0, t.get24Hour(), t.getMinute(),
													t.getSecond(),
													(int) (t.getMilliSecond() / Math.pow(10, 6)),
													t.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof String) {
			OADateTime dt = OADateTime.valueOf((String) value, fmt);
			ZonedDateTime zdt = ZonedDateTime.of(	dt.getYear(), dt.getMonth() + 1, dt.getDay(), dt.get24Hour(), dt.getMinute(),
													dt.getSecond(),
													(int) (dt.getMilliSecond() / Math.pow(10, 6)),
													dt.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof java.sql.Time) {
			OADateTime dt = new OADateTime((java.sql.Time) value);
			ZonedDateTime zdt = ZonedDateTime.of(	0, Month.JANUARY.getValue(), 0, dt.get24Hour(), dt.getMinute(),
													dt.getSecond(),
													(int) (dt.getMilliSecond() / Math.pow(10, 6)),
													dt.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof Date) {
			OADateTime dt = new OADateTime((Date) value);
			ZonedDateTime zdt = ZonedDateTime.of(	0, Month.JANUARY.getValue(), 0, 0, 0, 0, 0,
													dt.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof byte[]) {
			OADateTime dt = new OADateTime(new java.math.BigInteger((byte[]) value).longValue());
			ZonedDateTime zdt = ZonedDateTime.of(	dt.getYear(), dt.getMonth() + 1, dt.getDay(), dt.get24Hour(), dt.getMinute(),
													dt.getSecond(),
													(int) (dt.getMilliSecond() / Math.pow(10, 6)),
													dt.getTimeZone().toZoneId());
			return zdt;
		}
		if (value instanceof Number) {
			OADateTime dt = new OADateTime(((Number) value).longValue());
			ZonedDateTime zdt = ZonedDateTime.of(	dt.getYear(), dt.getMonth() + 1, dt.getDay(), dt.get24Hour(), dt.getMinute(),
													dt.getSecond(),
													(int) (dt.getMilliSecond() / Math.pow(10, 6)),
													dt.getTimeZone().toZoneId());
			return zdt;
		}

		if (value instanceof Instant) {
			LocalDateTime ldt = LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
			ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
			return zdt;
		}

		if (value instanceof LocalDateTime) {
			LocalDateTime ldt = (LocalDateTime) value;
			ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
			return zdt;
		}

		if (value instanceof LocalDate) {
			LocalDate ld = (LocalDate) value;
			LocalDateTime ldt = LocalDateTime.of(ld.getYear(), ld.getMonth(), ld.getDayOfMonth(), 0, 0, 0);
			ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
			return zdt;
		}

		if (value instanceof LocalTime) {
			LocalTime lt = (LocalTime) value;
			LocalDateTime ldt = LocalDateTime.of(0, Month.JANUARY, 1, lt.getHour(), lt.getMinute(), lt.getSecond(), lt.getNano());
			ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.systemDefault());
			return zdt;
		}

		return null;
	}

	protected Object convertFromZonedDateTime(Class toClass, ZonedDateTime zdt, String fmt) {
		if (zdt == null || toClass == null) {
			return null;
		}
		if (toClass.equals(String.class)) {
			OADateTime dt = new OADateTime(new java.sql.Date(Date.from(zdt.toInstant()).getTime()));
			return dt.toString(fmt);
		}
		if (Number.class.isAssignableFrom(toClass)) {
			OADateTime dt = new OADateTime(new java.sql.Date(Date.from(zdt.toInstant()).getTime()));
			long lx = dt.getTime();
			return lx;
		}
		return null;
	}

}
