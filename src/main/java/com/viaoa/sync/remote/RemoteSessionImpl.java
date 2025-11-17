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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.object.*;
import com.viaoa.sync.model.ClientInfo;

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

    protected final int sessionId;

    /**
     *  List of guids that are on the client.
     *  This is used for filtering sync messages that are sent to clients.
     *  
     *  This is added:
     *  1: whenever objects are serialized to the client.
     *  2: when an object is created on client and objectCreated is called. 
     */
    protected final Map<Long, Boolean> hmGuid;
    
    
	protected final ConcurrentHashMap<OAObject, OAObject> hashLock = new ConcurrentHashMap<OAObject, OAObject>();
    protected final ConcurrentHashMap<Long, OAObject> hmObjectsWithoutHubs = new ConcurrentHashMap<>();

	public RemoteSessionImpl(int sessionId, Map<Long, Boolean> hmGuid) {
		this.sessionId = sessionId;
		this.hmGuid = hmGuid;
	}


	/**
	 * Add to guid to cache, to know what objects are on client.
	 */
    @Override
    public void objectCreated(long guid) {
        hmGuid.putIfAbsent(guid, false);
    }
    
    
    /**
     * Called by client side OAObject.finalize, to remove the guids from hmGuid/Cache.
     */
    @Override
    public void objectsFinalized(long[] guids) {
        if (guids == null) return;
        for (long guid : guids) {
            hmGuid.remove(guid);
            hmObjectsWithoutHubs.remove(guid);
        }
    }
	
    
    /**
     * Used to manage OAObject on client to make sure that they are not GCd on Server. 
     */
    @Override
	public void updateObjectsWithoutHubs(Class c, OAObjectKey ok, boolean bIsInHub) {
	    if (c == null || ok == null) return;
	    if (bIsInHub) {
	        hmObjectsWithoutHubs.remove(ok.getGuid());
	    }
	    else {
    	    OAObject obj = (OAObject) OAObjectCacheDelegate.get(c, ok);
    	    if (obj != null) {
                long guid = ok.getGuid();
                hmObjectsWithoutHubs.put(guid, obj);
    	    }
	    }
        int x = hmObjectsWithoutHubs.size();
        if (x % 100 == 0) {
            LOG.fine("sessionId=" + sessionId + ", cache size=" + x);
        }
	}
 
	// called by server to save any client cached objects
	public void saveCache(OACascade cascade, int iCascadeRule) {
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hmObjectsWithoutHubs.size());
		for (Map.Entry<Long, OAObject> entry : hmObjectsWithoutHubs.entrySet()) {
			OAObject obj = entry.getValue();
			if (!obj.wasDeleted()) {
				OAObjectSaveDelegate.save(obj, iCascadeRule, cascade);
			}
		}
	}

	// called by server when client is disconnected
	public void clearCaches() {
	    hmObjectsWithoutHubs.clear();
		if (hmGuid != null) hmGuid.clear();
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hmObjectsWithoutHubs.size());
	}

	
	@Override
	public boolean setLock(Class objectClass, OAObjectKey objectKey, boolean bLock) {
		OAObject obj = (OAObject) OAObjectCacheDelegate.get(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		setLock(obj, bLock);
		return true;
	}

	public void setLock(OAObject obj, boolean bLock) {
		if (bLock) {
			hashLock.put(obj, obj);
		} else {
			hashLock.remove(obj);
		}
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hashLock.size() + ", obj=" + obj + ", locked=" + bLock);
	}

	// this is used at disconnect
	public void clearLocks() {
		for (Map.Entry<OAObject, OAObject> entry : hashLock.entrySet()) {
			OAObject obj = entry.getKey();
			setLock(obj, false);
		}
		LOG.fine("sessionId=" + sessionId + ", cache size=" + hashLock.size());
	}

	// not used	
	// @Override
	public OAObject createNewObject(Class clazz) {
		OAObject obj = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);
        objectCreated(obj.getGuid());
		updateObjectsWithoutHubs(clazz, obj.getObjectKey(),  false);
		return obj;
	}
	
	@Override
	public boolean isLockedByThisClient(Class objectClass, OAObjectKey objectKey) {
		Object obj = OAObjectCacheDelegate.get(objectClass, objectKey);
		if (obj == null) {
			return false;
		}
		return (hashLock.get(obj) != null);
	}

	@Override
	public String ping(String msg) {
		return msg;
	}

	@Override
	public void ping2(String msg) {
	}

	@Override
	public abstract boolean isLocked(Class objectClass, OAObjectKey objectKey);

	@Override
	public abstract boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey);

	@Override
	public abstract void sendException(String msg, Throwable ex);

	@Override
	public void update(ClientInfo ci) {
	}
	
}
