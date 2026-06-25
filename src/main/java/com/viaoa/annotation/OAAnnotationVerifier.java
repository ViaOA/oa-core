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
package com.viaoa.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAArray;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextCode;

/*qqqqqqqqqqqqq
CODEX

6. src/main/java/com/viaoa/annotation/OAAnnotationVerifier.java:118 verify
     Bug/risk: verifier uses only clazz.getDeclaredMethods() while runtime annotation processing walks superclasses.
     Production/runtime impact: valid inherited annotations can be reported as missing/mismatched by verification.
     This creates false negatives in model validation and can hide real drift if teams stop trusting verifier output.
     Severity: Low
     Minimal hardening: make verifier mirror OAObjectAnnotationService.update() hierarchy traversal.
  7. src/main/java/com/viaoa/annotation/OAAnnotationVerifier.java:616 compare(OACalcInfo, OACalcInfo)
     Bug/risk: after handling the case where one dependent-property array is null, the method continues and
     dereferences p1.length / p2.length.
     Production/runtime impact: metadata comparison can throw NPE instead of reporting a clean mismatch.
     Severity: Low
     Minimal hardening: return immediately after the null-array comparison block.

1. OAAnnotationVerifier.verify / compare(OALinkInfo, OALinkInfo) — reverse-name mismatches can falsely pass
     Severity: Medium
     Execution path: annotation has @OAOne(reverseName="orders") or @OAMany(reverseName="customer"), but metadata has
     li.getReverseName() == null. The verifier condition checks s != annotation.reverseName() && (s != null && !
     s.equalsIgnoreCase(...)); when s is null, the mismatch is skipped. compare(OALinkInfo, OALinkInfo) similarly
     treats a null first reverse name as “not needed” and does not compare the second value.
     Why it matters: reverse-link metadata controls bidirectional Hub/object wiring. A verifier false positive can let
     broken relationship metadata ship.
     Minimal hardening: compare reverse names symmetrically: both null is OK, one null is mismatch, otherwise case-
     insensitive compare.
     Suggested CODEX comment location: OAAnnotationVerifier.verify, ONE/MANY reverse-name checks;
     OAAnnotationVerifier.compare(OALinkInfo, OALinkInfo).
  2. OAAnnotationVerifier.verify / compare(OALinkInfo, OALinkInfo) — autoCreateNew=true mismatches can falsely pass
     Severity: Medium
     Execution path: annotation declares @OAOne(autoCreateNew=true), but metadata has li.getAutoCreateNew() == false.
     The verifier only checks mismatch when li.getAutoCreateNew() is true, so false-vs-true passes.
     compare(OALinkInfo, OALinkInfo) has the same asymmetry.
     Why it matters: autoCreateNew changes runtime object/link creation semantics. A verifier false positive can hide
     a real metadata drift that changes graph behavior.
     Minimal hardening: compare li.getAutoCreateNew() != annotation.autoCreateNew() directly.
     Suggested CODEX comment location: OAAnnotationVerifier.verify, ONE-link autoCreateNew check;
     OAAnnotationVerifier.compare(OALinkInfo, OALinkInfo).

5. OAAnnotationVerifier.verify — ID verification can throw instead of reporting invalid metadata when ID metadata is
     absent
     Severity: Low
     Execution path: verify(oi) obtains String[] ids = oi.getIdProperties() and immediately creates new
     boolean[ids.length]. If a metadata object lacks ID properties, verifier throws NullPointerException instead of
     returning false with a diagnostic.
     Why it matters: verifier is supposed to validate metadata consistency. A thrown NPE makes model validation
     brittle and can hide the actual metadata error.
     Minimal hardening: treat null/empty ID arrays as explicit validation failure unless the class is intentionally
     ID-less by contract.
     Suggested CODEX comment location: OAAnnotationVerifier.verify, start of “Verify IDs”.

 1. OAAnnotationVerifier.verify — datasource/table/index annotation coverage is claimed but not implemented
     Severity: Medium
     Execution path: a model has @OATable(indexes=...), @OAIndex, @OAIndexColumn, or @OAColumn(sqlType=..., name=...,
     isFullTextIndex=...) drift from the physical/schema expectation. OAAnnotationVerifier documentation says it
     compares table, columns, SQL types, foreign keys, and indexes, but the implementation only checks a limited
     property subset and max length via datasource.
     Why it matters: this verifier can return success while datasource-critical annotation metadata is wrong. That
     creates false confidence for schema/model validation before production deployment.
     Minimal hardening: either implement explicit checks for @OATable, @OAIndex, @OAIndexColumn, and full @OAColumn
     metadata, or narrow the verifier contract so it does not imply datasource-schema validation.
     Suggested CODEX comment location: OAAnnotationVerifier.verify, class/property verification sections and verifier
     class header.


*/

