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
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Load oaobject classes into pojo structure that will allow pojo (json) to be loaded automatically. <br>
 * <p>
 * Note: the pojo generator for oamodels might not have pkey properties, so this is a way to uses importMatches and unique values that are
 * defined in the model to find exact matches.
 * <p>
 * <ul>
 * there are three ways to match/find (ex: when deserializing) a json object with it's matching OAObject
 * <li>Class has one or more defined pkey properties that are included in the pojo class.
 * <li>Class has one or more properties or linkOnes.
 * <li>A one2many link has a unique property, and a equals propertyPath for them to share a common root.
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
			PojoRegularProperty pojoRegularProperty = new PojoRegularProperty();
			pojo.getPojoRegularProperties().add(pojoRegularProperty);
			pojoRegularProperty.setPojo(pojo);

			PojoProperty pojoProperty = new PojoProperty();
			pojoProperty.setName(pd.getLowerName());
			pojoProperty.setUpperName(pd.getName());
			pojoProperty.setPropertyPath(pd.getLowerName());
			// pojoProperty.setJavaType(pd.getType());

			pojoRegularProperty.setPojoProperty(pojoProperty);
			pojoProperty.setPojoRegularProperty(pojoRegularProperty);
		}

		// 2: one link properties
		for (OALinkInfo lp : oi.getLinkInfos()) {

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
			pojoLink.setName(lp.getLowerName());
			pojo.getPojoLinks().add(pojoLink);

			PojoLinkOne pojoLinkOne = new PojoLinkOne();
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

			PojoLink pojoLink = new PojoLink();
			pojoLink.setName(lp.getLowerName());
			pojo.getPojoLinks().add(pojoLink);

			PojoLinkMany pojoLinkMany = new PojoLinkMany();
			pojoLink.setPojoLinkMany(pojoLinkMany);
		}
		return pojo;
	}

	// recursive when following link with importMatch or unique that is a LinkProperty
	protected void processPojoLinkOne(final OAObjectInfo oi, final String prefixPropertyPath, final Pojo pojo,
			final PojoLinkOne pojoLinkOne,
			final OALinkInfo lp) {

		// 2.A: fkeys
		for (OAFkeyInfo fk : lp.getFkeyInfos()) {
			OAPropertyInfo propertyDef = fk.getFromPropertyInfo();

			PojoLinkFkey plf = new PojoLinkFkey();
			pojoLinkOne.getPojoLinkFkeys().add(plf);

			PojoProperty pjp = new PojoProperty();
			pjp.setName(propertyDef.getLowerName());
			pjp.setUpperName(propertyDef.getName());

			String s;
			if (OAString.isEmpty(prefixPropertyPath)) {
				s = lp.getLowerName() + "." + fk.getToPropertyInfo().getLowerName();
			} else {
				s = prefixPropertyPath + "." + lp.getLowerName() + "." + fk.getToPropertyInfo().getLowerName();
			}
			pjp.setPropertyPath(s);

			plf.setPojoProperty(pjp);
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
			plo.getPojoImportMatches().add(pim);
			PojoProperty pjp = new PojoProperty();

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
			pim.setPojoProperty(pjp);
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
			plo.getPojoImportMatches().add(pim);

			PojoLinkOneReference plor = new PojoLinkOneReference();
			plor.setName(lpx.getName());
			pim.setPojoLinkOneReference(plor);

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
			plo.setPojoLinkUnique(plu);
			PojoProperty pjp = new PojoProperty();

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

			plu.setPojoProperty(pjp);
			return;
		}

		lpx = oix.getLinkInfo(uniqueName);
		if (lpx == null || lpx.getType() != OALinkInfo.TYPE_ONE) {
			return;
		}

		PojoLinkUnique plu = new PojoLinkUnique();
		plo.setPojoLinkUnique(plu);

		PojoLinkOneReference plor = new PojoLinkOneReference();
		plor.setName(lpx.getName());
		plu.setPojoLinkOneReference(plor);

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
}
