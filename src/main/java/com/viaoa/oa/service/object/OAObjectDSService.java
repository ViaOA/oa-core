package com.viaoa.oa.service.object;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

public abstract class OAObjectDSService {
	private static final Logger LOG = Logger.getLogger(OAObjectDSService.class.getName());
	
    public OAObjectDSService() {
    }

    /**
     * Assigns a primary key value to the specified object using the
     * configured {@link OADataSource}. While the DataSource is assigning
     * the ID, the assigning-id flag is set to suppress verification
     * during property-change events.
     *
     * @param oaObj the object to initialize with an assigned ID;
     *              ignored if {@code null}
     */
	public void assignId(OAObject oaObj) {
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
    
    private final ConcurrentHashMap<UUID, Long> hmAssigningId = new ConcurrentHashMap<>(17, 0.75F);
	
	/**
	 * Returns the internal map tracking GUIDs of objects currently
	 * undergoing ID assignment.
	 *
	 * @return the assigning-ID tracking map
	 */
    public Map<UUID, Long> getAssigningIdMap() {
        return hmAssigningId;
    }

    /**
     * Sets or clears the assigning-ID flag for the specified object.
     * When enabled, verification of ID changes during property-change
     * events is suppressed.
     *
     * @param obj the object whose flag is being updated; ignored if null
     * @param b {@code true} to mark ID assignment in progress,
     *          {@code false} to clear the flag
     */
	public void setAssigningId(OAObject obj, boolean b) {
		if (obj == null) {
			return;
		}
		UUID g = callGuidGetGuid(obj);
		if (b) {
			getAssigningIdMap().put(g, 0L);
		} else {
			getAssigningIdMap().remove(g);
		}
	}

	/**
	 * Determines whether the specified object is currently flagged as
	 * undergoing ID assignment.
	 *
	 * @param obj the object to check
	 * @return {@code true} if the object’s GUID is present in the
	 *         assigning-ID map; otherwise {@code false}
	 */
	public boolean isAssigningId(OAObject obj) {
		if (obj == null) return false;
		UUID g = callGuidGetGuid(obj);
		return getAssigningIdMap().containsKey(g);
	}
    
	/**
	 * Determines whether the DataSource for the object's class has been
	 * configured to assign IDs automatically when objects are created.
	 *
	 * @param oaObj the object whose DataSource is queried
	 * @return {@code true} if ID assignment on creation is enabled,
	 *         otherwise {@code false}
	 */
	public boolean getAssignIdOnCreate(OAObject oaObj) {
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
	 * Returns the {@link OADataSource} associated with the class of the
	 * specified object.
	 *
	 * @param obj the object whose DataSource is requested
	 * @return the DataSource for the object’s class, or {@code null}
	 */
	public OADataSource getDataSource(Object obj) {
		if (obj == null) return null;
		return OARuntime.datasource().get(obj.getClass());
	}

	public OADataSource getDataSource(Class<?> c) {
		return OARuntime.datasource().get(c);
	}
	
	/**
	 * Indicates whether a DataSource exists for the specified object's
	 * class.
	 *
	 * @param oaObj the object to evaluate
	 * @return {@code true} if a DataSource is registered; otherwise {@code false}
	 */
	protected static boolean hasDataSource(OAObject oaObj) {
		return oaObj != null && OARuntime.datasource().get(oaObj.getClass()) != null;
	}

	/**
	 * Indicates whether a DataSource exists for the specified class.
	 *
	 * @param c the class to evaluate
	 * @return {@code true} if a DataSource is registered; otherwise {@code false}
	 */
	public static boolean hasDataSource(Class<? extends OAObject> c) {
		return c != null && OARuntime.datasource().get(c) != null;
	}
	
	/**
	 * Determines whether the DataSource for the specified class supports
	 * persistent storage.
	 *
	 * @param clazz the class whose DataSource capabilities are checked
	 * @return {@code true} if the DataSource exists and supports storage,
	 *         otherwise {@code false}
	 */
	public boolean supportsStorage(Class<? extends OAObject> clazz) {
		OADataSource ds = OARuntime.datasource().get(clazz);
		return (ds != null && ds.supportsStorage());
	}

	/**
	 * Retrieves an object from the DataSource using the specified class
	 * and key. The key may be a raw value or an {@link OAObjectKey}; if it
	 * is not already a key, one is created automatically.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param key the key value or an {@code OAObjectKey}
	 * @return the retrieved object, or {@code null} if not found or no
	 *         DataSource is available
	 */
	public OAObject getObject(Class<? extends OAObject> clazz, Object key) {
		if (clazz == null || key == null) {
			return null;
		}
		OADataSource ds = OARuntime.datasource().get(clazz);
		OAObject oaObj = null;
		if (ds != null) {
			if (!(key instanceof OAObjectKey)) {
				key = callKeyCreateObjectKey(clazz, key);
			}
			oaObj = (OAObject) ds.getObject(clazz, key);
		}
		return oaObj;
	}

	/**
	 * Refreshes all properties of the specified object by requesting a
	 * reloaded version from the DataSource. The object's full property set
	 * is reloaded using its primary key.
	 *
	 * @param obj the object to refresh; ignored if {@code null}
	 */
	public void refreshObject(OAObject obj) {
		if (obj == null) {
			return;
		}
		Class<? extends OAObject> clazz = obj.getClass();
		OADataSource ds = OARuntime.datasource().get(clazz);
		if (ds != null) {
			OAObjectKey key = callKeyGetKey(obj);
			OAObjectInfo oi = callInfoGetObjectInfo(clazz);
			ds.getObject(oi, clazz, key, true); // true=reload all props
		}
	}
	
	/**
	 * Retrieves an object from the DataSource using the specified class
	 * and {@link OAObjectKey}.
	 *
	 * @param clazz the object's class
	 * @param key the object key
	 * @return the retrieved object, or {@code null} if none exists
	 */
	protected <T extends OAObject> T  getObject(Class<T> clazz, OAObjectKey key) {
		if (clazz == null) return null;
		OADataSource ds = OARuntime.datasource().get(clazz);
		if (ds == null) return null;
		return ds.getObject(clazz, key);
	}

	/**
	 * Retrieves an object from the DataSource using the supplied metadata,
	 * class, and key. Does not force property reload.
	 *
	 * @param oi the metadata describing the object's class
	 * @param clazz the class of the object to retrieve
	 * @param key the object's key
	 * @return the retrieved object, or {@code null} if no DataSource exists
	 */
	public <T extends OAObject> T getObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key) {
		if (clazz == null) return null;
		OADataSource ds = OARuntime.datasource().get(clazz);
		if (ds == null) return null;
		return ds.getObject(oi, clazz, key, false);
	}

	/**
	 * Retrieves a blob property value for the specified object from the
	 * DataSource.
	 *
	 * @param obj the object containing the blob property
	 * @param propName the name of the blob property
	 * @return the blob's value, or {@code null} if unavailable
	 */
	public Object getBlob(OAObject obj, String propName) {
		if (obj == null || propName == null) {
			return null;
		}
		Class<? extends OAObject> clazz = obj.getClass();
		OADataSource ds = OARuntime.datasource().get(clazz);
		if (ds == null) return null;
		return ds.getPropertyBlobValue(obj, propName);
	}

	/**
	 * Saves the specified object to the DataSource. If the object is new,
	 * an insert is performed; otherwise, an update is issued.
	 *
	 * @param oaObj the object to save
	 */
	public void save(OAObject oaObj) {
		OADataSource dataSource = getDataSource(oaObj);
		if (dataSource != null) {
			if (oaObj.getNew()) {
				dataSource.insert(oaObj);
			} else {
				dataSource.update(oaObj);
			}
		}
	}

	/**
	 * Saves a new object to the DataSource without persisting any of its
	 * reference properties. Intended only for new objects requiring a
	 * pre-save prior to establishing relationships.
	 *
	 * @param oaObj the object to save without references
	 */
	public void saveWithoutReferences(OAObject oaObj) {
		OADataSource dataSource = getDataSource(oaObj);
		if (dataSource != null) {
			if (oaObj.getNew()) {
				dataSource.insertWithoutReferences(oaObj);
			} else {
				// error, should only be used by new objects
			}
		}
	}

	/**
	 * Removes a single reference property from the specified object by
	 * issuing a targeted update to the DataSource. Only the link property
	 * defined by the supplied {@link OALinkInfo} is updated.
	 *
	 * @param oaObj the object whose reference is being removed
	 * @param li the link information describing the reference property
	 */
	public void removeReference(OAObject oaObj, OALinkInfo li) {
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

	/**
	 * Saves the specified object using the provided insert/update flag.
	 *
	 * @param obj the object to save
	 * @param bInsert {@code true} to perform an insert,
	 *                {@code false} to perform an update
	 */
	public void save(OAObject obj, boolean bInsert) {
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
	 * Deletes the specified object using the DataSource associated with
	 * its class. Performs no operation if no DataSource exists.
	 *
	 * @param obj the object to delete; ignored if {@code null}
	 */
	public void delete(OAObject obj) {
		if (obj == null) {
			return;
		}
		OADataSource ds = OARuntime.datasource().get(obj.getClass());
		if (ds != null) {
			ds.delete(obj);
		}
	}

	/**
	 * Determines whether the DataSource for the specified class allows
	 * primary key changes.
	 *
	 * @param c the class whose DataSource is queried
	 * @return {@code true} if ID changes are permitted, or if no
	 *         DataSource exists; otherwise {@code false}
	 */
	public boolean allowIdChange(Class<? extends OAObject> c) {
		OADataSource ds = OARuntime.datasource().get(c);
		return (ds == null || ds.getAllowIdChange());
	}

	/**
	 * Retrieves the DataSource-managed instance of the specified object
	 * using its primary key. Returns {@code null} if no DataSource is
	 * available.
	 *
	 * @param oaObj the object whose persistent instance is requested
	 * @return the object retrieved from the DataSource, or {@code null}
	 */
	public Object getObject(OAObject oaObj) {
		OADataSource ds = OARuntime.datasource().get(oaObj.getClass());
		if (ds == null) return null;
		// todo, check if needed:  if (ds == null || ds.isAssigningId(oaObj)) return null;  // datasource could be assigning the Id to a unique value
		return ds.getObject(oaObj.getClass(), callKeyGetKey(oaObj));
	}

	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz); 
	public abstract UUID callGuidGetGuid(OAObject oaObj);
	public abstract OAObjectKey callKeyCreateObjectKey(final Class<? extends OAObject> c, final Object ...ids);
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj); 
}


