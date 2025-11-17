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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;

/**
 * Low-level delegate that connects {@link OAObject} lifecycle operations
 * (save, delete, assign ID, refresh) to the configured {@link com.viaoa.datasource.OADataSource}.
 * <p>
 * This class provides the internal persistence bridge for the OA framework,
 * enabling {@link OAObjectSaveDelegate}, {@link OAObjectDeleteDelegate}, and
 * other components to interact with the DataSource layer without introducing
 * dependencies or reflection overhead.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>ID Assignment:</b> Coordinates creation of unique primary keys by
 *       calling {@link OADataSource#assignId(OAObject)} and tracking assignment
 *       state through {@code hmAssigningId} to prevent redundant change events.</li>
 *   <li><b>Persistence Operations:</b> Routes insert, update, and delete
 *       requests to the correct {@link OADataSource} for the object's class.</li>
 *   <li><b>Object Retrieval:</b> Provides DataSource lookups by key or
 *       {@link OAObjectKey}, including refresh and blob value access.</li>
 *   <li><b>Reference Updates:</b> Supports targeted link updates via
 *       {@link #removeReference(OAObject, OALinkInfo)} without performing
 *       full object saves.</li>
 *   <li><b>Thread Safety:</b> Uses a {@link ConcurrentHashMap} to record ID
 *       assignment activity across threads.</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>All methods safely no-op when no {@link OADataSource} is registered
 *       for a class.</li>
 *   <li>No reflection is used; all operations occur through the DataSource API.</li>
 *   <li>Supports any OA-compatible persistence provider (JDBC, REST, memory, etc.).</li>
 * </ul>
 *
 * @see OAObject
 * @see OAObjectSaveDelegate
 * @see OAObjectDeleteDelegate
 * @see com.viaoa.datasource.OADataSource
 * @see OAObjectKeyDelegate
 */
public class OAObjectDSDelegate {
	private static final Logger LOG = Logger.getLogger(OAObjectDSDelegate.class.getName());

    static private final ConcurrentHashMap<Long, Long> hmAssigningId = new ConcurrentHashMap<Long, Long>(17, 0.75F);
	
	/**
	 * Initialize a newly created OAObject.
	 */
	public static void assignId(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		// OADataSource is set up to check isLoading() so that it does not initialize the objects that it is creating
		OADataSource ds = getDataSource(oaObj);
		if (ds != null) {
			try {
				setAssigningId(oaObj, true);
				ds.assignId(oaObj); // datasource might need to set Id property
			} finally {
				setAssigningId(oaObj, false);
			}
		}
	}

    public static Map<Long, Long> getAssigningIdMap() {
        return hmAssigningId;
    }
	
	/**
	 * Flag to know that the DS is assigning the Id, and that the value does not need to be verified by the propertyChange event
	 */
	public static void setAssigningId(OAObject obj, boolean b) {
		if (obj == null) {
			return;
		}
		long g = OAObjectDelegate.getGuid(obj);
		if (b) {
			OAObjectDSDelegate.getAssigningIdMap().put(g, g);
		} else {
			OAObjectDSDelegate.getAssigningIdMap().remove(g);
		}
	}

	public static boolean isAssigningId(OAObject obj) {
		if (obj == null) return false;
		long g = OAObjectDelegate.getGuid(obj);
		return OAObjectDSDelegate.getAssigningIdMap().containsKey(g);
	}

	public static boolean getAssignIdOnCreate(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		// OADataSource is set up to check isLoading() so that it does not initialize the objects that it is creating
		OADataSource ds = getDataSource(oaObj);
		if (ds == null) {
			return false;
		}
		return ds.getAssignIdOnCreate();
	}

	/**
	 * Returns the OADataSource that works with this objects Class.
	 */
	protected static OADataSource getDataSource(Object obj) {
		return OADataSource.getDataSource(obj.getClass());
	}

	protected static boolean hasDataSource(OAObject oaObj) {
		return OADataSource.getDataSource(oaObj.getClass()) != null;
	}

	protected static boolean hasDataSource(Class c) {
		return OADataSource.getDataSource(c) != null;
	}

