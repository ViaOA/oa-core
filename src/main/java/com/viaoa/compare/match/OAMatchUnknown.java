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
 * represent an unknown value. Unlike other special comparison tokens, an
 * {@code OAMatchUnknown} instance is intentionally not equal to {@code null},
 * to any ordinary value, or even to another instance of the same class. It is
 * equal only to its own singleton instance. <p>
 *
 * This token is used in situations where a property is structurally present
 * but its actual value is unavailable or should be treated as opaque, such as
 * during diff operations, partial deserialization, or criteria evaluation
 * where no comparison should succeed. The class is immutable, stateless, and
 * fully thread-safe.
 */
public class OAMatchUnknown implements OAMatch, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing an unknown value.
     */
    public static final OAMatchUnknown instance = new OAMatchUnknown();
    
    /**
     * Private constructor to enforce singleton usage.
     */
    private OAMatchUnknown() {
    }
    
    /**
     * Compares this object with another for equality.
     * <p>
     * Equality is true only if the supplied object is the singleton instance.
     *
     * @param obj the object to compare
     * @return {@code true} if the object is the singleton instance; {@code false} otherwise
     */
    @Override
    public boolean matches(Object value, int decimalPlaces) {
    	return (value instanceof OAMatchUnknown);
    }

}
