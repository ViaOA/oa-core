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
 * Represents an intermediate step in a nested link-one reference path.
 * <p>
 * A {@code PojoLinkOneReference} is used when an import-match or unique
 * constraint refers not to a direct scalar property but to a property
 * reachable through another {@link PojoLinkOne}. This allows POJO key
 * matching logic to follow multi-hop association chains.
 * <p>
 * Only one of {@link #pojoImportMatch}, {@link #pojoLinkOne}, or
 * {@link #pojoLinkUnique} is expected to be populated for a given
 * instance. The {@link PojoLinkOneDelegate} resolves these references
 * recursively to collect the effective set of key {@link PojoProperty}
 * instances.
 */
public class PojoLinkOneReference implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// PojoImportMatch
	protected volatile PojoImportMatch pojoImportMatch;
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;

	public PojoLinkOneReference() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	@JsonIgnore
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	// @JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	@JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	@Override
	public String toString() {
		return "PojoLinkOneReference [" +
				"name=" + name +
				"]";
	}
}
