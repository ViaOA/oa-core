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

import java.util.ArrayList;
import java.util.List;

import com.viaoa.lang.OAString;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;

/**
 * Utility methods for querying {@link Pojo} metadata and interpreting which
 * POJO properties participate in key-matching logic.
 * <p>
 * This delegate provides:
 * <ul>
 *   <li>simple lookups for {@link PojoProperty}, {@link PojoRegularProperty}
 *       and {@link PojoLink} by name,</li>
 *   <li>helpers to retrieve all {@link PojoProperty} instances or only those
 *       that are marked as key parts (via {@code keyPos}), and</li>
 *   <li>{@link com.viaoa.metadata.OAObjectInfo}-aware helpers to determine
 *       whether a type has:
 *       <ul>
 *         <li>a primary-key based POJO key,</li>
 *         <li>an import-match based POJO key, or</li>
 *         <li>a "link unique" POJO key derived through associations.</li>
 *       </ul>
 *   </li>
 * </ul>
 * The key-marking is performed by {@link OAObjectPojoLoader} when the
 * {@link Pojo} model is created; this delegate only reads the resulting
 * {@code keyPos} flags to answer higher-level questions.
 */
public class PojoDelegate {

	/**
	 * Returns the {@link PojoProperty} for the given name, ignoring case.
	 * <p>
	 * Searches regular and link-based POJO properties. Returns {@code null}
	 * if no match exists, or if {@code pojo} or {@code name} is null/empty.
	 *
	 * @param pojo the POJO metadata to inspect
	 * @param name name of the property to locate
	 * @return the matching {@link PojoProperty}, or null if not found
	 */
	public static PojoProperty getPojoProperty(Pojo pojo, String name) {
		if (pojo == null || OAString.isEmpty(name)) {
			return null;
		}
		for (PojoProperty pp : getPojoProperties(pojo)) {
			if (name.equalsIgnoreCase(pp.getName())) {
				return pp;
			}
		}
		return null;
	}

