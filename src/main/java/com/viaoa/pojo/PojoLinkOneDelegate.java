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

import java.util.ArrayList;
import java.util.List;

import com.viaoa.util.OAString;

/**
 * Helper methods for working with {@link PojoLinkOne} instances and
 * extracting the {@link PojoProperty} set that participates in link
 * matching.
 * <p>
 * The delegate exposes three views over a link-one definition:
 * <ul>
 *   <li>foreign-key properties ({@link #getLinkFkeyPojoProperties}),</li>
 *   <li>import-match properties ({@link #getImportMatchPojoProperties}), and</li>
 *   <li>unique properties ({@link #getLinkUniquePojoProperties}).</li>
 * </ul>
 * The combined view returned by {@link #getLinkOnePojoProperties} applies
 * the same precedence rules that the JSON import logic uses:
 * foreign keys &gt; import matches &gt; unique properties, recursively
 * following nested {@link PojoLinkOneReference} definitions where needed.
 */
public class PojoLinkOneDelegate {

	/**
	 * Returns the {@link PojoLinkOne} metadata for the named link on the given
	 * {@link Pojo}, ignoring case.
	 * <p>
	 * Returns {@code null} if the POJO is null, the name is empty, or no such
	 * link exists.
	 *
	 * @param pojo     the POJO metadata to search
	 * @param linkName the link name to locate
	 * @return the matching {@link PojoLinkOne}, or null if none
	 */
	public static PojoLinkOne getPojoLinkOne(Pojo pojo, String linkName) {
		if (pojo == null) {
			return null;
		}
		if (OAString.isEmpty(linkName)) {
			return null;
		}

		for (PojoLink pl : pojo.getPojoLinks()) {
			if (linkName.equalsIgnoreCase(pl.getName())) {
				return pl.getPojoLinkOne();
			}
		}
		return null;
	}

