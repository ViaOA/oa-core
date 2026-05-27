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
 * represent the predicate “value does not exist”. When compared using
 * {@link #equals(Object)}, this instance evaluates to {@code true} if the
 * supplied object is {@code null}, is the same singleton instance, or is an
 * instance of {@code OAMatchNotExist}. <p>
 *
 * This object functions as a special comparison token rather than a general
 * purpose value, and equality is intentionally asymmetric with respect to other
 * types. The class is immutable, thread-safe, and uses a constant hash code
 * because a single shared instance is used for all comparisons.
 */
public class OAMatchNotExist implements OAMatch, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing the not-exist comparison token.
     */
    public static final OAMatchNotExist instance = new OAMatchNotExist();
    
    /**
     * Creates a new instance.
     */
    private OAMatchNotExist() {
    }
    
    /**
     * Compares the given object to determine whether it represents a non-existent value.
     *
     * @param obj the object to compare
     * @return true if the object another {@code OAMatchNotExist}
     */
    @Override
    public boolean matches(Object obj, int decimalPlaces) {
		if (obj instanceof OAMatchUnknown) return false;
        return (obj == null || obj instanceof OAMatchNotExist);
    }
}
