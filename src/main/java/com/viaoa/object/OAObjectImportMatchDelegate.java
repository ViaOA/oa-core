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
 * Find (or create) OAObject based on values from import matches.
 * <p>
 * This is mostly used by POJO classes (which could be generated by OABuilder) that are created and used for JSON, that dont have the pkey
 * property for the class (ex: autoSeq Id prop). Instead, there are properties or links that are importMatches that can be used to find the
 * matching object.
 * <p>
 * The "trick" is that there could be multiple importMatches (properties or links), and it is not totally unique by itself, but requires
 * matching other values, and following rules in linked references. <br>
 * If the importMatch is a link, it could have a hierarchy of links (recursive). <br>
 * If not found, this will create the object and it's (optional) hierarchy of links that would then satisfy the rules for the importMatch
 * and other relationship (link) requirements.
 * <p>
 *
 * @author vvia
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