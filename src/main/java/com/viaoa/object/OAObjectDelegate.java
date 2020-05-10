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
package com.viaoa.object;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.context.OAContext;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAString;

/**
 * This is the central Delegate class that performs services for OAObjects. The other Delegate classes are specialized for specific tasks.
 * This Delegate is used for multi-specific and misc functionality. The Delegates are designed so that the OAObject class can be light
 * weight and have various functionalities built in.
 * 
 * @author vincevia
 */
public class OAObjectDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectDelegate.class.getName());

	public static final String WORD_New = "NEW";
	public static final String WORD_Changed = "CHANGED";
	public static final String WORD_Deleted = "DELETED";
	public static final String WORD_AutoAdd = "AutoAdd";

	public static final Boolean TRUE = new Boolean(true);
	public static final Boolean FALSE = new Boolean(false);

	/** Static global lock used when setting global properties (ex: guidCounter) */

	/** global counter used for local objects. Value is positive */
	static protected AtomicInteger guidCounter = new AtomicInteger(); // unique identifier needed for objects past from client/server

	/** global counter used for local objects. Value is negative */
	static protected AtomicInteger localGuidCounter = new AtomicInteger();

	/** Flag to know if finalized objects should be automatically saved. Default is false. */
	protected static boolean bFinalizeSave = false;

	/** tracks which OAObjects should not automatically add themself to a detailHub when an oaObj property is set. */
	private static final ConcurrentHashMap<Integer, Integer> hmAutoAdd = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * Called by OAObject constructor to assign guid and initialize new OAObject. If OAObjectFlagDelegate.isLoading() == false then
	 * initialize(...) will be called using the values from getOAObjectInfo()
	 */
	protected static void initialize(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		assignGuid(oaObj); // must get a guid before calling setInConstructor, so that it will have a valid hash key

		/**
		 * set OAObject.nulls to know if a primitive property is null or not. All "bits" are flagged/set to 1. Ordering and positions are
		 * set by the position of uppercase/sorted property in array. See: OAObjectInfoDelegate.initialize()
		 */
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		String[] ps = oi.getPrimitiveProperties();
		int x = (ps == null) ? 0 : ((int) Math.ceil(ps.length / 8.0d));
		oaObj.nulls = new byte[x];

        if (OAThreadLocalDelegate.isLoading()) return; // dont initialize. Whatever is loading should call initialize below directly 
        
		boolean bInitializeWithCS = !oi.getLocalOnly() && OASync.isClient(oaObj.getClass());
		initialize(oaObj, oi, oi.getInitializeNewObjects(), oi.getUseDataSource(), oi.getAddToCache(), bInitializeWithCS, true);
	}

	/**
	 * @param oaObj
	 * @param oi                if null, then the correct oi will be retrieved.
	 * @param bInitializeNulls  set all primitive properties to null
	 * @param bInitializeWithDS will call OAObjectDSDelegateinitialize()
	 * @param bAddToCache       if true then call OAObjectCacheDelegate.add()
	 * @param bInitializeWithCS if true, then call OAObjectCSDelegate.initialize().
	 */
	protected static void initialize(OAObject oaObj, OAObjectInfo oi, boolean bInitializeNulls, boolean bInitializeWithDS,
			boolean bAddToCache, boolean bInitializeWithCS, boolean bSetChangedToFalse) {
		final boolean bWasLoading = OAThreadLocalDelegate.setLoading(true);
		try {
			if (oi == null) {
				oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			}

			if (bInitializeNulls) {
				/* 20180325 20180403 removed,not used
				byte[] bsMask = oi.getPrimitiveMask();
				for (int i=0; i<oaObj.nulls.length; i++) {
				    oaObj.nulls[i] |= (byte) bsMask[i]; 
				}
				*/
				// put this back
				for (int i = 0; i < oaObj.nulls.length; i++) {
					oaObj.nulls[i] = (byte) ~oaObj.nulls[i];
				}
			}

			if (!bWasLoading) {
				for (OALinkInfo li : oi.getLinkInfos()) {
					if (li.getCalculated()) {
						continue;
					}
					if (li.getPrivateMethod()) {
						continue;
					}
					if (!li.getUsed()) {
						continue;
					}
					if (li.getMatchProperty() != null) {
						// dont set to null, so that it will have to call oaObject.getHub(), which will then create hubAutoMatch
						continue;
					}
					// 20140409 added check for 1to1, in which case one side will not have an
					//    fkey, since it uses it's own pkey as the fkey

					// 20190205 set default linkOne 
					if (li.getType() == li.TYPE_ONE && OAString.isNotEmpty(li.getDefaultContextPropertyPath())) {
						OAObject objx = OAContext.getContextObject();
						if (objx != null) {
							if (!li.getDefaultContextPropertyPath().equals(".")) {
								OAFinder hf = new OAFinder(li.getDefaultContextPropertyPath());
								objx = hf.findFirst(objx);
							}
							OAObjectPropertyDelegate.unsafeAddProperty(oaObj, li.getName(), objx);
						}
					} else {
						if (!OAObjectInfoDelegate.isOne2One(li)) {
							OAObjectPropertyDelegate.unsafeAddProperty(oaObj, li.getName(), null);
						}
					}
				}
			}

			if (bAddToCache) { // needs to run before any property could be set, so that OACS changes will find this new object.
				OAObjectCacheDelegate.add(oaObj, false, false); // 20090525, was true,true:  dont add to selectAllHub until after loadingObject is false
			}

			if (bInitializeWithCS) {
				// must be before DS init, since it could add to local client cache
				OAObjectCSDelegate.initialize(oaObj);
			}
			if (!bWasLoading && bInitializeWithDS) {
				if (OAObjectDSDelegate.getAssignIdOnCreate(oaObj)) {
					OAObjectDSDelegate.assignId(oaObj);
				}
			}
			if (bSetChangedToFalse) {
				oaObj.setChanged(false);
			}
			/*
			OAObjectKey key = OAObjectKeyDelegate.getKey(oaObj);
			String s = String.format("New, class=%s, id=%s",
			        OAString.getClassName(oaObj.getClass()),
			        key.toString()
			);
			if (oi.bUseDataSource) {
			    OAObject.OALOG.fine(s);
			}
			*/
		} finally {
			OAThreadLocalDelegate.setLoading(false);
		}
		if (!bWasLoading) {
			OAObjectCacheDelegate.fireAfterLoadEvent(oaObj);
		}
		if (bAddToCache) { // 20090525 needs to run after setLoadingObject(false), so that add event is handled correctly.
			OAObjectCacheDelegate.addToSelectAllHubs(oaObj);
		}
	}

	/**
	 * Flag to know if object is new and has not been saved.
	 */
	public static void setNew(final OAObject oaObj, final boolean b) {
		if (b == oaObj.newFlag) {
			return;
		}
		boolean old = oaObj.newFlag;
		oaObj.newFlag = b;
		OAObjectEventDelegate.fireBeforePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);
		try {
			OAObjectKeyDelegate.updateKey(oaObj, false);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "oaObj=" + oaObj.getClass() + ", key=" + OAObjectKeyDelegate.getKey(oaObj), e);
		}
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_New, old ? TRUE : FALSE, b ? TRUE : FALSE, false, false);
		if (!b) {
			setAutoAdd(oaObj, true);
		}
	}

	protected static void assignGuid(OAObject obj) {
		if (obj == null) {
			return;
		}
		if (obj.guid != 0) {
			return;
		}
		if (OAObjectInfoDelegate.getOAObjectInfo(obj).getLocalOnly()) {
			obj.guid = localGuidCounter.decrementAndGet();
		} else {
			if (!OASyncDelegate.isServer(obj)) {
				obj.guid = OAObjectCSDelegate.getServerGuid(obj);
				if (obj.guid == 0) {
					obj.guid = getNextGuid();
				}
			} else {
				obj.guid = getNextGuid();
			}
		}
	}

	// 20151029 remove the Id props, set new=true, reassign guid    
	public static void setAsNewObject(final OAObject oaObj) {
		if (oaObj == null) {
			return;
		}
		int guid = OAObjectCSDelegate.getServerGuid(oaObj);
		if (oaObj.guid == 0) {
			oaObj.guid = getNextGuid();
		}
		setAsNewObject(oaObj, guid);
	}

	public static void setAsNewObject(final OAObject oaObj, int guid) {
		if (oaObj == null) {
			return;
		}
		oaObj.newFlag = true;
		oaObj.objectKey = null;
		oaObj.guid = guid;

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
		String[] ids = oi.getIdProperties();
		if (ids == null) {
			return;
		}

		OAThreadLocalDelegate.setLoading(true);
		try {
			for (String id : ids) {
				OAObjectReflectDelegate.setProperty(oaObj, id, null, null);
			}
		} finally {
			OAThreadLocalDelegate.setLoading(false);
		}

		//OAObjectCSDelegate.initialize(oaObj);
		if (OAObjectDSDelegate.getAssignIdOnCreate(oaObj)) {
			OAObjectDSDelegate.assignId(oaObj);
		}

		oaObj.getObjectKey();
	}

	/**
	 * This is used by RemoteSyncImpl on the server, when it has to reload a GCd object from DS. This happens when a client makes a change
	 * and the server does not have the object in memory.
	 * 
	 * @param obj     newly loaded object from DS
	 * @param origKey
	 */
	public static void reassignGuid(OAObject obj, OAObjectKey origKey) {
		if (obj != null && origKey != null) {
			obj.guid = origKey.getGuid();
		}
	}

	/**
	 * Gets the next GUID for the current computer. also called by OAObjectServerImpl.java
	 */
	public static int getNextGuid() {
		return guidCounter.incrementAndGet(); // cant be 0
	}

	public static int getNextFiftyGuids() {
		return guidCounter.getAndAdd(50) + 1;
	}

	public static void setNextGuid(int x) {
		guidCounter.set(x);
	}

	/**
	 * Used when there is a duplicate object created, so that it will not be finalized Called by OAObjectCacheDelegate.add(OAObject, ...)
	 * when an object already exists.
	 */
	protected static void dontFinalize(OAObject obj) {
		if (obj != null) {
			obj.guid = 0; // flag so that OAObject.finalize should ignore this object.
		}
	}

	/**
	 * Used when "reading" serialized objects.
	 */
	protected static void updateGuid(int guid) {
		for (;;) {
			int g = guidCounter.get();
			if (g >= guid) {
				break;
			}
			if (guidCounter.compareAndSet(g, guid)) {
				break;
			}
		}
	}

	/**
	 * Removes object from HubController and calls super.finalize().
	 */
	public static void finalizeObject(OAObject oaObj) {
		//System.out.println((++qq)+" finalizeObject: "+oaObj);
		if (oaObj.guid == 0) {
			return; // set to 0 by readResolve or ObjectCacheDelegate.add() to ignore finalization
		}
		if (oaObj.guid > 0 && !oaObj.deletedFlag) { // set to 0 by readResolve or ObjectCacheDelegate.add() to ignore finalization
			if ((oaObj.changedFlag || oaObj.newFlag) && !OAObjectCSDelegate.isWorkstation(oaObj)) {

				// 20131128 added autoAttach check
				if (OAObjectDelegate.getAutoAdd(oaObj)) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
					if (oi != null && oi.getUseDataSource()) {
						LOG.fine("object was not saved, object=" + oaObj.getClass().getName() + ", key=" + OAObjectKeyDelegate.getKey(oaObj)
								+ ", willSaveNow=" + bFinalizeSave);
						if (bFinalizeSave) {
							try {
								oaObj.save(OAObject.CASCADE_NONE);
							} catch (Exception e) {
								LOG.log(Level.WARNING, "object had error while saving, object=" + oaObj.getClass().getName() + ", key="
										+ OAObjectKeyDelegate.getKey(oaObj), e);
							}
						}
					}
				}
			}
		}
		OAObjectCacheDelegate.removeObject(oaObj); // remove from class cache

		hmAutoAdd.remove(oaObj.guid);
		oaObj.weakhubs = null;
	}

	/**
	 * Returns true if this object is new or any changes have been made to this object or any objects in Links that are TYPE=MANY and
	 * CASCADE=true that match the relationshipType parameter.
	 */
	public static boolean getChanged(OAObject oaObj, int changedRule) {
		if (changedRule == OAObject.CASCADE_NONE) {
			return (oaObj.changedFlag || oaObj.newFlag);
		}
		OACascade cascade = new OACascade();
		boolean b = getChanged(oaObj, changedRule, cascade);
		return b;
	}

	public static boolean getChanged(final OAObject oaObj, int iCascadeRule, OACascade cascade) {
		if (oaObj.changedFlag || oaObj.newFlag) {
			return true;
		}
		if (iCascadeRule == oaObj.CASCADE_NONE) {
			return false;
		}
		if (cascade.wasCascaded(oaObj, true)) {
			return false;
		}

		if (oaObj.properties == null) {
			return false;
		}

		// check link cascade objects
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			String prop = li.getName();
			if (prop == null || prop.length() < 1) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			// same as OAObjectSaveDelegate.cascadeSave()
			if (OAObjectReflectDelegate.isReferenceNullOrNotLoaded(oaObj, prop)) {
				continue;
			}

			boolean bValidCascade = false;
			if (iCascadeRule == OAObject.CASCADE_LINK_RULES && li.cascadeSave) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_OWNED_LINKS && li.getOwner()) {
				bValidCascade = true;
			} else if (iCascadeRule == OAObject.CASCADE_ALL_LINKS) {
				bValidCascade = true;
			}

			if (OAObjectInfoDelegate.isMany2Many(li)) {
				Hub hub = (Hub) OAObjectReflectDelegate.getRawReference(oaObj, prop);
				if (HubDelegate.getChanged(hub, OAObject.CASCADE_NONE, cascade)) {
					return true;
				}
			}
			if (!bValidCascade) {
				continue;
			}

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.name); // if Hub with Keys, then this will load the correct objects to check
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				if (OAObjectHubDelegate.getChanged((Hub) obj, iCascadeRule, cascade)) {
					return true; //  if there have been adds/removes to hub
				}
			} else {
				if (obj instanceof OAObject) { // 20110420 could be OANullObject
					if (getChanged((OAObject) obj, iCascadeRule, cascade)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Used to recursively get all reference objects below this one. All objects will only be visited once.
	 */
	public static void recurse(OAObject oaObj, OACallback callback) {
		OACascade cascade = new OACascade();
		recurse(oaObj, callback, cascade);
	}

	/** see #recurse(OACallback) */
	public static void recurse(OAObject oaObj, OACallback callback, OACascade cascade) {
		if (cascade.wasCascaded(oaObj, true)) {
			return;
		}

		if (callback != null) {
			callback.updateObject(oaObj);
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		List al = oi.getLinkInfos();
		for (int i = 0; i < al.size(); i++) {
			OALinkInfo li = (OALinkInfo) al.get(i);
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			String prop = li.name;

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.name); // select all
			if (obj == null) {
				continue;
			}

			if (obj instanceof Hub) {
				Hub h = (Hub) obj;
				for (int j = 0;; j++) {
					Object o = h.elementAt(j);
					if (o == null) {
						break;
					}
					if (o instanceof OAObject) {
						recurse((OAObject) o, callback, cascade);
					} else {
						if (callback != null) {
							callback.updateObject(o);
						}
					}
					Object o2 = h.elementAt(j);
					if (o != o2) {
						j--;
					}
				}
			} else {
				if (obj instanceof OAObject) {
					recurse((OAObject) obj, callback, cascade);
				} else {
					if (callback != null) {
						callback.updateObject(obj);
					}
				}
			}
		}
	}

	protected static Object[] find(OAObject base, String propertyPath, Object findValue, boolean bFindAll) {
		if (propertyPath == null || propertyPath.length() == 0) {
			return null;
		}
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		Object result = base;
		for (; st.hasMoreTokens();) {
			String s = st.nextToken();
			base = (OAObject) result; // previous object
			result = base.getProperty(s);

			if (!st.hasMoreTokens()) {
				// last property, check against findValue
				if (result == findValue || (result != null && result.equals(findValue))) {
					Object[] objs = new Object[] { base };
					return objs;
				}
				return null;
			}

			if (result == null) {
				return null;
			}

			if (result instanceof Hub) {
				String pp = null;
				for (; st.hasMoreTokens();) {
					s = st.nextToken();
					if (pp == null) {
						pp = s;
					} else {
						pp += "." + s;
					}
				}
				ArrayList al = null;
				Hub h = (Hub) result;
				for (int ii = 0;; ii++) {
					Object obj = h.elementAt(ii);
					if (obj == null) {
						break;
					}
					Object[] objs = find((OAObject) obj, pp, findValue, bFindAll);
					if (objs != null) {
						if (!bFindAll) {
							return objs;
						}
						if (al == null) {
							al = new ArrayList(10);
						}
						for (int i3 = 0; i3 < objs.length; i3++) {
							al.add(objs[i3]);
						}
					}
				}
				if (al == null) {
					return null;
				}
				Object[] objs = new Object[al.size()];
				objs = al.toArray(objs);
				return objs;
			}
			if (!(result instanceof OAObject)) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Central method that is used when the object property Key is changed (OAObjectKey) and needs to be rehashed in all Hashtables that it
	 * could exist in.
	 * 
	 * @param oaObj
	 * @param oldKey
	 */
	protected static void rehash(OAObject oaObj, OAObjectKey oldKey) {
		// Need to rehash all Hashtables that OAObject is stored in:
		// 1: CacheDelegate hashtable
		// 2: obj.Hubs - NOTE: not needed, since Hubs dont use hashtables anymore
		// 3: HashDelegate hashtables

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		if (oi.getAddToCache()) {
			OAObjectCacheDelegate.rehash(oaObj, oldKey);
		}
		OAObjectHashDelegate.rehash(oaObj, oldKey);
	}

	public static int getGuid(OAObject obj) {
		if (obj == null) {
			return -1;
		}
		return obj.guid;
	}

	/**
	 * Used to determine if an object should be added to a reference/master hub when one of it's OAObject properties is set. If false, then
	 * the object will not be added to masterHubs until this is called with "true" or when oaObj is saved.
	 * 
	 * @param oaObj
	 * @param bEnabled (default is true)
	 */
	public static void setAutoAdd(OAObject oaObj, boolean bEnabled) {
		if (oaObj == null) {
			return;
		}
		if (!bEnabled && !oaObj.isNew()) {
			return;
		}

		boolean bOld = !hmAutoAdd.containsKey(oaObj.guid);
		if (bOld == bEnabled) {
			return;
		}

		if (!bEnabled) {
			hmAutoAdd.put(oaObj.guid, oaObj.guid);
		} else {
			hmAutoAdd.remove(oaObj.guid);
		}
		OAObjectEventDelegate.firePropertyChange(oaObj, WORD_AutoAdd, bOld ? TRUE : FALSE, bEnabled ? TRUE : FALSE, false, false);

		if (!bEnabled || oaObj.deletedFlag) {
			return;
		}

		try {
			OAThreadLocalDelegate.setSuppressCSMessages(true);
			// need to see if object should be put into linkOne/masterObject hub(s)             
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			for (OALinkInfo li : oi.getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.getType() != li.ONE) {
					continue;
				}
				Object objx = OAObjectReflectDelegate.getRawReference(oaObj, li.getName());
				if (!(objx instanceof OAObject)) {
					continue;
				}

				OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
				if (liRev == null) {
					continue;
				}
				if (!liRev.getUsed()) {
					continue;
				}
				if (liRev.getType() != li.MANY) {
					continue;
				}
				if (liRev.getPrivateMethod()) {
					continue;
				}

				Object objz = OAObjectReflectDelegate.getProperty((OAObject) objx, liRev.getName());
				if (objz instanceof Hub) {
					((Hub) objz).add(oaObj);
				}
			}
		} finally {
			OAThreadLocalDelegate.setSuppressCSMessages(false);
		}
	}

	public static boolean getAutoAdd(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return !hmAutoAdd.containsKey(oaObj.guid);
	}
}
