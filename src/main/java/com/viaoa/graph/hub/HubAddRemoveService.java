package com.viaoa.graph.hub;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCSDelegate;
import com.viaoa.hub.HubData;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubDataUnique;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.util.*;

public class HubAddRemoveService {
	private final Logger LOG = Logger.getLogger(HubAddRemoveService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	private final HubData.FriendAccess faHubData;
	private final HubDataUnique.FriendAccess faHubDataUnique;
	
	public HubAddRemoveService(HubService srvcHub, 
			Hub.FriendAccess faHub,
			HubData.FriendAccess faHubData,
			HubDataUnique.FriendAccess faHubDataUnique
			) {
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
    	if (faHubData == null) throw new IllegalArgumentException("HubData.FriendAccess can not be null");
    	this.faHubData = faHubData;
    	if (faHubDataUnique == null) throw new IllegalArgumentException("HubDataUnique.FriendAccess can not be null");
    	this.faHubDataUnique = faHubDataUnique;
	}

	/**
	 * Removes the specified object from the hub using default options for force,
	 * event sending, deletion mode, active-object updates, master-reference updates,
	 * and remove-all behavior. Delegates to the full remove implementation.
	 *
	 * @param thisHub the hub from which the object will be removed
	 * @param obj     the object to remove
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	public boolean remove(final Hub thisHub, final Object obj) {
		return remove(thisHub, obj, false, true, false, true, true, false);
	}

	/**
	 * Removes the object at the specified position using default force behavior.
	 * Delegates to the internal positional remove method.
	 *
	 * @param thisHub the hub containing the object
	 * @param pos     the position of the object to remove
	 * @return the removed object, or {@code null} if removal failed
	 */
	public Object remove(final Hub thisHub, final int pos) {
		return remove(thisHub, pos, false);
	}

	/**
	 * Removes the object at the specified position. Retrieves the object and
	 * delegates to the main remove implementation. Returns {@code null} if the
	 * object could not be removed.
	 *
	 * @param thisHub the hub containing the object
	 * @param pos     the position of the object
	 * @param bForce  whether to force removal
	 * @return the removed object, or {@code null} if removal failed
	 */
	public Object remove(final Hub thisHub, final int pos, final boolean bForce) {
		Object obj = srvcHub.getHubDataService().getObjectAt(thisHub, pos);
		if (!remove(thisHub, obj, bForce, true, false, true, true, false)) {
			obj = null;
		}
		return obj;
	}

	/**
	 * Removes an object from the hub with full control over removal behavior.
	 * Performs validation, event notifications, server messaging, vector updates,
	 * reference cleanup, and master/detail property adjustments.
	 *
	 * @param thisHub         the hub from which the object will be removed
	 * @param obj             the object to remove
	 * @param bForce          whether to force removal
	 * @param bSendEvent      whether to send before/after remove events
	 * @param bDeleting       whether the removal is part of a delete operation
	 * @param bSetAO          whether to update active-object references
	 * @param bSetPropToMaster whether to clear master/detail references
	 * @param bIsRemovingAll  whether this is part of a bulk remove/all operation
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	public boolean remove(final Hub thisHub, Object obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll) {

		if (obj == null || thisHub == null) {
			return false;
		}

		Hub hubx = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (hubx != null) {
			remove(hubx, obj, bForce, bSendEvent, bDeleting, bSetAO, true, bIsRemovingAll);
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
					throw new RuntimeException("Cant remove object, "+obj.getClass().getSimpleName()+", Hub can remove returned false");
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
			
			if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null && li != null) {
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
				// Hub might be changing, wait until _remove is called
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

			OALinkInfo lix = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
			
			if (lix != null) {
				if (lix.getType() == OALinkInfo.ONE) {
					boolean b = false;
					for (OAFkeyInfo fki : lix.getFkeyInfos()) {
						b |= fki.getFromPropertyInfo() != null && fki.getFromPropertyInfo().getKey();
					}
					if (!b) {
						HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, null);
					}
				} else if (lix.getType() == OALinkInfo.MANY) {
					// 20210326 M2M
					hubx = (Hub) lix.getValue(obj);
					if (hubx != null) {
						hubx.remove(faHub.getHubDataMaster(thisHub).getMasterObject());
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
	 * Determines whether the specified object can be removed from the hub and
	 * returns a descriptive message if removal is not allowed.
	 *
	 * @param thisHub   the hub being evaluated
	 * @param obj       the object to check, or {@code null} to evaluate hub state
	 * @param checkType the callback check type
	 * @return a message describing why removal is not allowed, or {@code null} if allowed
	 */
	public String getCantRemoveMessage(final Hub thisHub, final Object obj, final int checkType) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class c = obj.getClass();
			
			if (thisHub.getObjectClass() == null) {
				HubDelegate.setObjectClass(thisHub, c);
			}
			if (!thisHub.getObjectClass().isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
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

		if (thisHub.getSharedHub() != null) {
			return getCantRemoveMessage(thisHub.getSharedHub(), obj, checkType);
		}
		return null;
	}

	/**
	 * Determines whether all objects can be removed from the hub and returns a
	 * descriptive message if removal is not permitted.
	 *
	 * @param thisHub   the hub being evaluated
	 * @param checkType the callback check type
	 * @return a message describing why remove-all is not allowed, or {@code null} if allowed
	 */
	public String getCantRemoveAllMessage(final Hub thisHub, final int checkType) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
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

		Hub hubx = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (hubx != null) {
			return getCantRemoveAllMessage(hubx, checkType);
		}
		return null;
	}

