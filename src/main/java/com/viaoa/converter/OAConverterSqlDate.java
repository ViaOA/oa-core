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
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Convert to/from a Date value. <br>
 * <b>Converting the following to a Date</b>
 * <ul>
 * <li>String: converts to a Date, using optional format for parsing.
 * <li>OADateTime: returns Date value.
 * <li>All others value will return null.
 * </ul>
 * <br>
 * <b>Converts a Date to any of the following</b>
 * <ul>
 * <li>String, using an optional format.
 * </ul>
 * 
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterSqlDate implements OAConverterInterface {
	// !!!!! REMEMBER:  date.month values are 0-11

	/**
	 * Convert to/from a Date value.
	 * 
	 * @param clazz Class to convert to.
	 * @param value to convert
	 * @param fmt   format string
	 * @return Object of type clazz if conversion can be done, else null.
	 * @see OADateTime
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(Date.class)) {
			return convertToDate(value, fmt);
		}
		if (value != null && value instanceof Date) {
			return convertFromDate(clazz, (Date) value, fmt);
		}
		return null;
	}

	protected Date convertToDate(Object value, String fmt) {
		if (value == null) {
			return null;
		}
		if (value instanceof Date) {
			return (Date) value;
		}

		if (value instanceof String) {
			if (((String) value).length() == 0) {
				return null;
			}
			OADateTime dt = (OADateTime) OADateTime.valueOf((String) value, fmt);
			if (dt == null) {
				return null;
			}
			Date d = new Date(dt.getTime());
			return d;
		}

		if (value instanceof byte[]) {
			return new Date(new java.math.BigInteger((byte[]) value).longValue());
		}

		if (value instanceof OADateTime) {
			Date d = new Date(((OADateTime) value).getTime());
			return d;
		}

		if (value instanceof Number) {
			long x = ((Number) value).longValue();
			return new Date(x);
		}

		if (value instanceof Instant) {
			Date out = new java.sql.Date(Date.from((Instant) value).getTime());
			return out;
		}

		if (value instanceof LocalDate) {
			LocalDate ld = (LocalDate) value;
			Date out = new Date(ld.getYear() - 1900, (ld.getMonth().getValue()) - 1, ld.getDayOfMonth());
			return out;
		}

		// if (value instanceof LocalTime) {

		if (value instanceof LocalDateTime) {
			LocalDateTime ldt = (LocalDateTime) value;
			Date out = new java.sql.Date(Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime());
			return out;
		}

		if (value instanceof ZonedDateTime) {
			ZonedDateTime zdt = (ZonedDateTime) value;
			Date out = new java.sql.Date(Date.from(zdt.toInstant()).getTime());
			return out;
		}

		return null;
	}

	protected Object convertFromDate(Class toClass, Date dateValue, String fmt) {
		if (toClass.equals(String.class)) {
			if (dateValue == null) {
				return null;
			}
			OADate od = new OADate(dateValue);
			return od.toString(fmt);
		}

		return null;
	}
}
