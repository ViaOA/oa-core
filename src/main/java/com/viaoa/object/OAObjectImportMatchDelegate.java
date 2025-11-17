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
package com.viaoa.object;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Resolves or creates {@link OAObject} instances during JSON or POJO import
 * when a primary key is not available, using declared "import match" rules.
 *
 * <p>Many generated model classes define one or more properties or links
 * as <i>import matches</i>—fields uniquely identifying an object within a
 * domain or hierarchy.  This delegate examines those match definitions to
 * locate the correct target object in cache or data source; if none exists,
 * it automatically constructs the object and any required link hierarchy.</p>
 *
 * <p><b>Core Responsibilities</b>:
 * <ul>
 *   <li>Combine multiple import-match properties and link paths into a single query.</li>
 *   <li>Traverse equal-property and owner-link rules recursively.</li>
 *   <li>Leverage {@link OASelect}, {@link OAFinder}, and {@link OAObjectCacheDelegate}
 *       for lookup before creating new objects via reflection.</li>
 *   <li>Maintain referential integrity between the source and newly created target objects.</li>
 * </ul>
 *
 * <p>This mechanism allows OA to reconstruct a full object graph from lightweight
 * JSON or external data that omits primary keys, providing “identity by content.”</p>
 */
public class OAObjectImportMatchDelegate {

	public static class ImportMatch {
		public OAObject fromObject;
		public OALinkInfo liTo;
		public final List<ImportMatchDetail> importMatchDetails = new ArrayList<>();

		// if liTo.object is owned, then this is the owner
		public ImportMatchDetail ownerDetail;
	}

	public static class ImportMatchDetail {
		public String propertyName; // used in pojo
		public Object value;
		public String propertyPath;
	}

	/**
	 * Used when importing (pojo Json) that only uses ImportMatches (does not have pkey property). This will find the correct object, or
	 * create and populate it.
	 *
	 * @param oaObjFrom    object that has references based on importMatch value (not f/pkey)
	 * @param liTo         object to find using import match.
	 * @param mapNameValue name(s)/value(s) for importMatch properties
	 */
	public static void process(final ImportMatch importMatch) {
		if (importMatch == null) {
			return;
		}
		if (importMatch.fromObject == null || importMatch.liTo == null) {
			return;
		}

		// check to see if the importMatch is null
		for (ImportMatchDetail imd : importMatch.importMatchDetails) {
			if (imd.value == null) {
				importMatch.fromObject.setProperty(importMatch.liTo.getName(), null);
				return;
			}
		}

		OAObject obj = (OAObject) importMatch.liTo.getValue(importMatch.fromObject);
		if (obj != null) {
			return; // already exists
		}

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(importMatch.fromObject);

		String sql = "";
		Object[] params = new Object[] {};

		for (ImportMatchDetail imd : importMatch.importMatchDetails) {
			if (OAString.isNotEmpty(sql)) {
				sql += " AND ";
			}
			sql += imd.propertyPath + " = ?";
			params = OAArray.add(Object.class, params, imd.value);
		}

		// check to see if there is an Owner for liTo
		OALinkInfo liToOwner = null;
		for (OALinkInfo li : importMatch.liTo.getToObjectInfo().getLinkInfos()) {
			if (li.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}
			OALinkInfo rli = li.getReverseLinkInfo();
			if (rli.getType() != OALinkInfo.TYPE_MANY) {
				continue;
			}
			if (rli.getOwner()) {
				liToOwner = li;
				break;
			}
		}
		OAObject objOwner = null; // owner of importMatch.liTo

		// this will add additional matching based on the link rules (ex:  equalPropertyPath)
		final OAObjectInfo oiTo = importMatch.liTo.getToObjectInfo();
		final String[] importMatchPropertyNames = oiTo.getImportMatchPropertyNames();

		boolean bWasSearched = false;
		if (importMatchPropertyNames != null && importMatchPropertyNames.length > 0) {
			String ppFromObjectEqual = importMatch.liTo.getReverseLinkInfo().getEqualPropertyPath();
			String ppToObjectEqual = importMatch.liTo.getEqualPropertyPath();

			if (OAString.isNotEmpty(ppFromObjectEqual) && OAString.isNotEmpty(ppToObjectEqual)) {
				Object val = importMatch.fromObject.getProperty(ppFromObjectEqual);

				if (OAString.isNotEmpty(sql)) {
					sql += " AND ";
				}
				sql += ppToObjectEqual + " = ?";
				params = OAArray.add(params, val);

				if (liToOwner != null && val instanceof OAObject) {
					OAPropertyPath ppx = new OAPropertyPath(importMatch.liTo.getToClass(), ppToObjectEqual);
					if (liToOwner == ppx.getEndLinkInfo() && ppx.getLinkInfos().length == 1) {
						objOwner = (OAObject) val;
					}
				}

				OALinkInfo li = oiTo.getLinkInfo(ppToObjectEqual);
				if (li != null) {
					OALinkInfo rli = li.getReverseLinkInfo();
					Object objx = rli.getValue(val);
					if (objx instanceof Hub) {
						Hub hub = (Hub) objx;

						OAFinder finder = new OAFinder();
						OAQueryFilter filter = new OAQueryFilter(oiTo.getForClass(), sql, params);
						finder.addFilter(filter);
						obj = finder.findFirst(hub);

						bWasSearched = true;
					} else if (objx instanceof OAObject) {
						OAObject oaobj = (OAObject) objx;

						OAFinder finder = new OAFinder();
						OAQueryFilter filter = new OAQueryFilter(oiTo.getForClass(), sql, params);
						finder.addFilter(filter);
						obj = finder.findFirst(oaobj);

						bWasSearched = true;
					}
				}
			}
		}

		if (!bWasSearched) {
			OASelect sel = new OASelect(importMatch.liTo.getToClass(), sql, params, "");
			obj = sel.next();
			sel.close();

			if (obj == null) {
				OAFinder finder = new OAFinder();
				OAQueryFilter filter = new OAQueryFilter(importMatch.liTo.getToClass(), sql, params);
				finder.addFilter(filter);

				obj = (OAObject) OAObjectCacheDelegate.find(importMatch.liTo.getToClass(), finder);
			}
		}

		if (obj == null) {
			obj = (OAObject) OAObjectReflectDelegate.createNewObject(importMatch.liTo.getToClass());

			for (ImportMatchDetail detail : importMatch.importMatchDetails) {
				createHierObjects(obj, OAObjectInfoDelegate.getOAObjectInfo(obj), detail.propertyPath, detail.value);
			}

			if (objOwner != null) {
				obj.setProperty(liToOwner.getName(), objOwner);
			}
		}
		importMatch.fromObject.setProperty(importMatch.liTo.getName(), obj);
	}

