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

/**
 * Singleton marker object used by OA's comparison and filtering framework to
 * represent an unknown value. Unlike other special comparison tokens, an
 * {@code OAUnknownObject} instance is intentionally not equal to {@code null},
 * to any ordinary value, or even to another instance of the same class. It is
 * equal only to its own singleton instance. <p>
 *
 * This token is used in situations where a property is structurally present
 * but its actual value is unavailable or should be treated as opaque, such as
 * during diff operations, partial deserialization, or criteria evaluation
 * where no comparison should succeed. The class is immutable, stateless, and
 * fully thread-safe.
 */
public class OAUnknownObject implements OASpecialCompareObject, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing an unknown value.
     */
    public static final OAUnknownObject instance = new OAUnknownObject();
    
    /**
     * Private constructor to enforce singleton usage.
     */
    private OAUnknownObject() {
    }
    
    /**
     * Returns the singleton unknown object instance.
     *
     * @return the singleton {@link OAUnknownObject} instance
     */
    public OAUnknownObject getUnknownObject() {
        return instance;
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
    public boolean equals(Object obj) {
        return (obj == instance);
    }

    /**
     * Returns the hash code for this object.
     *
     * @return a constant hash code value
     */
    @Override
    public int hashCode() {
        return 1;
    }
}
