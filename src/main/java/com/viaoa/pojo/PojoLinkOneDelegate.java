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

	public static List<PojoProperty> getLinkUniquePojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}

		return getLinkUniquePojoProperties(plo);
	}

	public static List<PojoProperty> getLinkUniquePojoProperties(final PojoLinkOne plo) {
		if (plo == null) {
			return null;
		}

		List<PojoProperty> alPjp = new ArrayList<>();
		_getLinkUniquePojoProperties(plo, alPjp);
		return alPjp;
	}

	protected static void _getLinkUniquePojoProperties(final PojoLinkOne plo, final List<PojoProperty> alPjp) {
		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu == null) {
			return;
		}
		PojoProperty pjp = plu.getPojoProperty();
		if (pjp != null) {
			alPjp.add(pjp);
		}
		PojoLinkOneReference plor = plu.getPojoLinkOneReference();
		if (plor != null) {
			PojoLinkOne plox = plor.getPojoLinkOne();
			_getLinkUniquePojoProperties(plox, alPjp);
		}
	}

	public static List<PojoProperty> getImportMatchPojoProperties(Pojo pojo, String linkName) {
		PojoLinkOne plo = getPojoLinkOne(pojo, linkName);
		if (plo == null) {
			return null;
		}

		List<PojoProperty> alPjp = new ArrayList<>();
		_getImportMatchPojoProperties(plo, alPjp);

		return alPjp;
	}

	public static List<PojoProperty> getImportMatchPojoProperties(final PojoLinkOne plo) {
		if (plo == null) {
			return null;
		}

		List<PojoProperty> alPjp = new ArrayList<>();
		_getImportMatchPojoProperties(plo, alPjp);

		return alPjp;
	}

	protected static void _getImportMatchPojoProperties(final PojoLinkOne plo, final List<PojoProperty> alPjp) {
		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			PojoProperty pjp = pim.getPojoProperty();
			if (pjp != null) {
				alPjp.add(pjp);
			}
			PojoLinkOneReference plor = pim.getPojoLinkOneReference();
			if (plor != null) {
				PojoLinkOne plox = plor.getPojoLinkOne();
				_getImportMatchPojoProperties(plox, alPjp);
			}
		}
	}

}
