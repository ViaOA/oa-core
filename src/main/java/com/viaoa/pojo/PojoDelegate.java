package com.viaoa.pojo;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.util.OAString;

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
		return getPojoProperties(pojo, true);
	}

	protected static List<PojoProperty> getPojoProperties(final Pojo pojo, final boolean bKeyOnly) {
		final List<PojoProperty> al = new ArrayList<>();

		int pos = 0;
		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			if (!bKeyOnly || prp.getPojoProperty().getKeyPos() > 0) {
				al.add(prp.getPojoProperty());
			}
		}

		for (PojoLink pl : pojo.getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo != null) {
				getPojoProperties(plo, al, bKeyOnly);
			}

		}
		return al;
	}

	protected static void getPojoProperties(final PojoLinkOne plo, List<PojoProperty> al, final boolean bKeyOnly) {

		for (PojoLinkFkey plf : plo.getPojoLinkFkeys()) {
			al.add(plf.getPojoProperty());
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
				getPojoProperties(plox, al, bKeyOnly);
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
				getPojoProperties(plox, al, bKeyOnly);
			}
		}
	}

}
