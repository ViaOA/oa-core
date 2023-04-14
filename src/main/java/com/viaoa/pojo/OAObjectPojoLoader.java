package com.viaoa.pojo;
/*
This software and documentation is the confidential and proprietary
information of ViaOA, Inc. ("Confidential Information").
You shall not disclose such Confidential Information and shall use
it only in accordance with the terms of the license agreement you
entered into with ViaOA, Inc..

ViaOA, Inc. MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE, OR NON-INFRINGEMENT. ViaOA, Inc. SHALL NOT BE LIABLE FOR ANY DAMAGES
SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
THIS SOFTWARE OR ITS DERIVATIVES.

Copyright (c) 2001 ViaOA, Inc.
All rights reserved.
*/

import java.io.Serializable;

import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Load oaobject classes into pojo structure that will allow pojo (json) to be loaded automatically into OAObjects. <br>
 * This is then used by json.OAJacksonDeserializerLoader to load POJOs to JSON to OAObjects.
 * <p>
 * Note: the pojo generator for oamodels might not have pkey properties for pojo objects, so this is a way to uses importMatches and link
 * unique values that are defined in the model to find exact matches.
 * <p>
 * <ul>
 * there are three ways to match/find (ex: when deserializing) a json object with it's matching OAObject
 * <li>Class has one or more defined pkey properties that are included in the pojo class.
 * <li>Class has one or more properties or linkOnes that are importMatches.
 * <li>A one2many link has a unique property, and a equals propertyPath for them to share a common root that make it unique.
 * </ul>
 * <p>
 * See OABuilder model OABuilderPojo.obx<br>
 * See PojoLoader in OABuilder for version used for code generator.
 *
 * @author vvia
 */
public class OAObjectPojoLoader implements Serializable {
	private static final long serialVersionUID = 1L;

	public OAObjectPojoLoader() {

	}

	public Pojo loadIntoPojo(final OAObjectInfo oi) {
		Pojo pojo = new Pojo();
		pojo.setName(oi.getName());

		// 1: properties
		for (OAPropertyInfo pd : oi.getPropertyInfos()) {
			if (pd.getIsFkeyOnly()) {
				continue;
			}
			if (pd.getNoPojo()) {
				continue;
			}
			PojoRegularProperty pojoRegularProperty = new PojoRegularProperty();
			pojoRegularProperty.setPojo(pojo);
			pojo.getPojoRegularProperties().add(pojoRegularProperty);

			PojoProperty pojoProperty = new PojoProperty();
			pojoProperty.setPojoRegularProperty(pojoRegularProperty);
			pojoRegularProperty.setPojoProperty(pojoProperty);
			pojoProperty.setName(pd.getLowerName());
			pojoProperty.setUpperName(pd.getName());
			pojoProperty.setPropertyPath(pd.getLowerName());
			// pojoProperty.setJavaType(pd.getType());
		}

		// 2: one link properties
		for (OALinkInfo lp : oi.getLinkInfos()) {
			if (lp.getToObjectInfo().getNoPojo()) {
				continue;
			}
			if (lp.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}
			if (lp.getCalculated()) {
				continue;
			}
			if (lp.getPrivateMethod()) {
				continue;
			}

			PojoLink pojoLink = new PojoLink();
			pojoLink.setPojo(pojo);
			pojo.getPojoLinks().add(pojoLink);
			pojoLink.setName(lp.getLowerName());

			PojoLinkOne pojoLinkOne = new PojoLinkOne();
			pojoLinkOne.setPojoLink(pojoLink);
			pojoLink.setPojoLinkOne(pojoLinkOne);

			processPojoLinkOne(oi, "", pojo, pojoLinkOne, lp);
		}

		// 3: many link
		for (OALinkInfo lp : oi.getLinkInfos()) {
			if (lp.getType() != OALinkInfo.TYPE_MANY) {
				continue;
			}
			if (lp.getPrivateMethod()) {
				continue;
			}
			if (lp.getToObjectInfo().getNoPojo()) {
				continue;
			}

			PojoLink pojoLink = new PojoLink();
			pojoLink.setPojo(pojo);
			pojo.getPojoLinks().add(pojoLink);
			pojoLink.setName(lp.getLowerName());

			PojoLinkMany pojoLinkMany = new PojoLinkMany();
			pojoLinkMany.setPojoLink(pojoLink);
			pojoLink.setPojoLinkMany(pojoLinkMany);
		}

		markAllPojoPropertyKeys(pojo, oi);

		return pojo;
	}

