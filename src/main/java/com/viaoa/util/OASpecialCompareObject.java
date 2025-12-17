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
 * Marker interface for OA's special comparison tokens. Implementations of this
 * interface define custom equality semantics that override the normal coercion
 * and comparison rules used by {@code OACompare}. These objects are used in
 * filtering, querying, criteria evaluation, and other comparison-intensive
 * parts of the framework where expressions such as “is null”, “not empty”,
 * “greater than zero”, or similar predicates must be represented as concrete
 * objects. <p>
 *
 * Implementations typically provide asymmetric {@code equals(Object)} logic
 * and are immutable singletons. The interface itself contains no methods and
 * serves only as a type marker for the comparison subsystem.
 */
public interface OASpecialCompareObject {

    // note:  OACompare allows objects to be coerced when comparing them.  T
    //   ex: a null that is compared to a boolean value will convert the null to false to do the comparision.
    // using OASpecial, comparison rules are customized. 
    
}
