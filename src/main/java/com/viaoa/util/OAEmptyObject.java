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
package com.viaoa.util;

/**
 * Object used to represent an empty value.
 *
 * @see OAString#isEmpty(Object)
 */
public class OAEmptyObject implements OASpecialCompareObject, java.io.Serializable {
	static final long serialVersionUID = 1L;

	/**
	 * Singleton instance representing an empty value.
	 */
	public static final OAEmptyObject instance = new OAEmptyObject();

	/**
	 * Creates a new instance.
	 */
	private OAEmptyObject() {
	}

	/**
	 * Returns the singleton non-empty representation.
	 *
	 * @return the singleton instance
	 */
	public OAEmptyObject getNotEmptyObject() {
		return instance;
	}

	/**
	 * Compares the given object to determine if it represents an empty value.
	 *
	 * @param obj the object to compare
	 * @return true if the object is considered empty
	 */
	@Override
	public boolean equals(Object obj) {
		return OAString.isEmpty(obj);
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
