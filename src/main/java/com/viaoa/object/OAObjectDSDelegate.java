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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectDSService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;

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

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectDSService().delete(oaObj);
*/
	
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
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
	public static void assignId(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDSService().assignId(oaObj);
	}

	/**
	 * Returns the internal map tracking GUIDs of objects currently
	 * undergoing ID assignment.
	 *
	 * @return the assigning-ID tracking map
	 */
    public static Map<Long, Long> getAssigningIdMap() {
        return OAObjectDSService.getAssigningIdMap();  //qqqqqqqq
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
	public static void setAssigningId(OAObject obj, boolean b) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectDSService().setAssigningId(obj, b);
	}

	/**
	 * Determines whether the specified object is currently flagged as
	 * undergoing ID assignment.
	 *
	 * @param obj the object to check
	 * @return {@code true} if the object’s GUID is present in the
	 *         assigning-ID map; otherwise {@code false}
	 */
	public static boolean isAssigningId(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().isAssigningId(obj);
	}

	/**
	 * Determines whether the DataSource for the object's class has been
	 * configured to assign IDs automatically when objects are created.
	 *
	 * @param oaObj the object whose DataSource is queried
	 * @return {@code true} if ID assignment on creation is enabled,
	 *         otherwise {@code false}
	 */
	public static boolean getAssignIdOnCreate(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().getAssignIdOnCreate(oaObj);
	}

	/**
	 * Returns the {@link OADataSource} associated with the class of the
	 * specified object.
	 *
	 * @param obj the object whose DataSource is requested
	 * @return the DataSource for the object’s class, or {@code null}
	 */
	protected static OADataSource getDataSource(Object obj) {
		return OADataSource.getDataSource(obj.getClass());
	}

	/**
	 * Indicates whether a DataSource exists for the specified object's
	 * class.
	 *
	 * @param oaObj the object to evaluate
	 * @return {@code true} if a DataSource is registered; otherwise {@code false}
	 */
	protected static boolean hasDataSource(OAObject oaObj) {
		return OADataSource.getDataSource(oaObj.getClass()) != null;
	}

	/**
	 * Indicates whether a DataSource exists for the specified class.
	 *
	 * @param c the class to evaluate
	 * @return {@code true} if a DataSource is registered; otherwise {@code false}
	 */
	protected static boolean hasDataSource(Class c) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().hasDataSource(c);
	}

	/**
	 * Determines whether the DataSource for the specified class supports
	 * persistent storage.
	 *
	 * @param clazz the class whose DataSource capabilities are checked
	 * @return {@code true} if the DataSource exists and supports storage,
	 *         otherwise {@code false}
	 */
	protected static boolean supportsStorage(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().supportsStorage(clazz);
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
	public static OAObject getObject(Class clazz, Object key) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectDSService().getObject(clazz, key);
	}

	/**
	 * Refreshes all properties of the specified object by requesting a
	 * reloaded version from the DataSource. The object's full property set
	 * is reloaded using its primary key.
	 *
	 * @param obj the object to refresh; ignored if {@code null}
	 */
	public static void refreshObject(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectDSService().refreshObject(obj);
	}

	/**
	 * Retrieves an object from the DataSource using the specified class
	 * and {@link OAObjectKey}.
	 *
	 * @param clazz the object's class
	 * @param key the object key
	 * @return the retrieved object, or {@code null} if none exists
	 */
	protected static Object getObject(Class clazz, OAObjectKey key) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectDSService().getObject(clazz, key);
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
	protected static Object getObject(OAObjectInfo oi, Class clazz, OAObjectKey key) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectDSService().getObject(oi, clazz, key);
	}

	/**
	 * Retrieves a blob property value for the specified object from the
	 * DataSource.
	 *
	 * @param obj the object containing the blob property
	 * @param propName the name of the blob property
	 * @return the blob's value, or {@code null} if unavailable
	 */
	protected static Object getBlob(OAObject obj, String propName) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().getBlob(obj, propName);
	}

	/**
	 * Saves the specified object to the DataSource. If the object is new,
	 * an insert is performed; otherwise, an update is issued.
	 *
	 * @param oaObj the object to save
	 */
	protected static void save(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDSService().save(oaObj);
	}

	/**
	 * Saves a new object to the DataSource without persisting any of its
	 * reference properties. Intended only for new objects requiring a
	 * pre-save prior to establishing relationships.
	 *
	 * @param oaObj the object to save without references
	 */
	protected static void saveWithoutReferences(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDSService().saveWithoutReferences(oaObj);
	}

	/**
	 * Removes a single reference property from the specified object by
	 * issuing a targeted update to the DataSource. Only the link property
	 * defined by the supplied {@link OALinkInfo} is updated.
	 *
	 * @param oaObj the object whose reference is being removed
	 * @param li the link information describing the reference property
	 */
	public static void removeReference(OAObject oaObj, OALinkInfo li) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectDSService().removeReference(oaObj, li);
	}

	/**
	 * Saves the specified object using the provided insert/update flag.
	 *
	 * @param obj the object to save
	 * @param bInsert {@code true} to perform an insert,
	 *                {@code false} to perform an update
	 */
	public static void save(OAObject obj, boolean bInsert) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectDSService().save(obj,bInsert);
	}

	/**
	 * Deletes the specified object using the DataSource associated with
	 * its class. Performs no operation if no DataSource exists.
	 *
	 * @param obj the object to delete; ignored if {@code null}
	 */
	public static void delete(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectDSService().delete(obj);
	}

	/**
	 * Determines whether the DataSource for the specified class allows
	 * primary key changes.
	 *
	 * @param c the class whose DataSource is queried
	 * @return {@code true} if ID changes are permitted, or if no
	 *         DataSource exists; otherwise {@code false}
	 */
	public static boolean allowIdChange(Class c) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectDSService().allowIdChange(c);
	}

	/**
	 * Retrieves the DataSource-managed instance of the specified object
	 * using its primary key. Returns {@code null} if no DataSource is
	 * available.
	 *
	 * @param oaObj the object whose persistent instance is requested
	 * @return the object retrieved from the DataSource, or {@code null}
	 */
	public static Object getObject(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectDSService().getObject(oaObj);
	}
}
