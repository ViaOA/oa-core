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

import java.util.ArrayList;
import java.util.List;

import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAString;

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
 *   <li>{@link com.viaoa.object.OAObjectInfo}-aware helpers to determine
 *       whether a type has:
 *       <ul>
 *         <li>a primary-key based POJO key,</li>
 *         <li>an import-match based POJO key, or</li>
 *         <li>a "link unique" POJO key derived through associations.</li>
 *       </ul>
 *   </li>
 * </ul>
 * The key-marking is performed by {@link OAObjectPojoLoader} when the
 * {@link Pojo} graph is created; this delegate only reads the resulting
 * {@code keyPos} flags to answer higher-level questions.
 */
public class PojoDelegate {

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

	public static boolean hasKey(Pojo pojo) {
		return getPojoProperties(pojo, true).size() > 0;
	}

	public static boolean hasCompoundKey(Pojo pojo) {
		return getPojoProperties(pojo, true).size() > 1;
	}

	public static List<PojoProperty> getPojoProperties(Pojo pojo) {
		return getPojoProperties(pojo, false);
	}

	public static List<PojoProperty> getPojoPropertyKeys(Pojo pojo) {
		List<PojoProperty> al = getPojoProperties(pojo, true);
		return al;
	}

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
