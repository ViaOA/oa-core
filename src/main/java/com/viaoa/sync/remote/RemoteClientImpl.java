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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.graph.service.object.OAObjectPropertyService;
import com.viaoa.graph.service.object.OAObjectReflectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
/**
 * Base server-side implementation of {@link RemoteClientInterface}. Each
 * connected client has its own concrete instance, created by the server's
 * {@code RemoteServerImpl}.
 * <p>
 * A {@code RemoteClientImpl} services requests originating from a client:
 * <ul>
 *   <li>detail loading via {@link ClientGetDetail},</li>
 *   <li>datasource operations such as {@code insert}, {@code update},
 *       {@code select}, and {@code count},</li>
 *   <li>object caching and cache retention on the server,</li>
 *   <li>copy operations and refresh operations.</li>
 * </ul>
 *
 * <h2>Detail Loading</h2>
 * Detail loading is delegated to a {@link ClientGetDetail} instance, which
 * manages sibling logic, depth rules, and object-graph serialization.
 *
 * <h2>Datasource Access</h2>
 * This class exposes a virtual {@link RemoteDataSource} to the client:
 * <ul>
 *   <li>All DS commands execute on the server's {@link OADataSource}.</li>
 *   <li>Iterator state for selects is tracked per client.</li>
 *   <li>Objects loaded from the datasource are optionally flagged in the
 *       client's GUID registry so the sync layer knows they exist on the
 *       client.</li>
 * </ul>
 *
 * <h2>Cache Retention</h2>
 * The abstract {@link #updateObjectCache(OAObject)} method allows the server
 * to mark objects as “in use” by that client, preventing premature GC or
 * eviction.
 *
 * <p>
 * This class contains substantial shared logic for all remote client sessions.
 */
public abstract class RemoteClientImpl implements RemoteClientInterface {
	private static Logger LOG = Logger.getLogger(RemoteClientImpl.class.getName());

	// protected ConcurrentHashMap<Object, Object> hashCache = new ConcurrentHashMap<Object, Object>();
	// protected ConcurrentHashMap<Object, Object> hashLock = new ConcurrentHashMap<Object, Object>();
	
	/**
	 * Helper responsible for servicing detail-loading requests from the client.
	 */
	private ClientGetDetail clientGetDetail;
	
	/**
	 * Lazily initialized remote data source wrapper used to execute datasource
	 * commands on behalf of the client.
	 */
	private volatile RemoteDataSource remoteDataSource;
	
	/**
	 * Identifier for the client session associated with this remote client instance.
	 */
	private int sessionId;
	
	/**
	 * Map tracking GUIDs of OAObjects known to exist on the client.
	 * <p>
	 * The value indicates whether the object has been fully sent with all references.
	 * </p>
	 */
	private final Map<UUID, Boolean> hmGuid;

	/**
	 * Creates a new remote client instance for a given session.
	 * <p>
	 * Initializes the {@link ClientGetDetail} helper and configures it to delegate
	 * background loading requests to this instance.
	 * </p>
	 *
	 * @param sessionId the unique session identifier
	 * @param hmGuid map used to track object GUIDs sent to the client
	 */
	public RemoteClientImpl(int sessionId, Map<UUID, Boolean> hmGuid) {
		this.sessionId = sessionId;
		this.hmGuid = hmGuid;
		clientGetDetail = new ClientGetDetail(sessionId, hmGuid) {
			@Override
			protected void loadDataInBackground(OAObject obj, String property) {
				RemoteClientImpl.this.loadDataInBackground(obj, property);
			}
		};
	}

	/**
	 * Called when property or sibling data cannot be loaded within the current
	 * request time budget.
	 * <p>
	 * The default implementation does nothing and may be overridden to perform
	 * background loading.
	 * </p>
	 *
	 * @param obj the object whose property should be loaded
	 * @param property the property name to load
	 */
	protected void loadDataInBackground(OAObject obj, String property) {
	}

	/**
	 * Closes this remote client instance.
	 * <p>
	 * Releases references to the {@link ClientGetDetail} helper and the
	 * remote datasource.
	 * </p>
	 */
	public void close() {
		clientGetDetail.close();
		clientGetDetail = null;
		remoteDataSource = null;
	}

