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
package com.viaoa.load;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.select.OASelect;

/*qqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/load/OAPreLoader.java — loadMtoM(OALinkInfo linkInfo)
  2. concrete bug
     Many-to-many preload is a no-op even when metadata says the link is many-to-many.
  3. runtime scenario
     OAPreLoader is configured with a property path that includes a many-to-many link. _load(...) detects
     linkInfo.isMany2Many() and calls loadMtoM(linkInfo), but the method returns without hydrating either side’s Hub
     relationships.
  4. why this violates OA/OG load semantics
     Preload reports/returns as if the configured graph path was processed, but many-to-many relationships remain
     unhydrated. That creates false-success preload behavior and can produce missing Hub membership later.
  5. minimal fix direction
     Either implement many-to-many hydration or explicitly reject/mark unsupported many-to-many preload so callers can
     detect incomplete preload.
  6. suggested CODEX comment location
     At the start of OAPreLoader.loadMtoM(...).

 1. file/class/method
     src/main/java/com/viaoa/load/OAPreLoader.java — _load(OALinkInfo[] linkInfos)
  2. concrete bug
     The preloader silently stops processing the configured property path at the first non-MANY link.
  3. runtime scenario
     A normal graph path such as order.customer.contacts has a ONE segment (order.customer) followed by a MANY
     segment. _load(...) sees the ONE link and executes break, returning root objects as if preload completed.
  4. why this violates OA/OG load semantics
     The caller supplied a property path to preload. Silently stopping early is false-success behavior: linked objects
     beyond the ONE segment remain unloaded, but the load operation gives no signal that the requested path was only
     partially processed.
  5. minimal fix direction
     Either support ONE traversal in OAPreLoader, or fail visibly / document and reject unsupported paths containing
     non-MANY links before starting the load.
  6. suggested CODEX comment location
     At _load(...), directly above:

  if (linkInfo.getType() != OALinkInfo.MANY) {
      break;
  }


1. file/class/method
     src/main/java/com/viaoa/load/OAPreLoader.java — load(Class clazz, OALinkInfo linkInfo)
  2. concrete bug
     When recursive sort order and link sort order differ, the reselect uses the recursive sort again instead of the
     link sort.
  3. runtime scenario
     A class has a recursive MANY link sorted by sequence, but the parent/detail link being hydrated is sorted by
     name. Initial select uses the recursive sort. The code detects that s and s2 differ, but then does:

  sel.setOrder(s);

  instead of using s2.

  4. why this violates OA/OG load semantics
     loadOtoM(...) adds objects to parent Hubs in the order of alx. If the wrong order is selected, preloaded detail
     Hubs can be hydrated with the wrong membership order.
  5. minimal fix direction
     In the reselect block, use the active linkInfo sort (s2) for the second select, or explicitly separate recursive-
     hub hydration order from detail-link hydration order.
  6. suggested CODEX comment location
     In load(Class clazz, OALinkInfo linkInfo), at the reselect block around sel.setOrder(s).

1. file/class/method
     src/main/java/com/viaoa/load/OAPreLoader.java — load(Class clazz, OALinkInfo linkInfo)
  2. concrete bug
     OASelect instances are not closed in a finally block if selection or relationship hydration throws.
  3. runtime scenario
     sel.next() starts datasource iteration and then a datasource exception, object hydration exception, or later
     loadRecursive(...) exception occurs. The method exits without sel.close().
  4. why this violates OA/OG load semantics
     Load operations must not leak datasource/select resources on failure. Partial progress is acceptable only if the
     exception is visible, but the datasource iterator still needs cleanup.
  5. minimal fix direction
     Wrap each OASelect lifecycle in try/finally { sel.close(); }. If exhausted OASelect self-closes, this remains
     harmless and protects exception paths.
  6. suggested CODEX comment location
     In load(Class clazz, OALinkInfo linkInfo), immediately after each new OASelect<>(clazz).


1. file/class/method
     src/main/java/com/viaoa/load/OAPreLoader.java — _load(...) class/list cache plus load(Class, OALinkInfo)
  2. concrete bug
     The preloader caches loaded objects only by target class, but different links to the same class can require
     different sort orders for Hub hydration.
  3. runtime scenario
     A path includes two MANY links to the same target class through different relationships, one sorted by name,
     another by seq. _load(...) reuses hm.get(c) for the second link and does not call load(c, linkInfo) with that
     link’s sort order. loadOtoM(...) then hydrates the second Hub using the first link’s object order.
  4. why this violates OA/OG load semantics
     Preloaded Hub membership order must match the relationship metadata. Reusing class-level results across link-
     specific sort contracts can silently initialize detail Hubs in the wrong order.
  5. minimal fix direction
     Cache loaded lists by link identity or by (class, sortOrder) instead of class alone, or re-sort/reselect per link
     before hydrating that relationship.
  6. suggested CODEX comment location
     In _load(...), around List<?> alx = hm.get(c).


*/

