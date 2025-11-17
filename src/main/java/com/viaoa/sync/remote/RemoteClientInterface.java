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

    @OARemoteMethod(returnOnQueueSocket = true)
	OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties);

	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger);

	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);

	// dont put in queue, but have it returned on vsocket for queued messages
	//     All of the other methods are put in queue to be processed and have the return value set.
	@OARemoteMethod(returnOnQueueSocket = true)
	Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);


	
	@OARemoteMethod(returnOnQueueSocket = true) // dont add response to queue, write directly to socket used by queue
	Object datasource(int command, Object[] objects);
	
	
	@OARemoteMethod() // add to queue
	Object datasourceReturnOnQueue(int command, Object[] objects);

	@OARemoteMethod(noReturnValue = true)
	void datasourceNoReturn(int command, Object[] objects);

    /* moved to  remoteSync  serverDelete, clientDelete
	boolean delete(Class objectClass, OAObjectKey objectKey);
	*/

	boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName);

	void refresh(Class clazz, OAObjectKey objectKey);

	void refresh(Class clazz, OAObjectKey objectKey, String propertyName);
}
