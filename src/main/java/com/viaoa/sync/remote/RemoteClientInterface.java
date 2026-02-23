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

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

/**
 * Remote interface representing client-side data access and detail-loading
 * operations executed on the server.
 * <p>
 * Methods on this interface are invoked from the client but executed on
 * {@code RemoteClientImpl} instances on the server. It provides:
 * <ul>
 *   <li>detail loading with optional sibling and property expansion,</li>
 *   <li>hub and object copy creation,</li>
 *   <li>datasource operations (insert, update, delete, select, count, etc.),</li>
 *   <li>refresh requests for individual objects or properties,</li>
 *   <li>hub-level delete support.</li>
 * </ul>
 *
 * <h2>Routing and Ordering</h2>
 * Methods annotated with {@code returnOnQueueSocket=true} bypass message
 * queues to ensure low-latency responses. Others participate in the normal
 * remote method queue for correct ordering relative to sync events.
 *
 * <p>
 * This interface is bound on the server and consumed by each {@code OASyncClient}.
 */
@OARemoteInterface()
public interface RemoteClientInterface {

	/**
	 * Creates and returns a copy of an existing object on the server.
	 *
	 * @param objectClass the class of the object to copy
	 * @param objectKey the key identifying the object to copy
	 * @param excludeProperties property names to exclude from the copy
	 * @return the copied object
	 */
    @OARemoteMethod(returnOnQueueSocket = true)
    <T extends OAObject> T createCopy(Class<T> objectClass, OAObjectKey objectKey, String[] excludeProperties);

    /**
     * Retrieves a detail property or hub value for a master object.
     *
     * @param id request identifier
     * @param masterClass the class of the master object
     * @param masterObjectKey key identifying the master object
     * @param property name of the property or reference to retrieve
     * @param bForHubMerger flag indicating hub-merger usage
     * @return the requested detail value or a serialized result
     */
	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger);

	/**
	 * Retrieves a detail property or hub value for a master object with optional
	 * master properties and sibling expansion.
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param masterProps additional master properties to retrieve
	 * @param siblingKeys keys of sibling objects to retrieve the same property from
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the requested detail value or a serialized result
	 */
	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);

	// dont put in queue, but have it returned on vsocket for queued messages
	//     All of the other methods are put in queue to be processed and have the return value set.
	/**
	 * Retrieves a detail property or hub value immediately, bypassing the normal
	 * remote message queue.
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param masterProps additional master properties to retrieve
	 * @param siblingKeys keys of sibling objects to retrieve the same property from
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the requested detail value or a serialized result
	 */
	@OARemoteMethod(returnOnQueueSocket = true)
	Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);


	
	/**
	 * Executes a datasource command on the server and returns the result.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the datasource command
	 * @return the result of the datasource operation
	 */
	@OARemoteMethod(returnOnQueueSocket = true) // dont add response to queue, write directly to socket used by queue
	Object datasource(int command, Object[] objects);
	
	
	/**
	 * Executes a datasource command on the server and returns the result
	 * through the normal remote queue.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the datasource command
	 * @return the result of the datasource operation
	 */
	@OARemoteMethod() // add to queue
	Object datasourceReturnOnQueue(int command, Object[] objects);

	/**
	 * Executes a datasource command on the server without returning a result.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the datasource command
	 */
	@OARemoteMethod(noReturnValue = true)
	void datasourceNoReturn(int command, Object[] objects);

    /* moved to  remoteSync  serverDelete, clientDelete
	boolean delete(Class objectClass, OAObjectKey objectKey);
	*/

	/**
	 * Deletes all objects from a hub property of the specified object.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @param hubPropertyName the name of the hub property to clear
	 * @return {@code true} if the hub existed and was cleared, otherwise {@code false}
	 */
	boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName);

	/**
	 * Refreshes an object from the datasource.
	 *
	 * @param clazz the class of the object
	 * @param objectKey the key identifying the object
	 */
	void refresh(Class clazz, OAObjectKey objectKey);

	/**
	 * Refreshes a specific property of an object from the datasource.
	 *
	 * @param clazz the class of the object
	 * @param objectKey the key identifying the object
	 * @param propertyName the name of the property to refresh
	 */
	void refresh(Class clazz, OAObjectKey objectKey, String propertyName);
}
