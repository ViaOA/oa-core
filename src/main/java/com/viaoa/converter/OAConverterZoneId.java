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

import java.time.ZoneId;
import java.util.TimeZone;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OADateTime;

/**
 * Convert to/from a ZoneId value. <br>
 * See OADateTime for format definitions.
 *
 * @see OAConverter
 * @see OADateTime
 */
public class OAConverterZoneId implements OAConverterInterface {

	/**
	 * Convert to/from a Instant value.
	 *
	 * @return Object of type clazz if conversion can be done, else null.
	 */
	public Object convert(Class clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(ZoneId.class)) {
			return convertToZoneId(value, fmt);
		}
		if (value != null && value instanceof ZoneId) {
			return convertFromZoneId(clazz, (ZoneId) value, fmt);
		}
		return null;
	}

	protected ZoneId convertToZoneId(Object value, String fmt) {
		if (value == null) {
			return null;
		}

		if (value instanceof ZoneId) {
			ZoneId zid = (ZoneId) value;
			return zid;
		}
		if (value instanceof TimeZone) {
			return ((TimeZone) value).toZoneId();
		}
		return null;
	}

	protected Object convertFromZoneId(Class toClass, ZoneId zid, String fmt) {
		if (zid == null || toClass == null) {
			return null;
		}
		if (toClass.equals(TimeZone.class)) {
			return TimeZone.getTimeZone(zid.getId());
		}
		return null;
	}

}