	/**
	 * Retrieves foreign-key POJO properties for the named link on a POJO.
	 * <p>
	 * Delegates to {@link #getPojoLinkOne} and then to
	 * {@link #getLinkFkeyPojoProperties(PojoLinkOne)}.
	 *
	 * @param pojo     the POJO metadata
	 * @param linkName the link name
	 * @return list of {@link PojoProperty} entries, or null if link missing
	 */
	public static List<PojoProperty> getLinkFkeyPojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getLinkFkeyPojoProperties(plo);
	}

	/**
	 * Returns the list of foreign-key {@link PojoProperty} values for a link-one
	 * definition.
	 *
	 * @param plo the link-one metadata
	 * @return list of POJO properties, or null if plo is null
	 */
	public static List<PojoProperty> getLinkFkeyPojoProperties(final PojoLinkOne plo) {
		if (plo == null) {
			return null;
		}
		List<PojoProperty> alPjp = new ArrayList<>();

		for (PojoLinkFkey plfk : plo.getPojoLinkFkeys()) {
			alPjp.add(plfk.getPojoProperty());
		}
		return alPjp;
	}

	/**
	 * Retrieves import-match POJO properties for a named link on a POJO.
	 * <p>
	 * Delegates to {@link #getPojoLinkOne} and then to
	 * {@link #getImportMatchPojoProperties(PojoLinkOne)}.
	 *
	 * @param pojo     the POJO metadata
	 * @param linkName the link name
	 * @return list of POJO properties, or null if link missing
	 */
	public static List<PojoProperty> getImportMatchPojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getImportMatchPojoProperties(plo);
	}

	/**
	 * Returns all POJO properties used for import-match comparisons for a link-one
	 * definition.
	 * <p>
	 * Handles both direct scalar import-match properties and nested
	 * {@link PojoLinkOneReference}-based matches.
	 *
	 * @param plo the link-one metadata
	 * @return list of POJO properties (never null)
	 */
	public static List<PojoProperty> getImportMatchPojoProperties(final PojoLinkOne plo) {
		List<PojoProperty> alPjp = new ArrayList<>();
		if (plo == null) {
			return alPjp;
		}

		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			PojoProperty pjp = pim.getPojoProperty();
			if (pjp != null) {
				alPjp.add(pjp);
			} else {
				PojoLinkOneReference plor = pim.getPojoLinkOneReference();
				if (plor != null) {
					PojoLinkOne plox = plor.getPojoLinkOne();
					_getLinkOnePojoProperties(plox, alPjp);
				}
			}
		}
		return alPjp;
	}

	/**
	 * Retrieves unique-property POJO values for a named link on a POJO.
	 * <p>
	 * Delegates to {@link #getPojoLinkOne} and then to
	 * {@link #getLinkUniquePojoProperties(PojoLinkOne)}.
	 *
	 * @param pojo     the POJO metadata
	 * @param linkName the link name
	 * @return list of POJO properties, or null if link missing
	 */
	public static List<PojoProperty> getLinkUniquePojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getLinkUniquePojoProperties(plo);
	}

	/**
	 * Returns POJO properties used for link-unique matching.
	 * <p>
	 * Handles direct unique properties and nested equal-property-path references.
	 *
	 * @param plo the link-one metadata
	 * @return list of unique-match POJO properties (never null)
	 */
	public static List<PojoProperty> getLinkUniquePojoProperties(final PojoLinkOne plo) {
		List<PojoProperty> alPjp = new ArrayList<>();
		if (plo == null) {
			return alPjp;
		}
		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu == null) {
			return alPjp;
		}

		PojoProperty pjp = plu.getPojoProperty();
		if (pjp != null) {
			alPjp.add(pjp);
		} else {
			PojoLinkOneReference plor = plu.getPojoLinkOneReference();
			if (plor != null) {
				PojoLinkOne plox = plor.getPojoLinkOne();
				_getLinkOnePojoProperties(plox, alPjp);
			}
		}
		return alPjp;
	}

	/**
	 * Returns all POJO properties that participate in matching a link-one:
	 * <ol>
	 *   <li>foreign-key properties,</li>
	 *   <li>import-match properties,</li>
	 *   <li>unique properties.</li>
	 * </ol>
	 * The method applies the same precedence used during JSON import.
	 *
	 * @param plo the link-one metadata
	 * @return ordered list of match-participating POJO properties
	 */
	public static List<PojoProperty> getLinkOnePojoProperties(final PojoLinkOne plo) {
		final List<PojoProperty> alPjp = new ArrayList<>();
		if (plo == null) {
			return alPjp;
		}
		_getLinkOnePojoProperties(plo, alPjp);
		return alPjp;
	}

	/**
	 * Internal recursive routine that populates the supplied list with the
	 * match-participating POJO properties for a link-one definition.
	 * <p>
	 * Precedence rules:
	 * <ol>
	 *   <li>If foreign-keys exist → use only those.</li>
	 *   <li>Else if import-matches exist → use all import-match keys (nested
	 *       definitions resolved recursively).</li>
	 *   <li>Else if a link-unique rule exists → use its properties (nested
	 *       definitions resolved recursively).</li>
	 * </ol>
	 *
	 * @param plo   the link-one metadata
	 * @param alPjp the output list to populate
	 */
	protected static void _getLinkOnePojoProperties(final PojoLinkOne plo, final List<PojoProperty> alPjp) {
		boolean b = false;
		for (PojoLinkFkey plfk : plo.getPojoLinkFkeys()) {
			alPjp.add(plfk.getPojoProperty());
			b = true;
		}
		if (b) {
			return;
		}

		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			b = true;
			PojoProperty pjp = pim.getPojoProperty();
			if (pjp != null) {
				alPjp.add(pjp);
			} else {
				PojoLinkOneReference plor = pim.getPojoLinkOneReference();
				if (plor != null) {
					PojoLinkOne plox = plor.getPojoLinkOne();
					_getLinkOnePojoProperties(plox, alPjp);
				}
			}
		}
		if (b) {
			return;
		}

		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu != null) {
			PojoProperty pjp = plu.getPojoProperty();
			if (pjp != null) {
				alPjp.add(pjp);
			} else {
				PojoLinkOneReference plor = plu.getPojoLinkOneReference();
				if (plor != null) {
					PojoLinkOne plox = plor.getPojoLinkOne();
					_getLinkOnePojoProperties(plox, alPjp);
				}
			}
		}
	}

}
