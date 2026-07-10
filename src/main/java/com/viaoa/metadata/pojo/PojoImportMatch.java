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

	/**
	 * The owning {@link PojoLinkOne} for which this import-match rule is defined.
	 */
	protected volatile PojoLinkOne pojoLinkOne;
	
	/**
	 * Optional nested {@link PojoLinkOneReference} describing an indirect path
	 * used for import matching when the match is not a simple scalar property.
	 */
	protected volatile PojoLinkOneReference pojoLinkOneReference;
	
	/**
	 * Optional direct {@link PojoProperty} that participates in the import-match
	 * criteria.
	 */
	protected volatile PojoProperty pojoProperty;

	/**
	 * Creates an empty {@code PojoImportMatch} definition.
	 */
	public PojoImportMatch() {
	}

	/**
	 * Returns the {@link PojoLinkOne} that owns this import-match definition.
	 *
	 * @return the owning {@link PojoLinkOne}, or {@code null} if not set
	 */
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	/**
	 * Sets the owning {@link PojoLinkOne} for this import-match rule.
	 *
	 * @param newValue the new owning {@link PojoLinkOne}
	 */
	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	/**
	 * Returns the nested {@link PojoLinkOneReference} used for indirect
	 * import-match resolution, if any.
	 *
	 * @return the nested link-one reference, or {@code null} if not defined
	 */
	//@JsonIgnore
	/**
	 * Returns the one-link reference that supplies this import-match value.
	 *
	 * @return mapped metadata reference, or {@code null}
	 */
	public PojoLinkOneReference getPojoLinkOneReference() {
		return pojoLinkOneReference;
	}

	/**
	 * Sets the nested {@link PojoLinkOneReference} used for indirect
	 * import-match resolution.
	 *
	 * @param newValue the new nested link-one reference
	 */
	public void setPojoLinkOneReference(PojoLinkOneReference newValue) {
		this.pojoLinkOneReference = newValue;
	}

	/**
	 * Returns the direct {@link PojoProperty} that participates in this
	 * import-match rule, if one is defined.
	 *
	 * @return the import-match property, or {@code null} if not defined
	 */
	// @JsonIgnore
	/**
	 * Returns the POJO property that supplies this import-match value.
	 *
	 * @return mapped metadata reference, or {@code null}
	 */
	public PojoProperty getPojoProperty() {
		return pojoProperty;
	}

	/**
	 * Sets the direct {@link PojoProperty} used for this import-match rule.
	 *
	 * @param newValue the new import-match property
	 */
	public void setPojoProperty(PojoProperty newValue) {
		this.pojoProperty = newValue;
	}

	/**
	 * Returns a simple string representation for debugging.
	 *
	 * @return string form of this {@code PojoImportMatch}
	 */
	@Override
	public String toString() {
		return "PojoImportMatch [" +
				"]";
	}
}
