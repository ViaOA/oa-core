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
package com.viaoa.hub;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.util.OAString;

/**
 * Delegate for handling adding and removing from Hub.
 *
 * @author vvia
 */
public class HubAddRemoveDelegate {

	private static Logger LOG = Logger.getLogger(HubAddRemoveDelegate.class.getName());

	public static boolean remove(final Hub thisHub, final Object obj) {
		return remove(thisHub, obj, false, true, false, true, true, false);
	}

	public static Object remove(final Hub thisHub, final int pos) {
		return remove(thisHub, pos, false);
	}

	protected static Object remove(final Hub thisHub, final int pos, final boolean bForce) {
		Object obj = HubDataDelegate.getObjectAt(thisHub, pos);
		if (!remove(thisHub, obj, bForce, true, false, true, true, false)) {
			obj = null;
		}
		return obj;
	}

	public static boolean remove(final Hub thisHub, Object obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll) {

		if (obj == null || thisHub == null) {
			return false;
		}

		if (thisHub.datau.getSharedHub() != null) {
			remove(thisHub.datau.getSharedHub(), obj, bForce, bSendEvent, bDeleting, bSetAO, true, bIsRemovingAll);
			return false;
		}

		if (!bIsRemovingAll && !thisHub.contains(obj)) {
			return false;
		}

		if (!bIsRemovingAll && !thisHub.getEnabled()) {
			throw new RuntimeException("Cant remove object, hub is disabled");
		}
		if (!bIsRemovingAll && !OARemoteThreadDelegate.isRemoteThread()) {
			if (!thisHub.getAllowRemove(OAObjectCallback.CHECK_CallbackMethod, obj)) {
				//was: if (!canRemove(thisHub, obj)) {
				if (!OAThreadLocalDelegate.isDeleting(obj)) {
					throw new RuntimeException("Cant remove object, can remove returned false");
				}
			}
		}
		if (!bIsRemovingAll) {
			obj = HubDelegate.getRealObject(thisHub, obj);
			if (obj == null) {
				return false;
			}

			// check to see if this hub is a detail with LinkInfo.Type.ONE
			OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
			if (thisHub.datam.liDetailToMaster != null && li != null) {
				li = OAObjectInfoDelegate.getReverseLinkInfo(li);
				if (li != null && li.getType() == OALinkInfo.ONE) {
					if (!OAThreadLocalDelegate.isDeleting(obj)) {
						if (!OARemoteThreadDelegate.isRemoteThread()) {
							throw new RuntimeException("Cant remove object from Hub that is based on a LinkInfo.ONE, hub=" + thisHub);
						}
					}
				}
			}
		}

		int pos = 0;
		if (!bIsRemovingAll || bSendEvent) {
			pos = HubDataDelegate.getPos(thisHub, obj, false, false); // dont adjust master or update link when finding the position of the object.
			if (pos < 0) {
				//20140312 Hub might be changing, wait until _remove is called
				// return;
			}
			if (bSendEvent) {
				HubEventDelegate.fireBeforeRemoveEvent(thisHub, obj, pos);
			}
		}
		// send message to OAServer
		// OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
		if (bSendEvent && !bIsRemovingAll && thisHub.isOAObject()) {
			HubCSDelegate.removeFromHub(thisHub, (OAObject) obj, pos);
		}

		// this will lock, sync(data), and startNextThread
		pos = HubDataDelegate._remove(thisHub, obj, bDeleting, bIsRemovingAll);
		if (!bIsRemovingAll && pos < 0) {
			LOG.finer("object not removed, obj=" + obj);
			return false;
		}

		if (bSetAO) {
			HubShareDelegate.setSharedHubsAfterRemove(thisHub, obj, pos);
		}

		/* 20110439 need to do this before sending event, since
		    hub.containds(obj) now uses obj.weakHubs to know if an object is in the hub.
		    20130726 moved before setPropertyToMaster
		*/
		if (thisHub.isOAObject()) {
			OAObjectHubDelegate.removeHub((OAObject) obj, thisHub, false);
		}

		if (bSetPropToMaster) {
			// set the reference in detailObject to null.  Ex: if this is DeptHub, and Obj is Emp then call emp.setDept(null)

			if (thisHub.datam.liDetailToMaster != null) {
				if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.ONE) {
					String[] ss = thisHub.datam.liDetailToMaster.getUsesProperties();
					boolean b = true;
					if (ss != null) { // dont set to null if it's this object's primary key
						OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
						for (String s : ss) {
							OAPropertyInfo pi = oi.getPropertyInfo(s);
							if (pi != null && oi.isIdProperty(s)) {
								b = false;
								break;
							}
						}
					}
					if (b) {
						HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, null);
					}
				} else if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.MANY) {
					// 20210326 M2M
					Hub hubx = (Hub) thisHub.datam.liDetailToMaster.getValue(obj);
					if (hubx != null) {
						hubx.remove(thisHub.datam.getMasterObject());
					}
				}
			}
		}

		// this must be after bSetAO, so that the active object is updated.
		if (bSendEvent) {
			HubEventDelegate.fireAfterRemoveEvent(thisHub, obj, pos);
		}
		HubDelegate.setReferenceable(thisHub, true);
		return true;
	}

	/**
	 * Checks to see if there is a reason why an object can not be removed.
	 *
	 * @param obj       object to be removed, if null then it checks if the hub allows removals.
	 * @param checkType from OAObjectEdit
	 * @return reason/message why the object can not be removed, else returns null if the obj can be removed
	 */
	public static String getCantRemoveMessage(final Hub thisHub, final Object obj, final int checkType) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class c = obj.getClass();
			if (thisHub.data.objClass == null) {
				HubDelegate.setObjectClass(thisHub, c);
			}
			if (!thisHub.data.objClass.isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (thisHub.datam.getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}

		if (checkType > 0) {
			OAObjectCallback eq = OAObjectCallbackDelegate.getAllowRemoveObjectCallback(thisHub, (OAObject) obj, checkType);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.allowRemove is false, msg: " + s;
			}

			if (obj instanceof OAObject) {
				eq = OAObjectCallbackDelegate.getVerifyRemoveObjectCallback(thisHub, (OAObject) obj, checkType);
				if (eq != null && !eq.getAllowed()) {
					String s = eq.getResponse();
					s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
					return "ObjectCallback.verifyRemove is false, msg: " + s;
				}
			}
		}

		if (thisHub.datau.getSharedHub() != null) {
			return getCantRemoveMessage(thisHub.datau.getSharedHub(), obj, checkType);
		}
		return null;
	}

	public static String getCantRemoveAllMessage(final Hub thisHub, final int checkType) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (thisHub.datam.getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}

		if (checkType > 0) {
			OAObjectCallback eq = OAObjectCallbackDelegate.getAllowRemoveAllObjectCallback(thisHub, checkType);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.allowRemoveAll is false, msg: " + s;
			}
			eq = OAObjectCallbackDelegate.getVerifyRemoveAllObjectCallback(thisHub, checkType);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.verifyRemoveAll is false, msg: " + s;
			}
		}

		if (thisHub.datau.getSharedHub() != null) {
			return getCantRemoveAllMessage(thisHub.datau.getSharedHub(), checkType);
		}
		return null;
	}

	public static void clear(final Hub thisHub) {
		clear(thisHub, true, true);
	}

	public static void clear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		if (!OARemoteThreadDelegate.isRemoteThread() && bSendNewList) {
			OAObjectCallback eq = OAObjectCallbackDelegate.getVerifyRemoveAllObjectCallback(thisHub, OAObjectCallback.CHECK_CallbackMethod);
			if (!eq.getAllowed()) {
				String s = eq.getResponse();
				if (OAString.isEmpty(s)) {
					s = "Cant clear, OAObjectCallback allowRemoveAll retured false";
				}
				throw new RuntimeException(s);
			}
		}
		boolean b = false;
		if (thisHub.getSize() == 0) {
			return;
		}
		try {
			OAThreadLocalDelegate.lock(thisHub);
			b = _clear(thisHub, bSetAOtoNull, bSendNewList);
		} finally {
			OAThreadLocalDelegate.unlock(thisHub);
		}
		if (b) {
			OARemoteThreadDelegate.startNextThread(); // if this is RemoteThread, then start the next one
			_afterClear(thisHub, bSetAOtoNull, bSendNewList);
		}
	}

	private static boolean _clear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		if (thisHub.datau.getSharedHub() != null) {
			return _clear(thisHub.datau.getSharedHub(), bSetAOtoNull, bSendNewList);
		}

		if (!thisHub.getEnabled()) {
			return false;
		}

		Thread thread = Thread.currentThread();
		if (thread instanceof OARemoteThread) {
			OARemoteThread rt = (OARemoteThread) thread;
			rt.setStartedNextThread(true); // keep it from being started
		}

		if (bSetAOtoNull) {
			thisHub.setAO(null);
		}
		HubSelectDelegate.cancelSelect(thisHub, false);

		// 20140616 moved this here since other objects (ex: HubMerger) uses the
		//   to fire new events, etc.
		HubEventDelegate.fireBeforeRemoveAllEvent(thisHub);

		//int x = HubDataDelegate.getCurrentSize(thisHub);

		// 20120627 need to send event to clients if there is a masterObject
		boolean bSendEvent = thisHub.datam.getMasterObject() != null;

		if (thisHub.isOAObject() && bSendEvent) {
			HubCSDelegate.removeAllFromHub(thisHub);
		}

		// 20160615
		Object[] objs = thisHub.toArray();
		thisHub.data.vector.removeAllElements();

		boolean bIsDeleting = OAThreadLocalDelegate.isDeleting(thisHub);
		if (!bIsDeleting && (thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()) && thisHub.isOAObject()) {
			Vector vecRemove = thisHub.data.getVecRemove();
			for (Object obj : objs) {
				if (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().removeElement(obj)) {
					// no-op
				} else {
					if (vecRemove == null) {
						vecRemove = HubDataDelegate.createVecRemove(thisHub);
					}
					if (vecRemove.indexOf(obj) < 0) {
						vecRemove.addElement(obj);
					}
				}
			}
			HubDataDelegate.setChanged(thisHub, (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0)
					|| (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0));
		} else {
			HubDataDelegate.setChanged(thisHub, true);
		}

		// if this is OAClientThread, so that OAClientMessageHandler can continue with next message
		if (thread instanceof OARemoteThread) {
			OARemoteThread rt = (OARemoteThread) thread;
			rt.setStartedNextThread(false);
		}
		OARemoteThreadDelegate.startNextThread();

		// need to now have the object ref to hub removed.
		if (!bIsDeleting) {
			for (Object obj : objs) {
				remove(thisHub, obj, false, false, bIsDeleting, bSetAOtoNull, true, true);
			}
		}

		/*was
		Object objLast = null;
		for (int pos=0 ; ; ) {
		    Object obj = thisHub.elementAt(pos);
		    if (obj == null) break;

		    if (obj == objLast) {
		        // object was not deleted
		        pos++;
		        continue;
		    }
		    objLast = obj;

		    // 20140422 set to false, since clients will now have clear msg
		    remove(thisHub, obj, false,
		            false, false, bSetAOtoNull,
		            true, true); // dont force, dont send remove events
		    //was: remove(thisHub, ho, false, bSendEvent, false, bSetAOtoNull, bSetAOtoNull, true); // dont force, dont send remove events
		}
		*/
		return true;
	}

	private static void _afterClear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		// 20140501
		if (bSetAOtoNull) {
			HubShareDelegate.setSharedHubsAfterRemoveAll(thisHub);
		}

		if (bSendNewList) {
			HubEventDelegate.fireOnNewListEvent(thisHub, true);
		}
		HubEventDelegate.fireAfterRemoveAllEvent(thisHub);
	}

	/**
	 * Used to find out if an object can be added/inserted to this Hub. Makes sure that object that being added is for the correct class.
	 * Calls all HubListeners.hubBeforeAdd() where HubEvent.object and pos are both set. Calls editQuer If objects are OAObjets, then canAdd
	 * is called for each object. If it is a recursive Hub, then it will verify that it can have the parent set.
	 */
	public static boolean canAdd(final Hub thisHub, final Object obj) {
		String s = canAddMsg(thisHub, obj);
		return s == null;
	}

	public static boolean canAdd(final Hub thisHub) {
		String s = canAddMsg(thisHub, null);
		return s == null;
	}

	public static String canAddMsg(final Hub thisHub) {
		return canAddMsg(thisHub, null);
	}

	// returns null if obj can be added; otherwise an error msg is returned.
	public static String canAddMsg(final Hub thisHub, final Object obj) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class c = obj.getClass();
			if (thisHub.data.objClass == null) {
				HubDelegate.setObjectClass(thisHub, c);
			}
			if (!thisHub.data.objClass.isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (thisHub.datam.getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}

		OAObject oaObj = (obj instanceof OAObject) ? (OAObject) obj : null;
		OAObjectCallback eq = OAObjectCallbackDelegate.getAllowAddObjectCallback(thisHub, oaObj, OAObjectCallback.CHECK_CallbackMethod);
		if (eq != null && !eq.getAllowed()) {
			String s = eq.getResponse();
			s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
			return "ObjectCallback.allowAdd is false, msg: " + s;
		}

		if (obj instanceof OAObject) {
			eq = OAObjectCallbackDelegate.getVerifyAddObjectCallback(thisHub, (OAObject) obj, OAObjectCallback.CHECK_CallbackMethod);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.verifyAdd is false, msg: " + s;
			}
		}

		if (thisHub.datau.getSharedHub() != null) {
			return canAddMsg(thisHub.datau.getSharedHub(), obj);
		}

		if (obj != null && (thisHub.data.getUniqueProperty() != null || thisHub.datam.getUniqueProperty() != null)) {
			if (!HubDelegate.verifyUniqueProperty(thisHub, obj)) {
				return "verifyUniqueProperty returned false for property " + thisHub.datam.getUniqueProperty();
			}
		}

		// 20140731 recursive hub check
		if (obj != null && HubDetailDelegate.isRecursiveMasterDetail(thisHub)) {
			final Class c = obj.getClass();
			// cant add a recursive object to its children Hub
			// cant make a recursive object have one of its children as the parent

			// was:
			// OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(c);
			// OALinkInfo li = oi.getRecursiveLinkInfo(OALinkInfo.ONE);

			OALinkInfo li = thisHub.datam.liDetailToMaster;
			if (li != null) {
				Object master = HubDetailDelegate.getMasterObject(thisHub);
				if (master != null && master.getClass().equals(c)) {
					for (; master != null;) {
						if (master == obj) {
							return "recursive hub, cant add child as parent";
						}
						master = li.getValue(master);
					}
				}
			}
		}
		return null;
	}

	public static boolean add(final Hub thisHub, final Object obj) {
		if (thisHub == null || obj == null) {
			return false;
		}
		if (thisHub.datau.getSharedHub() != null) {
			if (thisHub.getEnabled()) {
				return add(thisHub.datau.getSharedHub(), obj);
			}
		}

		final boolean bIsLoading = OAThreadLocalDelegate.isLoading();
		if (!bIsLoading && thisHub.data.getSortListener() != null) {
			// use getCurrentSize to guess that it will go at the end, in
			//  cases where this is loaded in order.
			insert(thisHub, obj, thisHub.getCurrentSize());
			return true;
		}

		if (!bIsLoading && !OARemoteThreadDelegate.isRemoteThread()) {
			String s = canAddMsg(thisHub, obj);
			if (s != null) {
				throw new RuntimeException(
						"Cant add object, can add returned false, hub=" + thisHub + ", add object=" + obj + ", Reason: " + s);
			}
		}

		boolean b = false;
		try {
			if (!bIsLoading) {
				OAThreadLocalDelegate.lock(thisHub);
			}
			b = _add(thisHub, obj, bIsLoading);
		} finally {
			if (!bIsLoading) {
				OAThreadLocalDelegate.unlock(thisHub);
			}
		}
		if (b) {
			_afterAdd(thisHub, obj);
		}
		return b;
	}

	private static boolean _add(final Hub thisHub, final Object obj, final boolean bIsLoading) {
		if (obj instanceof OAObjectKey) {
			// store OAObjectKey.  Real object will be retrieved when it is accessed
			return internalAdd(thisHub, obj, true, true);
		}

		if (thisHub.data.objClass == null || thisHub.data.objClass.equals(OAObject.class)) {
			Class c = obj.getClass();
			if (thisHub.data.objClass == null || !c.equals(OAObject.class)) {
				HubDelegate.setObjectClass(thisHub, c);
			}
		}

		// need to check even if isLoading=true, since datasource could autoadd to a cache hub
		if (thisHub.contains(obj)) {
			return false;
		}

		if (!bIsLoading) {
			HubEventDelegate.fireBeforeAddEvent(thisHub, obj, thisHub.getCurrentSize());
		}

		if (thisHub.isOAObject()) {
			HubCSDelegate.addToHub(thisHub, (OAObject) obj); // use OAThreadLocalDelegate.setSuppressCSMessages(true) to not have add sent to other clients/server
		}
		if (!internalAdd(thisHub, obj, true, false)) {
			//LOG.warning(" NOT ADDED <<<<<");
			return false;
		}

		if (obj instanceof OAObject) {
			if (HubDataDelegate.contains(thisHub, obj)) {
				// this code has been moved before the listeners are notified.  Else listeners could ask for more objects

				if (!bIsLoading) {
					if (thisHub.datam.getMasterObject() != null) {
						if (thisHub.datam.liDetailToMaster != null) {
							if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.ONE) {
								HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, thisHub.datam.getMasterObject());
							} else if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.MANY) {
								// 20210326 M2M
								Hub hubx = (Hub) thisHub.datam.liDetailToMaster.getValue(obj);
								if (hubx != null) {
									hubx.add(thisHub.datam.getMasterObject());
								}
							}
						}
					} else if (obj instanceof OAObject && ((OAObject) obj).isNew()) {
						// 20201212
						Hub hubx = HubSelectDelegate.getSelectWhereHub(thisHub);
						if (hubx != null) {
							Object objx = hubx.getAO();
							if (objx != null) {
								String ppx = HubSelectDelegate.getSelectWhereHubPropertyPath(thisHub);
								OALinkInfo lix = hubx.getOAObjectInfo().getLinkInfo(ppx);
								if (lix != null) {
									lix = lix.getReverseLinkInfo();
									if (lix != null) {
										if (((OAObject) obj).getProperty(lix.getName()) == null) {
											((OAObject) obj).setProperty(lix.getName(), objx);
										}
									}
								}
							}
						}
					}

					// if recursive and this is the root hub, then need to set parent to null (since object is now in root, it has no parent)
					Hub rootHub = thisHub.getRootHub();
					if (rootHub != null) {
						if (rootHub == thisHub) {
							OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(	thisHub.data.getObjectInfo(),
																								OALinkInfo.ONE);
							if (liRecursive != null) {
								OAObjectReflectDelegate.setProperty((OAObject) obj, liRecursive.getName(), null, null);
							}
						}
					}
				}
			}
		}
		return true;
	}

	private static void _afterAdd(final Hub thisHub, final Object obj) {
		HubEventDelegate.fireAfterAddEvent(thisHub, obj, thisHub.getCurrentSize() - 1);
		if (!OAThreadLocalDelegate.isLoading()) {
			HubDelegate.setReferenceable(thisHub, true);
		} else { // 20120425 need to send ObjectCache event
					// 20130518 dont send if bInFetch (too much noise)
					// 201512 not needed, too noisy
					// OAObjectCacheDelegate.fireAfterAddEvent(thisHub, obj, thisHub.getCurrentSize()-1);
		}
	}

	/**
	 * internal method to add to vector and hashtable
	 */
	protected static boolean internalAdd(final Hub thisHub, final Object obj, final boolean bHasLock, final boolean bCheckContains) {
		if (obj == null) {
			return false;
		}

		// this will lock, sync(data), and startNextThread
		if (!HubDataDelegate._add(thisHub, obj, bHasLock, bCheckContains)) {
			return false;
		}

		if (obj instanceof OAObject) {
			OAObjectHubDelegate.addHub((OAObject) obj, thisHub);
		}

		return true;
	}

	protected static void sortMove(final Hub thisHub, final Object obj) {
		for (int i = 0; i < 5; i++) {
			try {
				int pos = thisHub.getPos(obj);
				move(thisHub, pos, pos);
				break;
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Swap the position of two different objects within the hub. This will call the move method. Sends a hubMove event to all HubListeners.
	 *
	 * @param posFrom position of object to move
	 * @param posTo   position where object should be after the move
	 */
	protected static void move(final Hub thisHub, final int posFrom, int posTo) {
		if (posFrom == posTo) {
			if (thisHub.data.getSortListener() == null) {
				return;
			}
		}
		if (posFrom < 0 || posTo < 0) {
			return;
		}
		if (thisHub.datau.getSharedHub() != null) {
			move(thisHub.datau.getSharedHub(), posFrom, posTo);
			return;
		}

		Object objFrom = thisHub.elementAt(posFrom);
		if (objFrom == null) {
			return;
		}

		int max = thisHub.getSize();
		if (posFrom >= max) {
			return;
		}

		/* if Hub is sorted, need to find valid toPosition. */
		if (thisHub.data.getSortListener() != null) {
			boolean b = false;
			for (int i = 0;; i++) {
				Object cobj = thisHub.elementAt(i);
				if (cobj == null) {
					posTo = (i - 1);
					break;
				}
				if (cobj == objFrom) {
					b = true;
					continue; // skip object that is moving
				}
				if (thisHub.data.getSortListener().comparator.compare(objFrom, cobj) <= 0) {
					posTo = i;
					if (b) {
						posTo--;
					}
					break;
				}
			}
			if (posFrom == posTo) {
				return;
			}
		}
		if (posTo >= max) {
			posTo = (max - 1);
		}

		HubEventDelegate.fireBeforeMoveEvent(thisHub, posFrom, posTo);

		//  OAClient must send message to OAServer before continuing
		HubCSDelegate.moveObjectInHub(thisHub, posFrom, posTo);

		// this will lock
		HubDataDelegate._move(thisHub, objFrom, posFrom, posTo);

		HubEventDelegate.fireAfterMoveEvent(thisHub, posFrom, posTo);
		// dont reset activeObject, since it will reset detailHubs
	}

	/**
	 * Insert an Object at a position. Hub Listeners will be notified with an insert event.
	 * <p>
	 * If Hub is sorted, then object will be inserted at correct/sorted position.
	 *
	 * @param obj Object to insert, must be from the same class that was used when creating the Hub
	 * @param pos position to insert the object into the Hub. If greater then size of Hub, then it will be added to the end.
	 * @return true if object was added else false (event hubBeforeAdd() threw an exception)
	 */
	public static boolean insert(final Hub thisHub, final Object obj, final int pos) {
		if (obj == null) {
			return false;
		}
		if (thisHub.datau.getSharedHub() != null) {
			return insert(thisHub.datau.getSharedHub(), obj, pos);
		}

		if (!OAThreadLocalDelegate.isLoading()) {
			if (!OARemoteThreadDelegate.isRemoteThread()) {
				String s = canAddMsg(thisHub, obj);
				if (s != null) {
					throw new RuntimeException(
							"Cant insert object, can add returned false, hub=" + thisHub + ", object=" + obj + ", Reason: " + s);
				}
			}
		}
		int newPos = pos;
		try {
			OAThreadLocalDelegate.lock(thisHub);
			newPos = _insert(thisHub, obj, pos);
		} finally {
			OAThreadLocalDelegate.unlock(thisHub);
		}
		boolean bResult = newPos >= 0;
		if (bResult) {
			_afterInsert(thisHub, obj, newPos);
		}
		return bResult;
	}

	// returns new Pos
	private static int _insert(final Hub thisHub, final Object obj, int pos) {
		if (obj instanceof OAObjectKey) {
			// store OAObjectKey.  Real object will be retrieved when it is accessed
			boolean b = internalAdd(thisHub, obj, true, true);
			return b ? pos : -1;
		}
		if (thisHub.data.objClass == null || thisHub.data.objClass.equals(OAObject.class)) {
			Class c = obj.getClass();
			if (thisHub.data.objClass == null || !c.equals(OAObject.class)) {
				HubDelegate.setObjectClass(thisHub, c);
			}
		}

		// 20140826 removed to make faster.  Another object could have the same objectId
		/*
		OAObjectKey key;
		if (obj instanceof OAObject) key = OAObjectKeyDelegate.getKey((OAObject)obj);
		else key = OAObjectKeyDelegate.convertToObjectKey(thisHub.getObjectClass(), obj);
		*/
		// if (HubDataDelegate.getObject(thisHub, key) != null) return false;

		if (thisHub.data.getSortListener() != null) {
			// 20170608 quicksort
			int head = -1;
			int tail = thisHub.data.vector.size();
			for (;;) {
				if (head + 1 >= tail) {
					pos = tail;
					break;
				}

				int i = ((tail - head) / 2);
				i += head;

				if (i == head) {
					i++;
				} else if (i == tail) {
					i--;
				}

				Object cobj = thisHub.elementAt(i);
				if (obj == cobj || obj.equals(cobj)) {
					return -1;
				}
				int c = thisHub.data.getSortListener().comparator.compare(obj, cobj);

				if (c == 0) {
					pos = i;
					// see if it's already in the list
					for (; i >= head; i--) {
						cobj = thisHub.elementAt(i);
						if (obj == cobj || obj.equals(cobj)) {
							return -1;
						}
						if (thisHub.data.getSortListener().comparator.compare(obj, cobj) != 0) {
							break;
						}
						;
					}
					for (i = pos + 1; i < tail; i++) {
						cobj = thisHub.elementAt(i);
						if (obj == cobj || obj.equals(cobj)) {
							return -1;
						}
						if (thisHub.data.getSortListener().comparator.compare(obj, cobj) != 0) {
							break;
						}
						;
					}
					break;
				} else if (c < 0) {
					tail = i;
				} else {
					head = i;
				}
			}
		} else {
			if (thisHub.contains(obj)) {
				return -1;
			}
			if (pos > 0) {
				thisHub.elementAt(pos - 1); // make sure object is loaded
			}
		}

		if (pos < 0) {
			pos = 0;
		}

		int x = thisHub.getCurrentSize();
		if (pos > x) {
			pos = x;
		}

		HubEventDelegate.fireBeforeInsertEvent(thisHub, obj, pos);

		// send message to OAServer
		//  OAClient must send message to OAServer before continuing
		if (thisHub.isOAObject()) {
			if (HubCSDelegate.insertInHub(thisHub, (OAObject) obj, pos)) {
				if (thisHub.contains(obj)) {
					return -1; // already loaded (another thread)
				}
			}
			//was: 20140826 removed to make faster.  Another object could have the same objectId.  (should use contains instead of getObj)
			// if (HubDataDelegate.getObject(thisHub, key) != null) return false;
		}

		// this will lock, sync(data), and startNextThread
		//was: boolean b = HubDataDelegate._insert(thisHub, key, obj, pos, false);  // false=dont lock, since this method is locked
		boolean b = HubDataDelegate._insert(thisHub, obj, pos, true);
		if (!b) {
			return -1;
		}

		/* 20140904 this is moved before setPropertyToMasterHub, so that
		 * hub.contains(obj) will return true.
		 */
		if (thisHub.isOAObject()) {
			OAObjectHubDelegate.addHub((OAObject) obj, thisHub);
		}

		// moved before listeners are notified.  Else listeners could ask for it.
		if (thisHub.datam.getMasterObject() != null) {
			if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.ONE) {
				HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, thisHub.datam.getMasterObject());
			} else if (thisHub.datam.liDetailToMaster.getType() == OALinkInfo.MANY) {
				// 20210326 M2M
				Hub hubx = (Hub) thisHub.datam.liDetailToMaster.getValue(obj);
				if (hubx != null) {
					hubx.add(thisHub.datam.getMasterObject());
				}
			}
		} else if (obj instanceof OAObject && ((OAObject) obj).isNew()) {
			// 20201212
			Hub hubx = HubSelectDelegate.getSelectWhereHub(thisHub);
			if (hubx != null) {
				Object objx = hubx.getAO();
				if (objx != null) {
					String ppx = HubSelectDelegate.getSelectWhereHubPropertyPath(thisHub);
					OALinkInfo lix = hubx.getOAObjectInfo().getLinkInfo(ppx);
					if (lix != null) {
						lix = lix.getReverseLinkInfo();
						if (lix != null) {
							if (((OAObject) obj).getProperty(lix.getName()) == null) {
								((OAObject) obj).setProperty(lix.getName(), objx);
							}
						}
					}
				}
			}
		}

		// if recursive and this is the root hub, then need to set parent to null (since object is now in root, it has no parent)
		Hub rootHub = thisHub.getRootHub();
		if (rootHub != null) {
			if (rootHub == thisHub) {
				OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(	thisHub.data.getObjectInfo(),
																					OALinkInfo.ONE);
				if (liRecursive != null) {
					OAObjectReflectDelegate.setProperty((OAObject) obj, liRecursive.getName(), null, null);
				}
			}
		}

		// if recursive and this is the root hub, then need to set parent to null (since object is now in root, it has no parent)
		if (thisHub.getRootHub() == thisHub) {
			OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(thisHub.data.getObjectInfo(), OALinkInfo.ONE);
			if (liRecursive != null) {
				OAObjectReflectDelegate.setProperty((OAObject) obj, liRecursive.getName(), null, null);
			}
		}

		return pos;
	}

	private static void _afterInsert(final Hub thisHub, final Object obj, final int pos) {
		HubEventDelegate.fireAfterInsertEvent(thisHub, obj, pos);
		if (!OAThreadLocalDelegate.isLoading()) {
			HubDelegate.setReferenceable(thisHub, true);
		}
	}

	/**
	 * Swap the position of two different objects within the hub. This will call the move method.
	 *
	 * @param pos1 position of object to move from, if there is not an object at this position, then no move is performed.
	 * @param pos2 position of object to move to, if there is not an object at this position, then no move is performed.
	 * @see #move
	 */
	public static void swap(final Hub thisHub, int pos1, int pos2) {
		if (thisHub.datau.getSharedHub() != null) {
			swap(thisHub.datau.getSharedHub(), pos1, pos2);
			return;
		}
		if (pos1 == pos2) {
			return;
		}
		if (pos1 > pos2) {
			int i = pos2;
			pos2 = pos1;
			pos1 = i;
		}
		Object obj1 = thisHub.elementAt(pos1);
		Object obj2 = thisHub.elementAt(pos2);

		if (obj1 == null || obj2 == null) {
			return;
		}

		move(thisHub, pos2, pos1);
		move(thisHub, pos1 + 1, pos2);
	}

	public static OAObject[] getAddedObjects(Hub thisHub) {
		return HubDataDelegate.getAddedObjects(thisHub);
	}

	public static OAObject[] getRemovedObjects(Hub thisHub) {
		return HubDataDelegate.getRemovedObjects(thisHub);
	}

	public static boolean isAllowAddRemove(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		return thisHub.data.isDupAllowAddRemove();
	}

	// 20211211
	public static boolean isAllowRemove(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		if (!thisHub.data.isDupAllowAddRemove()) {
			return false;
		}

		// see if fkeys is also pkey
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
		if (li == null || li.getType() != li.TYPE_ONE) {
			return true;
		}
		String[] props = li.getUsesProperties();
		if (props == null) {
			return true;
		}
		OAObjectInfo oi = thisHub.getOAObjectInfo();
		for (String prop : props) {
			OAPropertyInfo pi = oi.getPropertyInfo(prop);
			if (pi != null && pi.getId()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This will load all of the objects into the hub without checking or sending events.
	 */
	public static void unsafeAddAll(Hub hub, List list) {
		hub.data.vector.addAll(list);
	}

	public static void refresh(Hub hub, Hub hubNew) {
		for (OAObject objx : (Hub<OAObject>) hub) {
			OAObjectHubDelegate.removeHub(objx, hub, false);
		}
		hub.data.vector.clear();
		hub.dataa.clear();

		for (OAObject objx : (Hub<OAObject>) hubNew) {
			hub.data.vector.add(objx);
			OAObjectHubDelegate.addHub(objx, hub);
		}
		HubEventDelegate.fireOnNewListEvent(hub, true);
	}
}
