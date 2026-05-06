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
package com.viaoa.cache;

import java.lang.ref.WeakReference;

import com.viaoa.callback.OACallback;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Helper class used by {@link OAObjectCacheDelegate} to populate
 * a {@link com.viaoa.hub.Hub} from the global cache.
 *
 * <p>Scans the object cache for all instances of a specific class and
 * adds those matching an optional {@link com.viaoa.filter.OAFilter}.
 * Optionally runs asynchronously for large caches.</p>
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>Supports background loading through {@code ThreadPoolExecutor}.</li>
 *   <li>Integrates with Hub refresh semantics.</li>
 *   <li>Thread-safe atomic counters track progress.</li>
 * </ul>
 *
 * @param <T> OAObject subtype
 */
public class OAObjectCacheHubAdder<T extends OAObject> implements OAObjectCacheListener<T> {
    static final long serialVersionUID = 1L;

    protected WeakReference<Hub<T>> wfHub;
    private Class<T> clazz;
    private volatile boolean bClosed; 

    /**
     * Creates a new cache-to-Hub adder that listens for objects of the Hub's
     * object type and automatically adds them to the Hub.
     * <p>
     * The supplied Hub must not be {@code null}. Its object class determines
     * which cached objects will be monitored. A listener is registered with
     * {@link OAObjectCacheDelegate} so that new or loaded objects of the
     * matching class can be added automatically when {@link #isUsed(Object)}
     * returns {@code true}.
     * </p>
     *
     * <p>After registration, a {@code callback} is executed to process all
     * currently loaded cached objects, ensuring the Hub is immediately
     * populated with any matching instances.</p>
     *
     * @param hub the Hub to receive matching cached objects; must not be {@code null}
     * @throws IllegalArgumentException if {@code hub} is {@code null}
     */
    public OAObjectCacheHubAdder(Hub<T> hub) {
        if (hub == null) throw new IllegalArgumentException("hub can not be null");
        clazz = hub.getObjectClass();
        wfHub = new WeakReference(hub);

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
                
		og.objectsInternal().callObjectCacheAddListener(clazz, this);
        
        // need to get objects that are already loaded 
		og.objectsInternal().callObjectCacheCallback(clazz, new OACallback() {
            @Override
            public boolean updateObject(Object obj) {
                Hub<T> h = wfHub.get();
                if (h == null) {
                	OAObjectCacheHubAdder.this.close();
                }
                else {
                    if (!h.contains(obj)) {
                        if (isUsed((T) obj)) {
                            h.add((T) obj);
                        }
                    }
                }
                return true;
            }
        });
    }

    /**
     * Unregisters this listener from the {@link OAObjectCacheDelegate}.
     * <p>
     * After closing, this adder will no longer receive cache events and will
     * stop adding objects to the associated Hub.
     * </p>
     */
    public void close() {
    	if (bClosed) return;
    	bClosed = true;
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
		og.objectsInternal().callObjectCacheRemoveListener(clazz, this);
    }

    /**
     * Ensures that this listener is unregistered before garbage collection.
     * <p>
     * Invokes {@link #close()} and then delegates to {@code super.finalize()}.
     * </p>
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    /**
     * Invoked after a property changes on an observed object.
     * <p>
     * This implementation performs no action.
     * </p>
     */
    @Override
    public void afterPropertyChange(T obj, String propertyName, Object oldValue, Object newValue) {
    }

    /**
     * Invoked when a new object of the monitored class is added to the
     * object cache.
     * <p>
     * If the object is not loading and {@link #isUsed(Object)} returns
     * {@code true}, the object is added to the associated Hub, provided the
     * Hub reference is still valid.
     * </p>
     *
     * @param obj the newly added object; ignored if {@code null} or loading
     */
    @Override
    public void afterAdd(T obj) {
        if (obj == null) return;
        if (obj.isLoading()) return;
        if (isUsed(obj)) {
            Hub<T> h = wfHub.get();
            if (h != null) h.add(obj);
        }
    }
    
    
    /**
     * Determines whether a newly added or loaded object should be added
     * to the Hub.
     * <p>
     * The default implementation always returns {@code true}, meaning all
     * objects of the monitored class are eligible for inclusion. Subclasses
     * may override this to apply filtering logic.
     * </p>
     *
     * @param obj the object to evaluate
     * @return always {@code true} in this implementation
     */
    public boolean isUsed(T obj) {
        return true;
    }

    /**
     * Invoked when an object is added directly to a Hub.
     * <p>
     * This implementation performs no action.
     * </p>
     */
    @Override
    public void afterAdd(Hub<T> hub, T obj) {
    }

    /**
     * Invoked when an object is removed from a Hub.
     * <p>
     * This implementation performs no action.
     * </p>
     */
    @Override
    public void afterRemove(Hub<T> hub, T obj) {
    }

    /**
     * Invoked when an object is loaded from the cache.
     * <p>
     * Delegates to {@link #afterAdd(Object)} so that loaded objects are
     * treated the same as newly added objects.
     * </p>
     *
     * @param obj the loaded object
     */
    @Override
    public void afterLoad(T obj) {
        afterAdd(obj);
    }
    
}

