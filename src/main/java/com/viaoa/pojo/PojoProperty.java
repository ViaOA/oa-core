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
 * Represents a single scalar property in the POJO metadata model.
 * <p>
 * A {@code PojoProperty} corresponds to either:
 * <ul>
 *   <li>a regular OA property declared on the base type, or</li>
 *   <li>a synthetic property created for foreign keys, import-match
 *       properties, or unique-key definitions.</li>
 * </ul>
 * The {@link #keyPos} field indicates whether the property participates
 * in POJO-key matching and, if so, its position within a compound key.
 * Back-references to other POJO structures (import-match, fkey, unique)
 * identify the context in which the property is used.
 */
public class PojoProperty implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The property name as represented in the POJO metadata.
	 */
	protected volatile String name;
	
	/**
	 * Uppercase version of the property name, typically used for
	 * case-insensitive comparisons.
	 */
	protected volatile String upperName;
	
	/**
	 * Optional OA-style dotted property path that this POJO property
	 * represents, especially for synthetic properties produced by link-one
	 * references, unique keys, or import-match definitions.
	 */
	protected volatile String propertyPath;
	
	/**
	 * Fully qualified Java type name for this property, used when generating
	 * JSON-import values or reflective assignments.
	 */
	protected volatile String javaType;

	/**
	 * Position of this property in a compound POJO key.
	 * <p>
	 * A value of {@code 0} indicates that the property is not a key part.
	 */
	protected volatile int keyPos;

	/**
	 * Reference back to the {@link PojoImportMatch} that produced this
	 * synthetic POJO property, if applicable.
	 */
	protected volatile PojoImportMatch pojoImportMatch;

	/**
	 * Reference to the foreign-key metadata entry that created this
	 * POJO property, when the property represents a scalar fkey.
	 */
	protected volatile PojoLinkFkey pojoLinkFkey;

	/**
	 * Reference to a {@link PojoLinkUnique} definition when this property
	 * participates in a unique-key rule.
	 */
	protected volatile PojoLinkUnique pojoLinkUnique;

	/**
	 * Reference to the base-model {@link PojoRegularProperty} when the property
	 * corresponds directly to a declared scalar property.
	 */
	protected volatile PojoRegularProperty pojoRegularProperty;

	/**
	 * Constructs an empty {@code PojoProperty}.
	 */
	public PojoProperty() {
	}

	/**
	 * Returns the property name.
	 *
	 * @return the name value
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the property name.
	 *
	 * @param newValue the new property name
	 */
	public void setName(String newValue) {
		this.name = newValue;
	}

	/**
	 * Returns the uppercase form of the property name.
	 *
	 * @return uppercase name
	 */
	public String getUpperName() {
		return upperName;
	}

	/**
	 * Sets the uppercase name value.
	 *
	 * @param newValue the new uppercase representation
	 */
	public void setUpperName(String newValue) {
		this.upperName = newValue;
	}

	/**
	 * Returns the OA-style property path associated with this metadata
	 * property, if any.
	 *
	 * @return the property path or {@code null}
	 */
	public String getPropertyPath() {
		return propertyPath;
	}

	/**
	 * Assigns the OA-style property path for this property.
	 *
	 * @param newValue the new property path
	 */
	public void setPropertyPath(String newValue) {
		this.propertyPath = newValue;
	}

	/**
	 * Returns the fully qualified Java type for this property.
	 *
	 * @return Java type name
	 */
	public String getJavaType() {
		return javaType;
	}

	/**
	 * Sets the Java type name for this property.
	 *
	 * @param newValue the new Java type
	 */
	public void setJavaType(String newValue) {
		this.javaType = newValue;
	}

	/**
	 * Returns the position of this property within a compound key.
	 *
	 * @return key position, or {@code 0} if not a key part
	 */
	public int getKeyPos() {
		return keyPos;
	}

	/**
	 * Sets the key position for this property.
	 * <p>
	 * A value of {@code 0} indicates that the property is not part of a key.
	 *
	 * @param newValue the new key position
	 */
	public void setKeyPos(int newValue) {
		this.keyPos = newValue;
	}

	/**
	 * Returns the {@link PojoImportMatch} metadata associated with this
	 * property, if the property was created as part of an import-match rule.
	 *
	 * @return the import-match metadata or {@code null}
	 */
	@JsonIgnore
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	/**
	 * Assigns the {@link PojoImportMatch} metadata associated with this
	 * property.
	 *
	 * @param newValue the new import-match metadata
	 */
	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	/**
	 * Returns the {@link PojoLinkFkey} metadata that created this synthetic
	 * property when representing a foreign-key column.
	 *
	 * @return the foreign-key metadata or {@code null}
	 */
	@JsonIgnore
	public PojoLinkFkey getPojoLinkFkey() {
		return pojoLinkFkey;
	}

	/**
	 * Sets the {@link PojoLinkFkey} metadata for this property.
	 *
	 * @param newValue the new foreign-key metadata
	 */
	public void setPojoLinkFkey(PojoLinkFkey newValue) {
		this.pojoLinkFkey = newValue;
	}

	/**
	 * Returns the {@link PojoLinkUnique} metadata when this property
	 * participates in a unique-key rule.
	 *
	 * @return the unique-key metadata or {@code null}
	 */
	@JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	/**
	 * Assigns the {@link PojoLinkUnique} metadata for this property.
	 *
	 * @param newValue the new unique-key metadata
	 */
	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	/**
	 * Returns the {@link PojoRegularProperty} that this metadata property
	 * corresponds to when representing a base scalar property.
	 *
	 * @return the regular property metadata or {@code null}
	 */
	@JsonIgnore
	public PojoRegularProperty getPojoRegularProperty() {
		return pojoRegularProperty;
	}

	/**
	 * Sets the {@link PojoRegularProperty} associated with this metadata
	 * property.
	 *
	 * @param newValue the new regular-property metadata
	 */
	public void setPojoRegularProperty(PojoRegularProperty newValue) {
		this.pojoRegularProperty = newValue;
	}

	/**
	 * Returns a string representation containing the property name and
	 * key position.
	 *
	 * @return formatted string representation
	 */
	@Override
	public String toString() {
		return "PojoProperty [" +
				"name=" + name +
				", upperName=" + upperName +
				", propertyPath=" + propertyPath +
				", javaType=" + javaType +
				"]";
	}
}
