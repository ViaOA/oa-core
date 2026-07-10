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
 * Describes a single link property on a {@link Pojo}.
 * <p>
 * A {@code PojoLink} is the root for either:
 * <ul>
 *   <li>a {@link PojoLinkOne} (for {@code TYPE_ONE} associations), or</li>
 *   <li>a {@link PojoLinkMany} (for {@code TYPE_MANY} associations).</li>
 * </ul>
 * The name corresponds to the OA link name on the source {@code OAObject}
 * type. The {@link OAObjectPojoLoader} populates this structure when it
 * converts {@link com.viaoa.metadata.OAObjectInfo} metadata into POJO
 * descriptors.
 */
public class PojoLink implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Name of the link property, matching the OAObject link definition.
	 */
	protected volatile String name;

	/**
	 * Reference to the owning {@link Pojo} metadata object.
	 */
	protected volatile Pojo pojo;

	/**
	 * Link-many metadata structure when this link represents a one-to-many
	 * association.
	 */
	protected volatile PojoLinkMany pojoLinkMany;

	/**
	 * Link-one metadata structure when this link represents a one-to-one
	 * association.
	 */
	protected volatile PojoLinkOne pojoLinkOne;

	/**
	 * Creates an empty {@code PojoLink} instance.
	 */
	public PojoLink() {
	}

	/**
	 * Returns the link name associated with this POJO link metadata.
	 *
	 * @return the link name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the link name.
	 *
	 * @param newValue the new link name
	 */
	public void setName(String newValue) {
		this.name = newValue;
	}

	/**
	 * Returns the {@link Pojo} metadata object that owns this link definition.
	 *
	 * @return the owning {@link Pojo}
	 */
	public Pojo getPojo() {
		return pojo;
	}

	/**
	 * Assigns the {@link Pojo} metadata object that owns this link definition.
	 *
	 * @param newValue the owning POJO metadata
	 */
	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	/**
	 * Returns the {@link PojoLinkMany} metadata for a one-to-many association.
	 *
	 * @return the link-many metadata, or {@code null} if this link is not many-valued
	 */
	//@JsonIgnore
	/**
	 * Returns the many-link metadata specialization for this link.
	 *
	 * @return mapped metadata reference, or {@code null}
	 */
	public PojoLinkMany getPojoLinkMany() {
		return pojoLinkMany;
	}

	/**
	 * Sets the {@link PojoLinkMany} metadata structure.
	 *
	 * @param newValue the new metadata for a many-valued link
	 */
	public void setPojoLinkMany(PojoLinkMany newValue) {
		this.pojoLinkMany = newValue;
	}

	/**
	 * Returns the {@link PojoLinkOne} metadata for a one-to-one association.
	 *
	 * @return the link-one metadata, or {@code null} if this link is not one-valued
	 */
	//@JsonIgnore
	/**
	 * Returns the one-link metadata specialization for this link.
	 *
	 * @return mapped metadata reference, or {@code null}
	 */
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	/**
	 * Sets the {@link PojoLinkOne} metadata structure.
	 *
	 * @param newValue the new metadata for a one-valued link
	 */
	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	/**
	 * Returns a simple string representation for debugging.
	 *
	 * @return string form of this {@code PojoImportMatch}
	 */
	@Override
	public String toString() {
		return "PojoLink [" +
				"name=" + name +
				"]";
	}
}
