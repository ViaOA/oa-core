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
 * Describes a scalar foreign-key property that participates in a
 * {@link PojoLinkOne} association.
 * <p>
 * Each instance represents one OA foreign-key column on the source
 * {@code OAObject} type and the corresponding {@link PojoProperty} in
 * the generated POJO model. These are used as part of the key-matching
 * logic when resolving link-one relationships during JSON import.
 */
public class PojoLinkFkey implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoLinkFkey() {
	}

	@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
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
		return "PojoLinkFkey [" +
				"]";
	}
}
