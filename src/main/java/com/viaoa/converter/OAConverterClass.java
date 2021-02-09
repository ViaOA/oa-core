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

public class OAConverterClass implements OAConverterInterface {

	/**
	 * Used to convert Class into another value type (usually String).
	 */
	public Object convert(Class convertToClass, Object value, String fmt) {
		if (Class.class.equals(convertToClass)) {
			if (value instanceof String) {
				try {
					Class c = Class.forName((String) value);
					return c;
				} catch (Exception e) {
				}
			}
			return null;
		}

		if (!String.class.equals(convertToClass)) {
			return null;
		}

		String s = ((Class) value).toString();
		return s;
	}
}