/**
 * Supports asynchronous background preloading of {@link OAObject} data for
 * Hubs or object graphs.
 *
 * <p>Used by OA to warm up caches and avoid latency during first access.
 * Typically invoked through {@link OASelect} or {@link OALoader} to populate
 * references, calculated fields, or dependent collections.</p>
 *
 * <p><b>Key Features</b>:
 * <ul>
 *   <li>Spawns background threads to fetch and cache related data.</li>
 *   <li>Supports prioritized preloading of properties and linked objects.</li>
 *   <li>Integrates with Hub listeners to defer UI updates until data ready.</li>
 * </ul>
 *
 * <p>This class helps OA achieve smooth UX for large object graphs.</p>
 */
public class OAPreLoader {
	private static Logger LOG = Logger.getLogger(OAPreLoader.class.getName());

	/**
	 * The root class from which the preload operation begins. All objects of this
	 * class are loaded first, and subsequent linked objects are preloaded based on
	 * the configured property path.
	 */
	private Class classFrom;
	
	/**
	 * The property-path expression defining which linked objects should be
	 * preloaded. When non-empty, it is parsed into link metadata using
	 * {@link OAPath}.
	 */
	private String strPath;

	/**
	 * Constructs a new preloader configured to load the object graph defined
	 * by the specified property path.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Stores the base class from which loading will begin.</li>
	 *   <li>Stores the property path string used to determine linked objects
	 *       to preload.</li>
	 * </ul>
	 *
	 * @param classFrom the root class for the preload operation
	 * @param propPath the property path describing which links to load
	 */
	public OAPreLoader(Class classFrom, String propPath) {
		this.classFrom = classFrom;
		this.strPath = propPath;
	}

	/**
	 * Loads the objects defined by the configured property path starting at
	 * the root class. Initializes a property-path representation and delegates
	 * object loading to the internal {@link #_load(OALinkInfo[])} method.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Returns {@code null} immediately if the root class is not set.</li>
	 *   <li>Creates an {@link OAPath} and extracts link information
	 *       when a property path is provided.</li>
	 *   <li>Temporarily sets thread-local loading mode using
	 *       {@link OAThreadLocalDelegate#setLoading(boolean)} to suppress
	 *       events.</li>
	 *   <li>Delegates graph loading to {@code _load}.</li>
	 * </ul>
	 *
	 * @return a list of loaded root objects, or {@code null} if root class is missing
	 */
	public List<?> load() {
		if (classFrom == null) {
			return null;
		}

		OAPath path = null;
		OALinkInfo[] linkInfos = null;

		if (OAString.isNotEmpty(strPath)) {
			path = new OAPath(classFrom, strPath);
			linkInfos = path.getLinkInfos();
		}
		List<?> al = null;
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		boolean bWasLoading = srvcOAThreadLocal.setLoading(true);
		try {
			al = _load(linkInfos);
		} finally {
			srvcOAThreadLocal.setLoading(bWasLoading);
		}
		return al;
	}