	// recursive when following link with importMatch or unique that is a LinkProperty
	protected void processPojoLinkOne(final OAObjectInfo oi, final String prefixPropertyPath, final Pojo pojo,
			final PojoLinkOne pojoLinkOne,
			final OALinkInfo lp) {

		// 2.A: fkeys
		for (OAFkeyInfo fk : lp.getFkeyInfos()) {
			OAPropertyInfo propertyDef = fk.getToPropertyInfo();
			if (propertyDef.getNoPojo()) {
				continue;
			}
			propertyDef = fk.getFromPropertyInfo();
			PojoLinkFkey plf = new PojoLinkFkey();
			plf.setPojoLinkOne(pojoLinkOne);
			pojoLinkOne.getPojoLinkFkeys().add(plf);

			PojoProperty pjp = new PojoProperty();
			pjp.setPojoLinkFkey(plf);
			plf.setPojoProperty(pjp);
			pjp.setName(propertyDef.getLowerName());
			pjp.setUpperName(propertyDef.getName());

			String s;
			if (OAString.isEmpty(prefixPropertyPath)) {
				s = lp.getLowerName() + "." + fk.getToPropertyInfo().getLowerName();
			} else {
				s = prefixPropertyPath + "." + lp.getLowerName() + "." + fk.getToPropertyInfo().getLowerName();
			}
			pjp.setPropertyPath(s);

		}

		// 2.B: import match
		processPojoLinkOneWithImportMatches(oi, prefixPropertyPath, pojo, pojoLinkOne, lp);

		// 2.C: links with selectFromPp that has a unique property
		processPojoLinkOneWithEqualPropPathsAndUnique(oi, prefixPropertyPath, pojo, lp, pojoLinkOne);

	}

	protected void processPojoLinkOneWithImportMatches(final OAObjectInfo oi, final String prefixPropertyPath, final Pojo pojo,
			final PojoLinkOne plo,
			OALinkInfo lp) {

		OAObjectInfo oix = lp.getToObjectInfo();

		for (OAPropertyInfo px : oix.getPropertyInfos()) {
			if (!px.getImportMatch()) {
				continue;
			}
			PojoImportMatch pim = new PojoImportMatch();
			pim.setPojoLinkOne(plo);
			plo.getPojoImportMatches().add(pim);

			PojoProperty pjp = new PojoProperty();
			pjp.setPojoImportMatch(pim);
			pim.setPojoProperty(pjp);

			String s = lp.getLowerName() + px.getName();
			pjp.setName(s);
			s = lp.getName() + px.getName();
			pjp.setUpperName(s);
			// pjp.setJavaType(px.getType());

			if (OAString.isEmpty(prefixPropertyPath)) {
				s = lp.getLowerName() + "." + px.getLowerName();
			} else {
				s = prefixPropertyPath + "." + lp.getLowerName() + "." + px.getLowerName();
			}
			pjp.setPropertyPath(s);
		}

		for (OALinkInfo lpx : oix.getLinkInfos()) {
			if (lp.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}

			if (!lpx.getImportMatch()) {
				continue;
			}
			if (lpx.getReverseLinkInfo() == lp) {
				continue;
			}

			PojoImportMatch pim = new PojoImportMatch();
			pim.setPojoLinkOne(plo);
			plo.getPojoImportMatches().add(pim);

			PojoLinkOneReference plor = new PojoLinkOneReference();
			plor.setPojoImportMatch(pim);
			pim.setPojoLinkOneReference(plor);
			plor.setName(lpx.getName());

			PojoLinkOne plox = new PojoLinkOne();
			plor.setPojoLinkOne(plox);

			String s;
			if (OAString.isEmpty(prefixPropertyPath)) {
				s = lp.getLowerName();
			} else {
				s = prefixPropertyPath + "." + lp.getName();
			}
			processPojoLinkOne(oi, s, pojo, plox, lpx);

		}
	}

