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
 * Describes a unique-key definition for a {@link PojoLinkOne}. A unique key
 * can either be:
 * <ul>
 *   <li>a direct scalar {@link PojoProperty}, or</li>
 *   <li>a nested {@link PojoLinkOneReference} leading to such a property on
 *       a related type.</li>
 * </ul>
 * This structure supports POJO-key resolution using
 * {@code equalPropertyPath}-based uniqueness on associations.
 */
public class PojoLinkUnique implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoLinkOneReference
	protected volatile PojoLinkOneReference pojoLinkOneReference;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoLinkUnique() {
	}

	@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	// @JsonIgnore
	public PojoLinkOneReference getPojoLinkOneReference() {
		return pojoLinkOneReference;
	}

	public void setPojoLinkOneReference(PojoLinkOneReference newValue) {
		this.pojoLinkOneReference = newValue;
	}

	// @JsonIgnore
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	@Override
	public String toString() {
		return "PojoLinkUnique [" +
				"]";
	}
}