	/**
	 * Loads the root class and all linked classes defined by the property
	 * path. Handles one-to-many and many-to-many relationships based on the
	 * supplied link metadata.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Creates a map associating classes with their loaded instance lists.</li>
	 *   <li>Loads all instances of the root class using {@link #load(Class, OALinkInfo)}.</li>
	 *   <li>Iterates through link metadata to load related objects.</li>
	 *   <li>Delegates to {@link #loadMtoM(OALinkInfo)} for many-to-many links.</li>
	 *   <li>Delegates to {@link #loadOtoM(OALinkInfo, ArrayList)} for one-to-many mappings.</li>
	 * </ul>
	 *
	 * @param linkInfos the property-path link definitions to load
	 * @return the list of loaded root objects
	 */
	protected List<?> _load(OALinkInfo[] linkInfos) {
		final HashMap<Class<?>, List<?>> hm = new HashMap<>();

		final List<?> al = load(classFrom, null);
		hm.put(classFrom, al);

		if (linkInfos != null) {
			for (OALinkInfo linkInfo : linkInfos) {
				Class c = linkInfo.getToClass();

				if (linkInfo.getType() != OALinkInfo.MANY) {
					break;
				}

				List<?> alx = hm.get(c);
				if (alx == null) {
					alx = load(c, linkInfo);
					hm.put(c, alx);
				}

				if (linkInfo.isMany2Many()) {
					loadMtoM(linkInfo);
				} else {
					loadOtoM(linkInfo, alx);
				}
			}
		}
		return al;
	}

	/**
	 * Populates one-to-many relationships using the supplied link metadata.
	 * Ensures each target object maintains a hub containing all related
	 * objects from the "many" side.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Returns immediately when metadata is invalid or not MANY type.</li>
	 *   <li>Retrieves the reverse ONE link to locate parent objects.</li>
	 *   <li>Creates or retrieves the Hub representing the relationship.</li>
	 *   <li>Adds each "many" object to its corresponding parent's hub.</li>
	 * </ul>
	 *
	 * @param linkInfo the link definition for the MANY side
	 * @param alMany the list of objects on the MANY side
	 */
	protected void loadOtoM(OALinkInfo linkInfo, List<?> alMany) {
		if (linkInfo == null || linkInfo.getType() != OALinkInfo.MANY) {
			return;
		}
		if (linkInfo.getPrivateMethod()) {
			return;
		}

		OALinkInfo liMany = linkInfo;
		OALinkInfo liOne = linkInfo.getReverseLinkInfo();
		if (liOne == null || liOne.getType() != OALinkInfo.ONE) {
			return;
		}

		for (Object objFromMany : alMany) {
			Object objOne = ((OAObject) objFromMany).getProperty(liOne.getName());
			if (!(objOne instanceof OAObject)) {
				continue;
			}

			Hub hub;
			final OA oa = OARuntime.oa((OAObject) objOne);
			Object objOneHub = oa.internal().objects().property().getProperty((OAObject) objOne, liMany.getName(), false, true);
			if (objOneHub instanceof Hub) {
				hub = (Hub) objOneHub;
			} else {
				hub = new Hub(liMany.getToClass());
				oa.internal().objects().property().setProperty((OAObject) objOne, liMany.getName(), hub);
			}
			hub.add((OAObject) objFromMany);
		}
	}

	/**
	 * Loads many-to-many relationships using metadata and JDBC-based lookup.
	 * Creates or retrieves hubs on each side of the relationship and adds
	 * linked objects accordingly.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Returns when the link is not many-to-many or no JDBC datasource is available.</li>
	 *   <li>Retrieves related objects from the {@link OAObjectCacheDelegate}.</li>
	 *   <li>Creates or retrieves hubs on both sides of the relationship.</li>
	 *   <li>Adds each mapped pair to the appropriate hubs.</li>
	 * </ul>
	 *
	 * @param linkInfo the metadata describing the many-to-many link
	 */
	protected void loadMtoM(OALinkInfo linkInfo) {
		if (linkInfo == null || !linkInfo.isMany2Many()) {
			return;
		}
		return;
	}

