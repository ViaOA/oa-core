/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
    public static final OAAnyValueObject instance = new OAAnyValueObject();
    
    private OAAnyValueObject() {
    }
    
    public OAAnyValueObject getNullObject() {
        return instance;
    }
    
    @Override
    public boolean equals(Object obj) {
        return true;
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
