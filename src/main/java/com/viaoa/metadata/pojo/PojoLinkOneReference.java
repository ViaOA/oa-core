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
package com.viaoa.metadata.pojo;

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

	/**
	 * Optional name describing this reference step. Used mainly for debugging
	 * and traceability when navigating nested link-one chains.
	 */
	protected volatile String name;

	/**
	 * Optional {@link PojoImportMatch} representing an import-match rule
	 * originating from this reference step.
	 */
	protected volatile PojoImportMatch pojoImportMatch;

	/**
	 * Optional {@link PojoLinkOne} representing a link-one relationship that
	 * should be recursively expanded by matching logic.
	 */
	protected volatile PojoLinkOne pojoLinkOne;

	/**
	 * Optional {@link PojoLinkUnique} definition attached to this reference,
	 * used to resolve unique-property match keys.
	 */
	protected volatile PojoLinkUnique pojoLinkUnique;

	/**
	 * Constructs an empty {@code PojoLinkOneReference}.
	 */
	public PojoLinkOneReference() {
	}

	/**
	 * Returns the debugging/descriptive name for this reference.
	 *
	 * @return the reference name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the descriptive name for this reference.
	 *
	 * @param newValue the new name value
	 */
	public void setName(String newValue) {
		this.name = newValue;
	}

	/**
	 * Returns the {@link PojoImportMatch} associated with this reference,
	 * if any.
	 *
	 * @return the import-match definition or {@code null}
	 */
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	/**
	 * Assigns the {@link PojoImportMatch} associated with this reference step.
	 *
	 * @param newValue the new import-match metadata
	 */
	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	/**
	 * Returns the {@link PojoLinkOne} that this reference points to.
	 * <p>
	 * This method is intentionally not annotated with {@code @JsonIgnore},
	 * allowing JSON frameworks to access the link-one metadata if needed.
	 *
	 * @return the link-one metadata or {@code null}
	 */
	// @JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	/**
	 * Assigns the {@link PojoLinkOne} metadata for this reference.
	 *
	 * @param newValue the new link-one metadata
	 */
	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	/**
	 * Returns the {@link PojoLinkUnique} associated with this reference,
	 * if any.
	 *
	 * @return the unique-match metadata or {@code null}
	 */
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	/**
	 * Assigns the {@link PojoLinkUnique} metadata for this reference.
	 *
	 * @param newValue the new unique-property metadata
	 */
	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	/**
	 * Returns a string representation including the reference name.
	 *
	 * @return string form of this reference
	 */
	@Override
	public String toString() {
		return "PojoLinkOneReference [" +
				"name=" + name +
				"]";
	}
}