	protected void processPojoLinkOneWithEqualPropPathsAndUnique(final OAObjectInfo oi, final String prefixPropertyPath, final Pojo pojo,
			final OALinkInfo lp,
			final PojoLinkOne plo) {

		if (OAString.isEmpty(lp.getEqualPropertyPath())) {
			return;
		}

		final OALinkInfo lpRev = lp.getReverseLinkInfo();
		final String pp = lpRev.getEqualPropertyPath();
		if (OAString.isEmpty(pp)) {
			return;
		}

		OALinkInfo lpx = new OAPropertyPath(lp.getToClass(), pp).getReversePropertyPath().getEndLinkInfo();
		if (lpx == null) {
			return;
		}

		if (lpx.getType() != OALinkInfo.TYPE_MANY) {
			return;
		}

		String uniqueName = lpx.getUniqueProperty();
		if (OAString.isEmpty(uniqueName)) {
			return;
		}

		final OAObjectInfo oix = lp.getToObjectInfo();
		OAPropertyInfo px = oix.getPropertyInfo(uniqueName);
		if (px != null) {
			PojoLinkUnique plu = new PojoLinkUnique();
			plu.setPojoLinkOne(plo);
			plo.setPojoLinkUnique(plu);

			PojoProperty pjp = new PojoProperty();
			pjp.setPojoLinkUnique(plu);
			plu.setPojoProperty(pjp);

			pjp.setName(lp.getLowerName() + px.getName());
			pjp.setUpperName(lp.getName() + px.getName());
			// pjp.setJavaType(px.getType());

			String s;
			if (OAString.isEmpty(prefixPropertyPath)) {
				s = lp.getLowerName() + "." + px.getLowerName();
			} else {
				s = prefixPropertyPath + "." + lp.getLowerName() + "." + px.getLowerName();
			}
			pjp.setPropertyPath(s);

			return;
		}

		lpx = oix.getLinkInfo(uniqueName);
		if (lpx == null || lpx.getType() != OALinkInfo.TYPE_ONE) {
			return;
		}

		PojoLinkUnique plu = new PojoLinkUnique();
		plu.setPojoLinkOne(plo);
		plo.setPojoLinkUnique(plu);

		PojoLinkOneReference plor = new PojoLinkOneReference();
		plor.setPojoLinkUnique(plu);
		plu.setPojoLinkOneReference(plor);
		plor.setName(lpx.getName());

		PojoLinkOne plox = new PojoLinkOne();
		plor.setPojoLinkOne(plox);

		String s;
		if (OAString.isEmpty(prefixPropertyPath)) {
			s = lp.getLowerName();
		} else {
			s = prefixPropertyPath + "." + lp.getName();
		}

		processPojoLinkOne(oix, s, pojo, plox, lpx);
		return;
	}

	protected void markAllPojoPropertyKeys(final Pojo pojo, final OAObjectInfo oi) {

		// properties that are key(s)
		boolean bFound = false;
		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			OAPropertyInfo pi = oi.getPropertyInfo(prp.getPojoProperty().getName());
			if (pi.getPojoKeyPos() > 0) {
				prp.getPojoProperty().setKeyPos(pi.getPojoKeyPos());
				bFound = true;
			}
		}
		if (bFound) {
			return;
		}

		// keys are numbered if they are compound. Since it's not found, then it is a single key.

