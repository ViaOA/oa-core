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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Convert to/from a LocalTimeDateTime value. <br>
 * See OADateTime for format definitions.
 *
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterLocalTime implements OAConverterInterface {

	/**
	 * Convert to/from a LocalTime value.
	 *
	 * @return Object of type clazz if conversion can be done, else null.
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(LocalTime.class)) {
			return convertToLocalTime(value, fmt);
		}
		if (value != null && value instanceof LocalTime) {
			return convertFromLocalTime(clazz, (LocalTime) value, fmt);
		}
		return null;
	}

	protected LocalTime convertToLocalTime(Object value, String fmt) {
		if (value == null) {
			return null;
		}

		if (value instanceof LocalTime) {
			LocalTime lt = (LocalTime) value;
		}
		if (value instanceof OADate) {
			OADate d = (OADate) value;
			LocalTime lt = LocalTime.of(0, 0, 0);
			return lt;
		}
		if (value instanceof OATime) {
			OATime t = (OATime) value;
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}
		if (value instanceof String) {
			OATime t = (OATime) OATime.valueOf((String) value, fmt);
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}
		if (value instanceof java.sql.Time) {
			OATime t = new OATime((java.sql.Time) value);
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}
		if (value instanceof Date) {
			OADateTime t = new OADateTime((Date) value);
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}
		if (value instanceof byte[]) {
			OADateTime t = new OADateTime(new java.math.BigInteger((byte[]) value).longValue());
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}
		if (value instanceof Number) {
			OADateTime t = new OADateTime(((Number) value).longValue());
			LocalTime lt = LocalTime.of(t.get24Hour(), t.getMinute(), t.getSecond(), (int) (t.getMilliSecond() * (Math.pow(10, 6))));
			return lt;
		}

		if (value instanceof Instant) {
			LocalDateTime ldt = LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());
			LocalTime lt = LocalTime.of(ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());
			return lt;
		}

		if (value instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) value;
			return zdt.toLocalTime();
		}

		return null;
	}

	protected Object convertFromLocalTime(Class toClass, LocalTime lt, String fmt) {
		if (lt == null || toClass == null) {
			return null;
		}
		if (toClass.equals(String.class)) {
			OATime t = new OATime(lt.getHour(), lt.getMinute(), lt.getSecond(), (int) (lt.getNano() / Math.pow(10, 6)));
			return t.toString(fmt);
		}
		if (Number.class.isAssignableFrom(toClass)) {
			OATime t = new OATime(lt.getHour(), lt.getMinute(), lt.getSecond(), (int) (lt.getNano() / Math.pow(10, 6)));
			long lx = t.getTime();
			return lx;
		}
		return null;
	}

}
