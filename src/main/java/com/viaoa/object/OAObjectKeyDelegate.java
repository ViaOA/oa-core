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

import java.util.Arrays;
import java.util.List;
import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAConverter;

/**
 * Internal delegate responsible for resolving, merging, and maintaining identity
 * semantics for OAObjects and their OAObjectKeys. All identity lookups and
 * reconciliations route through this delegate.
 *
 * <p>The resolution strategy is GUID-first: if a GUID match is found in the
 * cache it is always considered the authoritative identity. If only business
 * keys are provided, a secondary lookup is performed. When both refer to
 * different cached objects, the identities are reconciled to maintain a single
 * instance of each real-world entity in the object graph.</p>
 *
 * <p>This delegate also performs conversion from OAObject references to
 * OAObjectKeys (and back), enabling lazy loading and safe reference storage
 * without forcing object materialization. It ensures identity consistency when
 * objects move between states such as new, loaded, remote, or partially
 * referenced.</p>
 *
 * <p>These mechanisms enable OA's distributed and offline-first behavior:
 * references may be communicated using only GUIDs, caches may contain only keys
 * until objects are needed, and identity never drifts even when primary key
 * properties change or are not yet assigned.</p>
 *
 * @see OAObjectKey
 * @see OAObjectCacheDelegate
 * @see OAObject
 */
public class OAObjectKeyDelegate {

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectKeyService().??(oaObj);
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
	 * Creates an {@link OAObjectKey} for the given object.
	 * <p>
	 * The key is constructed using the object's current ID property values and its GUID.
	 *
	 * @param obj the source object, or {@code null}
	 * @return a new {@link OAObjectKey} for the object, or {@code null} if the object is {@code null}
	 */
	public static OAObjectKey createObjectKey(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().createObjectKey(obj);
	}

	
	/**
	 * Creates an {@link OAObjectKey} using the provided ID values and GUID.
	 *
	 * @param ids  the ID values to include in the key
	 * @param guid the GUID to associate with the key
	 * @return a new {@link OAObjectKey} instance
	 */
	public static OAObjectKey createObjectKey(Object[] ids, long guid) {
		//qqqqqq cant get to Service
		return createObjectKey((OAObjectInfo) null, ids, guid);
	}
	
