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
package com.viaoa.sync.remote;


import java.util.Comparator;

import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.remote.multiplexer.annotation.*;

/**
 * Remote interface defining broadcast-style synchronization messages exchanged
 * between server and clients.
 * <p>
 * Methods on this interface represent live updates to the distributed object
 * graph, including:
 * <ul>
 *   <li>property changes,</li>
 *   <li>hub insert/remove/move operations,</li>
 *   <li>detail and sibling refreshes,</li>
 *   <li>server-side and client-side deletes,</li>
 *   <li>hub sorting and collection resets.</li>
 * </ul>
 *
 * <h2>Execution Context</h2>
 * Many methods are invoked by the server to broadcast changes to all clients
 * that have the relevant objects. Others are invoked client → server (e.g.,
 * data entry from UI clients).
 *
 * <h2>Routing and Ordering</h2>
 * Some methods specify special queueing via:
 * <ul>
 *   <li>{@code @OARemoteMethod(runInRemoteThread = true)}</li>
 *   <li>{@code isOASync = true} on the interface</li>
 * </ul>
 * ensuring propagation ordering that preserves hub and object graph integrity.
 *
 * <p>
 * {@code RemoteSyncInterface} is the heart of OA’s distributed sync protocol.
 */

@OARemoteInterface(isOASync=true)
public interface RemoteSyncInterface {


	/**
	 * Used when an OAObject is saved or the first time it's added to a Hub,
	 * so that it exists on the SyncServer.
	*/
	void addNewToCache(OAObjectSerializer obj);
	
	
    // OAObjectCSDelegate    
	/**
	 * Applies a property change to an object identified by class and key.
	 *
	 * @param objectClass the class of the object
	 * @param origKey the key identifying the object
	 * @param propertyName the name of the property to change
	 * @param newValue the new value for the property
	 * @param bIsBlob {@code true} if the property represents a blob value
	 * @return {@code true} if the object was found and updated, otherwise {@code false}
	 */
    boolean propertyChange(Class objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob);    

    // HubCSDelegate
    /**
     * Adds an object to a hub property on a master object.
     *
     * @param masterObjectClass the class of the master object
     * @param masterObjectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @param obj the object to add to the hub
     * @return {@code true} if the object was added, otherwise {@code false}
     */
    boolean addToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj);
    
    
    /**
     * Inserts an object into a hub property at a specified position.
     *
     * @param masterObjectClass the class of the master object
     * @param masterObjectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @param obj the object to insert
     * @param pos the position at which to insert the object
     * @return {@code true} if the object was inserted, otherwise {@code false}
     */
    boolean insertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos);
    
    /**
     * Removes an object from a hub property on a master object.
     *
     * @param objectClass the class of the master object
     * @param objectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @param objectClassX the class of the object to remove
     * @param objectKeyX the key identifying the object to remove
     * @return {@code true} if the object was removed, otherwise {@code false}
     */
    boolean removeFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassX, OAObjectKey objectKeyX);   

    /**
     * Removes all objects from a hub property on a master object.
     *
     * @param objectClass the class of the master object
     * @param objectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @return {@code true} if the hub was cleared, otherwise {@code false}
     */
    boolean removeAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName);   
    
    /**
     * Moves an object within a hub from one position to another.
     *
     * @param objectClass the class of the master object
     * @param objectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @param posFrom the original position
     * @param posTo the destination position
     * @return {@code true} if the move was applied, otherwise {@code false}
     */
    boolean moveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName,  int posFrom, int posTo);

    /**
     * Sorts a hub property using the specified property paths and order.
     *
     * @param objectClass the class of the master object
     * @param objectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     * @param propertyPaths property paths used for sorting
     * @param bAscending {@code true} for ascending order, {@code false} for descending
     * @param comp optional comparator
     * @return {@code true} if the hub was sorted, otherwise {@code false}
     */
    boolean sort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator comp);

    /**
     * Clears pending change state for a hub property.
     *
     * @param masterObjectClass the class of the master object
     * @param masterObjectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     */
    void clearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);

    /**
     * Refreshes a hub property by replacing its contents with server-provided data.
     *
     * @param masterObjectClass the class of the master object
     * @param masterObjectKey the key identifying the master object
     * @param hubPropertyName the name of the hub property
     */
    void refresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
    
    /**
     * Applies a server-initiated delete operation.
     *
     * @param objectClass the class of the object to delete
     * @param objectKey the key identifying the object
     */
    @OARemoteMethod(runInRemoteThread = true) 
    void serverDelete(Class objectClass, OAObjectKey objectKey);

    /**
     * Applies a client-initiated delete operation.
     *
     * @param objectClass the class of the object to delete
     * @param objectKey the key identifying the object
     */
    void clientDelete(Class objectClass, OAObjectKey objectKey);
}
