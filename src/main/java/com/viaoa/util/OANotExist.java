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
 * represent the predicate “value does not exist”. When compared using
 * {@link #equals(Object)}, this instance evaluates to {@code true} if the
 * supplied object is {@code null}, is the same singleton instance, or is an
 * instance of {@code OANotExist}. <p>
 *
 * This object functions as a special comparison token rather than a general
 * purpose value, and equality is intentionally asymmetric with respect to other
 * types. The class is immutable, thread-safe, and uses a constant hash code
 * because a single shared instance is used for all comparisons.
 */
public class OANotExist implements OASpecialCompareObject, java.io.Serializable {
    static final long serialVersionUID = 1L;
    public static final OANotExist instance = new OANotExist();
    
    private OANotExist() {
    }
    
    public OANotExist getNotExistObject() {
        return instance;
    }
    
    @Override
    public boolean equals(Object obj) {
        return (obj == null || obj == OANotExist.instance || obj instanceof OANotExist);
    }
    @Override
    public int hashCode() {
        return 1;
    }
}