	protected static void createHierObjects(final OAObject objThis, final OAObjectInfo oiThis, final String propertyPath,
			final Object value) {

		OAPropertyPath pp = new OAPropertyPath(oiThis.getForClass(), propertyPath);
		OALinkInfo[] linkInfos = pp.getLinkInfos();

		if (linkInfos == null || linkInfos.length == 0) {
			objThis.setProperty(pp.getEndPropertyInfo().getName(), value);
			return;
		}
		final OALinkInfo liNext = linkInfos[0];
		final OAObjectInfo oiNext = liNext.getToObjectInfo();

		final String propertyPathNext = OAString.field(propertyPath, '.', 2, 999);

		final String sql = propertyPathNext + " = ?";
		final Object[] params = new Object[] { value };

		OASelect sel = new OASelect(oiNext.getForClass(), propertyPathNext + " = ?", params, "");
		sel.select();
		OAObject objNext = sel.next();
		sel.close();

		if (objNext == null) {
			OAFinder finder = new OAFinder();
			OAQueryFilter filter = new OAQueryFilter(oiNext.getForClass(), sql, params);
			finder.addFilter(filter);
			objNext = (OAObject) OAObjectCacheDelegate.find(oiNext.getForClass(), finder);
		}

		if (objNext == null) {
			boolean b = OAThreadLocalDelegate.isLoading();
			if (b) {
				OAThreadLocalDelegate.setLoading(false);
			}

			objNext = (OAObject) OAObjectReflectDelegate.createNewObject(oiNext.getForClass());

			if (b) {
				OAThreadLocalDelegate.setLoading(true);
			}

			final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

			createHierObjects(objNext, oiNext, propertyPathNext, value);
		}
		objThis.setProperty(liNext.getName(), objNext);
	}

}