		// pkey property that is single key
		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			OAPropertyInfo pi = oi.getPropertyInfo(prp.getPojoProperty().getName());
			if (pi.getKey() && !pi.getNoPojo()) {
				prp.getPojoProperty().setKeyPos(1);
				pi.setPojoKeyPos(1);
				bFound = true;
			}
		}
		if (bFound) {
			return;
		}

		// importMatch property that is single key
		for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
			OAPropertyInfo pi = oi.getPropertyInfo(prp.getPojoProperty().getName());
			if (pi.getImportMatch()) {
				prp.getPojoProperty().setKeyPos(1);
				pi.setPojoKeyPos(1);
				bFound = true;
			}
		}
		if (bFound) {
			return;
		}

		for (PojoLink pl : pojo.getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo == null) {
				continue;
			}

			OALinkInfo li = oi.getLinkInfo(pl.getName());
			if (li == null || li.isMany()) {
				continue;
			}

			if (!li.getImportMatch()) {
				continue;
			}

			bFound = markAllPojoPropertyKeys(plo, oi);
		}
		if (bFound) {
			return;
		}

		// linkOne w/ unique and equalPp to a root/singleton object
		// check to see if there is linkOne that isPojoSingleton, that revLink is many and has a unique prop
		for (PojoLink pl : pojo.getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo == null) {
				continue;
			}

			OALinkInfo li = oi.getLinkInfo(pl.getName());

			final OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev == null || !liRev.isMany()) {
				continue;
			}

			//qqq should it also be owner ??

			final String uniquePropName = liRev.getUniqueProperty();
			if (OAString.isEmpty(uniquePropName)) {
				continue;
			}

			if (!OAObjectInfoDelegate.isPojoSingleton(li.getToObjectInfo())) {
				continue;
			}

			// flag unique prop PojoProperty.keyPos
			OAPropertyInfo pi = oi.getPropertyInfo(uniquePropName);
			if (pi != null) {
				// find PojoProperty
				for (PojoRegularProperty prp : pojo.getPojoRegularProperties()) {
					if (uniquePropName.equalsIgnoreCase(prp.getPojoProperty().getName())) {
						int kpos = pi.getPojoKeyPos();
						prp.getPojoProperty().setKeyPos(kpos == 0 ? 1 : kpos);
						if (kpos == 0) {
							pi.setPojoKeyPos(1);
						}
						bFound = true;
						break;
					}
				}
			} else {
				li = oi.getLinkInfo(uniquePropName);
				for (PojoLink plx : pojo.getPojoLinks()) {
					if (uniquePropName.equalsIgnoreCase(plx.getName())) {
						bFound = markAllPojoPropertyKeys(plx.getPojoLinkOne(), oi);
					}
				}
			}
		}
	}

	protected boolean markAllPojoPropertyKeys(final PojoLinkOne plo, final OAObjectInfo oi) {
		boolean bFound = false;
		for (PojoLinkFkey plf : plo.getPojoLinkFkeys()) {
			PojoProperty pp = plf.getPojoProperty();

			PojoLink pl = plo.getPojoLink();
			OALinkInfo li = oi.getLinkInfo(pl.getName());
			OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(OAString.field(pp.getPropertyPath(), ".", 2));

			int kpos = pi.getPojoKeyPos();
			pp.setKeyPos(kpos == 0 ? 1 : kpos);
			bFound = true;
		}
		if (bFound) {
			return true;
		}

		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			PojoProperty pp = pim.getPojoProperty();

			PojoLink pl = plo.getPojoLink();
			OALinkInfo li = oi.getLinkInfo(pl.getName());
			OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(OAString.field(pp.getPropertyPath(), ".", 2));

			if (pp != null) {
				int kpos = pi.getPojoKeyPos();
				pp.setKeyPos(kpos == 0 ? 1 : kpos);
				bFound = true;
			} else {
				PojoLinkOneReference plof = pim.getPojoLinkOneReference();
				PojoLinkOne plox = plof.getPojoLinkOne();
				bFound = markAllPojoPropertyKeys(plox, oi);
			}
		}
		if (bFound) {
			return true;
		}

		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu == null) {
			return false;
		}

		PojoProperty pp = plu.getPojoProperty();
		if (pp != null) {
			PojoLink pl = plo.getPojoLink();
			OALinkInfo li = oi.getLinkInfo(pl.getName());
			OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(OAString.field(pp.getPropertyPath(), ".", 2));

			int kpos = pi.getPojoKeyPos();
			pp.setKeyPos(kpos == 0 ? 1 : kpos);
			bFound = true;
		} else {
			PojoLinkOneReference plof = plu.getPojoLinkOneReference();
			PojoLinkOne plox = plof.getPojoLinkOne();
			bFound = markAllPojoPropertyKeys(plox, oi);
		}

		return bFound;
	}
}
