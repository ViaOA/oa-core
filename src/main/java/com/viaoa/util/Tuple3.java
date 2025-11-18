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
 * Immutable three-value container used as a lightweight tuple within the OA
 * framework. Each element is stored in a final field and may be {@code null}.
 * The class defines no additional behavior, equality, or hashing semantics and
 * is intended solely as a simple carrier for temporary or auxiliary data
 * structures. Instances are inherently thread-safe.
 */
public class Tuple3<A, B, C> {

    public final A a;
    public final B b;
    public final C c;
    
    public Tuple3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
}
