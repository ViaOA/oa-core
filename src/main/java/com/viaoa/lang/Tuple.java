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
package com.viaoa.lang;

/**
 * Immutable two-value container used as a lightweight pair within the OA
 * framework. Both elements are stored in final fields and may be {@code null}.
 * The class provides no additional behavior, equality logic, or hashing
 * semantics and is intended solely as a simple carrier for temporary or
 * auxiliary data structures. Instances are inherently thread-safe.
 */
public class Tuple<A, B> {

	/**
	 * First value in the tuple.
	 */
    public final A a;

    /**
     * Second value in the tuple.
     */
    public final B b;
    
    /**
     * Creates a new tuple containing the supplied values.
     *
     * @param a the first value
     * @param b the second value
     */
    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }
    
}