/**
 * Validates that OA model annotations match the runtime metadata generated
 * by {@link OAObjectInfo} and, optionally, the physical JDBC database schema.
 *
 * <p>This verifier performs deep structural validation of an OA model class by
 * comparing all annotations—{@link OAClass}, {@link OAProperty},
 * {@link OACalculatedProperty}, {@link OAId}, {@link OAOne}, {@link OAMany},
 * {@link OATable}, {@link OAColumn}, {@link OAIndex}, and {@link OAIndexColumn}—
 * against the computed {@link OAObjectInfo} and/or the database metadata from
 * {@link com.viaoa.datasource.jdbc.db.Database}.</p>
 *
 * <p><b>Validation Coverage</b>:
 * <ul>
 *   <li>Class-level settings (useDataSource, localOnly, cache, initialization).</li>
 *   <li>ID properties and their ordering.</li>
 *   <li>All properties including maxLength, required, id-flag, and SQL types.</li>
 *   <li>Calculated properties and dependent property lists.</li>
 *   <li>All link definitions (one-to-one, one-to-many, many-to-many).</li>
 *   <li>Database table, columns, foreign keys, and index structures.</li>
 * </ul>
 *
 * <p>This tool is mainly used during development or model generation to ensure
 * that annotations, OAObjectInfo metadata, and the relational schema are mutually
 * consistent. A mismatch indicates either a model definition issue or a schema
 * drift.</p>
 */
public class OAAnnotationVerifier {

	private static Logger LOG = Logger.getLogger(OAAnnotationVerifier.class.getName());

	
	
