package com.viaoa.graph.object;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

public class OAObjectImportMatchService {
	private static final Logger LOG = Logger.getLogger(OAObjectImportMatchService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	
    public OAObjectImportMatchService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }

	/**
	 * Defines a single import-match operation used during JSON/POJO import
	 * when a target OAObject must be located or created without relying on
	 * primary keys.  
	 *
	 * <p>An ImportMatch bundles together:</p>
	 * <ul>
	 *   <li>The source object participating in the import.</li>
	 *   <li>The link definition pointing to the target object type.</li>
	 *   <li>A list of {@link ImportMatchDetail} items, each defining
	 *       a property/value rule used to identify the target object.</li>
	 *   <li>An optional {@code ownerDetail} identifying which match rule
	 *       resolves the owner when the target type is an owned object.</li>
	 * </ul>
	 *
	 * <p>Instances of this class are consumed by
	 * {@link OAObjectImportMatchDelegate#process(ImportMatch)}, which performs
	 * the actual resolution: searching cache, search queries, or creating new
	 * objects and required hierarchy when no match is found.</p>
	 */
	public static class ImportMatch {
		
		public ImportMatch() {
			
		}
		
		/**
		 * The source object from which import-match resolution begins.
		 * Represents the object whose property or link requires identifying
		 * or constructing a corresponding target object.
		 */
		public OAObject fromObject;
		
		/**
		 * Link definition describing the relationship from the source object
		 * to the target object being resolved or created.
		 */
		public OALinkInfo liTo;

		/**
		 * Collection of property/value match definitions used to identify the
		 * correct target object. Each detail corresponds to one matching rule.
		 */
		public final List<ImportMatchDetail> importMatchDetails = new ArrayList<>();

		/**
		 * Optional detail indicating which matching rule identifies the
		 * owner of the target object, when the link-to object is owned.
		 */
		public ImportMatchDetail ownerDetail;
	}

	/**
	 * Represents a single property/value rule used during an import-match
	 * resolution.  
	 *
	 * <p>Each detail corresponds to one matching criterion, defining:</p>
	 * <ul>
	 *   <li>{@code propertyName} — the name used in the POJO or source data.</li>
	 *   <li>{@code value} — the imported value used for lookup.</li>
	 *   <li>{@code propertyPath} — the full OA property path to apply when
	 *       building search queries or creating required hierarchy objects.</li>
	 * </ul>
	 *
	 * <p>Multiple details can be combined to form a composite uniqueness
	 * definition (“identity by content”), enabling OA to reconstruct or
	 * locate objects without primary keys.</p>
	 */
	public static class ImportMatchDetail {
		
		public ImportMatchDetail() {
			
		}
		
		/**
		 * Name of the property on the source POJO used during import.
		 * May differ from the property path if the rule is defined
		 * against a linked object's field.
		 */
		public String propertyName; 
		
		/**
		 * The value supplied by the import source used to identify or
		 * create the target object. Null values indicate that no target
		 * object should be created.
		 */
		public Object value;

		/**
		 * Full property path (possibly multi-level) used for matching.
		 * This path is used both for query-building and for creation of
		 * any required intermediate hierarchical objects.
		 */
		public String propertyPath;
	}

	
	
	/**
	 * Resolves or creates the target object defined by the supplied
	 * {@link ImportMatch}. Performs validation, evaluates match
	 * properties, builds a query, and searches for an existing object
	 * via {@link OASelect}, {@link OAFinder}, or
	 * {@link OAObjectCacheDelegate}. If no match is found, constructs
	 * a new object and initializes required hierarchy and owner links.
	 *
	 * @param importMatch definition of the source object, link info,
	 *        and match property values used to locate or create the
	 *        target object.
	 */
	public void process(final ImportMatch importMatch) {
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
	
	
	/**
	 * Recursively ensures that all objects required by a property path
	 * exist. Navigates each segment of the path, searching for an
	 * existing object via {@link OASelect} or
	 * {@link OAObjectCacheDelegate}. If none is found, creates a new
	 * object using reflection. Finally assigns the provided value to
	 * the terminal property of the hierarchy.
	 *
	 * @param objThis       the current object in the traversal.
	 * @param oiThis        metadata describing objThis.
	 * @param propertyPath  full path leading to the property to set.
	 * @param value         value to assign at the end of the path.
	 */
	protected void createHierObjects(final OAObject objThis, final OAObjectInfo oiThis, final String propertyPath,
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

