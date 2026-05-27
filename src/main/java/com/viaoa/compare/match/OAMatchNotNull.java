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

/**
 * Singleton marker object used by OA's comparison and filtering framework to
 * represent the predicate “value is not null”. When compared using
 * {@link #equals(Object)}, this instance evaluates to {@code true} for any
 * non-null object and {@code false} for {@code null}. <p>
 *
 * This object is intended solely as a special comparison token and not a
 * general-purpose value. Equality is intentionally asymmetric with respect to
 * other types, and a constant hash code is used because a single shared
 * instance is employed for all comparisons. The class is immutable and fully
 * thread-safe.
 */
public class OAMatchNotNull implements OAMatch, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing the not-null comparison token.
     */
    public static final OAMatchNotNull instance = new OAMatchNotNull();
    
    /**
     * Creates a new instance.
     */
    private OAMatchNotNull() {
    }

    /**
     * Compares the given object to determine whether it is non-null.
     *
     * @param obj the object to compare
     * @return true if the object is not null
     */
    @Override
    public boolean matches(Object value, int decimalPlaces) {
    	return value != null && !(value instanceof OAMatchUnknown);
    }
}
