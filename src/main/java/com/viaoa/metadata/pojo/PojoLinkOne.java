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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Describes the "one" side of a link property in the POJO metadata model.
 * <p>
 * A {@code PojoLinkOne} may carry:
 * <ul>
 *   <li>one or more {@link PojoLinkFkey} entries that represent scalar
 *       foreign-key properties on the source type,</li>
 *   <li>zero or more {@link PojoImportMatch} entries that describe
 *       alternative import-match paths, and</li>
 *   <li>an optional {@link PojoLinkUnique} definition describing a unique
 *       property (possibly reachable via {@code equalPath})
 *       that can be used for matching.</li>
 * </ul>
 * The {@link OAObjectPojoLoader} populates this structure based on
 * {@link com.viaoa.metadata.OALinkInfo} metadata.
 */
public class PojoLinkOne implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Reference to the owning {@link PojoLink} that represents the link property
	 * in the parent {@link Pojo}.
	 */
	protected volatile PojoLink pojoLink;

	/**
	 * Optional {@link PojoLinkUnique} metadata describing unique-property rules
	 * associated with this link-one definition.
	 */
	protected volatile PojoLinkUnique pojoLinkUnique;

	/**
	 * List of {@link PojoImportMatch} definitions describing import-match
	 * strategies for resolving this link during JSON import.
	 */
	protected volatile CopyOnWriteArrayList<PojoImportMatch> alPojoImportMatches = new CopyOnWriteArrayList<>();

	/**
	 * List of {@link PojoLinkFkey} entries representing scalar foreign-key
	 * properties on the source type used to resolve this link-one relationship.
	 */
	protected volatile CopyOnWriteArrayList<PojoLinkFkey> alPojoLinkFkeys = new CopyOnWriteArrayList<>();

	/**
	 * Creates an empty {@code PojoLinkOne} metadata instance.
	 */
	public PojoLinkOne() {
	}

	/**
	 * Returns the {@link PojoLink} that owns this link-one definition.
	 *
	 * @return the parent link metadata
	 */
	public PojoLink getPojoLink() {
		return pojoLink;
	}

	/**
	 * Sets the owning {@link PojoLink} for this link-one metadata entry.
	 *
	 * @param newValue the new parent link metadata reference
	 */
	public void setPojoLink(PojoLink newValue) {
		this.pojoLink = newValue;
	}

	/**
	 * Returns the {@link PojoLinkUnique} metadata associated with this link-one
	 * definition, if any.
	 *
	 * @return the unique-property metadata or {@code null} if not defined
	 */
	// @JsonIgnore
	/**
	 * Returns the unique-link metadata used to resolve this one-link.
	 *
	 * @return mapped metadata reference, or {@code null}
	 */
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	/**
	 * Sets the {@link PojoLinkUnique} metadata describing unique-property rules
	 * for this link-one association.
	 *
	 * @param newValue the new unique-property metadata
	 */
	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	/**
	 * Returns the list of {@link PojoImportMatch} entries associated with this
	 * link-one definition.
	 *
	 * @return list of import-match metadata
	 */
	public CopyOnWriteArrayList<PojoImportMatch> getPojoImportMatches() {
		return alPojoImportMatches;
	}

	/**
	 * Replaces the list of {@link PojoImportMatch} entries.
	 * <p>
	 * If {@code list} is null, the existing list is cleared; otherwise a new
	 * thread-safe list is created from the supplied entries.
	 *
	 * @param list the new import-match list or null to clear
	 */
	public void setPojoImportMatches(List<PojoImportMatch> list) {
		if (list == null) {
			this.alPojoImportMatches.clear();
		} else {
			this.alPojoImportMatches = new CopyOnWriteArrayList<>(list);
		}
	}

	/**
	 * Returns the list of {@link PojoLinkFkey} foreign-key mappings associated
	 * with this link-one definition.
	 *
	 * @return list of fkey metadata entries
	 */
	public CopyOnWriteArrayList<PojoLinkFkey> getPojoLinkFkeys() {
		return alPojoLinkFkeys;
	}

	/**
	 * Replaces the list of {@link PojoLinkFkey} entries.
	 * <p>
	 * If {@code list} is null, the existing list is cleared; otherwise the list
	 * is replaced with a thread-safe copy.
	 *
	 * @param list the new list of fkey metadata or null to clear
	 */
	public void setPojoLinkFkeys(List<PojoLinkFkey> list) {
		if (list == null) {
			this.alPojoLinkFkeys.clear();
		} else {
			this.alPojoLinkFkeys = new CopyOnWriteArrayList<>(list);
		}
	}

	/**
	 * Returns a simple string representation for debugging.
	 *
	 * @return formatted metadata description string
	 */
	@Override
	public String toString() {
		return "PojoLinkOne [" +
				"]";
	}
}
