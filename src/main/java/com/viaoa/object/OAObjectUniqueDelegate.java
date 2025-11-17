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

import com.viaoa.datasource.OASelect;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.util.OAString;

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

    private static final Object Lock = new Object();

    /**
     * Find and/or create unique OAObject.
     */
    public static OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate) {
        
        if (clazz == null) return null;
        if (uniqueKey == null) return null;
        if (OAString.isEmpty(propertyName)) return null;
        
        OAObject oaObj = (OAObject) OAObjectCacheDelegate.find(clazz, propertyName, uniqueKey);
        if (oaObj != null) return oaObj;
        
        // not found
        if (OASyncDelegate.isClient(clazz)) {
            OASyncClient sc = OASync.getSyncClient();
            RemoteServerInterface rs;
            try {
                rs = sc.getRemoteServer();

                if (rs != null) {
                    oaObj = rs.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
                    return oaObj;
                }
            }
            catch (Exception e) {
                throw new RuntimeException("getUnique() getRemoteServer() exception", e);
            }
        }
        
        OASelect select = new OASelect(clazz);
        select.setWhere(propertyName+" = ?", new Object[] {uniqueKey});
        oaObj = select.next();
        if (oaObj != null) {
            return oaObj;
        }
        if (!bAutoCreate) return null;

        // need to create new, this needs to be synchronized
        synchronized (Lock) {
            oaObj = getUnique(clazz, propertyName, uniqueKey, false);
            if (oaObj != null) return oaObj;
            oaObj = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);
            try {
                OAThreadLocalDelegate.setLoading(true);
                oaObj.setProperty(propertyName, uniqueKey);
            }
            finally {
                OAThreadLocalDelegate.setLoading(false);
            }
        }
        
        return oaObj;
    }
    
    
    
}
