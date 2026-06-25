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
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

/*qqqqqqqqqqqqqqqqqqq
CODEX

  3. src/main/java/com/viaoa/sync/remote/RemoteClientImpl.java:140 / src/main/java/com/viaoa/sync/remote/
     RemoteDataSource.java:536

  Concrete bug:
  RemoteClientImpl.close() nulls remoteDataSource but does not close or drain active server-side iterators held inside
  RemoteDataSource.hashIterator.

  Runtime scenario:
  Client opens a remote datasource select, reads only part of the result set, then disconnects.
  OASyncServer.onClientDisconnect(...) calls cx.remoteClient.close(). The RemoteDataSource object is dropped without
  closing/removing active datasource iterators.

  Why this violates sync semantics:
  Normal disconnect can leak datasource iterator/result resources on the server until GC or datasource timeout. For
  long-lived server processes, this can leak cursors/connections from ordinary OA client behavior.

  Minimal fix direction:
  Add RemoteDataSource.close() to remove/close all active iterators where supported, and call it from
  RemoteClientImpl.close() before nulling the field.

  Suggested CODEX comment location:
  RemoteClientImpl.close() and RemoteDataSource.hashIterator.

  Suggested regression test:
  testRemoteClientCloseReleasesActiveRemoteDatasourceIterators()

2. src/main/java/com/viaoa/sync/remote/RemoteClientImpl.java:501 / refresh(Class, OAObjectKey) and src/main/java/
     com/viaoa/sync/remote/RemoteClientImpl.java:517 / refresh(Class, OAObjectKey, String)

  Concrete bug: client-requested refresh silently no-ops when the server object is not currently in cache.

  Runtime scenario: client calls OAObject.refresh(); server weak cache no longer has the object, but the datasource
  does. RemoteClientImpl.refresh(...) uses only cache lookup and returns void if missing. Client returns from refresh
  without updated values and without failure.

  Why this violates sync semantics: refresh is an authoritative server operation. Cache eviction should not turn a
  valid refresh into silent success/no-op.

  Minimal fix direction: resolve through the same cache-or-datasource path used by other server object operations, or
  throw/return failure if refresh cannot be applied.

  Suggested CODEX comment location: around both refresh(...) methods.

  Suggested regression test: testClientRefreshLoadsServerObjectWhenEvictedFromCache()

  3. src/main/java/com/viaoa/sync/remote/RemoteClientImpl.java:373 / createCopy(...)

  Concrete bug: remote create-copy uses cache-only lookup and returns null on server cache miss.

  Runtime scenario: client asks to create a copy of an object it still has by key, but the server weak cache has
  evicted the object. The authoritative datasource can reload it, but createCopy(...) returns null.

  Why this violates sync semantics: client-visible copy behavior depends on transient server cache residency, not
  object identity/datasource authority.

  Minimal fix direction: use the existing cache-or-datasource object resolution helper before copying, or fail visibly
  if the source cannot be resolved.

  Suggested CODEX comment location: RemoteClientImpl.createCopy(...), around lines 373-379.

  Suggested regression test: testRemoteCreateCopyReloadsSourceObjectAfterServerCacheEviction()


4. src/main/java/com/viaoa/sync/OASyncServer.java:1384 / start()

  Concrete bug: partial server startup failure leaves already-started components running.

  Runtime scenario: request logger and multiplexer start, then remote multiplexer or file service startup throws. The
  exception is visible, but sockets/threads already started by earlier steps are not stopped, so retry can run against
  partially live server state.

  Why this violates sync semantics: startup failure becomes retry-hostile and can leave live sync endpoints after
  caller believes startup failed.

  Minimal fix direction: track completed startup steps and clean them up in a catch block, or stage startup so
  components become visible only after all required services start.

  Suggested CODEX comment location: OASyncServer.start(), around lines 1384-1390.

  Suggested regression test: testSyncServerStartFailureCleansPreviouslyStartedComponents()

1. file/class/method
     src/main/java/com/viaoa/sync/remote/RemoteClientImpl.java:294
     RemoteClientImpl.getRemoteDataSource().setCached(...)

  concrete bug
  hmGuid.putIfAbsent(guid, false) happens before updateObjectCache(obj). If updateObjectCache fails, the server-side
  session GUID map can say the client has an object that the client did not successfully receive/retain.

  runtime scenario
  During remote datasource iteration, RemoteDataSource.datasourceNext(...) calls setCached(oa) before returning the
  batch. If updateObjectCache(obj) throws, the remote call fails and the client may receive no batch, but hmGuid
  already contains the object GUID.

  why this violates OA/OG sync semantics
  Sync filtering depends on hmGuid being truthful. A precommitted GUID can cause later sync/detail serialization to
  send only references or suppress messages because the server believes the client already has the object.

  minimal fix direction
  Move hmGuid.putIfAbsent(...) after successful updateObjectCache(obj), or roll it back if updateObjectCache fails.

  suggested CODEX comment location
  RemoteClientImpl.getRemoteDataSource().setCached(...), around the hmGuid.putIfAbsent call.






*/

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
			OADataSource ds = OARuntime.datasource().get(c);
			if (ds != null) {
				return ds;
			}
		}
		for (OADataSource ds : OARuntime.datasource().getAll()) {
			return ds;
		}
		return null;
	}


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
		final OAGraph og = OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.internal().objects().cache().getObject(objectClass, objectKey);
		if (obj == null) {
			return null;
		}
		OAObject objx = og.internal().objects().reflect().createCopy(obj, excludeProperties);
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
			final OAGraph og = OARuntime.graph(objectClass);
			if (!og.internal().sync().isServer()) {
				og.internal().objects().property().setPropertyCAS(obj, hubPropertyName, null, null, true, false);
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
		final OAGraph og = OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.internal().objects().cache().get(objectClass, origKey);
		if (obj == null && og.internal().sync().isServer()) {
			
			OADataSource ds = OARuntime.datasource().get(objectClass);
			if (ds != null) {
				obj = (OAObject) ds.getObject(objectClass, origKey);
			}
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
		final OAGraph og = OARuntime.graph(obj);
		boolean bWasLoaded = og.internal().objects().reflect().isReferenceHubLoaded(obj, hubPropertyName);
		if (!bWasLoaded && !og.internal().sync().isServer()) {
			return null;
		}
		Object objx = og.internal().objects().reflect().getProperty(obj, hubPropertyName);
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
		final OAGraph og = OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.internal().objects().cache().get(objectClass, objectKey);
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
		final OAGraph og = OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.internal().objects().cache().get(objectClass, objectKey);
		if (obj != null) {
			obj.refresh(propertyName);
		}
	}
}
