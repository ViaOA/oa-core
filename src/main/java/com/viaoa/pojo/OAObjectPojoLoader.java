package com.viaoa.pojo;
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

import java.io.Serializable;

import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Builds a {@link Pojo} metadata tree from an {@link OAObjectInfo} so that
 * JSON POJOs can be mapped back into live {@code OAObject} instances.
 * <p>
 * This loader is used by the JSON / Jackson integration (see
 * {@code json.OAJacksonDeserializerLoader}) to describe, for a single
 * OAObject type, which POJO fields:
 * <ul>
 *   <li>represent regular scalar properties,</li>
 *   <li>represent {@code TYPE_ONE} links and their foreign keys,</li>
 *   <li>represent import-match properties, and</li>
 *   <li>represent "unique via equalPropertyPath" link patterns.</li>
 * </ul>
 * The resulting {@link Pojo} graph (links, import matches, unique keys) is
 * later used to locate the correct target {@code OAObject} instance when
 * deserializing JSON:
 * <ol>
 *   <li>by primary-key properties,</li>
 *   <li>by import-match properties / link-ones, or</li>
 *   <li>by unique properties reachable through an {@code equalPropertyPath}
 *       on a one-to-many association.</li>
 * </ol>
 * <p>
 * See the OABuilder model {@code OABuilderPojo.obx} and the OABuilder
 * {@code PojoLoader} for the generator-side implementation.
 */
public class OAObjectPojoLoader implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@code OAObjectPojoLoader} instance.
	 */
	public OAObjectPojoLoader() {

	}

	/**
	 * Generates a {@link Pojo} metadata definition for the supplied
	 * {@link OAObjectInfo}.
	 * <p>
	 * Populates POJO regular properties, link-one structures (including fkeys,
	 * import matches, and unique-property patterns), and link-many structures.
	 * Key positions are finalized using {@link #markAllPojoPropertyKeys(Pojo, OAObjectInfo)}.
	 *
	 * @param oi the {@link OAObjectInfo} describing the OAObject model
	 * @return a fully populated {@link Pojo} metadata tree
	 */
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
	/**
	 * Processes a link-one association to populate its fkey, import-match,
	 * and unique-property POJO metadata.
	 * <p>
	 * May recurse into deeper link-one structures when import-match or
	 * unique-property rules require following nested paths.
	 *
	 * @param oi                  OAObject metadata
	 * @param prefixPropertyPath  accumulated property-path prefix
	 * @param pojo                the root {@link Pojo} metadata tree
	 * @param pojoLinkOne         link-one metadata container
	 * @param lp                  OA link-one definition
	 */
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

	/**
	 * Adds POJO import-match properties for a link-one association.
	 * <p>
	 * Includes both direct property-based import matches and nested matches
	 * traversed through link-one paths.
	 *
	 * @param oi                  OAObject metadata
	 * @param prefixPropertyPath  accumulated property-path prefix
	 * @param pojo                root POJO metadata
	 * @param plo                 link-one metadata holder
	 * @param lp                  OA link-one definition
	 */
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
			if (lpx.getType() != OALinkInfo.TYPE_ONE) {
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
				s = prefixPropertyPath + "." + lp.getLowerName();
			}
			processPojoLinkOne(oi, s, pojo, plox, lpx);

		}
	}

	/**
	 * Adds POJO metadata for link-one relationships that participate in
	 * equal-property-path uniqueness rules.
	 * <p>
	 * Handles both simple unique-property cases and nested link-one unique
	 * references that require recursion.
	 *
	 * @param oi                  OAObject metadata
	 * @param prefixPropertyPath  accumulated property-path prefix
	 * @param pojo                root POJO metadata
	 * @param lp                  OA link-one definition
	 * @param plo                 link-one POJO metadata holder
	 */
	protected void processPojoLinkOneWithEqualPropPathsAndUnique(final OAObjectInfo oi, final String prefixPropertyPath, final Pojo pojo,
			final OALinkInfo lp,
			final PojoLinkOne plo) {

		if (OAString.isEmpty(lp.getEqualPropertyPath())) {
			return;
		}

		final OALinkInfo lpRev = lp.getReverseLinkInfo();
		if (lpRev == null) return;
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

	/**
	 * Determines and assigns key positions for all POJO properties derived from
	 * {@link OAObjectInfo}.
	 * <p>
	 * Considers pkey fields, compound keys, import-match keys, and nested
	 * link-one uniqueness structures.
	 *
	 * @param pojo the POJO metadata tree to update
	 * @param oi   OAObject metadata providing property definitions
	 */
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

			final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(li.getToClass()).objects().getOAObjectInfoService();
			if (!srvcObjectInfo.isPojoSingleton(li.getToObjectInfo())) {
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

	/**
	 * Recursively assigns key positions within a {@link PojoLinkOne} subtree.
	 * <p>
	 * Processes foreign-key POJO properties, import-match POJO properties,
	 * and unique-property link-one references.
	 *
	 * @param plo link-one POJO metadata node
	 * @param oi  OAObject metadata for key lookups
	 * @return true if any keys were assigned; otherwise false
	 */
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