	/**
	 * Clears all objects from the hub using default options for resetting the
	 * active object and sending a new-list event. Delegates to the full clear method.
	 *
	 * @param thisHub the hub to clear
	 */
	public void clear(final Hub thisHub) {
		clear(thisHub, true, true);
	}

	/**
	 * Clears all objects from the hub. Performs callback checks, locking, event
	 * notifications, removal operations, and active-object updates.
	 *
	 * @param thisHub       the hub to clear
	 * @param bSetAOtoNull  whether to set the active object to {@code null}
	 * @param bSendNewList  whether to fire a new-list event
	 */
	public void clear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
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

	/**
	 * Internal implementation for clearing all objects from the hub. Removes
	 * objects without direct event notifications, updates change tracking,
	 * manages remote-thread behavior, and resets references.
	 *
	 * @param thisHub      the hub to clear
	 * @param bSetAOtoNull whether to set the active object to {@code null}
	 * @param bSendNewList whether to send a new-list event
	 * @return {@code true} if the hub was cleared, otherwise {@code false}
	 */
	private boolean _clear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		if (thisHub.getSharedHub() != null) {
			return _clear(thisHub.getSharedHub(), bSetAOtoNull, bSendNewList);
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
		boolean bSendEvent = faHub.getHubDataMaster(thisHub).getMasterObject() != null;

		if (thisHub.isOAObject() && bSendEvent) {
			HubCSDelegate.removeAllFromHub(thisHub);
		}

		// 20160615
		Object[] objs = thisHub.toArray();
		faHubData.getVector(thisHub).removeAllElements();
		
		boolean bIsDeleting = OAThreadLocalDelegate.isDeleting(thisHub);
		if (!bIsDeleting && (faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges()) && thisHub.isOAObject()) {
			Vector vecRemove = faHub.getHubData(thisHub).getVecRemove();
			final boolean bWasEmpty = vecRemove == null ? true : vecRemove.size() == 0;
			for (Object obj : objs) {
				Vector vx = faHub.getHubData(thisHub).getVecAdd();
				if (vx != null && vx.removeElement(obj)) {
					// no-op
				} else {
					if (vecRemove == null) {
						vecRemove = HubDataDelegate.createVecRemove(thisHub);
					}
					if (bWasEmpty || vecRemove.indexOf(obj) < 0) {
						vecRemove.addElement(obj);
					}
				}
			}
			HubDataDelegate.setChanged(thisHub, (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0)
					|| (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0));
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

	/**
	 * Performs tasks after clearing the hub, including notifying shared hubs,
	 * firing new-list and after-remove-all events.
	 *
	 * @param thisHub      the hub that was cleared
	 * @param bSetAOtoNull whether the active object was reset
	 * @param bSendNewList whether a new-list event should be fired
	 */
	private void _afterClear(final Hub thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
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
	 * Determines whether the specified object can be added to the hub.
	 * Delegates to {@link #canAddMsg(Hub, Object)}.
	 *
	 * @param thisHub the hub to evaluate
	 * @param obj     the object to test
	 * @return {@code true} if the object can be added, otherwise {@code false}
	 */
	public boolean canAdd(final Hub thisHub, final Object obj) {
		String s = canAddMsg(thisHub, obj);
		return s == null;
	}

	/**
	 * Determines whether an object can be added to the hub. Delegates to
	 * {@link #canAddMsg(Hub, Object)} using {@code null}.
	 *
	 * @param thisHub the hub to evaluate
	 * @return {@code true} if adding is allowed, otherwise {@code false}
	 */
	public boolean canAdd(final Hub thisHub) {
		String s = canAddMsg(thisHub, null);
		return s == null;
	}

	/**
	 * Returns a message describing why an object cannot be added to the hub.
	 * Delegates to {@link #canAddMsg(Hub, Object)} using {@code null}.
	 *
	 * @param thisHub the hub to evaluate
	 * @return a message describing the restriction, or {@code null} if allowed
	 */
	public String canAddMsg(final Hub thisHub) {
		return canAddMsg(thisHub, null);
	}

	// returns null if obj can be added; otherwise an error msg is returned.
	/**
	 * Determines whether an object can be added to the hub. Performs enabled,
	 * class, master/detail, uniqueness, callback, and recursion checks.
	 *
	 * @param thisHub the hub to evaluate
	 * @param obj     the object to test
	 * @return {@code null} if adding is allowed, otherwise an error message
	 */
	public String canAddMsg(final Hub thisHub, final Object obj) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class c = obj.getClass();
			
			if (faHubData.getObjClass(thisHub) == null) {
				HubDelegate.setObjectClass(thisHub, c);
			}
			if (!faHubData.getObjClass(thisHub).isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
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

		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			return canAddMsg(faHub.getHubDataUnique(thisHub).getSharedHub(), obj);
		}

		if (obj != null && (faHub.getHubData(thisHub).getUniqueProperty() != null || faHub.getHubDataMaster(thisHub).getUniqueProperty() != null)) {
			if (!HubDelegate.verifyUniqueProperty(thisHub, obj)) {
				return "verifyUniqueProperty returned false for property " + faHub.getHubDataMaster(thisHub).getUniqueProperty();
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

			OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
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
	
	/**
	 * Adds an object to the hub using default contains-check behavior.
	 * Delegates to {@link #add(Hub, Object, boolean)}.
	 *
	 * @param thisHub the hub receiving the object
	 * @param obj     the object to add
	 * @return {@code true} if the object was added, otherwise {@code false}
	 */
    public boolean add(final Hub thisHub, final Object obj) {
        return add(thisHub, obj, false);
    }

    /**
     * Adds an object to the hub after performing validation, event dispatch,
     * locking, and link updates. Delegates to the internal add method.
     *
     * @param thisHub                the hub receiving the object
     * @param obj                    the object to add
     * @param bAlreadyCalledContains whether the caller has already checked contains()
     * @return {@code true} if the object was added
     */
    public boolean add(final Hub thisHub, final Object obj, final boolean bAlreadyCalledContains) {
		if (thisHub == null || obj == null) {
			return false;
		}
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			if (thisHub.getEnabled()) {
				return add(faHub.getHubDataUnique(thisHub).getSharedHub(), obj, bAlreadyCalledContains);
			}
		}

		final boolean bIsLoading = OAThreadLocalDelegate.isLoading();
		if (!bIsLoading && faHub.getHubData(thisHub).getSortListener() != null) {
			// use getCurrentSize to "guess" that it will go at the end, in
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
			b = _add(thisHub, obj, bIsLoading, bAlreadyCalledContains);
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

    /**
     * Internal implementation that performs the actual add logic, including
     * validation, event notifications, vector modifications, master/detail link
     * adjustments, and server messaging.
     *
     * @param thisHub                the hub receiving the object
     * @param obj                    the object to add
     * @param bIsLoading             whether the hub is in loading mode
     * @param bAlreadyCalledContains whether contains() has already been checked
     * @return {@code true} if the object was successfully added
     */
	private boolean _add(final Hub thisHub, final Object obj, final boolean bIsLoading, final boolean bAlreadyCalledContains) {
		if (obj instanceof OAObjectKey) {
			// store OAObjectKey.  Real object will be retrieved when it is accessed
			return internalAdd(thisHub, obj, true, true);
		}

		if (thisHub.getObjectClass() == null || thisHub.getObjectClass().equals(OAObject.class)) {
			Class c = obj.getClass();
			if (thisHub.getObjectClass() == null || !c.equals(OAObject.class)) {
				HubDelegate.setObjectClass(thisHub, c);
			}
		}

		// need to check even if isLoading=true, since datasource could autoadd to a cache hub
		if (!bAlreadyCalledContains && thisHub.contains(obj)) {
			if (thisHub.isOAObject()) {
				return false;
			}
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
			if (HubDataDelegate.contains(thisHub, obj, true)) {
				// this code has been moved before the listeners are notified.  Else listeners could ask for more objects

				if (!bIsLoading) {
					if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
						OALinkInfo lix = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
						if (lix != null) {
							if (lix.getType() == OALinkInfo.ONE) {
								HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, faHub.getHubDataMaster(thisHub).getMasterObject());
							} else if (lix.getType() == OALinkInfo.MANY) {
								// 20210326 M2M
								Hub hubx = (Hub) lix.getValue(obj);
								if (hubx != null) {
									hubx.add(faHub.getHubDataMaster(thisHub).getMasterObject());
								}
							}
						}
					} else if (((OAObject) obj).isNew()) {
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
							OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(	thisHub.getOAObjectInfo(), OALinkInfo.ONE);
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

	/**
	 * Fires the after-add event and updates the hub's referenceable state unless
	 * operating in loading mode.
	 *
	 * @param thisHub the hub to update
	 * @param obj     the object that was added
	 */
	private void _afterAdd(final Hub thisHub, final Object obj) {
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
	 * Adds the object to the hub's internal vector and registers hub membership
	 * for OAObjects. Does not perform validation or event notifications.
	 *
	 * @param thisHub       the hub receiving the object
	 * @param obj           the object to add
	 * @param bHasLock      whether the caller holds the lock
	 * @param bCheckContains whether to check for existing membership
	 * @return {@code true} if the object was added
	 */
	public boolean internalAdd(final Hub thisHub, final Object obj, final boolean bHasLock, final boolean bCheckContains) {
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

	/**
	 * Attempts to reposition an object within a sorted hub up to five times by
	 * retrieving its position and delegating to the move operation.
	 *
	 * @param thisHub the hub containing the object
	 * @param obj     the object to reposition
	 */
	public void sortMove(final Hub thisHub, final Object obj) {
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
	 * Moves an object from one position to another within the hub. Adjusts the
	 * target position for sorted hubs, fires before/after move events, and
	 * updates the internal data structure. Sends server notifications when needed.
	 *
	 * @param thisHub the hub containing the object
	 * @param posFrom the original position of the object
	 * @param posTo   the target position for the object
	 */
	public void move(final Hub thisHub, final int posFrom, int posTo) {
		if (posFrom == posTo) {
			if (faHub.getHubData(thisHub).getSortListener() == null) {
				return;
			}
		}
		if (posFrom < 0 || posTo < 0) {
			return;
		}
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			move(faHub.getHubDataUnique(thisHub).getSharedHub(), posFrom, posTo);
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
		if (faHub.getHubData(thisHub).getSortListener() != null) {
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
				if (faHub.getHubData(thisHub).getSortListener().getComparator().compare(objFrom, cobj) <= 0) {
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
	 * Inserts an object into the hub at the specified position. If the hub is
	 * sorted, the object is inserted at the correct sorted position. Performs
	 * validation, locking, internal insertion, and event dispatch.
	 *
	 * @param thisHub the hub receiving the object
	 * @param obj     the object to insert
	 * @param pos     the requested insert position
	 * @return {@code true} if insertion succeeded, otherwise {@code false}
	 */
	public boolean insert(final Hub thisHub, final Object obj, final int pos) {
		if (obj == null) {
			return false;
		}
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			return insert(faHub.getHubDataUnique(thisHub).getSharedHub(), obj, pos);
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

	/**
	 * Internal implementation of the insert operation. Handles sorted and
	 * unsorted hubs, duplicate checks, server notifications, vector insertion,
	 * and master/detail link adjustments.
	 *
	 * @param thisHub the hub receiving the object
	 * @param obj     the object to insert
	 * @param pos     the requested insert position
	 * @return the final insert position, or {@code -1} if insertion failed
	 */
	private int _insert(final Hub thisHub, final Object obj, int pos) {
		if (obj instanceof OAObjectKey) {
			// store OAObjectKey.  Real object will be retrieved when it is accessed
			boolean b = internalAdd(thisHub, obj, true, true);
			return b ? pos : -1;
		}
		if (thisHub.getObjectClass() == null || thisHub.getObjectClass().equals(OAObject.class)) {
			Class c = obj.getClass();
			if (thisHub.getObjectClass() == null || !c.equals(OAObject.class)) {
				HubDelegate.setObjectClass(thisHub, c);
			}
		}
		
		if (faHub.getHubData(thisHub).getSortListener() != null) {
		    // 20240118 need to make sure object is not already loaded
		    //    the Id could match, but the sort prop does not match
            if (thisHub.contains(obj) && thisHub.isOAObject()) {
                return -1;
            }
		    
			// 20170608 quicksort
			int head = -1;
			int tail = faHubData.getVector(thisHub).size();
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
				int c = faHub.getHubData(thisHub).getSortListener().getComparator().compare(obj, cobj);

				if (c == 0) {
					pos = i;
					// see if it's already in the list
					for (; i >= head; i--) {
						cobj = thisHub.elementAt(i);
						if (obj == cobj || obj.equals(cobj)) {
							return -1;
						}
						if (faHub.getHubData(thisHub).getSortListener().getComparator().compare(obj, cobj) != 0) {
							break;
						}
					}
					for (i = pos + 1; i < tail; i++) {
						cobj = thisHub.elementAt(i);
						if (obj == cobj || obj.equals(cobj)) {
							return -1;
						}
						if (faHub.getHubData(thisHub).getSortListener().getComparator().compare(obj, cobj) != 0) {
							break;
						}
					}
					break;
				} else if (c < 0) {
					tail = i;
				} else {
					head = i;
				}
			}
		} else {
			if (thisHub.contains(obj) && thisHub.isOAObject()) {
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
		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getType() == OALinkInfo.ONE) {
				HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, faHub.getHubDataMaster(thisHub).getMasterObject());
			} else if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getType() == OALinkInfo.MANY) {
				// 20210326 M2M
				Hub hubx = (Hub) faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getValue(obj);
				if (hubx != null) {
					hubx.add(faHub.getHubDataMaster(thisHub).getMasterObject());
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
				OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(	 thisHub.getOAObjectInfo(),
																					OALinkInfo.ONE);
				if (liRecursive != null) {
					OAObjectReflectDelegate.setProperty((OAObject) obj, liRecursive.getName(), null, null);
				}
			}
		}

		// if recursive and this is the root hub, then need to set parent to null (since object is now in root, it has no parent)
		if (thisHub.getRootHub() == thisHub) {
			OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(thisHub.getOAObjectInfo(), OALinkInfo.ONE);
			if (liRecursive != null) {
				OAObjectReflectDelegate.setProperty((OAObject) obj, liRecursive.getName(), null, null);
			}
		}

		return pos;
	}

	/**
	 * Fires the after-insert event and marks the hub as referenceable when not
	 * in loading mode.
	 *
	 * @param thisHub the hub where the object was inserted
	 * @param obj     the inserted object
	 * @param pos     the position of the inserted object
	 */
	private void _afterInsert(final Hub thisHub, final Object obj, final int pos) {
		HubEventDelegate.fireAfterInsertEvent(thisHub, obj, pos);
		if (!OAThreadLocalDelegate.isLoading()) {
			HubDelegate.setReferenceable(thisHub, true);
		}
	}

	/**
	 * Swaps the positions of two objects within the hub. Uses the move operation
	 * to reposition each object. No operation is performed if either index is
	 * invalid or no object exists at the given positions.
	 *
	 * @param thisHub the hub containing the objects
	 * @param pos1    the position of the first object
	 * @param pos2    the position of the second object
	 */
	public void swap(final Hub thisHub, int pos1, int pos2) {
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			swap(faHub.getHubDataUnique(thisHub).getSharedHub(), pos1, pos2);
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

	/**
	 * Retrieves the list of objects tracked as added to the hub.
	 *
	 * @param thisHub the hub to inspect
	 * @return an array of added {@link OAObject} instances
	 */
	public OAObject[] getAddedObjects(Hub thisHub) {
		return HubDataDelegate.getAddedObjects(thisHub);
	}

	/**
	 * Retrieves the list of objects tracked as removed from the hub.
	 *
	 * @param thisHub the hub to inspect
	 * @return an array of removed {@link OAObject} instances
	 */
	public OAObject[] getRemovedObjects(Hub thisHub) {
		return HubDataDelegate.getRemovedObjects(thisHub);
	}

	/**
	 * Determines whether the hub permits duplicate add/remove operations based on
	 * its configuration.
	 *
	 * @param thisHub the hub to check
	 * @return {@code true} if duplicate add/remove operations are allowed
	 */
	public boolean isAllowAddRemove(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		return faHub.getHubData(thisHub).isDupAllowAddRemove();
	}

	/**
	 * Determines whether objects can be removed from the hub. Considers duplicate
	 * add/remove configuration flags and foreign-key/primary-key constraints
	 * related to master/detail ONE-type links.
	 *
	 * @param thisHub the hub to evaluate
	 * @return {@code true} if removal is permitted
	 */
	public boolean isAllowRemove(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		if (!faHub.getHubData(thisHub).isDupAllowAddRemove()) {
			return false;
		}

		// see if fkeys is also pkey
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
		if (li == null || li.getType() != li.TYPE_ONE) {
			return true;
		}

		for (OAFkeyInfo fki : li.getFkeyInfos()) {
		    OAPropertyInfo pi = fki.getFromPropertyInfo();
			if (pi != null && pi.getKey()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds all objects from the specified list to the hub without performing
	 * validation, event dispatch, or link updates. Directly modifies the hub's
	 * internal vector.
	 *
	 * @param hub  the hub to modify
	 * @param list the objects to add
	 */
	public void unsafeAddAll(Hub hub, List list) {
		faHubData.getVector(hub).addAll(list);
	}

	/**
	 * Replaces all objects in the hub with those contained in another hub.
	 * Clears internal structures, removes old hub references, adds new objects,
	 * and fires a new-list event.
	 *
	 * @param hub    the hub being updated
	 * @param hubNew the hub providing the new objects
	 */
	public void refresh(Hub hub, Hub hubNew) {
		for (OAObject objx : (Hub<OAObject>) hub) {
			OAObjectHubDelegate.removeHub(objx, hub, false);
		}
		faHubData.getVector(hub).clear();
		
		faHub.getHubDataActive(hubNew).clear();

		for (OAObject objx : (Hub<OAObject>) hubNew) {
			faHubData.getVector(hub).add(objx);
			OAObjectHubDelegate.addHub(objx, hub);
		}
		HubEventDelegate.fireOnNewListEvent(hub, true);
	}

	
}
