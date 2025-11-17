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

	protected volatile String name;
	protected volatile String upperName;
	protected volatile String propertyPath;
	protected volatile String javaType;
	protected volatile int keyPos;

	// References to other objects
	// PojoImportMatch
	protected volatile PojoImportMatch pojoImportMatch;
	// PojoLinkFkey
	protected volatile PojoLinkFkey pojoLinkFkey;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;
	// PojoRegularProperty
	protected volatile PojoRegularProperty pojoRegularProperty;

	public PojoProperty() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public String getUpperName() {
		return upperName;
	}

	public void setUpperName(String newValue) {
		this.upperName = newValue;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public void setPropertyPath(String newValue) {
		this.propertyPath = newValue;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(String newValue) {
		this.javaType = newValue;
	}

	public int getKeyPos() {
		return keyPos;
	}

	public void setKeyPos(int newValue) {
		this.keyPos = newValue;
	}

	@JsonIgnore
	public PojoImportMatch getPojoImportMatch() {
		return pojoImportMatch;
	}

	public void setPojoImportMatch(PojoImportMatch newValue) {
		this.pojoImportMatch = newValue;
	}

	@JsonIgnore
	public PojoLinkFkey getPojoLinkFkey() {
		return pojoLinkFkey;
	}

	public void setPojoLinkFkey(PojoLinkFkey newValue) {
		this.pojoLinkFkey = newValue;
	}

	@JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	@JsonIgnore
	public PojoRegularProperty getPojoRegularProperty() {
		return pojoRegularProperty;
	}

	public void setPojoRegularProperty(PojoRegularProperty newValue) {
		this.pojoRegularProperty = newValue;
	}

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
