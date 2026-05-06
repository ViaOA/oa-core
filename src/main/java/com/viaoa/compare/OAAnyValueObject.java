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
 * represent a match-all value. <p>
 *
 * An {@code OAAnyValueObject} is considered equal to any other object,
 * including {@code null}. It is used internally by filters, matchers, and
 * query logic that require a token meaning “no restriction” or “any value
 * accepted.” <p>
 *
 * Equality and hash semantics are intentionally overridden so that all
 * comparisons succeed and the object behaves consistently when used in hashed
 * collections. This class has no state and is safe for concurrent use.
 */
public class OAAnyValueObject implements OASpecialCompareObject, java.io.Serializable {
    static final long serialVersionUID = 1L;
    
    /**
     * Singleton instance of {@code OAAnyValueObject}.
     * <p>
     * This instance should be used wherever a match-all comparison value
     * is required.
     */
    public static final OAAnyValueObject instance = new OAAnyValueObject();
    
    /**
     * Private constructor to prevent external instantiation.
     * <p>
     * Enforces the singleton pattern for this class.
     */
    private OAAnyValueObject() {
    }
    
    /**
     * Returns the singleton match-all instance.
     * <p>
     * This method provides a semantic accessor used by comparison
     * and filtering frameworks to retrieve the null-equivalent object.
     *
     * @return the singleton {@code OAAnyValueObject} instance
     */
    public OAAnyValueObject getNullObject() {
        return instance;
    }
    
    /**
     * Indicates equality with any object.
     * <p>
     * This method always returns {@code true}, allowing this object
     * to match any comparison target, including {@code null}.
     *
     * @param obj the object to compare against
     * @return {@code true} for all comparisons
     */
    @Override
    public boolean equals(Object obj) {
        return true;
    }

    /**
     * Returns a constant hash code value.
     * <p>
     * This implementation ensures consistent behavior when this object
     * is used in hashed collections, aligning with its universal
     * equality semantics.
     *
     * @return a constant hash code value
     */
    @Override
    public int hashCode() {
        return 1;
    }
}
