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

	// References to other objects
	// Pojo
	protected volatile Pojo pojo;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoRegularProperty() {
	}

	@JsonIgnore
	public Pojo getPojo() {
		return pojo;
	}

	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	@Override
	public String toString() {
		return "PojoRegularProperty [" +
				"]";
	}
}
