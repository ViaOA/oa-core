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
package com.viaoa.compare;

import com.viaoa.converter.OAConverter;

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
public class OANotEmptyObject implements OASpecialCompareObject, java.io.Serializable {
	static final long serialVersionUID = 1L;

	/**
	 * Singleton instance representing the not-empty comparison token.
	 */
	public static final OANotEmptyObject instance = new OANotEmptyObject();

	/**
	 * Creates a new instance.
	 */
	private OANotEmptyObject() {
	}

	/**
	 * Returns the singleton not-empty comparison instance.
	 *
	 * @return the singleton instance
	 */
	public OANotEmptyObject getNotEmptyObject() {
		return instance;
	}

	/**
	 * Compares the given object to determine whether it is considered non-empty.
	 *
	 * @param obj the object to compare
	 * @return true if the object is non-null and considered not empty
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == OANotEmptyObject.instance) return true;
		return OAConverter.isNotEmpty(obj);
	}

	/**
	 * Returns a constant hash code value.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return 1;
	}
}
