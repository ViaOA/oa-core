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
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAThrottle;

/**
 * Concrete implementation of {@link RemoteSyncInterface} used by both server
 * and clients to propagate live changes to OAObjects and Hubs.
 * <p>
 * A {@code RemoteSyncImpl} instance exists:
 * <ul>
 *   <li>on the server – to broadcast updates to all connected clients,</li>
 *   <li>on each client – to apply updates originating from the server.</li>
 * </ul>
 *
 * <h2>Property Changes</h2>
 * {@link #propertyChange(Class, OAObjectKey, String, Object, boolean)} resolves
 * objects by key, applies property updates, and (for blob properties) clears
 * cached values so the next getter retrieves the blob from the server.
 *
 * <h2>Hub Operations</h2>
 * Methods such as:
 * <ul>
 *   <li>{@link #addToHub(Class, OAObjectKey, String, Object)}</li>
 *   <li>{@link #removeFromHub(Class, OAObjectKey, String, Class, OAObjectKey)}</li>
 *   <li>{@link #moveObjectInHub(Class, OAObjectKey, String, int, int)}</li>
 *   <li>{@link #sort(Class, OAObjectKey, String, String, boolean, Comparator)}</li>
 * </ul>
 * modify the client’s or server’s local hub in real time.
 *
 * <h2>Object Resolution</h2>
 * {@code getObject()} loads objects from cache or from the datasource if
 * required (server-side). When an object is refetched after GC, the original
 * GUID is reassigned to preserve identity across the distributed graph.
 *
 * <h2>Hub Refresh and Cleanup</h2>
 * <ul>
 *   <li>{@code clearHubChanges} resets hub edit state.</li>
 *   <li>{@code refresh} performs full hub replacement (for server-side
 *       {@code Hub.sendRefresh}).</li>
 * </ul>
 *
 * <h2>Delete Propagation</h2>
 * <ul>
 *   <li>{@code serverDelete} – server-driven delete with cascading rules.</li>
 *   <li>{@code clientDelete} – client-driven delete which must be applied
 *       locally on the client.</li>
 * </ul>
 *
 * <p>
 * {@code RemoteSyncImpl} is the concrete engine that applies distributed
 * changes in OA’s executable object-graph model.
 */
public class RemoteSyncImpl implements RemoteSyncInterface {
	private static Logger LOG = Logger.getLogger(RemoteSyncImpl.class.getName());

	/**
	 * Throttle used to limit repeated property-change error logging.
	 */
	private final OAThrottle throttlePropertyChangeError = new OAThrottle(5000);

	/**
	 * Applies a property change to an object identified by class and key.
	 * <p>
	 * Resolves the object, updates the specified property, and clears cached
	 * blob values when required so that the value is reloaded from the server.
	 * </p>
	 *
	 * @param objectClass the class of the object
	 * @param origKey the key identifying the object
	 * @param propertyName the name of the property to update
	 * @param newValue the new property value
	 * @param bIsBlob {@code true} if the property represents a blob value
	 * @return {@code true} if the object was found and updated, otherwise {@code false}
	 */
	@Override
	public boolean propertyChange(Class objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob) {
		OAObject obj = getObject(objectClass, origKey, true);
		if (obj == null) {
			if (OASync.isServer()) {
				if (throttlePropertyChangeError.check()) {
					LOG.warning("Object not found, class=" + objectClass + ", key=" + origKey + ", propName=" + propertyName);
				}
			}
			return false;
		}
		OAObjectReflectDelegate.setProperty((OAObject) obj, propertyName, newValue, null);

		// blob value does not get sent, so clear the property so that a getXxx will retrieve it from server
		if (bIsBlob && newValue == null) {
			((OAObject) obj).removeProperty(propertyName);
		}
		return true;
	}