	/**
	 * Finds a {@link PojoRegularProperty} by POJO property name.
	 * <p>
	 * Returns {@code null} when the POJO or name is null/empty, or when
	 * no matching regular property exists.
	 *
	 * @param pojo the POJO metadata to inspect
	 * @param name name of the property to locate
	 * @return the matching {@link PojoRegularProperty}, or null if not found
	 */
	public static PojoRegularProperty getPojoRegularProperty(Pojo pojo, String name) {
		if (pojo == null || OAString.isEmpty(name)) {
			return null;
		}
		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			PojoProperty pp = prp.getPojoProperty();
			if (name.equalsIgnoreCase(pp.getName())) {
				return prp;
			}
		}
		return null;
	}

	/**
	 * Finds a {@link PojoLink} (link-one or link-many) by name.
	 *
	 * @param pojo the POJO metadata to inspect
	 * @param name case-insensitive name of the link
	 * @return matching {@link PojoLink}, or null if none found
	 */
	public static PojoLink getPojoLink(Pojo pojo, String name) {
		if (pojo == null || OAString.isEmpty(name)) {
			return null;
		}
		for (PojoLink pl : pojo.getPojoLinks()) {
			if (name.equalsIgnoreCase(pl.getName())) {
				return pl;
			}
		}
		return null;
	}

	/**
	 * Determines whether the POJO defines at least one property marked
	 * as participating in key logic ({@code keyPos > 0}).
	 *
	 * @param pojo the POJO metadata
	 * @return true if at least one key property exists
	 */
	public static boolean hasKey(Pojo pojo) {
		return getPojoProperties(pojo, true).size() > 0;
	}

	/**
	 * Determines whether the POJO has more than one key property.
	 *
	 * @param pojo the POJO metadata
	 * @return true if multiple key properties are defined
	 */
	public static boolean hasCompoundKey(Pojo pojo) {
		return getPojoProperties(pojo, true).size() > 1;
	}

	/**
	 * Returns all POJO properties, including nested link-one-derived properties.
	 *
	 * @param pojo the POJO metadata
	 * @return list of all {@link PojoProperty} entries
	 */
	public static List<PojoProperty> getPojoProperties(Pojo pojo) {
		return getPojoProperties(pojo, false);
	}

	/**
	 * Returns all POJO properties that participate in key logic
	 * ({@code keyPos > 0}), sorted by ascending key position.
	 *
	 * @param pojo the POJO metadata
	 * @return list of key POJO properties
	 */
	public static List<PojoProperty> getPojoPropertyKeys(Pojo pojo) {
		List<PojoProperty> al = getPojoProperties(pojo, true);
		return al;
	}

	/**
	 * Retrieves POJO properties, optionally restricting to key-only properties.
	 * <p>
	 * Delegates to the private recursive collector and sorts results
	 * by {@code keyPos}.
	 *
	 * @param pojo      the POJO metadata
	 * @param bKeyOnly  true to return only key properties
	 * @return collected and sorted list of POJO properties
	 */
	protected static List<PojoProperty> getPojoProperties(final Pojo pojo, final boolean bKeyOnly) {
		final List<PojoProperty> al = _getPojoProperties(pojo, bKeyOnly);

		al.sort((o1, o2) -> {
			if (o1.getKeyPos() > o2.getKeyPos()) {
				return 1;
			}
			if (o1.getKeyPos() < o2.getKeyPos()) {
				return -1;
			}
			return 0;
		});

		return al;
	}

	/**
	 * Recursively collects POJO properties, including:
	 * <ul>
	 *   <li>regular scalar properties,</li>
	 *   <li>link-one fkey properties,</li>
	 *   <li>import-match properties,</li>
	 *   <li>link-unique properties,</li>
	 *   <li>nested link-one subtrees.</li>
	 * </ul>
	 *
	 * @param pojo      POJO metadata root
	 * @param bKeyOnly  true to include only properties with {@code keyPos > 0}
	 * @return list of collected properties
	 */
	private static List<PojoProperty> _getPojoProperties(final Pojo pojo, final boolean bKeyOnly) {
		final List<PojoProperty> al = new ArrayList<>();

		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			if (!bKeyOnly || prp.getPojoProperty().getKeyPos() > 0) {
				al.add(prp.getPojoProperty());
			}
		}

		if (!bKeyOnly || al.size() == 0) {
			for (PojoLink pl : pojo.getPojoLinks()) {
				PojoLinkOne plo = pl.getPojoLinkOne();
				if (plo != null) {
					_getPojoProperties(plo, al, bKeyOnly);
				}
			}
		}
		return al;
	}

	/**
	 * Recursively collects POJO properties from a {@link PojoLinkOne} subtree.
	 * <p>
	 * Includes fkey, import-match, and unique-property mappings, expanding nested
	 * link-one references when necessary.
	 *
	 * @param plo      the link-one metadata subtree
	 * @param al       target list receiving collected properties
	 * @param bKeyOnly whether non-key properties should be omitted
	 */
	private static void _getPojoProperties(final PojoLinkOne plo, List<PojoProperty> al, final boolean bKeyOnly) {
		for (PojoLinkFkey plf : plo.getPojoLinkFkeys()) {
			if (!bKeyOnly || plf.getPojoProperty().getKeyPos() > 0) {
				al.add(plf.getPojoProperty());
			}
		}

		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			PojoProperty pp = pim.getPojoProperty();
			if (pp != null) {
				if (!bKeyOnly || pp.getKeyPos() > 0) {
					al.add(pp);
				}
			} else {
				PojoLinkOneReference plof = pim.getPojoLinkOneReference();
				PojoLinkOne plox = plof.getPojoLinkOne();
				_getPojoProperties(plox, al, bKeyOnly);
			}
		}

		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu != null) {
			PojoProperty pp = plu.getPojoProperty();
			if (pp != null) {
				if (!bKeyOnly || pp.getKeyPos() > 0) {
					al.add(pp);
				}
			} else {
				PojoLinkOneReference plof = plu.getPojoLinkOneReference();
				PojoLinkOne plox = plof.getPojoLinkOne();
				_getPojoProperties(plox, al, bKeyOnly);
			}
		}
	}

	/**
	 * Determines whether the model represented by {@link OAObjectInfo}
	 * includes at least one POJO property that:
	 * <ul>
	 *   <li>has {@code keyPos > 0}, and</li>
	 *   <li>corresponds to an OA property marked as a primary key.</li>
	 * </ul>
	 *
	 * @param oi metadata describing the OAObject model
	 * @return true if a primary-key POJO property exists
	 */
	public static boolean hasPkey(final OAObjectInfo oi) {
		for (PojoRegularProperty prp : oi.getPojo().getPojoRegularProperties()) {
			if (prp.getPojoProperty().getKeyPos() > 0) {
				OAPropertyInfo pi = oi.getPropertyInfo(prp.getPojoProperty().getName());
				if (pi.getKey()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines whether the model contains a POJO property used as an
	 * import-match key. A property qualifies when both:
	 * <ul>
	 *   <li>its POJO property has {@code keyPos > 0}, and</li>
	 *   <li>the underlying OA property has {@code importMatch = true}.</li>
	 * </ul>
	 *
	 * @param oi OAObject metadata
	 * @return true if an import-match key is present
	 */
	public static boolean hasImportMatchKey(final OAObjectInfo oi) {
		for (PojoRegularProperty prp : oi.getPojo().getPojoRegularProperties()) {
			if (prp.getPojoProperty().getKeyPos() > 0) {
				OAPropertyInfo pi = oi.getPropertyInfo(prp.getPojoProperty().getName());
				if (!pi.getKey() && pi.getImportMatch()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines whether the model has at least one POJO property participating
	 * in a “link unique” key.
	 * <p>
	 * A property qualifies when it is a POJO key part but the underlying OA
	 * property is neither an ID nor an import-match field.
	 *
	 * @param oi OAObject metadata
	 * @return true if a link-unique key is present
	 */
	public static boolean hasLinkUniqueKey(final OAObjectInfo oi) {
		for (final PojoProperty pp : getPojoPropertyKeys(oi.getPojo())) {
			OAPropertyInfo pi = oi.getPropertyInfo(pp.getName());
			if (pi != null && !pi.getId() && !pi.getImportMatch()) {
				return true;
			}
		}
		return false;
	}

}