	/**
	 * Retrieves a detail property or hub value for a master object.
	 * <p>
	 * Delegates the request to {@link ClientGetDetail} and returns either the
	 * direct value or a serialized result.
	 * </p>
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param masterProps additional master properties to load
	 * @param siblingKeys keys of sibling objects
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the requested detail value or serialized result
	 */
	@Override
	public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
			OAObjectKey[] siblingKeys, boolean bForHubMerger) {
		LOG.fine(id + ") masterClass=" + masterClass + ", prop=" + property);
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
		return obj;
	}

	/**
	 * Retrieves a detail property or hub value immediately, bypassing the message queue.
	 * <p>
	 * Uses the same logic as {@link #getDetail(int, Class, OAObjectKey, String, String[], OAObjectKey[], boolean)}
	 * but writes the response directly.
	 * </p>
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param masterProps additional master properties to load
	 * @param siblingKeys keys of sibling objects
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the requested detail value or serialized result
	 */
	@Override
	public Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
			OAObjectKey[] siblingKeys, boolean bForHubMerger) {
		LOG.fine(id + ") masterClass=" + masterClass + ", prop=" + property);
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
		return obj;
	}

	/**
	 * Retrieves a detail property or hub value for a master object without
	 * additional master or sibling properties.
	 *
	 * @param id request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey key identifying the master object
	 * @param property name of the property or reference to retrieve
	 * @param bForHubMerger flag indicating hub-merger usage
	 * @return the requested detail value or serialized result
	 */
	@Override
	public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger) {
		Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, null, null, bForHubMerger);
		return obj;
	}

	/**
	 * Returns the remote data source for this client.
	 * <p>
	 * Lazily creates the data source wrapper and ensures objects loaded
	 * from the datasource are tracked in the client GUID registry.
	 * </p>
	 *
	 * @return the remote data source
	 */
	public RemoteDataSource getRemoteDataSource() {
		if (remoteDataSource == null) {
			synchronized (this) {
				if (remoteDataSource == null) {
					remoteDataSource = new RemoteDataSource() {
						// used when an object from ds is not already in a hub with master.
						@Override
						public void setCached(OAObject obj) {
                            if (hmGuid != null) {
                                UUID guid = obj.getGuid();
                                hmGuid.putIfAbsent(guid, false);
                            }
							RemoteClientImpl.this.updateObjectCache(obj);
						}
					};
				}
			}
		}
		return remoteDataSource;
	}

	/**
	 * Executes a datasource command on behalf of the client.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the command
	 * @return the result of the datasource operation
	 * @throws RuntimeException if execution fails
	 */
	@Override
	public Object datasource(int command, Object[] objects) {
		Object result = null;
		try {
			result = getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasource, command=" + command + ", original exception msg=" + e.toString(), e);
			throw (ex);
		}
		return result;
	}

	
	/**
	 * Executes a datasource command and returns the result on the message queue.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the command
	 * @return the result of the datasource operation
	 * @throws RuntimeException if execution fails
	 */
	@Override
	public Object datasourceReturnOnQueue(int command, Object[] objects) {
		Object result = null;
		try {
			result = getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasource, command=" + command + ", original exception msg=" + e.toString(), e);
			throw (ex);
		}
		return result;
	}
	
	
	/**
	 * Executes a datasource command without returning a result.
	 *
	 * @param command the datasource command identifier
	 * @param objects arguments for the command
	 * @throws RuntimeException if execution fails
	 */
	@Override
	public void datasourceNoReturn(int command, Object[] objects) {
		try {
			getRemoteDataSource().datasource(command, objects);
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException(
					"Exception in remoteClient.datasourceNoReturn, command=" + command + ", original exception msg=" + e.toString());
			throw (ex);
		}
	}

	/**
	 * Resolves the appropriate datasource for the specified class.
	 *
	 * @param c the class used to determine the datasource
	 * @return the resolved datasource, or a default datasource if not found
	 */
	protected OADataSource getDataSource(Class c) {
		if (c != null) {
			OADataSource ds = OADataSource.getDataSource(c);
			if (ds != null) {
				return ds;
			}
		}
		if (defaultDataSource == null) {
			OADataSource[] dss = OADataSource.getDataSources();
			if (dss != null && dss.length > 0) {
				return dss[0];
			}
		}
		return defaultDataSource;
	}

	/**
	 * Default datasource used when no class-specific datasource is available.
	 */
	protected OADataSource defaultDataSource;

	/**
	 * Returns the default datasource.
	 *
	 * @return the default datasource
	 */
	protected OADataSource getDataSource() {
		return getDataSource(null);
	}

	/**
	 * Creates a copy of an existing object.
	 * <p>
	 * Retrieves the object from cache and creates a copy, optionally excluding
	 * specified properties.
	 * </p>
	 *
	 * @param objectClass the object class
	 * @param objectKey key identifying the object
	 * @param excludeProperties property names to exclude from the copy
	 * @return the copied object, or {@code null} if the source object is not found
	 */
	@Override
	public OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
		OAObject obj = (OAObject) srvcObjectCache.getObject(objectClass, objectKey);
		if (obj == null) {
			return null;
		}
		final OAObjectReflectService srvcOAObjectReflect = og.getOAObjectService().getOAObjectReflectService();
		OAObject objx = srvcOAObjectReflect.createCopy(obj, excludeProperties);
		return objx;
	}

	/**
	 * Updates the server-side cache to mark an object as retained for this client.
	 *
	 * @param obj the object to retain in the server cache
	 */
	public abstract void updateObjectCache(OAObject obj);

	/**
	 * Deletes all objects from a hub property of the specified object.
	 * <p>
	 * Retrieves the target object, resolves the hub by property name, and deletes
	 * all hub entries. If the hub is not found and the call is not on the server,
	 * an empty hub reference may be set on the object.
	 * </p>
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @param hubPropertyName the name of the hub property to clear
	 * @return {@code true} if the hub existed and was cleared, otherwise {@code false}
	 */
	@Override
	public boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
		OAObject obj = getObject(objectClass, objectKey);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			// store null so that it can be an empty hub if needed (and wont have to get from server)
			final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
			if (!og.getSyncService().isServer()) {
                final OAObjectPropertyService srvcOAObjectProperty = og.getOAObjectService().getOAObjectPropertyService();
                srvcOAObjectProperty.setPropertyCAS(obj, hubPropertyName, null, null, true, false);
			}
			return false;
		}
		h.deleteAll();
		return true;
	}

	// on the server, if the object is not found in the cache, then it will be loaded by the datasource
	/**
	 * Retrieves an object by key from cache or datasource.
	 * <p>
	 * On the server, if the object is not found in cache, it is loaded from the
	 * datasource and reassigned the original GUID.
	 * </p>
	 *
	 * @param objectClass the class of the object
	 * @param origKey the original object key
	 * @return the resolved object, or {@code null} if not found
	 */
	private OAObject getObject(Class objectClass, OAObjectKey origKey) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
		OAObject obj = (OAObject) srvcObjectCache.get(objectClass, origKey);
		if (obj == null && og.getSyncService().isServer()) {
			obj = (OAObject) OADataSource.getObject(objectClass, origKey);
			if (obj != null) {
				// object must have been GCd, use the original guid
//qqqqqqqqqqqqqqqqqqqqqqqqq 20260121 WAS: 				
//qq				og.getOAObjectService().getOAObjectGuidService().reassignGuid(obj, origKey);
			}
		}
		return obj;
	}

	// on the server, if the Hub is not found in the cache, then it will be loaded by the datasource
	/**
	 * Retrieves a hub property from an object.
	 * <p>
	 * Ensures the hub is loaded when required and returns it if present.
	 * </p>
	 *
	 * @param obj the master object
	 * @param hubPropertyName the hub property name
	 * @return the hub instance, or {@code null} if not available
	 */
	private Hub getHub(OAObject obj, String hubPropertyName) {
		if (obj == null) {
			return null;
		}
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(obj);
		final OAObjectReflectService srvcOAObjectReflect = og.getOAObjectService().getOAObjectReflectService();
		boolean bWasLoaded = srvcOAObjectReflect.isReferenceHubLoaded(obj, hubPropertyName);
		if (!bWasLoaded && !og.getSyncService().isServer()) {
			return null;
		}
		Object objx = srvcOAObjectReflect.getProperty(obj, hubPropertyName);
		if (!(objx instanceof Hub)) {
			return null;
		}

		// loadCachedOwners will have been done by the call to getObject(masterObj)
		return (Hub) objx;
	}

	/* moved to  remoteSync  serverDelete, clientDelete
	@Override
	public boolean delete(Class objectClass, OAObjectKey objectKey) {
		OAObject obj = getObject(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		obj.delete();
		return true;
	}
	*/

	/**
	 * Refreshes an object from the datasource.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 */
	@Override
	public void refresh(Class objectClass, OAObjectKey objectKey) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
		OAObject obj = (OAObject) srvcObjectCache.get(objectClass, objectKey);
		if (obj != null) {
			obj.refresh();
		}
	}

	/**
	 * Refreshes a specific property of an object from the datasource.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @param propertyName the name of the property to refresh
	 */
	@Override
	public void refresh(Class objectClass, OAObjectKey objectKey, String propertyName) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.getOAObjectService().getOAObjectCacheService();
		OAObject obj = (OAObject) srvcObjectCache.get(objectClass, objectKey);
		if (obj != null) {
			obj.refresh(propertyName);
		}
	}
}
