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
 * Describes an "import match" strategy for a {@link PojoLinkOne}.
 * <p>
 * An import match defines how a related {@code OAObject} can be located
 * when the JSON does not carry a primary key for the target type. The
 * match can either be:
 * <ul>
 *   <li>a direct scalar {@link PojoProperty}, or</li>
 *   <li>a nested {@link PojoLinkOneReference} that ultimately leads to one
 *       or more scalar properties used for matching.</li>
 * </ul>
 * Instances of this class are created by {@link OAObjectPojoLoader} based
 * on {@code importMatch} flags on {@code OAPropertyInfo} /
 * {@code OALinkInfo}.
 */
public class PojoImportMatch implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoLinkOneReference
	protected volatile PojoLinkOneReference pojoLinkOneReference;
	// PojoProperty
	protected volatile PojoProperty pojoProperty;

	public PojoImportMatch() {
	}

	@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	//@JsonIgnore
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
		return "PojoImportMatch [" +
				"]";
	}
}
