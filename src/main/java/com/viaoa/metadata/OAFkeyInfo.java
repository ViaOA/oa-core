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
package com.viaoa.metadata;

import com.viaoa.annotation.OAFkey;

/**
 * Metadata that defines a foreign-key to primary/business-key mapping used by
 * {@link OALinkInfo} for ONE-side relationships. Each instance pairs a source
 * (foreign-key) property with its target (primary/unique-key) property so the
 * OA runtime can reconcile references and perform identity-safe lookups.
 *
 * <p>The mapping originates from {@link com.viaoa.annotation.OAFkey} on the
 * model and is consulted during lazy loading and reverse-link fix-up to ensure
 * that the correct target object is resolved without requiring full graph
 * materialization.</p>
 *
 * @see OALinkInfo
 * @see OAPropertyInfo
 * @see com.viaoa.annotation.OAFkey
 */
public class OAFkeyInfo implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	/**
	 * Metadata for the source (foreign-key) property participating in the
	 * foreign-key mapping.
	 */
	private OAPropertyInfo fromPropertyInfo;
	
	/**
	 * Metadata for the target property referenced by this foreign-key
	 * relationship, typically a primary or unique key property.
	 */
	private OAPropertyInfo toPropertyInfo;

	/**
	 * The {@link OAFkey} annotation instance defining this foreign-key
	 * relationship.
	 */
	private OAFkey oaFkey;

	/**
	 * Returns the source (foreign-key) property participating in the
	 * foreign-key mapping.
	 *
	 * @return the foreign-key property metadata
	 */
	public OAPropertyInfo getFromPropertyInfo() {
		return fromPropertyInfo;
	}

	/**
	 * Sets the source (foreign-key) property for this mapping.
	 *
	 * @param pi the foreign-key property metadata
	 */
	public void setFromPropertyInfo(OAPropertyInfo pi) {
		this.fromPropertyInfo = pi;
	}

	/**
	 * Returns the target (primary-key or unique-key) property referenced by
	 * this foreign-key mapping.
	 *
	 * @return the target property metadata
	 */
	public OAPropertyInfo getToPropertyInfo() {
		return toPropertyInfo;
	}

	/**
	 * Sets the target (primary-key or unique-key) property for this mapping.
	 *
	 * @param pi the target property metadata
	 */
	public void setToPropertyInfo(OAPropertyInfo pi) {
		this.toPropertyInfo = pi;
	}

	/**
	 * Assigns the {@link OAFkey} annotation instance that defines this
	 * foreign-key relationship.
	 *
	 * @param f the associated {@code OAFkey} annotation
	 */
	public void setOAFkey(OAFkey f) {
		oaFkey = f;
	}

	/**
	 * Returns the {@link OAFkey} annotation associated with this foreign-key
	 * mapping.
	 *
	 * @return the {@code OAFkey} annotation, or {@code null} if none assigned
	 */
	public OAFkey getOAFkey() {
		return oaFkey;
	}
}
