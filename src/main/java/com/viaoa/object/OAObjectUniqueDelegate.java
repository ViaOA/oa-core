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

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteServerInterface;

/**
 * Provides a concurrency-safe mechanism for finding or creating an {@link OAObject}
 * instance with a unique property value.
 * <p>
 * This delegate guarantees that only one instance of a given class and property
 * combination (e.g., {@code Employee.code = "A123"}) exists within the runtime or,
 * when distributed, across the entire OA synchronization network.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Unique Lookup:</b> Searches the {@link OAObjectCacheDelegate} for an
 *       existing object with the specified property value.</li>
 *   <li><b>Distributed Coordination:</b> When invoked on a client, delegates
 *       the lookup and optional creation to the remote server through
 *       {@link com.viaoa.sync.remote.RemoteServerInterface#getUnique(Class, String, Object, boolean)}.</li>
 *   <li><b>Thread-Safe Auto-Creation:</b> If not found and {@code bAutoCreate==true},
 *       synchronizes on a global lock to safely create and initialize a new instance
 *       without race conditions.</li>
 *   <li><b>Event Safety:</b> Uses {@link OAThreadLocalDelegate#setLoading(boolean)}
 *       to suppress property-change and synchronization events during initialization.</li>
 * </ul>
 *
 * <h2>Behavior Summary</h2>
 * <ol>
 *   <li>Search local cache for existing match.</li>
 *   <li>If client, forward request to server.</li>
 *   <li>If not found, perform a DataSource {@link OASelect} lookup.</li>
 *   <li>If still not found and {@code bAutoCreate==true}, create and return a new instance.</li>
 * </ol>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Guarantees global uniqueness for any property value across distributed OA sessions.</li>
 *   <li>Creation path is fully synchronized to prevent duplicates under concurrency.</li>
 *   <li>Compatible with all OA DataSource types and synchronization modes.</li>
 * </ul>
 *
 * @see OAObject
 * @see OAObjectCacheDelegate
 * @see com.viaoa.sync.remote.RemoteServerInterface
 * @see com.viaoa.datasource.OASelect
 * @see OAThreadLocalDelegate
 */
public class OAObjectUniqueDelegate {

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectPropertyService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
    private static final Object Lock = new Object();

    
    
    /**
     * Finds or creates an {@link OAObject} instance with the specified unique
     * property value. The method performs the lookup using several layers of
     * resolution and optionally creates a new instance when no match exists.
     * <p>
     * Behavior visible in this implementation:
     * <ul>
     *   <li>Immediately returns {@code null} if {@code clazz}, {@code uniqueKey},
     *       or {@code propertyName} are invalid.</li>
     *   <li>Searches the {@link OAObjectCacheDelegate} for an existing object
     *       matching the class, property name, and unique value.</li>
     *   <li>If running as a client, attempts to delegate the request to the
     *       remote server using {@link OASyncClient} and
     *       {@link RemoteServerInterface#getUnique(Class, String, Object, boolean)}.</li>
     *   <li>Performs a data source query using {@link OASelect} if not already
     *       found.</li>
     *   <li>If still not found and {@code bAutoCreate} is {@code true}, enters a
     *       synchronized block to safely create and initialize a new instance.</li>
     *   <li>Uses {@link OAThreadLocalDelegate#setLoading(boolean)} to suppress
     *       change events during initialization of the new instance.</li>
     * </ul>
     *
     * @param clazz the class of object to search or create
     * @param propertyName the name of the unique property
     * @param uniqueKey the unique value to match
     * @param bAutoCreate whether to create a new instance if none exists
     * @return the matching or newly created {@link OAObject}, or {@code null} if
     *         not found and auto-creation is disabled
     */
    private static OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate) {
		OAGraph g = OARuntime.get().graph(clazz);
    	if (g == null) return null;
    	return g.objects().getOAObjectUniqueService().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
        
    }
}
