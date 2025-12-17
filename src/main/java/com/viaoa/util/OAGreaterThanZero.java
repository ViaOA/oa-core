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
 * Singleton marker object used by OA's comparison and filtering framework to
 * represent the predicate "numeric value greater than zero". When compared
 * using {@link #equals(Object)}, this instance returns {@code true} if and
 * only if the argument can be converted to a {@link Number} via
 * {@link OAConv#convert(Class, Object, Object)} and the resulting numeric
 * value is strictly greater than {@code 0.0}. <p>
 *
 * This class is intended for use as a special compare token, not as a general
 * purpose value object. Equality is intentionally asymmetric with respect to
 * other types (for example, {@code instance.equals(5)} is true, but
 * {@code Integer.valueOf(5).equals(instance)} is false), and the hash code is
 * constant because only a single shared instance is used. The class is
 * stateless and safe for concurrent use.
 */
public class OAGreaterThanZero implements OASpecialCompareObject, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing the greater-than-zero comparison token.
     */
    public static final OAGreaterThanZero instance = new OAGreaterThanZero();
    
    /**
     * Creates a new instance.
     */
    private OAGreaterThanZero() {
    }

    /**
     * Returns the singleton greater-than-zero comparison instance.
     *
     * @return the singleton instance
     */
    public OAGreaterThanZero getGreaterThanZeroObject() {
        return instance;
    }
    
    /**
     * Compares the given object to determine whether it represents a numeric value
     * greater than zero.
     *
     * @param obj the object to compare
     * @return true if the object can be converted to a number and is greater than zero
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        Number num = (Number) OAConv.convert(Number.class, obj, null);
        if (num == null) return false;
        return (num.doubleValue() > 0.0);
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
