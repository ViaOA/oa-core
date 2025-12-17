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

import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.sync.model.ClientInfo;

/**
 * Remote interface representing a single client session on the server.
 * <p>
 * Each connected client receives its own server-side session object, which:
 * <ul>
 *   <li>tracks GUIDs known to exist on the client,</li>
 *   <li>keeps objects alive server-side when the client still references them,</li>
 *   <li>manages per-object locks,</li>
 *   <li>captures and forwards exceptions,</li>
 *   <li>receives periodic client-side update messages,</li>
 *   <li>supports liveness (ping/ping2).</li>
 * </ul>
 *
 * <h2>GUID Tracking</h2>
 * {@link #objectCreated(long)} and {@link #objectsFinalized(long[])} allow the
 * server to maintain an accurate record of which objects the client owns,
 * enabling correct sync filtering and preventing premature GC.
 *
 * <h2>Locks</h2>
 * Methods such as {@link #setLock(Class, OAObjectKey, boolean)} and
 * {@link #isLockedByAnotherClient(Class, OAObjectKey)} support shared editing
 * environments where multiple clients interact with the same model.
 *
 * <p>
 * {@code RemoteSessionInterface} forms the server-side state container for a
 * single client connection.
 */
@OARemoteInterface()
public interface RemoteSessionInterface {
    
	/**
	 * Notifies the server session that a new object was created on the client.
	 *
	 * @param guid the GUID of the newly created object
	 */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void objectCreated(long guid);

    /**
     * Notifies the server session that client-side objects have been finalized.
     *
     * @param guids array of GUIDs for objects that were finalized on the client
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void objectsFinalized(long[] guids);

    /**
     * Updates server-side tracking for objects that are no longer in any hub.
     *
     * @param c the class of the object
     * @param ok the object key
     * @param bIsInHub {@code true} if the object is in a hub, {@code false} otherwise
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void updateObjectsWithoutHubs(Class c, OAObjectKey ok, boolean bIsInHub);
    
    /**
     * Sets or clears a lock on an object for this client session.
     *
     * @param objectClass the class of the object
     * @param objectKey the key identifying the object
     * @param bLock {@code true} to lock, {@code false} to unlock
     * @return {@code true} if the object was found and the lock state was applied
     */
    boolean setLock(Class objectClass, OAObjectKey objectKey, boolean bLock);

    /**
     * Determines whether an object is locked by any client.
     *
     * @param objectClass the class of the object
     * @param objectKey the key identifying the object
     * @return {@code true} if the object is locked, otherwise {@code false}
     */
    boolean isLocked(Class objectClass, OAObjectKey objectKey);
    
    /**
     * Determines whether an object is locked by a different client session.
     *
     * @param objectClass the class of the object
     * @param objectKey the key identifying the object
     * @return {@code true} if locked by another client, otherwise {@code false}
     */
    boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey);
    
    /**
     * Determines whether an object is locked by this client session.
     *
     * @param objectClass the class of the object
     * @param objectKey the key identifying the object
     * @return {@code true} if locked by this client, otherwise {@code false}
     */
    boolean isLockedByThisClient(Class objectClass, OAObjectKey objectKey);

    /**
     * Updates this server-side session with client information.
     *
     * @param ci the client information to apply
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void update(ClientInfo ci); 
    
    /**
     * Sends an exception notification to the client.
     *
     * @param msg a message describing the exception
     * @param ex the exception to report
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void sendException(String msg, Throwable ex);

    /**
     * Sends a ping message to the server session and returns a response.
     *
     * @param msg the ping message
     * @return the response message
     */
    @OARemoteMethod(dontUseQueue=true)
    String ping(String msg);
    
    /**
     * Sends a ping message to the server session with no return value.
     *
     * @param msg the ping message
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void ping2(String msg);
}