	/**
	 * Verifies that the annotations declared on the class associated with the
	 * given {@link OAObjectInfo} match the metadata computed in the
	 * {@link OAObjectInfo} instance.
	 * <p>
	 * This includes validating:
	 * <ul>
	 *   <li>Class-level settings such as data-source usage, caching, and initialization.</li>
	 *   <li>ID property count, ordering, and annotation placement.</li>
	 *   <li>All regular properties including required flag, maxLength, and type consistency.</li>
	 *   <li>Calculated properties and their dependent property definitions.</li>
	 *   <li>ONE and MANY link definitions and link-metadata alignment.</li>
	 * </ul>
	 * Any mismatch prints a diagnostic message and sets the result to {@code false}.
	 *
	 * @param oi the metadata object to verify
	 * @return {@code true} if all annotations match the metadata; otherwise {@code false}
	 * @throws Exception if reflection access errors occur during verification
	 */
	public boolean verify(OAObjectInfo oi) throws Exception {
		final Class clazz = oi.getForClass();
		final OA oa = OARuntime.oa(clazz);

		String s;

		OAClass oaclass = (OAClass) clazz.getAnnotation(OAClass.class);
		if (oaclass == null) {
			p("no oaclass annotation");
			return false;
		}

		boolean bResult = true;
		if (oi.getUseDataSource() != oaclass.useDataSource()) {
			p("useDatasource");
			bResult = false;
		}
		if (oi.getLocalOnly() != oaclass.localOnly()) {
			p("LocalOnly");
			bResult = false;
		}
		if (oi.getAddToCache() != oaclass.addToCache()) {
			p("addToCache");
			bResult = false;
		}
		if (oi.getInitializeNewObjects() != oaclass.initialize()) {
			p("initializeNewObjects");
			bResult = false;
		}

		// Verify IDs
		String[] ids = oi.getIdProperties();
		Method[] methods = clazz.getDeclaredMethods(); // need to get all access types, since some could be private. qqqqqq does not get superclass methods

		boolean[] bs = new boolean[ids.length];
		for (Method m : methods) {
			OAId oaid = (OAId) m.getAnnotation(OAId.class);
			if (oaid == null) {
				continue;
			}
			s = OATextCode.getPropertyName(m.getName());

			
			
			int x = OAArray.indexOf(ids, s, true);
			if (x >= 0) {
				bs[x] = true;
				if (oaid.pos() != x) {
					p("id prop wrong pos");
					bResult = false;
				}
			} else {
				p("id prop mismatch");
				bResult = false;
			}
		}
		for (boolean b : bs) {
			if (!b) {
				p("id prop mismatch2");
				bResult = false;
			}
		}

		// Verify properties
		ArrayList<OAPropertyInfo> al = oi.getPropertyInfos();
		bs = new boolean[al.size()];
		for (Method m : methods) {
			OAProperty oaprop = (OAProperty) m.getAnnotation(OAProperty.class);
			if (oaprop == null) {
				continue;
			}
			String name = OATextCode.getPropertyName(m.getName());

			int x = 0;
			for (OAPropertyInfo pi : al) {
				s = pi.getName();
				if (name.equalsIgnoreCase(s)) {
					bs[x] = true;

					if (m.getReturnType().equals(String.class) && pi.getMaxLength() != oaprop.maxLength()) {
						OADataSource ds = OARuntime.datasource().get(clazz);
						if (ds != null) {
							x = ds.getMaxLength(clazz, s);
							if (x != oaprop.maxLength()) {
								if (x > 0) {
									p(name + " maxLength, " + x + ", " + oaprop.maxLength());
									bResult = false;
								}
							}
						}
					}
					if (pi.getRequired() != oaprop.required()) {
						p("required");
						bResult = false;
					}
					if (pi.getId()) {
						if (m.getAnnotation(OAId.class) == null) {
							p("id");
							bResult = false;
						}
					}
					x = -1;
					break;
				}
				x++;
			}
			if (x != -1) {
				p("prop mismatch3 " + name);
				bResult = false;
			}
		}
		for (boolean b : bs) {
			if (!b) {
				p("prop mismatch4");
				bResult = false;
			}
		}

		// Verify calcProperties
		ArrayList<OACalcInfo> alCalc = oi.getCalcInfos();
		bs = new boolean[alCalc.size()];
		for (Method m : methods) {
			OACalculatedProperty annotation = (OACalculatedProperty) m.getAnnotation(OACalculatedProperty.class);
			if (annotation == null) {
				continue;
			}

			String name = OATextCode.getPropertyName(m.getName());

			OACalcInfo ci = oa.internal().objects().info().getCalcInfo(oi, name);

			if (ci == null) {
				p("calcinfo not in objectInfo");
				bResult = false;
				continue;
			}
			int pos = alCalc.indexOf(ci);
			if (pos < 0 || pos >= bs.length) {
				p("method with calc not in calsInfos list");
				bResult = false;
				continue;
			}
			else bs[pos] = true;

			// compare properties
			String[] ss1 = ci.getDependentProperties();
			String[] ss2 = annotation.properties();
			if (ss1.length != ss2.length) {
				p("calc props mismatch");
				bResult = false;
			} else {
				for (int j = 0; j < ss1.length; j++) {
					boolean b = false;
					for (int k = 0; k < ss2.length; k++) {
						if (ss1[j].equalsIgnoreCase(ss2[k])) {
							b = true;
						}
					}
					if (!b) {
						p("calc prop name mismatch");
						bResult = false;
						break;
					}
				}
			}
		}
		for (boolean b : bs) {
			if (!b) {
				p("calcInfo mismatch");
				bResult = false;
			}
		}

		// Verify links
		List<OALinkInfo> alLinkInfo = oi.getLinkInfos();
		bs = new boolean[alLinkInfo.size()];
		// Ones
		for (Method m : methods) {
			OAOne annotation = (OAOne) m.getAnnotation(OAOne.class);
			Class c = m.getReturnType();
			if (annotation == null) {
				if (OAObject.class.isAssignableFrom(c)) {
					p("method should be OAOne");
					bResult = false;
				}
				continue;
			}
			if (!OAObject.class.isAssignableFrom(c)) {
				p("method should return subclass of OAObject");
				bResult = false;
			}

			String name = OATextCode.getPropertyName(m.getName());

			OALinkInfo li = oa.internal().objects().info().getLinkInfo(oi, name);
			if (li == null) {
				p("link does not exist");
				bResult = false;
			} else {
				if (li.getToClass() != m.getReturnType()) {
					p("wrong link class");
					bResult = false;
				}

				if (li.getCascadeSave() != annotation.cascadeSave()) {
					p("wrong cascade save");
					bResult = false;
				}
				if (li.getCascadeDelete() != annotation.cascadeDelete()) {
					p("wrong cascade delete");
					bResult = false;
				}
				s = li.getReverseName();
				if (s != annotation.reverseName() && (s != null && !s.equalsIgnoreCase(annotation.reverseName()))) {
					p("wrong reverse name");
					bResult = false;
				}
				if (li.getOwner() != annotation.owner()) {
					p("wrong owner");
					bResult = false;
				}

				if (li.getAutoCreateNew() && li.getAutoCreateNew() != annotation.autoCreateNew()) {
					p("wrong autoCreateNew");
					bResult = false;
				}
			}
			int x = alLinkInfo.indexOf(li);
			if (x < 0 || x >= bs.length) {
				p("method for linkInfo not found in linkInfos");
				bResult = false;
			}
			else bs[x] = true;
		}
		// Manys
		for (Method m : methods) {
			OAMany annotation = (OAMany) m.getAnnotation(OAMany.class);
			Class c = m.getReturnType();
			if (annotation == null) {
				if (Hub.class.isAssignableFrom(c)) {
					p("method should be OAMany");
					bResult = false;
				}
				continue;
			}
			if (!Hub.class.isAssignableFrom(c)) {
				p("method should return a Hub");
				bResult = false;
			}

			String name = OATextCode.getPropertyName(m.getName());
			OALinkInfo li = oa.internal().objects().info().getLinkInfo(oi, name);
			if (li == null) {
				p("link does not exist");
				bResult = false;
			} else {
				if (li.getCascadeSave() != annotation.cascadeSave()) {
					p("wrong cascade save");
					bResult = false;
				}
				if (li.getCascadeDelete() != annotation.cascadeDelete()) {
					p("wrong cascade delete");
					bResult = false;
				}
				s = li.getReverseName();
				if (s != annotation.reverseName() && (s != null && !s.equalsIgnoreCase(annotation.reverseName()))) {
					p("wrong reverse name");
					bResult = false;
				}
				if (li.getOwner() != annotation.owner()) {
					p("wrong owner");
					bResult = false;
				}
			}
			int x = alLinkInfo.indexOf(li);
			if (x < 0 || x >= bs.length) {
				p("method for linkInfo not found in linkInfos");
				bResult = false;
			}
			else bs[x] = true;
		}

		int i = 0;
		for (boolean b : bs) {
			if (!b) {
				OALinkInfo li = alLinkInfo.get(i);
				p("link mismatch, name=" + li.getName());
				bResult = false;
			}
			i++;
		}
		return bResult;
	}


