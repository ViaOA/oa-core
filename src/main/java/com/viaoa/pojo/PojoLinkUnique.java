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

	/**
	 * The owning {@link PojoLinkOne} to which this unique-key rule applies.
	 */
	protected volatile PojoLinkOne pojoLinkOne;

	/**
	 * Optional nested {@link PojoLinkOneReference} used when uniqueness is
	 * defined through a related association rather than a direct scalar property.
	 */
	protected volatile PojoLinkOneReference pojoLinkOneReference;

	/**
	 * Optional direct scalar {@link PojoProperty} that participates in this
	 * unique-key definition.
	 */
	protected volatile PojoProperty pojoProperty;

	/**
	 * Constructs an empty {@code PojoLinkUnique} definition.
	 */
	public PojoLinkUnique() {
	}

	/**
	 * Returns the {@link PojoLinkOne} that owns this unique-key definition.
	 *
	 * @return the owning link-one metadata, or {@code null} if not set
	 */
	@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	/**
	 * Assigns the {@link PojoLinkOne} that owns this unique-key definition.
	 *
	 * @param newValue the new link-one metadata reference
	 */
	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	/**
	 * Returns the nested {@link PojoLinkOneReference} used to resolve a unique
	 * key through a related type.
	 *
	 * @return nested reference metadata, or {@code null} if not defined
	 */
	// @JsonIgnore
	public PojoLinkOneReference getPojoLinkOneReference() {
		return pojoLinkOneReference;
	}

	/**
	 * Assigns the nested {@link PojoLinkOneReference} used to resolve the unique
	 * key through another link-one association.
	 *
	 * @param newValue the new nested reference metadata
	 */
	public void setPojoLinkOneReference(PojoLinkOneReference newValue) {
		this.pojoLinkOneReference = newValue;
	}

	/**
	 * Returns the direct {@link PojoProperty} used as the unique-key value,
	 * if one is defined.
	 *
	 * @return the direct scalar property or {@code null}
	 */
	// @JsonIgnore
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	/**
	 * Assigns the direct scalar {@link PojoProperty} for this unique-key rule.
	 *
	 * @param newValue the new unique-key property
	 */
	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	/**
	 * Returns a simple string representation of this unique-key metadata.
	 *
	 * @return formatted string representation
	 */
	@Override
	public String toString() {
		return "PojoLinkUnique [" +
				"]";
	}
}
