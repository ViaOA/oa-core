/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.sync.remote;

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCSDelegate;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAThrottle;

/**
 * Remote broadcast methods used to keep OAObjects, Hubs in sync with all computers. Note: there is an instance on the server and on each
 * client. The server needs to always try to update, even if the object is no longer in memory, then it will need to get from datasource.
 */
public class RemoteSyncImpl implements RemoteSyncInterface {
	private static Logger LOG = Logger.getLogger(RemoteSyncImpl.class.getName());

	private final OAThrottle throttlePropertyChangeError = new OAThrottle(5000);

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

	@Override
	public boolean addNewToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, OAObjectSerializer obj) {
		Object objx = obj.getObject();
		return addToHub(masterObjectClass, masterObjectKey, hubPropertyName, objx);
	}

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
	private OAObject getObject(final Class objectClass, final OAObjectKey origKey, final boolean bCheckGuidKey) {
		if (origKey == null) {
			return null;
		}
		OAObject obj = (OAObject) OAObjectCacheDelegate.get(objectClass, origKey);

		// 20200922 try to find using guid only version
		//   this is for cases where the new object's Id is assigned on one computer before other computers have received the
		//      change and only have the objKey guid.
		if (obj == null && bCheckGuidKey && !origKey.isEmpty()) {
			OAObjectKey ok = new OAObjectKey(null, origKey.getGuid(), origKey.isNew());
			obj = (OAObject) OAObjectCacheDelegate.get(objectClass, ok);
		}

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

	// 20150420
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
	 * Used when the server Hub.sendRefresh() is called, so that clients can replace with new collection.
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

}
