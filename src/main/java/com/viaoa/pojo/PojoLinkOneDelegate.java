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

	public static List<PojoProperty> getLinkFkeyPojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getLinkFkeyPojoProperties(plo);
	}

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

	public static List<PojoProperty> getImportMatchPojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getImportMatchPojoProperties(plo);
	}

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

	public static List<PojoProperty> getLinkUniquePojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}
		return getLinkUniquePojoProperties(plo);
	}

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

	public static List<PojoProperty> getLinkOnePojoProperties(final PojoLinkOne plo) {
		final List<PojoProperty> alPjp = new ArrayList<>();
		if (plo == null) {
			return alPjp;
		}
		_getLinkOnePojoProperties(plo, alPjp);
		return alPjp;
	}

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
