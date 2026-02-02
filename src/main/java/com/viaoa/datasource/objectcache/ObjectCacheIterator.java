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
package com.viaoa.datasource.objectcache;

import java.util.ArrayList;

import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;

/**
 * Iterator over objects stored in the OA object cache.
 * <p>
 * {@code ObjectCacheIterator} retrieves batches of objects from
 * {@link com.viaoa.object.OAObjectCacheDelegate} that match a given
 * {@link com.viaoa.util.OAFilter}. It is used by
 * {@link OADataSourceObjectCache#select} to perform in-memory queries.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Fetches objects incrementally (default batch = 100).</li>
 *   <li>Applies optional filter predicates during iteration.</li>
 *   <li>Supports {@link #setMax(int)} to cap the number of returned objects.</li>
 *   <li>Thread-safe for sequential iteration within a single thread.</li>
 * </ul>
 *
 * @param <T> the OAObject type being iterated
 */
public class ObjectCacheIterator<T> implements OADataSourceIterator {
	
	/**
	 * The class type of objects that this iterator will return. Used by the
	 * fetch logic to request objects of a specific type from the cache.
	 */
	protected Class<T> clazz;
	
	/**
	 * Optional filter applied to each fetched object. Only objects for which
	 * {@link OAFilter#isUsed(Object)} returns true will be returned by the iterator.
	 */
	protected OAFilter<T> filter;
	
	/**
	 * nextObject: a buffered object returned on the next call to {@link #next()}.
	 * lastFetchObject: the last object returned by the cache delegate fetch
	 * operation, used as a continuation marker for incremental retrieval.
	 */
	protected T nextObject, lastFetchObject;
	
	/**
	 * Batch buffer for fetched objects. New objects are added by the cache
	 * delegate in batches (default size = 100) and consumed sequentially during
	 * iteration.
	 */
	protected ArrayList<T> alFetchObjects = new ArrayList<T>(50);
	
	/**
	 * Current index within the fetched-objects batch. When the index reaches the
	 * batch size, a new fetch request is triggered.
	 */
	protected int posFetchObjects;
	
	/**
	 * Indicates whether all available objects have been fetched from the cache.
	 * Once true, no further fetch operations will be performed.
	 */
	protected boolean bFetchIsDone;
	
	/**
	 * Maximum number of objects the iterator is allowed to return. A value of
	 * zero indicates no limit.
	 */
	protected int max;
	
	/**
	 * Counter tracking the number of objects returned so far. Used in conjunction
	 * with {@link #max} to enforce maximum-result limits.
	 */
	private int nextCount;

	/**
	 * Creates a new iterator for the specified class type without a filter.
	 *
	 * @param c the class of objects to iterate
	 */
	public ObjectCacheIterator(Class<T> c) {
		this.clazz = c;
	}

	/**
	 * Creates a new iterator for the specified class type with an optional filter.
	 *
	 * @param c the class of objects to iterate
	 * @param filter the filter applied to fetched objects
	 */
	public ObjectCacheIterator(Class<T> c, OAFilter<T> filter) {
		this.clazz = c;
		this.filter = filter;
	}

	/**
	 * Retrieves the next object in the iteration. Delegates to {@link #getNext()}.
	 *
	 * @return the next matching object, or null if iteration is complete
	 */
	public T next() {
		return getNext();
	}

	/**
	 * Retrieves the next object, applying the filter if present. Uses an internal
	 * buffer when {@link #hasNext()} has pre-fetched an object. Also increments
	 * the returned-object counter.
	 *
	 * @return the next filtered object, or null if none remain
	 */
	private synchronized T getNext() {
		T obj;
		if (nextObject != null) {
			obj = nextObject;
			nextObject = null;
			return obj;
		}
		for (;;) {
			obj = _next();
			if (obj == null) {
				break;
			}
			if (filter == null || filter.isUsed(obj)) {
				break;
			}
		}
		if (obj != null) {
			nextCount++;
		}
		return obj;
	}

	/**
	 * Fetches the next object from the batch buffer. If the buffer is empty,
	 * requests a new batch from {@link OAObjectCacheDelegate#find}. Stops when
	 * the maximum limit is reached or no more objects are available.
	 *
	 * @return the next unfiltered object, or null if iteration is complete
	 */
	protected T _next() {
		if (max > 0 && nextCount >= max) {
			return null;
		}
		if (posFetchObjects >= alFetchObjects.size()) {
			posFetchObjects = 0;
			alFetchObjects.clear();
			if (bFetchIsDone) {
				return null;
			}
			final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(clazz);
	    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
			lastFetchObject = (T) srvcObjectCache.find(lastFetchObject, clazz, filter, false, false, 100, (ArrayList) alFetchObjects);
			if (lastFetchObject == null) {
				bFetchIsDone = true;
				if (alFetchObjects.size() == 0) {
					return null;
				}
			}
		}
		T obj = alFetchObjects.get(posFetchObjects++);
		return obj;
	}

	/**
	 * Indicates whether another object is available. If no pre-fetched object is
	 * present, this method attempts to fetch one via {@link #getNext()}.
	 *
	 * @return true if a next object exists, otherwise false
	 */
	public synchronized boolean hasNext() {
		if (nextObject == null) {
			nextObject = getNext();
		}
		return (nextObject != null);
	}

	/**
	 * Sets the maximum number of objects that the iterator may return.
	 *
	 * @param x the maximum number of results, or zero for unlimited
	 */
	public void setMax(int x) {
		this.max = x;
	}

	/**
	 * Returns the maximum number of objects allowed to be returned by this
	 * iterator.
	 *
	 * @return the maximum result count, or zero if unlimited
	 */
	public int getMax() {
		return max;
	}
}
