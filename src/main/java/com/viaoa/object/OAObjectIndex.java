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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectKeyService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAArray;

/**
 * Maintains a runtime index of OAObject instances by their primary/business key
 * property values, enabling fast lookup of an object GUID when only its
 * identifier fields are known. This supports identity reconciliation during
 * lazy loading and distributed synchronization.
 *
 * <p>The index is maintained by {@code OAObjectCache}, including updates when
 * primary key values change. Only keys with all non-null identifier values are
 * indexed to ensure correct and deterministic lookup behavior.</p>
 *
 * <p>Map entries are removed automatically when their referenced OAObject is
 * garbage collected, preserving memory and avoiding stale identity entries.</p>
 *
 * @see OAObjectKey
 * @see OAObjectKeyDelegate#getKey
 * @see OAObjectCache
 */
public class OAObjectIndex {
	/**
	 * Top-level index mapping each OAObject subclass to its corresponding
	 * map of {@link OAObjectIndexKey} → GUID entries.  
	 *
	 * <p>The outer map is keyed by OAObject class.  
	 * The inner map stores business-key–based index keys that resolve to
	 * the GUID of the matching object.  
	 * Entries are updated by OAObjectCache when identity properties change
	 * and cleared automatically when objects are garbage collected.</p>
	 */
	private final ConcurrentHashMap<Class<? extends OAObject>, ConcurrentHashMap<OAObjectIndexKey, UUID>> hmGuidByIndexKey = new ConcurrentHashMap<>(151, 0.75F);

	/**
	 * Adds the object's primary/business key to the index. Retrieves
	 * its identifier values via {@link OAObjectKeyDelegate#getKey}
	 * and stores the mapping of index key → GUID.
	 *
	 * @param obj the object to index.
	 * @return true if successfully added; false otherwise.
	 */
	public boolean addToIndex(final OAObject obj) {
		if (obj == null) return false;
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(obj);
		final OAObjectKeyService srvcObjectKey = og.getOAObjectService().getOAObjectKeyService();
		OAObjectKey ok = srvcObjectKey.getKey(obj);
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		Class<? extends OAObject> c = obj.getClass();
		return addToIndex(c, ik, obj.getGuid());
	}

	/**
	 * Adds the supplied object and key to the index. Constructs an
	 * {@link OAObjectIndexKey} from the object's ID values and
	 * delegates to the class-based add operation.
	 *
	 * @param obj the object being indexed.
	 * @param ok  the resolved object key.
	 * @return true if the index entry was stored.
	 */
	protected boolean addToIndex(final OAObject obj, OAObjectKey ok) {
		if (obj == null) return false;
		if (ok == null) return false;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return addToIndex(obj.getClass(), ik, obj.getGuid());
	}
	
	/**
	 * Core add operation. Ensures the class, index key, and GUID are
	 * valid, then inserts the mapping into the internal concurrent map.
	 *
	 * @param c     the OAObject class.
	 * @param ik    index key built from ID values.
	 * @param guid  GUID of the object.
	 * @return true if the entry was successfully added.
	 */
	protected boolean addToIndex(final Class<? extends OAObject> c, OAObjectIndexKey ik, UUID guid) {
		if (c == null || ik == null || guid == null || !ik.hasValidIds()) return false;
		ConcurrentHashMap<OAObjectIndexKey, UUID> hm = hmGuidByIndexKey.computeIfAbsent(c, k -> new ConcurrentHashMap<>());
		hm.put(ik, guid);
		return true;
	}
	
	
	/**
	 * Looks up an object's GUID using a raw array of identifier
	 * values. Constructs an {@link OAObjectIndexKey} and delegates
	 * to the key-based lookup.
	 *
	 * @param c   the class of the object.
	 * @param ids the identifier values.
	 * @return the resolved GUID, or 0 if not found.
	 */
	public UUID lookupGuid(Class<? extends OAObject> c, Object[] ids) {
		if (c == null) return null;
		OAObjectIndexKey ik = new OAObjectIndexKey(ids);
		return lookupGuid(c, ik);
	}

