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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.model.ClientInfo;


/*qqqqqqqqqqqqq
CODEX

2. file/class/method
     src/main/java/com/viaoa/sync/remote/RemoteSessionImpl.java:162
     RemoteSessionImpl.updateObjectsWithoutHubs(...)

  concrete bug
  When bIsInHub == false, the method only retains the object if it is already present in the server object cache. If
  the server weak cache has evicted it, the method silently does nothing.

  runtime scenario
  A client still references an object outside any hub and reports that state to the server. The server cache no longer
  contains the object, but the object may still exist in datasource and on the client. The server fails to add it to
  hmObjectsWithoutHubs, so later saveCache(...) and per-session retention do not include it.

  why this violates OA/OG sync semantics
  The client is explicitly reporting “I still hold this object outside hubs.” That retention state should not depend
  solely on a transient weak-cache hit. Silent no-op can let the server lose track of client-held objects and skip
  later save/retention behavior.

  minimal fix direction
  Resolve the object from datasource on cache miss, store a key-only retention marker, or make the failure visible/
  logged so the session does not falsely appear to retain the object.

  suggested CODEX comment location
  RemoteSessionImpl.updateObjectsWithoutHubs(...), inside the bIsInHub == false branch after cache lookup.

2. file/class/method
     src/main/java/com/viaoa/sync/remote/RemoteSessionImpl.java:245
     RemoteSessionImpl.setLock(...)

  concrete bug
  Distributed lock/unlock resolves the target object only from the server object cache. If the object is not currently
  cached but can be resolved by datasource/key, lock returns false.

  runtime scenario
  A client attempts to lock an existing persisted object after the server cache has dropped it. The object identity is
  valid, but callObjectCacheGet(...) returns null, so the lock is denied without attempting authoritative object
  resolution.

  why this violates OA/OG sync semantics
  Distributed lock semantics are identity/key-based, not transient-cache-presence based. A valid remote lock request
  can fail only because the server weak cache does not currently hold the object.

  minimal fix direction
  Resolve from datasource on cache miss, or explicitly document that remote locks require server-cache residency and
  make callers handle that contract.

  suggested CODEX comment location
  RemoteSessionImpl.setLock(...), around the cache lookup and return false.



*/

/**
 * Base server-side implementation of {@link RemoteSessionInterface} representing
 * a single connected client session.
 * <p>
 * A {@code RemoteSessionImpl} maintains all per-client state required by the
 * synchronization layer, including:
 * <ul>
 *   <li>the GUID registry tracking which objects exist on the client,</li>
 *   <li>the set of objects that must be retained server-side even if they are
 *       no longer reachable through hubs,</li>
 *   <li>per-object locks held by this client,</li>
 *   <li>session liveness (ping) and diagnostic reporting,</li>
 *   <li>support for saving cached objects and releasing locks at disconnect.</li>
 * </ul>
 *
 * <h2>GUID and Cache Management</h2>
 * <ul>
 *   <li>{@link #objectCreated(long)} – records new objects appearing on client.</li>
 *   <li>{@link #objectsFinalized(long[])} – removes client-side objects from
 *       cache and lock structures.</li>
 *   <li>{@link #updateObjectsWithoutHubs(Class, OAObjectKey, boolean)} – keeps
 *       objects from being GC’d server-side when the client still references
 *       them but they are no longer reachable through any hub.</li>
 * </ul>
 *
 * <h2>Lock State</h2>
 * Per-object locks are tracked in {@link #hashLock}. These support collaborative
 * editing and remote conflict detection. All locks are cleared on disconnect.
 *
 * <h2>Session Shutdown</h2>
 * {@link #saveCache(OACascade, int)} persists all cached objects prior to
 * session termination. {@link #clearCaches()} removes GUID and cache
 * references to allow GC of server-side objects once the client disconnects.
 *
 * <p>
 * Subclasses implement:
 * <ul>
 *   <li>{@link #isLocked(Class, OAObjectKey)}</li>
 *   <li>{@link #isLockedByAnotherClient(Class, OAObjectKey)}</li>
 *   <li>{@link #sendException(String, Throwable)}</li>
 * </ul>
 * giving the server full control over session behavior.
 */
public abstract class RemoteSessionImpl implements RemoteSessionInterface {
	private static Logger LOG = Logger.getLogger(RemoteSessionImpl.class.getName());

	/**
	 * Identifier for this remote client session.
	 */
    protected final int sessionId;

    /*
     *  List of guids that are on the client.
     *  This is used for filtering sync messages that are sent to clients.
     *  
     *  This is added:
     *  1: whenever objects are serialized to the client.
     *  2: when an object is created on client and objectCreated is called. 
     */
    /**
     * Map tracking GUIDs of OAObjects that exist on the client.
     * <p>
     * The value indicates whether the object has been fully sent with all references.
     * </p>
     */
    protected final Map<UUID, Boolean> hmGuid;
    
    
    /**
     * Map tracking objects locked by this client session.
     */
	protected final ConcurrentHashMap<OAObject, OAObject> hashLock = new ConcurrentHashMap<OAObject, OAObject>();

	/**
	 * Map of objects that are referenced by the client but are not currently
	 * reachable through any hub, preventing server-side garbage collection.
	 */
	protected final ConcurrentHashMap<UUID, OAObject> hmObjectsWithoutHubs = new ConcurrentHashMap<>();

	/**
	 * Creates a new remote session for a client.
	 *
	 * @param sessionId the unique session identifier
	 * @param hmGuid map used to track object GUIDs present on the client
	 */
	public RemoteSessionImpl(int sessionId, Map<UUID, Boolean> hmGuid) {
		this.sessionId = sessionId;
		this.hmGuid = hmGuid;
	}


