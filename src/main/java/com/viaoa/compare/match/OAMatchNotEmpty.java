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
package com.viaoa.compare.match;

import com.viaoa.compare.OACompare;
import com.viaoa.converter.OAConverter;
import com.viaoa.lang.OAString;

/**
 * Singleton marker object used by OA's comparison and filtering framework to
 * represent the predicate “value is not empty”. When compared using
 * {@link #equals(Object)}, this instance evaluates to {@code true} if the
 * supplied object is non-null and considered non-empty according to
 * {@link OAConverter#isNotEmpty(Object)}. This includes non-empty strings,
 * non-zero numeric values, {@code true} booleans, non-empty arrays or
 * collections, and other types for which OA defines an emptiness rule. <p>
 *
 * Equality is intentionally asymmetric: this object can match a wide variety
 * of values, while those values do not consider this object equal in return.
 * The class is immutable, thread-safe, and uses a constant hash code because a
 * single shared instance is used for all comparisons.
 */
public class OAMatchNotEmpty implements OAMatch, java.io.Serializable {
	static final long serialVersionUID = 1L;

	/**
	 * Singleton instance representing an empty value.
	 */
	public static final OAMatchNotEmpty instance = new OAMatchNotEmpty();

	/**
	 * Creates a new instance.
	 */
	private OAMatchNotEmpty() {
	}

	@Override
	public boolean matches(Object value, int decimalPlaces) {
		if (value instanceof OAMatchEmpty) return false;
		if (value instanceof OAMatchNotEmpty) return true;
		if (value instanceof OAMatchUnknown) return false;
		return !OACompare.isEmpty(value, false);
	}
}