	/**
	 * Looks up an object's GUID using an {@link OAObjectKey}.
	 * Converts the key to an {@link OAObjectIndexKey} and delegates
	 * to the core lookup method.
	 *
	 * @param c  the object's class.
	 * @param ok the object key.
	 * @return the resolved GUID, or 0 if missing.
	 */
	public UUID lookupGuid(final Class<? extends OAObject> c, final OAObjectKey ok) {
		if (c == null) return null;
		if (ok == null) return null;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return lookupGuid(c, ik);
	}

	/**
	 * Core GUID lookup operation. Validates the class and index key,
	 * retrieves the map for the class, and returns the GUID mapped
	 * to the index key if present.
	 *
	 * @param c  the object's class.
	 * @param ik the index key.
	 * @return the matching GUID, or 0 if not found.
	 */
	protected UUID lookupGuid(final Class<? extends OAObject> c, final OAObjectIndexKey ik) {
		if (c == null || ik == null || !ik.hasValidIds()) return null;
		ConcurrentHashMap<OAObjectIndexKey, UUID> hm = hmGuidByIndexKey.get(c);
		if (hm == null) return null;
		UUID guidx = hm.get(ik);
		return guidx;
	}
	
	/**
	 * Removes the given object's index entry. Builds an object key
	 * via {@link OAObjectKeyDelegate#createObjectKey} and delegates
	 * to the class/key-based remove method.
	 *
	 * @param obj the object to remove.
	 * @return true if the entry was removed.
	 */
	public boolean removeFromIndex(OAObject obj) {
		if (obj == null) return false;
		Class<? extends OAObject> c = obj.getClass();
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(c);
		final OAObjectKeyService srvcObjectKey = og.getOAObjectService().getOAObjectKeyService();
		OAObjectKey ok = srvcObjectKey.createObjectKey(obj);
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return removeFromIndex(c, ik);
	}
	
	/**
	 * Removes the entry identified by the supplied class and object
	 * key. Converts the key to an {@link OAObjectIndexKey} and
	 * delegates to the internal remove method.
	 *
	 * @param c  the object's class.
	 * @param ok the object key.
	 * @return true if the entry was deleted.
	 */
	public boolean removeFromIndex(Class<? extends OAObject> c, OAObjectKey ok) {
		if (c == null || ok == null) return false;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return removeFromIndex(c, ik);
	}

	/**
	 * Core remove operation. Ensures the class and key are valid,
	 * retrieves or creates the map for the class, and removes the
	 * entry for the given index key.
	 *
	 * @param c  the object's class.
	 * @param ik the index key.
	 * @return true if an entry was removed.
	 */
	protected boolean removeFromIndex(final Class<? extends OAObject> c, OAObjectIndexKey ik) {
		if (c == null || ik == null || !ik.hasValidIds()) return false;
		ConcurrentHashMap<OAObjectIndexKey, UUID> hm = hmGuidByIndexKey.computeIfAbsent(c, k -> new ConcurrentHashMap<>());
		if (hm == null) return false;
		return (hm.remove(ik) != null);
	}
	
	/**
	 * Updates an index entry when an object's key values change.
	 * If the new key differs from the old key, adds the new entry
	 * and removes the old one.
	 *
	 * @param obj   the object being updated.
	 * @param okNew the new object key.
	 * @param okOld the previous object key.
	 */
	public void updateIndex(final OAObject obj, OAObjectKey okNew, OAObjectKey okOld) {
		if (obj == null) return;

		if (okNew != null) {
			addToIndex(obj, okNew);
		}
		if (okOld != null) {
			removeFromIndex(obj.getClass(), okOld);
		}
	}
	
	/**
	 * Removes all indexed entries for all OAObject classes,
	 * clearing the internal maps entirely.
	 */
	public void clear() {
		hmGuidByIndexKey.clear();
	}
	
}