	/**
	 * Compares two {@link OAObjectInfo} metadata objects for structural
	 * equivalence.
	 * <p>
	 * Validation includes:
	 * <ul>
	 *   <li>Class identity and class-level flags.</li>
	 *   <li>ID property arrays and primitive property lists.</li>
	 *   <li>Link definitions and their structure.</li>
	 *   <li>Calculated property definitions and dependencies.</li>
	 * </ul>
	 * Any mismatch prints a diagnostic message and returns {@code false}.
	 *
	 * @param oi1 the first metadata object
	 * @param oi2 the second metadata object
	 * @return {@code true} if the metadata matches; otherwise {@code false}
	 */
	public boolean compare(OAObjectInfo oi1, OAObjectInfo oi2) {
		if (oi1.getForClass() != oi2.getForClass()) {
			p("class mismatch");
			return false;
		}
		if (oi1.getUseDataSource() != oi2.getUseDataSource()) {
			p("class bUseDataSource");
			return false;
		}
		if (oi1.getLocalOnly() != oi2.getLocalOnly()) {
			p("class bLocalOnly");
			return false;
		}
		if (oi1.getAddToCache() != oi2.getAddToCache()) {
			p("class bAddToCache");
			return false;
		}
		if (oi1.getInitializeNewObjects() != oi2.getInitializeNewObjects()) {
			p("class bInitializeNewObjects");
			return false;
		}
		if (oi1.getDisplayName() != oi2.getDisplayName()) {
			if (oi1.getDisplayName() == null || !oi1.getDisplayName().equalsIgnoreCase(oi2.getDisplayName())) {
				// p("class displayName");
			}
		}
		if (oi1.getIdProperties() != oi2.getIdProperties()) {
			boolean b = true;
			if (oi1.getIdProperties() == null || oi2.getIdProperties() == null) {
				b = false;
			} else {
				if (oi1.getIdProperties().length != oi2.getIdProperties().length) {
					b = false;
				} else {
					int x = oi1.getIdProperties().length;
					for (int i = 0; i < x; i++) {
						if (oi1.getIdProperties()[i] == null || oi2.getIdProperties()[i] == null) {
							b = false;
							break;
						}
						if (!oi1.getIdProperties()[i].equalsIgnoreCase(oi2.getIdProperties()[i])) {
							b = false;
							break;
						}
					}
				}
			}
			if (!b) {
				p("class idProperties");
				return false;
			}
		}

		oi1.getFriendAccess().getPrimitiveProps(oi1);
		
		if (OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1) != OAObjectInfo.getFriendAccess().getPrimitiveProps(oi2)) {
			boolean b = true;
			if (OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1) == null || OAObjectInfo.getFriendAccess().getPrimitiveProps(oi2) == null) {
				b = false;
			} else {
				if (OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1).length != OAObjectInfo.getFriendAccess().getPrimitiveProps(oi2).length) {
					b = false;
				} else {
					int x = OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1).length;
					for (int i = 0; i < x; i++) {
						if (OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1)[i] == null || OAObjectInfo.getFriendAccess().getPrimitiveProps(oi2)[i] == null) {
							b = false;
							break;
						}
						if (!OAObjectInfo.getFriendAccess().getPrimitiveProps(oi1)[i].equalsIgnoreCase(OAObjectInfo.getFriendAccess().getPrimitiveProps(oi2)[i])) {
							b = false;
							break;
						}
					}
				}
			}
			if (!b) {
				p("class primitiveProps");
				return false;
			}
		}

		List<OALinkInfo> al = oi1.getLinkInfos();
		List<OALinkInfo> al2 = oi2.getLinkInfos();

		if (al != al2 && (al == null || al2 == null || al.size() != al2.size())) {
			p("LinkInfos mismatch");
			return false;
		}
		int x = al.size();
		for (int i = 0; i < x; i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			boolean b = false;
			for (int j = 0; j < x; j++) {
				OALinkInfo li2 = (OALinkInfo) al2.get(j);
				if (li2.getName() != null && li2.getName().equalsIgnoreCase(li.getName())) {
					if (!compare(li, li2)) {
						return false;
					}
					b = true;
					break;
				}
			}
			if (!b) {
				p("no matching linkInfo");
				return false;
			}
		}

		ArrayList<OACalcInfo> alCalc = oi1.getCalcInfos();
		ArrayList<OACalcInfo> alCalc2 = oi2.getCalcInfos();
		if (alCalc != alCalc2 && (alCalc == null || alCalc2 == null || alCalc.size() != alCalc2.size())) {
			p("CalcInfos mismatch");
			return false;
		}
		x = alCalc.size();
		for (int i = 0; i < x; i++) {
			OACalcInfo ci = (OACalcInfo) alCalc.get(i);
			boolean b = false;
			for (int j = 0; j < x; j++) {
				OACalcInfo ci2 = (OACalcInfo) alCalc2.get(j);
				if (ci.getName() != null && ci.getName().equalsIgnoreCase(ci2.getName())) {
					if (!compare(ci, ci2)) {
						return false;
					}
					b = true;
					break;
				}
			}
			if (!b) {
				p("calc matching name not found");
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares two {@link OALinkInfo} instances for equality.
	 * <p>
	 * Validates link name, target class, link type, cascade settings,
	 * reverse-name, owner flag, and auto-create-new settings.  
	 * Any mismatch prints a diagnostic message and returns {@code false}.
	 *
	 * @param li   the first link metadata
	 * @param li2  the second link metadata
	 * @return {@code true} if both link definitions match; otherwise {@code false}
	 */
	boolean compare(OALinkInfo li, OALinkInfo li2) {
		if (li == null || li2 == null) {
			return false;
		}
		if (li.getName() == null || !li.getName().equalsIgnoreCase(li2.getName())) {
			p("link name dont match");
			return false;
		}
		if (li.getToClass() == null || !li.getToClass().equals(li2.getToClass())) {
			p("link toClass dont match");
			return false;
		}
		if (li.getType() != li2.getType()) {
			p("link type dont match");
			return false;
		}
		if (li.getCascadeSave() != li2.getCascadeSave()) {
			p("link cascadeSave dont match");
			return false;
		}
		if (li.getCascadeDelete() != li2.getCascadeDelete()) {
			p("link cascadeDelete dont match");
			return false;
		}

		if (li.getReverseName() == null) {
			// method not created, not needed
		} else if (!li.getReverseName().equalsIgnoreCase(li2.getReverseName())) {
			p("link reverseName dont match");
			return false;
		}
		if (li.getOwner() != li2.getOwner()) {
			p("link owner dont match");
			return false;
		}
		if (li.getAutoCreateNew() && li.getAutoCreateNew() != li2.getAutoCreateNew()) {
			p("link autoCreateNew dont match");
			return false;
		}
		return true;
	}

	/**
	 * Compares two {@link OACalcInfo} instances for equality.
	 * <p>
	 * Validates the calculated-property name and the full set of dependent
	 * property names, ensuring that both lists contain the same values
	 * (case-insensitive and order-independent).  
	 * Any mismatch prints a diagnostic message and returns {@code false}.
	 *
	 * @param ci   the first calculated-property metadata
	 * @param ci2  the second calculated-property metadata
	 * @return {@code true} if both definitions match; otherwise {@code false}
	 */
	boolean compare(OACalcInfo ci, OACalcInfo ci2) {
		if (ci == null || ci2 == null) {
			return false;
		}
		if (ci.getName() == null || !ci.getName().equals(ci2.getName())) {
			p("calcProperty name dont match");
			return false;
		}
		String[] p1 = ci.getDependentProperties();
		String[] p2 = ci2.getDependentProperties();
		if (p1 == null || p2 == null) {
			if (p1 != p2) {
				p("calc properties dont match");
				return false;
			}
		}
		if (p1.length != p2.length) {
			p("calc property count dont match");
			return false;
		}
		boolean b = false;
		for (int i = 0; !b && i < p1.length; i++) {
			b = false;
			for (int j = 0; !b && j < p2.length; j++) {
				if (p1[i].equalsIgnoreCase(p2[j])) {
					b = true;
					break;
				}
			}
			if (!b) break;
		}
		if (!b && p1.length > 0) {
			p("calc property name dont match");
			return false;
		}
		return true;
	}

	void p(String msg) {
		//LOG.warning(msg);
		System.out.println("Error: " + msg);
	}
}
