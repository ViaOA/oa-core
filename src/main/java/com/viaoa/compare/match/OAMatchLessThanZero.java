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

import java.math.BigDecimal;

import com.viaoa.compare.OACompare;
import com.viaoa.lang.OAString;

/*qqqqqqqqqqqq
CODEX

#4 — OAMatchGreaterThanZero.equals(...) / OAMatchLessThanZero.equals(...)

  File/class/method:
  src/main/java/com/viaoa/compare/OAMatchGreaterThanZero.java, equals
  src/main/java/com/viaoa/compare/OAMatchLessThanZero.java, equals

  Concrete bug: both special numeric predicates convert to Number and compare using doubleValue().

  Runtime scenario: a very small non-zero BigDecimal, such as new BigDecimal("1e-400"), can underflow to 0.0.
  OAMatchGreaterThanZero.instance.equals(value) then returns false even though the value is greater than zero.

  Why this violates OA/OG comparison semantics: precision loss before comparison causes false negatives for numeric
  predicates. These tokens can affect filters, query conditions, and object matching.

  Minimal fix direction: compare BigDecimal/BigInteger using exact compareTo(BigDecimal.ZERO) / signum() before
  falling back to double for floating types.

  Suggested CODEX comment location: above the num.doubleValue() comparisons in both classes.


*/

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
public class OAMatchLessThanZero implements OAMatch, java.io.Serializable {
    static final long serialVersionUID = 1L;

    /**
     * Singleton instance representing the greater-than-zero comparison token.
     */
    public static final OAMatchLessThanZero instance = new OAMatchLessThanZero();
    
    /**
     * Creates a new instance.
     */
    private OAMatchLessThanZero() {
    }

    /**
     * Compares the given object to determine whether it represents a numeric value
     * greater than zero.
     *
     * @param obj the object to compare
     * @return true if the object can be converted to a number and is greater than zero
     */
    @Override
    public boolean matches(Object obj, int decimalPlaces) {
        if (obj == null) return false;
        if (obj instanceof OAMatchUnknown) return false;
        if (!(obj instanceof Number) && !(obj instanceof String && OAString.isNumber((String) obj))) return false;
        return OACompare.compare(obj, BigDecimal.ZERO, decimalPlaces) < 0;
    }

}
