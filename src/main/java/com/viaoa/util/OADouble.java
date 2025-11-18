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
 * Mutable wrapper for a {@code double} value with an optional “set” flag used
 * throughout OA for accumulation and by-reference numeric semantics. An
 * {@code OADouble} can be queried to determine whether a value has been
 * explicitly assigned using {@link #isSet()}, which allows callers to
 * distinguish between an uninitialized value and a value that is legitimately
 * zero. <p>
 *
 * The stored value can be modified through {@link #set(double)} or by using
 * the {@link #add(double)} and {@link #subtract(int)} convenience methods,
 * which update the internal value and return the updated result. This class is
 * not synchronized and is intended for single-threaded or externally managed
 * use.
 */
public class OADouble {
    private double x;
    private boolean bIsSet;
    
    public OADouble() {
    }
    public OADouble(double x) {
        this.x = x;
        bIsSet = true;
    }
    
    public void set(double x) {
        this.x = x;
        bIsSet = true;
    }
    public double get() {
        return x;
    }
    
    public double add(double x) {
        this.x += x;
        return this.x;
    }
    public double add() {
        return this.add(1);
    }
    public double subtract(int x) {
        this.x -= x;
        return this.x;
    }
    public double subtract() {
        return this.subtract(1);
    }

    public boolean isSet() {
        return bIsSet;
    }
    
}