	/**
	 * Records that a new object has been created on the client.
	 *
	 * @param guid the GUID of the newly created object
	 */
    @Override
    public void objectCreated(UUID guid) {
        hmGuid.putIfAbsent(guid, false);
    }
    
    
    /**
     * Removes finalized objects from client tracking and cache structures.
     *
     * @param guids array of object GUIDs that have been finalized on the client
     */
    @Override
    public void objectsFinalized(UUID[] guids) {
        if (guids == null) return;
        for (UUID guid : guids) {
            hmGuid.remove(guid);
            hmObjectsWithoutHubs.remove(guid);
        }
    }
	
    
    /**
     * Updates tracking for objects that are not currently in any hub.
     * <p>
     * Ensures such objects are retained server-side while still referenced
     * by the client.
     * </p>
     *
     * @param c the class of the object
     * @param ok the object key
     * @param bIsInHub {@code true} if the object is in a hub, {@code false} otherwise
     */
    @Override
	public void updateObjectsWithoutHubs(Class c, OAObjectKey ok, boolean bIsInHub) {
	    if (c == null || ok == null) return;
	    if (bIsInHub) {
	        hmObjectsWithoutHubs.remove(ok.getGuid());
	    }
	    else {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
    	    OAObject obj = (OAObject) og.internal().objects().cache().get(c, ok);
    	    if (obj != null) {
                UUID guid = ok.getGuid();
                hmObjectsWithoutHubs.put(guid, obj);
    	    }
	    }
        int x = hmObjectsWithoutHubs.size();
        if (x % 100 == 0) {
            LOG.fine("sessionId=" + sessionId + ", cache size=" + x);
        }
	}
 
    /**
     * Saves all cached objects retained for this client session.
     *
     * @param cascade cascade behavior used during save
     * @param iCascadeRule cascade rule applied when saving objects
     */
	public void saveCache(OACascade cascade, int iCascadeRule) {
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hmObjectsWithoutHubs.size());
		for (Map.Entry<UUID, OAObject> entry : hmObjectsWithoutHubs.entrySet()) {
			OAObject obj = entry.getValue();
			if (!obj.wasDeleted()) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
				og.internal().objects().save().save(obj, iCascadeRule, cascade);
			}
		}
	}

	/**
	 * Clears all server-side caches associated with this client session.
	 */
	public void clearCaches() {
	    hmObjectsWithoutHubs.clear();
		if (hmGuid != null) hmGuid.clear();
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hmObjectsWithoutHubs.size());
	}

	
	/**
	 * Sets or clears a lock on an object for this client session.
	 *
	 * @param objectClass the class of the object to lock or unlock
	 * @param objectKey the key identifying the object
	 * @param bLock {@code true} to lock, {@code false} to unlock
	 * @return {@code true} if the object was found, otherwise {@code false}
	 */
	@Override
	public boolean setLock(Class objectClass, OAObjectKey objectKey, boolean bLock) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.internal().objects().cache().get(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		setLock(obj, bLock);
		return true;
	}

	/**
	 * Sets or clears a lock on the specified object for this client session.
	 *
	 * @param obj the object to lock or unlock
	 * @param bLock {@code true} to lock, {@code false} to unlock
	 */
	public void setLock(OAObject obj, boolean bLock) {
		if (bLock) {
			hashLock.put(obj, obj);
		} else {
			hashLock.remove(obj);
		}
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hashLock.size() + ", obj=" + obj + ", locked=" + bLock);
	}

	/**
	 * Clears all locks held by this client session.
	 */
	public void clearLocks() {
		for (Map.Entry<OAObject, OAObject> entry : hashLock.entrySet()) {
			OAObject obj = entry.getKey();
			setLock(obj, false);
		}
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hashLock.size());
	}

	// not used	
	// @Override
	/**
	 * Creates a new object instance on the server for this client session.
	 *
	 * @param clazz the class of the object to create
	 * @return the newly created object
	 */
	public OAObject createNewObject(Class clazz) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
		OAObject obj = (OAObject) og.internal().objects().reflect().createNewObject(clazz);
        objectCreated(obj.getGuid());
		updateObjectsWithoutHubs(clazz, obj.getObjectKey(),  false);
		return obj;
	}
	
	/**
	 * Determines whether an object is locked by this client session.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @return {@code true} if locked by this client, otherwise {@code false}
	 */
	@Override
	public boolean isLockedByThisClient(Class objectClass, OAObjectKey objectKey) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
		Object obj = og.internal().objects().cache().get(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		return (hashLock.get(obj) != null);
	}

	/**
	 * Echoes a ping message for session liveness checking.
	 *
	 * @param msg the ping message
	 * @return the same message that was received
	 */
	@Override
	public String ping(String msg) {
		return msg;
	}

	/**
	 * Receives a ping message with no return value.
	 *
	 * @param msg the ping message
	 */
	@Override
	public void ping2(String msg) {
	}

	/**
	 * Determines whether an object is locked by any client.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @return {@code true} if the object is locked, otherwise {@code false}
	 */
	@Override
	public abstract boolean isLocked(Class objectClass, OAObjectKey objectKey);

	/**
	 * Determines whether an object is locked by another client session.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @return {@code true} if locked by another client, otherwise {@code false}
	 */
	@Override
	public abstract boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey);

	/**
	 * Sends an exception notification to the client.
	 *
	 * @param msg a message describing the exception
	 * @param ex the exception to report
	 */
	@Override
	public abstract void sendException(String msg, Throwable ex);

	/**
	 * Updates this session with new client information.
	 *
	 * @param ci the client information to apply
	 */
	@Override
	public void update(ClientInfo ci) {
	}
	
}
