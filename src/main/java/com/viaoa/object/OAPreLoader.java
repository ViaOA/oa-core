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
package com.viaoa.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.datasource.jdbc.OADataSourceJDBC;
import com.viaoa.datasource.jdbc.db.ManyToMany;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.graph.object.OAObjectPropertyService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

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
	 * {@link OAPropertyPath}.
	 */
	private String strPropertyPath;

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
		this.strPropertyPath = propPath;
	}

	/**
	 * Loads the objects defined by the configured property path starting at
	 * the root class. Initializes a property-path representation and delegates
	 * object loading to the internal {@link #_load(OALinkInfo[])} method.
	 * <p>
	 * Behavior visible in this method:
	 * <ul>
	 *   <li>Returns {@code null} immediately if the root class is not set.</li>
	 *   <li>Creates an {@link OAPropertyPath} and extracts link information
	 *       when a property path is provided.</li>
	 *   <li>Temporarily sets thread-local loading mode using
	 *       {@link OAThreadLocalDelegate#setLoading(boolean)} to suppress
	 *       events.</li>
	 *   <li>Delegates graph loading to {@code _load}.</li>
	 * </ul>
	 *
	 * @return a list of loaded root objects, or {@code null} if root class is missing
	 */
	public ArrayList load() {
		if (classFrom == null) {
			return null;
		}

		OAPropertyPath propertyPath = null;
		OALinkInfo[] linkInfos = null;

		if (OAString.isNotEmpty(strPropertyPath)) {
			propertyPath = new OAPropertyPath(classFrom, strPropertyPath);
			linkInfos = propertyPath.getLinkInfos();
		}
		ArrayList al = null;
		try {
			OARuntime.get().threadLocals().setLoading(true);
			al = _load(linkInfos);
		} finally {
			OARuntime.get().threadLocals().setLoading(false);
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
	protected ArrayList _load(OALinkInfo[] linkInfos) {
		final HashMap<Class, ArrayList> hm = new HashMap<>();

		final ArrayList al = load(classFrom, null);
		hm.put(classFrom, al);

		if (linkInfos != null) {
			for (OALinkInfo linkInfo : linkInfos) {
				Class c = linkInfo.getToClass();

				if (linkInfo.getType() != OALinkInfo.MANY) {
					break;
				}

				ArrayList alx = hm.get(c);
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
	protected void loadOtoM(OALinkInfo linkInfo, ArrayList alMany) {
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
			OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph((OAObject) objOne).objects().getOAObjectPropertyService();
			Object objOneHub = srvcOAObjectProperty.getProperty((OAObject) objOne, liMany.getName(), false, true);
			if (objOneHub instanceof Hub) {
				hub = (Hub) objOneHub;
			} else {
				hub = new Hub(liMany.getToClass());
				srvcOAObjectProperty.setProperty((OAObject) objOne, liMany.getName(), hub);
			}
			hub.add(objFromMany);
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
		OADataSource ds = OADataSource.getDataSource(linkInfo.getToClass());
		if (!(ds instanceof OADataSourceJDBC)) {
			return;
		}

		OALinkInfo liA = linkInfo;
		OALinkInfo liB = linkInfo.getReverseLinkInfo();
		if (liB == null) {
			return;
		}

		Class classA = liB.getToClass();
		Class classB = liA.getToClass();

		ArrayList<ManyToMany> alManyToMany = ((OADataSourceJDBC) ds).getManyToMany(linkInfo);
		if (alManyToMany == null) {
			return;
		}

    	OAGraph og = OARuntime.get().graph(classA);
    	final OAObjectCacheService srvcObjectCacheA = og.objects().getOAObjectCacheService();
    	og = OARuntime.get().graph(classB);
    	final OAObjectCacheService srvcObjectCacheB = og.objects().getOAObjectCacheService();
		
		for (ManyToMany mm : alManyToMany) {
			Object objA = srvcObjectCacheA.get(classA, mm.ok1);
			Object objB = srvcObjectCacheB.get(classB, mm.ok2);
			if (objA == null || objB == null) {
				continue;
			}

			if (!liA.getPrivateMethod()) {
				Hub hub;
				OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph((OAObject) objA).objects().getOAObjectPropertyService();
				Object objx = srvcOAObjectProperty.getProperty((OAObject) objA, liA.getName(), false, true);
				if (objx instanceof Hub) {
					hub = (Hub) objx;
				} else {
					hub = new Hub(classB);
					srvcOAObjectProperty.setProperty((OAObject) objA, liA.getName(), hub);
				}
				hub.add(objB);
			}

			if (!liB.getPrivateMethod()) {
				Hub hub;
				OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph((OAObject) objB).objects().getOAObjectPropertyService();
				Object objx = srvcOAObjectProperty.getProperty((OAObject) objB, liB.getName(), false, true);
				if (objx instanceof Hub) {
					hub = (Hub) objx;
				} else {
					hub = new Hub(classA);
					srvcOAObjectProperty.setProperty((OAObject) objB, liB.getName(), hub);
				}
				hub.add(objA);
			}
		}
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
	protected ArrayList load(Class clazz, final OALinkInfo linkInfo) {
		OASelect sel = new OASelect<>(clazz);
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(clazz).objects().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getObjectInfo(clazz);
		OALinkInfo liRecursive = srvcObjectInfo.getRecursiveLinkInfo(oi, OALinkInfo.MANY);

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

		ArrayList al = new ArrayList<>();
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
	protected void loadRecursive(Class clazz, ArrayList al, OALinkInfo liMany) {
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
			OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph((OAObject) fParent).objects().getOAObjectPropertyService();
			Object objx = srvcOAObjectProperty.getProperty((OAObject) fParent, liMany.getName(), false, true);
			if (objx instanceof Hub) {
				hub = (Hub) objx;
			} else {
				hub = new Hub(clazz);
				srvcOAObjectProperty.setProperty((OAObject) fParent, liMany.getName(), hub);
			}
			hub.add(f);
		}
	}

}
