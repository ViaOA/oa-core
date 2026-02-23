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

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;
import com.viaoa.sync.model.ClientInfo;

/**
 * Remote interface representing the authoritative server-side object model.
 * <p>
 * A concrete {@code RemoteServerImpl} instance is created by the
 * {@code OASyncServer} and bound to the multiplexer so that each
 * {@code OASyncClient} can:
 * <ul>
 *   <li>load individual objects from the server cache or datasource,</li>
 *   <li>save objects with server-side cascade rules,</li>
 *   <li>open remote sessions and obtain {@link RemoteSessionInterface},</li>
 *   <li>obtain per-client {@link RemoteClientInterface} for detail loading
 *       and datasource access,</li>
 *   <li>invoke remote methods on OAObjects and Hubs,</li>
 *   <li>retrieve GUID sequences,</li>
 *   <li>refresh server caches,</li>
 *   <li>locate unique OAObjects using key-based resolution.</li>
 * </ul>
 *
 * <h2>Queueing Semantics</h2>
 * Some methods bypass the sync queue to avoid reordering:
 * <ul>
 *   <li>{@code ping}, {@code ping2}, {@code getDisplayMessage},</li>
 *   <li>{@code getNextFiftyObjectGuids},</li>
 *   <li>methods using {@code @OARemoteParameter(dontUseQueue = true)},</li>
 *   <li>methods using {@code returnOnQueueSocket = true} for low-latency
 *       return values.</li>
 * </ul>
 *
 * <p>
 * This interface defines the top-level RPC surface between the client and the
 * authoritative server OA model.
 */

@OARemoteInterface
public interface RemoteServerInterface {

	/**
	 * Saves an object on the server using the specified cascade rule.
	 *
	 * @param objectClass the class of the object to save
	 * @param objectKey the key identifying the object to save
	 * @param iCascadeRule the cascade rule to apply during save
	 * @return {@code true} if the object was found and saved, otherwise {@code false}
	 */
	boolean save(Class objectClass, OAObjectKey objectKey, int iCascadeRule);

	/**
	 * Retrieves an object from the server cache or datasource.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @return the resolved object, or {@code null} if not found
	 */
    @OARemoteMethod(returnOnQueueSocket = true)
    <T extends OAObject> T getObject(Class<T> objectClass, OAObjectKey objectKey);

    @OARemoteMethod(returnOnQueueSocket = true)
    <T extends OAObject> T getObjectUsingPkey(Class<T> objectClass, OAObjectKey objectKey);
    
    /**
     * Creates or retrieves a remote session for a client.
     *
     * @param clientInfo information describing the client
     * @param callback callback interface implemented by the client
     * @return a remote session interface instance
     */
	RemoteSessionInterface getRemoteSession(
			ClientInfo clientInfo,
			@OARemoteParameter(dontUseQueue = true) RemoteClientCallbackInterface callback);

	/**
	 * Creates or retrieves a remote client interface for a client.
	 *
	 * @param clientInfo information describing the client
	 * @return a remote client interface instance
	 */
	RemoteClientInterface getRemoteClient(ClientInfo clientInfo);

	/**
	 * Sends a ping message to the server and returns a response.
	 *
	 * @param msg the ping message
	 * @return the response message
	 */
	@OARemoteMethod(dontUseQueue = true)
	String ping(String msg);

	/**
	 * Sends a ping message to the server with no return value.
	 *
	 * @param msg the ping message
	 */
	@OARemoteMethod(noReturnValue = true, dontUseQueue = true)
	void ping2(String msg);

	/**
	 * Returns a display message identifying the server.
	 *
	 * @return a display message string
	 */
	@OARemoteMethod(dontUseQueue = true)
	String getDisplayMessage();

	
//qqqqqqqqqqqq no longer needed qqqqqqqqqqqqqqqqqqq	
	/**
	 * Retrieves the next block of object GUIDs.
	 *
	 * @return the starting GUID of the next block of fifty GUIDs
	 */
	@OARemoteMethod(dontUseQueue = true)
	long getNextFiftyObjectGuids();

	/**
	 * Refreshes the server-side cache for the specified class.
	 *
	 * @param clazz the class whose cache should be refreshed
	 */
	@OARemoteMethod(noReturnValue = true, dontUseQueue = true)
	void refreshCache(Class clazz);

	/**
	 * Invokes an instance method on a server-side object using reflection.
	 *
	 * @param clazz the class of the target object
	 * @param objKey the key identifying the target object
	 * @param methodName the name of the method to invoke
	 * @param args arguments to pass to the method
	 * @return the result returned by the invoked method
	 */
	@OARemoteMethod(returnOnQueueSocket = true)
	Object runRemoteMethod(Class clazz, OAObjectKey objKey, String methodName, Object[] args);

	/**
	 * Invokes an instance method on a provided server-side object using reflection.
	 *
	 * @param obj the target object
	 * @param methodName the name of the method to invoke
	 * @param args arguments to pass to the method
	 * @return the result returned by the invoked method
	 */
    @OARemoteMethod(returnOnQueueSocket = true)
    Object runRemoteMethod2(OAObject obj, String methodName, Object[] args);
	
    /**
     * Invokes a static hub-based method on the server using reflection.
     *
     * @param hub the hub passed as the first argument to the static method
     * @param methodName the name of the method to invoke
     * @param args additional arguments to pass to the method
     * @return the result returned by the invoked method
     */
	@OARemoteMethod(returnOnQueueSocket = true)
	Object runRemoteMethod(Hub hub, String methodName, Object[] args);

	/**
	 * Requests a server-side thread dump.
	 *
	 * @param msg a message describing the thread dump request
	 * @return the thread dump output as a string
	 */
	@OARemoteMethod(dontUseQueue = true)
	public String performThreadDump(String msg);

	/**
	 * Retrieves or creates a unique object based on a property value.
	 *
	 * @param clazz the class of the object
	 * @param propertyName the name of the unique property
	 * @param uniqueKey the unique key value to match
	 * @param bAutoCreate flag indicating whether the object should be created if not found
	 * @return the resolved or newly created object
	 */
	@OARemoteMethod(returnOnQueueSocket = true)
	OAObject getUnique(Class<? extends OAObject> clazz, final String propertyName, Object uniqueKey, boolean bAutoCreate);
}
