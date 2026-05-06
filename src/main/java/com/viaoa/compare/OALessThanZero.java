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

import com.viaoa.converter.OAConv;

/**
 * Singleton marker object used by OA's comparison and filtering framework to
 * represent the predicate “value less than zero or null”. When compared using
 * {@link #equals(Object)}, this instance returns {@code true} if the supplied
 * object is {@code null}, or if it can be converted to a {@link Number} via
 * {@link OAConv#convert(Class, Object, Object)} and the resulting numeric value
 * is strictly less than {@code 0.0}. <p>
 *
 * This object is intended only as a special comparison token; equality is
 * intentionally asymmetric with respect to other types, and the hash code is
 * constant because a single shared instance is used. The class is immutable
 * and safe for concurrent use.
 */
public class OALessThanZero implements OASpecialCompareObject, java.io.Serializable {
    static final long serialVersionUID = 1L;
    
    /**
     * Singleton instance representing the less-than-zero comparison token.
     */
    public static final OALessThanZero instance = new OALessThanZero();
    
    /**
     * Creates a new instance.
     */
    private OALessThanZero() {
    }

    /**
     * Returns the singleton less-than-zero comparison instance.
     *
     * @return the singleton instance
     */
    public OALessThanZero getLessThanZeroObject() {
        return instance;
    }
    
    /**
     * Compares the given object to determine whether it represents a numeric value
     * less than zero or is null.
     *
     * @param obj the object to compare
     * @return true if the object is null or can be converted to a number less than zero
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return true;
        Number num = (Number) OAConv.convert(Number.class, obj, null);
        if (num == null) return false;
        return (num.doubleValue() < 0.0);
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
