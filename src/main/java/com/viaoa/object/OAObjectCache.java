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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Weak reference object cache used to maintain a single instance of each
 * OAObject within the current JVM. This provides identity consistency across
 * the object graph while allowing unused objects to be reclaimed by the garbage
 * collector when no longer referenced by the application.
 *
 * <p>The cache is keyed by GUID (Globally Unique Identifier), ensuring that each
 * real-world domain entity is represented by exactly one OAObject instance at
 * runtime. Secondary lookup by business (primary) keys is supported through
 * OAObjectIndexKey to align with persistence identity.</p>
 *
 * <p>Active or UI-visible objects are retained through strong references in
 * the application or by explicit server-side session tracking, preventing
 * premature garbage collection. Distributed notifications reference only GUIDs,
 * enabling fast and efficient lookup through this cache.</p>
 *
 * <p>This class does not enforce any storage lifetime; it simply provides
 * object identity resolution for the OA Object Graph framework while
 * cooperating with Java GC for scalable memory usage.</p>
 *
 * @see OAObjectCacheDelegate
 * @see OAObject
 * @see OAObjectKey
 * @see OAObjectIndexKey
 */public class OAObjectCache {
	private static Logger LOG = Logger.getLogger(OAObjectCache.class.getName());

	private final ConcurrentHashMap<
	    Class<? extends OAObject>,
	    ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>>> 
	    hmOAObjectByGuid = new ConcurrentHashMap<>(151, 0.75F);	
	
	private final ReferenceQueue<OAObject> refQueue = new ReferenceQueue<>();
	private final OAObjectIndex objectIndex = new OAObjectIndex();

	private int cntGetObject;
	private int cntGCd;
	
	/**
	 * Returns the list of OAObject classes currently represented in the
	 * cache. Each class corresponds to a distinct top-level entry in the
	 * object-by-guid map.
	 *
	 * @return an array of OAObject classes known to the cache
	 */
	public Class<?>[] getClasses() {
		return hmOAObjectByGuid.keySet().toArray(new Class[0]);
	}

	/**
	 * Returns the number of cached objects for the specified OAObject class.
	 * If the class has no cache entry, this method returns {@code 0}.
	 *
	 * @param clazz the OAObject class whose cache size is requested
	 * @return the number of cached objects for the class, or {@code 0} if none exist
	 */
	public int getTotal(Class<? extends OAObject> clazz) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return 0;
		return hm.size();
	}

	/**
	 * Clears all cached objects for the specified OAObject class. If the
	 * class is not present in the cache, no action is taken.
	 *
	 * @param clazz the OAObject class whose cache entry should be cleared
	 */
	public void clearCache(Class<? extends OAObject> clazz) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return;
		hm.clear();
	}
	
	/**
	 * Clears all cached objects across all classes and resets the
	 * associated object index.
	 */
	public void clearCache() {
		hmOAObjectByGuid.clear();
		objectIndex.clear();
	}
	
	/**
	 * Looks up an object in the cache by its class and GUID. Returns the
	 * cached object instance if present; otherwise returns {@code null}.
	 * Periodically checks the reference queue to purge entries whose referents
	 * have been garbage-collected.
	 *
	 * @param c     the class of the object to retrieve
	 * @param guid  the GUID of the desired object
	 * @return the cached object instance, or {@code null} if not found or reclaimed
	 */
	@SuppressWarnings("unchecked")
	public <T extends OAObject> T getObject(Class<T> c, long guid) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(c);
		if (hm == null) return null;
		OAWeakRef<? extends OAObject> wr = hm.get(guid);
		if ((++cntGetObject % 100) == 0) checkReferenceQueue();
		if (wr == null) return null;
		return (T) wr.get();
	}
	
	/**
	 * Retrieves an object from the cache using its primary key values.
	 * Constructs an {@link OAObjectKey} from the supplied ID array and
	 * delegates to {@link #getObject(Class, OAObjectKey)}.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param ids   the primary key values used to identify the object
	 * @return the matching cached object, or {@code null} if not found
	 */
	public <T extends OAObject> T getObject(Class<T> clazz, Object[] ids) {
		if (clazz == null || ids == null) return null;
		OAObjectKey ok = new OAObjectKey(ids);
		return getObject(clazz, ok);
	}

	/**
	 * Retrieves an object from the cache using an {@link OAObjectKey}.
	 * If the key does not contain a GUID, the GUID is resolved through
	 * the internal {@code objectIndex}. If a valid GUID is found, the
	 * lookup delegates to {@link #getObject(Class, long)}.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param ok    the object key containing primary key values and/or GUID
	 * @return the cached object instance, or {@code null} if not found
	 */
	public <T extends OAObject> T getObject(Class<T> clazz, OAObjectKey ok) {
		if (clazz == null || ok == null) return null;
		long guid = ok.getGuid();
		if (guid == 0) {
			guid = objectIndex.lookupGuid(clazz, ok);
		}
		if (guid == 0) return null;
		return getObject(clazz, guid);
	}
	
	
	/**
	 * Updates the cache entry for the given object. A new
	 * {@link OAObjectKey} is created for the object, and the update
	 * is delegated to {@link #updateObject(OAObject, OAObjectKey, Class)}.
	 *
	 * @param obj the object being loaded or whose primary key has changed
	 * @return {@code true} if the object already existed in the cache,
	 *         otherwise {@code false}
	 */
	public <T extends OAObject> boolean updateObject(final T obj) {
		if (obj == null) return false;
		final OAObjectKey ok = OAObjectKeyDelegate.createObjectKey((OAObject) obj);
		final Class<T> clazz = (Class<T>) obj.getClass();
		return updateObject(obj, ok, clazz);
	}	
	
	/**
	 * Updates or inserts the specified object into the cache using the
	 * provided {@link OAObjectKey}. If the object already exists in the
	 * cache, its key is updated and the index is adjusted accordingly.
	 * Otherwise, the object is added as a new entry.
	 *
	 * @param obj   the object to update or insert
	 * @param ok    the object key identifying the object
	 * @param clazz the class of the object
	 * @return {@code true} if the object already existed in the cache,
	 *         otherwise {@code false}
	 */
	protected <T extends OAObject> boolean updateObject(final T obj, final OAObjectKey ok, final Class<T> clazz) {
		if (obj == null || ok == null) return false;
		final ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
		
		boolean[] bsWasFound = new boolean[] {true};

		OAWeakRef<? extends OAObject> wrOld = hm.computeIfAbsent(ok.getGuid(), k -> {
			bsWasFound[0] = false;
			return new OAWeakRef(obj, ok, refQueue);
		});
		
		if (bsWasFound[0]) {
			OAObjectKey okOld = wrOld.key;
			wrOld.key = ok;
			objectIndex.updateIndex(obj, ok, okOld);
		}
		else {
			objectIndex.addToIndex(obj, ok);
		}
		checkReferenceQueue();
		return bsWasFound[0];
	}

	/**
	 * Removes the specified object from the cache. The object's
	 * {@link OAObjectKey} is created and used to locate and remove its
	 * weak-reference entry. If found, the corresponding index entry is
	 * also removed.
	 *
	 * @param obj the object to remove from the cache
	 * @return {@code true} if the object was present and removed,
	 *         otherwise {@code false}
	 */
	public <T extends OAObject> boolean removeObject(final T obj) {
		if (obj == null) return false;
		final OAObjectKey ok = OAObjectKeyDelegate.createObjectKey((OAObject) obj);
		final Class<T> clazz = (Class<T>) obj.getClass();
		
		final ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return false;
		
		OAWeakRef<? extends OAObject> wrOld = hm.remove(ok.getGuid());
		if (wrOld == null) return false;
		
		objectIndex.removeFromIndex(clazz, wrOld.key);
		return true;
	}	
	
	/**
	 * Processes the reference queue to remove cache entries whose objects
	 * have been garbage-collected. Up to 5000 queued references are handled
	 * per invocation, removing each corresponding GUID entry from the cache
	 * and clearing its index entry.
	 */
	protected void checkReferenceQueue() {
		for (int i=0; i<5000; i++) {
			@SuppressWarnings("unchecked")
			OAWeakRef<? extends OAObject> wr = (OAWeakRef<? extends OAObject>) refQueue.poll();
			if (wr == null) break;
			++cntGCd;
			ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(wr.clazz);
			if (hm != null) hm.remove(wr.key.getGuid());
			objectIndex.removeFromIndex(wr.clazz, wr.key);
		}
	}
	
	/**
	 * Visits all cached objects across all classes by invoking the supplied
	 * {@link OACallback}. Each object currently referenced in the cache is
	 * passed to the callback.
	 *
	 * @param callback the callback invoked for each cached object
	 */
	public void visit(OACallback callback) {
		for (Class<? extends OAObject> c : hmOAObjectByGuid.keySet()) {
			visit(c, callback);
		}
	}

	/**
	 * Visits all cached objects of the specified class by invoking the
	 * supplied {@link OACallback}. Only objects that have not been
	 * garbage-collected are passed to the callback.
	 *
	 * @param clazz    the OAObject class whose cached instances will be visited
	 * @param callback the callback invoked for each object
	 */
	public void visit(Class<? extends OAObject> clazz, OACallback callback) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return;
		for (OAWeakRef<? extends OAObject> wr : hm.values()) {
			OAObject obj = wr.get();
			if (obj != null) callback.updateObject(obj);
		}
	}
	
	/**
	 * Searches the cache for objects of the specified class that match the
	 * criteria defined by the provided {@link OAFinder}. Iteration begins
	 * after the specified {@code fromObject}, if provided. Matching objects
	 * may be added to {@code alResults}, or the first match may be returned
	 * directly when {@code alResults} is {@code null}.
	 *
	 * <p>New objects may optionally be skipped. Iteration stops once the
	 * number of collected results reaches {@code fetchAmount}.</p>
	 *
	 * @param fromObject the object after which iteration should begin,
	 *                   or {@code null} to start from the beginning
	 * @param clazz      the class of objects to search
	 * @param finder     the finder used to evaluate each object
	 * @param bSkipNew   whether to skip objects marked as new
	 * @param fetchAmount the maximum number of results to retrieve
	 * @param alResults   the list to accumulate matching results, or {@code null}
	 * @return the first matching object if {@code alResults} is {@code null},
	 *         otherwise {@code null} after result collection completes
	 */
	public Object find(final Object fromObject, final Class<? extends OAObject> clazz, final OAFinder finder,
		boolean bSkipNew, int fetchAmount, final List<OAObject> alResults) 
	{
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) {
			return null;
		}
		boolean bFoundFirst = fromObject == null;
		for (OAWeakRef<? extends OAObject> wr : hm.values()) {
			OAObject obj = wr.get();
			if (obj == null) continue;
			if (!bFoundFirst) {
				if (obj != fromObject) continue;
				bFoundFirst = true;
				continue;
			}
			if (bSkipNew && obj.isNew()) continue;
			
			if (finder == null || finder.findFirst(obj) != null) {
				if (alResults == null) {
					return obj;
				}
				alResults.add(obj);
				if (alResults.size() >= fetchAmount) {
					return obj;
				}
			}
		}
		return null;
	}
}

final class OAWeakRef<T extends OAObject> extends WeakReference<T> {
    final Class<? extends OAObject> clazz;
    OAObjectKey key; 

    OAWeakRef(T obj, OAObjectKey key, ReferenceQueue<? super T> queue) {
        super(obj, queue);
        this.clazz = obj.getClass();
        this.key = key;
    }
}


