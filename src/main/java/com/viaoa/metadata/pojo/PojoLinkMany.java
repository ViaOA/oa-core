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
 * Marker object for a {@code TYPE_MANY} association on a {@link Pojo}.
 * <p>
 * A {@link PojoLinkMany} simply indicates that the owning {@link PojoLink}
 * represents a to-many relationship. All key / import-match handling is
 * done on the one-side ({@link PojoLinkOne}); the many-side structure is
 * currently just a placeholder for completeness and future extensions.
 */
public class PojoLinkMany implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Reference to the owning {@link PojoLink} metadata, which represents the
	 * to-many association.
	 */
	protected volatile PojoLink pojoLink;

	/**
	 * Creates an empty {@code PojoLinkMany} metadata instance.
	 */
	public PojoLinkMany() {
	}

	/**
	 * Returns the {@link PojoLink} metadata object that owns this to-many
	 * definition.
	 *
	 * @return the owning {@link PojoLink}, or {@code null} if not assigned
	 */
	public PojoLink getPojoLink() {
		return pojoLink;
	}

	/**
	 * Sets the owning {@link PojoLink} for this to-many association metadata.
	 *
	 * @param newValue the new link metadata reference
	 */
	public void setPojoLink(PojoLink newValue) {
		this.pojoLink = newValue;
	}

	/**
	 * Returns a simple string representation for debugging.
	 *
	 * @return formatted string description of this {@code PojoLinkMany}
	 */
	@Override
	public String toString() {
		return "PojoLinkMany [" +
				"]";
	}
}
