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
    
	/**
	 * Stores the current numeric value held by this instance.
	 */
	private double x;
    
	/**
	 * Indicates whether a value has been explicitly assigned.
	 */
	private boolean bIsSet;
    
	/**
	 * Creates a new instance with the default numeric value.
	 */
    public OADouble() {
    }

    /**
     * Creates a new instance initialized with the given value.
     *
     * @param x the initial numeric value
     */
    public OADouble(double x) {
        this.x = x;
        bIsSet = true;
    }
    
    /**
     * Assigns a new numeric value and marks it as set.
     *
     * @param x the value to assign
     */
    public void set(double x) {
        this.x = x;
        bIsSet = true;
    }

    /**
     * Returns the current numeric value.
     *
     * @return the stored value
     */
    public double get() {
        return x;
    }
    
    /**
     * Adds the given amount to the current value and returns the result.
     *
     * @param x the amount to add
     * @return the updated value
     */
    public double add(double x) {
        this.x += x;
        return this.x;
    }

    /**
     * Delegates to {@link #add(double)}.
     */
    public double add() {
        return this.add(1);
    }

    /**
     * Subtracts the given amount from the current value and returns the result.
     *
     * @param x the amount to subtract
     * @return the updated value
     */
    public double subtract(int x) {
        this.x -= x;
        return this.x;
    }

    /**
     * Delegates to {@link #subtract(int)}.
     */
    public double subtract() {
        return this.subtract(1);
    }

    /**
     * Returns whether a value has been explicitly assigned.
     *
     * @return true if a value has been set
     */
    public boolean isSet() {
        return bIsSet;
    }
    
}
