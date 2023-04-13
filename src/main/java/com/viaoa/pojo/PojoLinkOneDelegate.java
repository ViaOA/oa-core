package com.viaoa.pojo;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.util.OAString;

public class PojoLinkOneDelegate {

	public static PojoLinkOne getPojoLinkOne(Pojo pojo, String linkName) {
		if (pojo == null) {
			return null;
		}
		if (OAString.isEmpty(linkName)) {
			return null;
		}

		List<PojoProperty> alPjp = new ArrayList<>();
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