	protected static boolean supportsStorage(Class clazz) {
		OADataSource ds = OADataSource.getDataSource(clazz);
		return (ds != null && ds.supportsStorage());
	}

	/**
	 * Find the OAObject given a key value. This will look in the Cache and the DataSource.
	 *
	 * @param clazz class of reference of to find.
	 * @param key   can be the value of the key or an OAObjectKey
	 */
	public static OAObject getObject(Class clazz, Object key) {
		if (clazz == null || key == null) {
			return null;
		}
		OADataSource ds = OADataSource.getDataSource(clazz);
		OAObject oaObj = null;
		if (ds != null) {
			if (!(key instanceof OAObjectKey)) {
				key = OAObjectKeyDelegate.createObjectKey(clazz, key);
			}
			oaObj = (OAObject) ds.getObject(clazz, key);
		}
		return oaObj;
	}

	public static void refreshObject(OAObject obj) {
		if (obj == null) {
			return;
		}
		Class clazz = obj.getClass();
		OADataSource ds = OADataSource.getDataSource(clazz);
		if (ds != null) {
			OAObjectKey key = OAObjectKeyDelegate.getKey(obj);
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
			ds.getObject(oi, clazz, key, true); // true=reload all props
		}
	}

	protected static Object getObject(Class clazz, OAObjectKey key) {
		return OADataSource.getObject(clazz, key);
	}

	protected static Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key) {
		OADataSource ds = OADataSource.getDataSource(clazz);
		if (ds == null) {
			return null;
		}
		return ds.getObject(oi, clazz, key, false);
	}

	protected static Object getBlob(OAObject obj, String propName) {
		if (obj == null || propName == null) {
			return null;
		}
		Class clazz = obj.getClass();
		OADataSource ds = OADataSource.getDataSource(clazz);
		if (ds == null) return null;
		return ds.getPropertyBlobValue(obj, propName);
	}

	/* param bFullSave false=dont flag as unchanged, used when object needs to be saved twice. First to create
	    object in datasource so that reference objects can refer to it
	*/
	protected static void save(OAObject oaObj) {
		OADataSource dataSource = getDataSource(oaObj);
		if (dataSource != null) {
			if (oaObj.getNew()) {
				dataSource.insert(oaObj);
			} else {
				dataSource.update(oaObj);
			}
		}
	}

	protected static void saveWithoutReferences(OAObject oaObj) {
		OADataSource dataSource = getDataSource(oaObj);
		if (dataSource != null) {
			if (oaObj.getNew()) {
				dataSource.insertWithoutReferences(oaObj);
			} else {
				// error, should only be used by new objects
			}
		}
	}

	public static void removeReference(OAObject oaObj, OALinkInfo li) {
		if (li == null) {
			return;
		}
		OADataSource dataSource = getDataSource(oaObj);
		if (dataSource != null) {
			if (!oaObj.getNew()) {
				dataSource.update(oaObj, new String[] { li.getName() }, null); // only update the link property name (which is null)
			}
		}
	}

	public static void save(OAObject obj, boolean bInsert) {
		OADataSource dataSource = getDataSource(obj);
		if (dataSource != null) {
			if (bInsert) {
				dataSource.insert(obj);
			} else {
				dataSource.update(obj);
			}
		}
	}

	/**
	 * called after all listeners have been called. It will find the OADataSource to use and call its "delete(this)"
	 */
	public static void delete(OAObject obj) {
		if (obj == null) {
			return;
		}
		OADataSource ds = OADataSource.getDataSource(obj.getClass());
		if (ds != null) {
			ds.delete(obj);
		}
	}

	public static boolean allowIdChange(Class c) {
		OADataSource ds = OADataSource.getDataSource(c);
		return (ds == null || ds.getAllowIdChange());
	}

	public static Object getObject(OAObject oaObj) {
		OADataSource ds = OADataSource.getDataSource(oaObj.getClass());
		// todo: check this out: if (ds == null || ds.isAssigningId(oaObj)) return null;  // datasource could be assigning the Id to a unique value
		return ds.getObject(oaObj.getClass(), OAObjectKeyDelegate.getKey(oaObj));
	}
}
