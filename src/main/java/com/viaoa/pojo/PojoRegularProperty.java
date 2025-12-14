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
package com.viaoa.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Wrapper for a regular scalar property declared directly on a POJO type.
 * <p>
 * A {@code PojoRegularProperty} pairs a {@link Pojo} with its associated
 * {@link PojoProperty}. Unlike foreign-key, import-match, or unique-key
 * properties, regular properties do not participate in POJO-key matching.
 */
public class PojoRegularProperty implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Reference to the {@link Pojo} that owns this regular property.
	 */
	protected volatile Pojo pojo;

	/**
	 * Reference to the {@link PojoProperty} representing the scalar property
	 * declared directly on the base POJO type.
	 */
	protected volatile PojoProperty pojoProperty;

	/**
	 * Constructs an empty {@code PojoRegularProperty} instance.
	 */
	public PojoRegularProperty() {
	}

	/**
	 * Returns the {@link Pojo} that owns this regular property.
	 *
	 * @return the owning POJO metadata, or {@code null} if not set
	 */
	@JsonIgnore
	public Pojo getPojo() {
		return pojo;
	}

	/**
	 * Sets the {@link Pojo} that owns this regular property.
	 *
	 * @param newValue the new owning POJO metadata
	 */
	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	/**
	 * Returns the {@link PojoProperty} that this regular property wraps.
	 *
	 * @return the underlying POJO property definition
	 */
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	/**
	 * Assigns the {@link PojoProperty} wrapped by this regular property.
	 *
	 * @param newValue the new POJO property metadata
	 */
	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	/**
	 * Returns a simple string representation of this metadata object.
	 *
	 * @return formatted string representation
	 */
	@Override
	public String toString() {
		return "PojoRegularProperty [" +
				"]";
	}
}