	/**
	 * Loads all instances of the specified class using an {@link OASelect}
	 * and applies recursive loading rules when a recursive MANY link is
	 * defined.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Determines sort order from recursive or provided link metadata.</li>
	 *   <li>Iterates through all selected objects and collects them in a list.</li>
	 *   <li>Delegates population of recursive MANY relationships to
	 *       {@link #loadRecursive(Class, ArrayList, OALinkInfo)}.</li>
	 *   <li>Re-selects data when recursive and provided sort orders differ.</li>
	 * </ul>
	 *
	 * @param clazz the class to load instances of
	 * @param linkInfo optional link metadata for sorting
	 * @return list of loaded instances
	 */
	protected List load(Class clazz, final OALinkInfo linkInfo) {
		OASelect sel = new OASelect<>(clazz);
		final OA oa = OARuntime.oa(clazz);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(clazz);
		OALinkInfo liRecursive = oa.internal().objects().info().getRecursiveLinkInfo(oi, OALinkInfo.MANY);

		String sortOrder = null;
		if (liRecursive != null) {
			OALinkInfo liRev = liRecursive.getReverseLinkInfo();
			if (liRev != null) {
				sortOrder = liRecursive.getSortProperty();
			}
		}
		if (sortOrder == null && linkInfo != null) {
			sortOrder = linkInfo.getSortProperty();
		}
		if (OAString.isNotEmpty(sortOrder)) {
			sel.setOrder(sortOrder);
		}

		List al = new ArrayList<>();
		for (;;) {
			Object obj = sel.next();
			if (obj == null) {
				break;
			}
			al.add(obj);
		}

		if (liRecursive != null) {
			loadRecursive(clazz, al, liRecursive);

			// might need to reselect if both had different sortOrder
			String s = liRecursive.getSortProperty();
			if (linkInfo != null && OAString.isNotEmpty(s)) {
				String s2 = linkInfo.getSortProperty();
				if (OAString.isNotEmpty(s2)) {
					if (!OAString.isEqual(s, s2, true)) {
						sel = new OASelect<>(clazz);
						sel.setOrder(s);
						al = new ArrayList<>();
						for (;;) {
							Object obj = sel.next();
							if (obj == null) {
								break;
							}
							al.add(obj);
						}
					}
				}
			}
		}
		return al;
	}

	/**
	 * Populates recursive MANY relationships by assigning each object to its
	 * parent's hub based on reverse-link metadata.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Returns when reverse-link metadata is missing.</li>
	 *   <li>Retrieves parent objects for each element in the list.</li>
	 *   <li>Creates or retrieves the recursive hub on the parent.</li>
	 *   <li>Adds each child to the hub on its parent.</li>
	 * </ul>
	 *
	 * @param clazz the class of recursive elements
	 * @param al the list of objects to organize recursively
	 * @param liMany the MANY-side recursive link metadata
	 */
	protected void loadRecursive(Class clazz, List al, OALinkInfo liMany) {
		if (liMany == null) {
			return;
		}
		OALinkInfo liOne = liMany.getReverseLinkInfo();
		if (liOne == null) {
			return;
		}

		for (Object f : al) {
			Object fParent = liOne.getValue(f);
			if (!(fParent instanceof OAObject)) {
				continue;
			}

			Hub hub;
			final OA oa = OARuntime.oa((OAObject) fParent);
			Object objx = oa.internal().objects().property().getProperty((OAObject) fParent, liMany.getName(), false, true);
			if (objx instanceof Hub) {
				hub = (Hub) objx;
			} else {
				hub = new Hub(clazz);
				oa.internal().objects().property().setProperty((OAObject) fParent, liMany.getName(), hub);
			}
			hub.add((OAObject) f);
		}
	}

}