	/**
	 * Creates an {@link OAObjectKey} for the given class using the supplied ID values.
	 * <p>
	 * Delegates to {@link #createObjectKey(Class, long, Object...)} with a GUID of {@code 0L}.
	 *
	 * @param c   the class used to resolve object metadata, or {@code null}
	 * @param ids the ID values or an {@link OAObject} whose key should be returned
	 * @return a newly created {@link OAObjectKey}, or {@code null} if no IDs are provided
	 */
	public static OAObjectKey createObjectKey(final Class c, final Object ...ids) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().createObjectKey(c, ids);
	}

	/**
	 * Creates an {@link OAObjectKey} for the specified class, GUID, and ID values.
	 * <p>
	 * If a single ID value is provided and it is an {@link OAObject}, that object's key
	 * is returned directly. Otherwise, the class metadata is retrieved and used to
	 * construct a new key.
	 *
	 * @param c    the object's class, or {@code null}
	 * @param guid the GUID to associate with the key
	 * @param ids  the ID values or an {@link OAObject} reference
	 * @return the resulting {@link OAObjectKey}
	 */
	public static OAObjectKey createObjectKey(final Class<? extends OAObject> c, final long guid, final Object ...ids) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().createObjectKey(c, guid, ids);
	}
	
	/**
	 * Creates an {@link OAObjectKey} using the provided object info, ID values, and GUID.
	 * <p>
	 * If metadata is available, each ID value is converted to the expected property type
	 * unless it is already an {@link OAObjectKey} or an {@link OAObject}. After any
	 * necessary conversions, a new {@link OAObjectKey} is created.
	 *
	 * @param oi   the {@link OAObjectInfo} describing ID properties, or {@code null}
	 * @param ids  the ID values to include in the key
	 * @param guid the GUID to associate with the key
	 * @return a new {@link OAObjectKey} instance
	 */
	public static OAObjectKey createObjectKey(OAObjectInfo oi, Object[] ids, long guid) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().createObjectKey(oi, ids, guid);
	}
	
	/**
	 * Creates an {@link OAObjectKey} from a single ID value.
	 * <p>
	 * If the value is already an {@link OAObjectKey}, it is returned unchanged.
	 * If it is an {@link OAObject}, a key is created from that object.
	 * If it is an array, the elements are treated as ID values.
	 *
	 * @param id the ID value, array of IDs, {@link OAObject}, or {@link OAObjectKey}
	 * @return a corresponding {@link OAObjectKey}, or {@code null} if {@code id} is {@code null}
	 */
	public static OAObjectKey createObjectKey(Object id) {
		//qqqqqq cant get to Service
		if (id == null) return null;
		if (id instanceof OAObjectKey) return (OAObjectKey) id;
		if (id instanceof OAObject) return createObjectKey((OAObject) id);
		if (id.getClass().isArray()) return createObjectKey((OAObjectInfo) null, (Object[]) id, 0L);
		return createObjectKey((OAObjectInfo) null, new Object[] {id}, 0L);
	}

	/**
	 * Creates an {@link OAObjectKey} from the supplied ID values.
	 * <p>
	 * If no IDs are provided, {@code null} is returned. Otherwise, a new key is
	 * created using the values as the object ID components.
	 *
	 * @param ids the ID values used to build the key
	 * @return a new {@link OAObjectKey}, or {@code null} if no IDs are supplied
	 */
	public static OAObjectKey createObjectKey(Object... ids) {
		if (ids == null || ids.length == 0) return null;
		return createObjectKey((OAObjectInfo) null, (Object[]) ids, 0L);
	}
	

	/**
	 * Determines whether two {@link OAObjectKey} instances refer to the same object.
	 * <p>
	 * Keys are considered to represent the same object if:
	 * <ul>
	 *   <li>they are equal, or</li>
	 *   <li>both have non-zero GUIDs that match, or</li>
	 *   <li>their ID arrays are equal, or</li>
	 *   <li>one has only a GUID and the other only ID values, and the object
	 *       resolved from cache provides matching IDs</li>
	 * </ul>
	 *
	 * @param clazz the object class used for cache lookup when resolving mixed key formats
	 * @param key   the first key to compare
	 * @param key2  the second key to compare
	 * @return {@code true} if both keys refer to the same object; otherwise {@code false}
	 */
	public static boolean isForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey key, final OAObjectKey key2) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return false;
		return g.objects().getOAObjectKeyService().isForSameOAObject(clazz, key, key2);
	}
	
	/**
	 * Retrieves an {@link OAObject} for the given class and key.
	 * <p>
	 * The cache is checked first; if not found, the object is requested from
	 * the datasource.
	 *
	 * @param c   the object's class
	 * @param key the object key to locate
	 * @return the matching object, or {@code null} if none is found
	 */
	public static <T extends OAObject> OAObject getOAObject(Class<T> c, OAObjectKey key) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().getOAObject(c, key);
	}
	
	
	/**
	 * Convenience wrapper around {@link #createObjectKey(OAObject)}.
	 *
	 * @param oaObj the source object
	 * @return the object's {@link OAObjectKey}
	 */
	public static OAObjectKey getKey(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().getKey(oaObj);
	}

	/**
	 * Convenience wrapper around {@link #createObjectKey(OAObject)}.
	 *
	 * @param oaObj the source object
	 * @return the object's {@link OAObjectKey}
	 */
	public static OAObjectKey getObjectKey(OAObject oaObj) {
		return createObjectKey(oaObj);		
	}

	/**
	 * Returns the GUID of the specified object.
	 *
	 * @param oaObj the object whose GUID is requested
	 * @return the object's GUID, or {@code 0} if the object is {@code null}
	 */
	public static long getGuid(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return 0;
		return g.objects().getOAObjectKeyService().getGuid(oaObj);
	}

	/**
	 * Creates a new {@link OAObjectKey} reflecting a change to one ID property.
	 * <p>
	 * The object's key properties are retrieved from metadata. A new key array is
	 * constructed by copying existing ID values and replacing the one that matches
	 * the specified property name. The GUID is preserved if an original key exists.
	 *
	 * @param clazz        the object's class used to retrieve key metadata
	 * @param objKey       the original key, or {@code null}
	 * @param propertyName the ID property being changed
	 * @param newValue     the new value for the changed property
	 * @return a new {@link OAObjectKey} containing the updated ID values
	 */
	public static OAObjectKey createChangedObjectKey(Class<? extends OAObject> clazz, OAObjectKey objKey, String propertyName, Object newValue) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().createChangedObjectKey(clazz, objKey, propertyName, newValue);
	}


	/**
	 * Handles updates after an object's ID-related property value changes.
	 * <p>
	 * A new {@link OAObjectKey} is created for the object, and—if verification is
	 * enabled—checks are performed to ensure that no other object already uses the
	 * resulting key. Cache indexes are then updated, and any child objects whose
	 * keys include this object are also updated.
	 *
	 * @param oaObj   the object whose key-related property changed
	 * @param okOrig  the original key (not used)
	 * @param bVerify whether to verify that the new key does not conflict with another object
	 * @return {@code true} after processing the key change
	 */
	public static boolean afterChangedObjectKeyProperty(final OAObject oaObj, final OAObjectKey okOrig, boolean bVerify) {
		//qqqq method was protected
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectKeyService().afterChangedObjectKeyProperty(oaObj, okOrig, bVerify);
	}

	/**
	 * Verifies that the supplied key change does not conflict with an existing object.
	 * <p>
	 * Ensures that ID changes are allowed, checks for duplicates in the cache, on the
	 * server, and in the datasource, and returns an error message if the new key is
	 * already in use. If no conflict exists, {@code null} is returned.
	 *
	 * @param oaObj         the object whose key is being changed
	 * @param newObjectKey  the newly generated key
	 * @return a descriptive error message if the new key is already used; otherwise {@code null}
	 */
	public static String verifyKeyChange(final OAObject oaObj, OAObjectKey newObjectKey) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().verifyKeyChange(oaObj, newObjectKey);
	}

	
	/**
	 * Retrieves the value of an ID property from the specified {@link OAObjectKey}.
	 * <p>
	 * The object's metadata is inspected to locate the index of the requested
	 * property name within the key's ID array. If found, the corresponding ID
	 * component is returned.
	 *
	 * @param clazz         the object's class used to obtain key metadata
	 * @param objectKey     the key containing ID values
	 * @param propertyName  the name of the ID property to retrieve
	 * @return the matching ID value, or {@code null} if not found
	 */
	public static Object getProperty(final Class<? extends OAObject> clazz, final OAObjectKey objectKey, final String propertyName) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectKeyService().getProperty(clazz, objectKey, propertyName);
	}

	
}
