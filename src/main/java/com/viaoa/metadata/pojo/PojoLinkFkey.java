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

	/**
	 * The {@link PojoLinkOne} metadata object that owns this foreign-key entry.
	 */
	protected volatile PojoLinkOne pojoLinkOne;

	/**
	 * The {@link PojoProperty} representing the POJO-side scalar value that maps
	 * to the underlying foreign-key column.
	 */
	protected volatile PojoProperty pojoProperty;

	/**
	 * Creates an empty {@code PojoLinkFkey} metadata instance.
	 */
	public PojoLinkFkey() {
	}

	/**
	 * Returns the owning {@link PojoLinkOne} metadata object.
	 *
	 * @return the owning link-one metadata, or {@code null} if not assigned
	 */
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	/**
	 * Sets the owning {@link PojoLinkOne} metadata object.
	 *
	 * @param newValue the new link-one metadata reference
	 */
	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	/**
	 * Returns the {@link PojoProperty} that maps to the source foreign-key
	 * column for this association.
	 *
	 * @return the property metadata, or {@code null} if not assigned
	 */
	// @JsonIgnore
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	/**
	 * Sets the {@link PojoProperty} metadata representing the foreign-key value.
	 *
	 * @param newValue the new POJO property metadata
	 */
	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	/**
	 * Returns a simple string representation of this foreign-key mapping.
	 *
	 * @return formatted string for debugging
	 */
	@Override
	public String toString() {
		return "PojoLinkFkey [" +
				"]";
	}
}