	/**
	 * Adds an object to a hub property on a master object.
	 *
	 * @param masterObjectClass the class of the master object
	 * @param masterObjectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 * @param objAdd the object to add to the hub
	 * @return {@code true} if the object was added, otherwise {@code false}
	 */
	@Override
	public boolean addToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object objAdd) {
		OAObject obj = getObject(masterObjectClass, masterObjectKey, true);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			// OAObjectPropertyDelegate.removePropertyIfNull((OAObject)obj, hubPropertyName, false); // if hub is null (empty), then need to get from server
			return false;
		}
		h.add(objAdd);
		return true;
	}

	/**
	 * Adds a newly created object to a hub property on a master object.
	 *
	 * @param masterObjectClass the class of the master object
	 * @param masterObjectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 * @param obj serializer containing the object to add
	 * @return {@code true} if the object was added, otherwise {@code false}
	 */
	@Override
	public boolean addNewToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, OAObjectSerializer obj) {
		Object objx = obj.getObject();
		return addToHub(masterObjectClass, masterObjectKey, hubPropertyName, objx);
	}

	/**
	 * Inserts an object into a hub property at a specific position.
	 *
	 * @param masterObjectClass the class of the master object
	 * @param masterObjectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 * @param objInsert the object to insert
	 * @param pos the position at which to insert the object
	 * @return {@code true} if the object was inserted, otherwise {@code false}
	 */
	@Override
	public boolean insertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object objInsert, int pos) {
		OAObject obj = getObject(masterObjectClass, masterObjectKey, true);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			// OAObjectPropertyDelegate.removePropertyIfNull((OAObject)obj, hubPropertyName, false);
			return false;
		}
		h.insert(objInsert, pos);
		return true;
	}

	/**
	 * Removes an object from a hub property on a master object.
	 *
	 * @param objectClass the class of the master object
	 * @param objectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 * @param objectClassRemove the class of the object to remove
	 * @param objectKeyRemove the key identifying the object to remove
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	@Override
	public boolean removeFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassRemove,
			OAObjectKey objectKeyRemove) {
		OAObject obj = getObject(objectClass, objectKey, true);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			return false;
		}

		OAObject objectRemove = getObject(objectClassRemove, objectKeyRemove, true);
		if (objectRemove == null) {
			return false;
		}

		h.remove(objectRemove);
		return true;
	}

	/* moved to RemoteClientImpl, so that it would be ran on the server
	@Override
	public boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
	    OAObject obj = getObject(objectClass, objectKey);
	    if (obj == null) return false;

	    Hub h = getHub(obj, hubPropertyName);
	    if (h == null) {
	        // store null so that it can be an empty hub if needed (and wont have to get from server)
	        if (!OASyncDelegate.isServer()) {
	            OAObjectPropertyDelegate.setPropertyCAS(obj, hubPropertyName, null, null, true, false);
	        }
	        return false;
	    }
	    h.deleteAll();
	    return true;
	}
	*/

	/**
	 * Removes all objects from a hub property on a master object.
	 *
	 * @param objectClass the class of the master object
	 * @param objectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 * @return {@code true} if the hub was cleared, otherwise {@code false}
	 */
	@Override
	public boolean removeAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
		OAObject obj = getObject(objectClass, objectKey, false);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			if (!OASyncDelegate.isServer(objectClass)) {
				OAObjectPropertyDelegate.setProperty(obj, hubPropertyName, null);
			}
			return false;
		}
		h.removeAll();
		return true;
	}

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
	@Override
	public boolean moveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, int posFrom, int posTo) {
		OAObject obj = getObject(objectClass, objectKey, false);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			return false;
		}

		h.move(posFrom, posTo);
		return true;
	}

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
	@Override
	public boolean sort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending,
			Comparator comp) {
		OAObject obj = getObject(objectClass, objectKey, true);
		if (obj == null) {
			return false;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			return false;
		}

		h.sort(propertyPaths, bAscending, comp);
		return true;
	}

	/**
	 * this was removed, since caching can cause GC on server and it will then later refetch the object, etc
	 *
	 * @Override public boolean removeObject(Class objectClass, OAObjectKey objectKey) { Object obj = OAObjectCacheDelegate.get(objectClass,
	 *           objectKey); if (obj == null) return false; OAObjectCacheDelegate.removeObject((OAObject) obj); return true; }
	 */

	// on the server, if the object is not found in the cache, then it will be loaded by the datasource
	/**
	 * Resolves an object by class and key from cache or datasource.
	 *
	 * @param objectClass the class of the object
	 * @param origKey the object key
	 * @param bCheckGuidKey flag indicating GUID validation behavior
	 * @return the resolved object, or {@code null} if not found
	 */
	private OAObject getObject(final Class objectClass, final OAObjectKey origKey, final boolean bCheckGuidKey) {
		if (origKey == null) {
			return null;
		}
    	final OAGraph og = OARuntime.get().graph(objectClass);
    	final OAObjectCacheService srvcObjectCache = og.objects().getOAObjectCacheService();
		OAObject obj = (OAObject) srvcObjectCache.get(objectClass, origKey);

		if (obj == null && OASyncDelegate.isServer(objectClass)) {
			obj = (OAObject) OADataSource.getObject(objectClass, origKey);
			if (obj != null) {
				// object must have been GCd, use the original guid
				OAObjectDelegate.reassignGuid(obj, origKey);
			}
		}
		return obj;
	}

	// on the server, if the Hub is not found in the cache, then it will be loaded by the datasource
	/**
	 * Resolves a hub property from an object.
	 *
	 * @param obj the master object
	 * @param hubPropertyName the name of the hub property
	 * @return the hub instance, or {@code null} if not available
	 */
	private Hub getHub(OAObject obj, String hubPropertyName) {
		if (obj == null) {
			return null;
		}
		boolean bWasLoaded = OAObjectReflectDelegate.isReferenceHubLoaded(obj, hubPropertyName);
		if (!bWasLoaded && !OASyncDelegate.isServer(obj.getClass())) {
			return null;
		}
		Object objx = OAObjectReflectDelegate.getProperty(obj, hubPropertyName);
		if (!(objx instanceof Hub)) {
			return null;
		}

		// loadCachedOwners will have been done by the call to getObject(masterObj)
		return (Hub) objx;
	}

	/**
	 * Clears pending change state for a hub property.
	 *
	 * @param masterObjectClass the class of the master object
	 * @param masterObjectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 */
	@Override
	public void clearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
		OAObject obj = getObject(masterObjectClass, masterObjectKey, false);
		if (obj == null) {
			return;
		}

		Hub h = getHub(obj, hubPropertyName);
		if (h == null) {
			return;
		}

		HubDataDelegate.clearHubChanges(h);
	}

	/**
	 * Refreshes a hub property by replacing its contents with server-provided data.
	 *
	 * @param masterObjectClass the class of the master object
	 * @param masterObjectKey the key identifying the master object
	 * @param hubPropertyName the name of the hub property
	 */
	@Override
	public void refresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
		if (OASync.isServer()) {
			return;
		}

		OAObject obj = getObject(masterObjectClass, masterObjectKey, false);
		if (obj == null) {
			return;
		}

		Hub hub = getHub(obj, hubPropertyName);
		if (hub == null) {
			return;
		}

		Hub<OAObject> hubNew = OAObjectCSDelegate.getServerReferenceHub(obj, hubPropertyName);

		HubAddRemoveDelegate.refresh(hub, hubNew);
	}

	/**
	 * Applies a server-initiated delete operation.
	 *
	 * @param objectClass the class of the object to delete
	 * @param objectKey the key identifying the object
	 */
    @Override
    public void serverDelete(Class objectClass, OAObjectKey objectKey) {
        OAObject obj = getObject(objectClass, objectKey, false);
        if (obj == null) {
            return;
        }
        if (!OASync.isServer(obj)) return;
        OAObjectDeleteDelegate.syncServerDelete(obj);
    }
	
	
    /**
     * Applies a client-initiated delete operation.
     *
     * @param objectClass the class of the object to delete
     * @param objectKey the key identifying the object
     */
    @Override
    public void clientDelete(Class objectClass, OAObjectKey objectKey) {
        OAObject obj = getObject(objectClass, objectKey, false);
        if (obj == null) {
            return;
        }
        if (!OASync.isClient(obj)) return;
        OAObjectDeleteDelegate.syncClientDelete(obj);
    }

    
}
