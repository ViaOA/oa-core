package com.viaoa.oa.service.hub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OAFkeyInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;


/*qqqqqqqqqqq
CODEX

 #3
  File/Class/Method: src/main/java/com/viaoa/oa/service/hub/HubAddRemoveService.java, _clear(...)

  Exact execution path: on a remote thread, _clear(...) calls callRemoteThreadSetStartedNextThread(true), then
  performs AO reset, select cancel, before-remove-all event, CS remove-all, vector clear, change tracking, and
  reverse cleanup. If any step before the explicit reset throws, startedNextThread is never restored and
  startNextThread() is not called.

  Why it is a correctness bug: one failed clear can stall remote-thread message progression and leave the remote-
  thread scheduler in a suppressed state.

  Semantic/invariant violated: remote-thread suppression flags must be restored in finally.

  Minimal fix: wrap the body after setStartedNextThread(true) in try/finally and restore/start-next in the finally
  path.

  Suggested test: remote-thread clear where a before-remove-all listener throws; assert startedNextThread is
  restored and next remote message can run.

 #5
  File/Class/Method: src/main/java/com/viaoa/oa/service/hub/HubAddRemoveService.java, _add(...), remove(...),
  move(...)

  Exact execution path: _add(...) sends callHubCSAddToHub(...) before internalAdd(...); remove(...) sends
  callHubCSRemoveFromHub(...) before callHubData_remove(...); move(...) sends callHubCSMoveObjectInHub(...) before
  callHubData_move(...). If the local mutation later fails or returns false, the remote/server side may already have
  accepted the mutation.

  Why it is a correctness bug: sync messages can describe a Hub transition that did not commit locally, causing
  local/server divergence under listener reentrancy, races, or local vector failure.

  Semantic/invariant violated: sync publication must correspond to a committed local transition, or the call must be
  explicitly authoritative and rollback local state on failure.

  Minimal fix: either move CS publication after successful local mutation, or make the CS call an authoritative
  prepare/commit with local rollback/abort semantics when local mutation fails.

  Suggested test: before-add listener or injected HubData failure causes internalAdd(...) to fail after CS add is
  sent; assert no remote add is emitted unless local membership exists.
>NOTE: CS is done first to have it 'ran' on Server, which can fail with exception, that is thrown on local



*/

/**
 * Coordinates adding, inserting, moving, and removing objects from Hubs.
 */

public abstract class HubAddRemoveService {
	private final Logger LOG = Logger.getLogger(HubAddRemoveService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubAddRemoveService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Removes or unregisters Hub state using this service.
	 *
	 * @param thisHub method input
	 * @param pos method input
	 * @return result value
	 */

	public <T extends OAObject> T remove(final Hub<T> thisHub, final int pos) {
		return remove(thisHub, pos, false);
	}
	/**
	 * Removes or unregisters Hub state using this service.
	 *
	 * @param thisHub method input
	 * @param pos method input
	 * @param bForce method input
	 * @return result value
	 */
	public <T extends OAObject> T remove(Hub<T> thisHub, int pos, boolean bForce) {
		T obj = callHubDataGetObjectAt(thisHub, pos);
		if (obj == null) return null;
		remove(thisHub, obj, bForce, true, false, true, true, false);
		return obj;
	}

	/**
	 * @param obj can be an OAObjet, OAObjectKey, pkey value
	 */
	public <T extends OAObject> boolean remove(Hub<T> hub, Object obj) {
		T t = remove(hub, obj, false, true, false, true, true, false);
 		return t != null;
	}

	/**
	 * Removes or unregisters Hub state using this service.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @return result value
	 */

	public <T extends OAObject> boolean remove(Hub<T> thisHub, T obj) {
 		T t = remove(thisHub, obj, false, true, false, true, true, false);
 		return t != null;
	}
	
	/**
	 * Removes an object from the hub with full control over removal behavior.
	 * Performs validation, event notifications, server messaging, vector updates,
	 * reference cleanup, and master/detail property adjustments.
	 *
	 * @param thisHub         the hub from which the object will be removed
	 * @param obj             the object to remove (OAObject, OAObjectKey, pkey value(s))
	 * @param bForce          whether to force removal
	 * @param bSendEvent      whether to send before/after remove events
	 * @param bDeleting       whether the removal is part of a delete operation
	 * @param bSetAO          whether to update active-object references
	 * @param bSetPropToMaster whether to clear master/detail references
	 * @param bIsRemovingAll  whether this is part of a bulk remove/all operation
	 * @return {@code true} if the object was removed, otherwise {@code false}
	 */
	public <T extends OAObject> T remove(final Hub<T> thisHub, final Object objOrig, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll) {

		if (objOrig == null || thisHub == null) {
			return null;
		}

		Hub<T> hubx = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (hubx != null) {
			T objx = remove(hubx, objOrig, bForce, bSendEvent, bDeleting, bSetAO, bSetPropToMaster, bIsRemovingAll);
			return objx;
		}
		
		T obj = callHubFindGetRealObject(thisHub, objOrig);
		if (obj == null) return null;
		
		if (!bIsRemovingAll && !thisHub.contains(obj)) {
			return null;
		}

		if (!bIsRemovingAll && !thisHub.getEnabled()) {
			throw new RuntimeException("Cant remove object, hub is disabled");
		}

		if (!bIsRemovingAll && !callRemoteThreadIsRemoteThread()) {
			if (!thisHub.getAllowRemove(obj, true, true)) {
				if (!callThreadLocalIsDeleting(obj)) {
					throw new RuntimeException("Cant remove object, "+obj.getClass().getSimpleName()+", Hub can remove returned false");
				}
			}
		}
		if (!bIsRemovingAll) {
			// check to see if this hub is a detail with LinkInfo.Type.ONE
			OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(thisHub);
			
			if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null && li != null) {
				li = callObjectInfoGetReverseLinkInfo(li);
				if (li != null && li.getType() == OALinkInfo.ONE) {
					if (!callThreadLocalIsDeleting(obj)) {
						if (!callRemoteThreadIsRemoteThread()) {
							throw new RuntimeException("Cant remove object from Hub that is based on a LinkInfo.ONE, hub=" + thisHub);
						}
					}
				}
			}
		}

		int pos = 0;
		if (!bIsRemovingAll || bSendEvent) {
			pos = callHubDataGetPos(thisHub, obj, false, false); // dont adjust master or update link when finding the position of the object.
			if (pos < 0) {
				// Hub might be changing, wait until _remove is called
				// return;
			}
			if (bSendEvent) {
				callHubEventFireBeforeRemoveEvent(thisHub, obj, pos);
			}
		}
		// send message to OAServer
		// OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(thisHub.getObjectClass());
		if (bSendEvent && !bIsRemovingAll) {
			callHubCSRemoveFromHub(thisHub, obj, pos);
		}

		// this will lock, sync(data), and startNextThread
		pos = callHubData_remove(thisHub, obj, bDeleting, bIsRemovingAll);
		if (!bIsRemovingAll && pos < 0) {
			LOG.finer("object not removed, obj=" + obj);
			return null;
		}

		if (bSetAO) {
			callHubShareSetSharedHubsAfterRemove(thisHub, obj, pos);
		}

		/* 20110439 need to do this before sending event, since
		    hub.containds(obj) now uses obj.weakHubs to know if an object is in the hub.
		    20130726 moved before setPropertyToMaster
		*/
		callObjectHubRemoveHub(obj, thisHub, false);

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
						callHubDetailSetPropertyToMasterHub(thisHub, obj, null);
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
			callHubEventFireAfterRemoveEvent(thisHub, obj, pos);
		}
		callHubStatusSetReferenceable(thisHub, true);
		return obj;
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
	@SuppressWarnings({"unchecked"})
	public <T extends OAObject> String getCantRemoveMessage(final Hub<T> thisHub, final T obj, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class<T> c = (Class<T>) obj.getClass();
			
			if (thisHub.getObjectClass() == null) {
				callHubDataSetObjectClass(thisHub, c);
			}
			if (!thisHub.getObjectClass().isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = callHubDetailGetDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}

		if (onlyCheckTypes != null) {
			OAObjectCallback eq = callObjectCallbackGetAllowRemoveObjectCallback(thisHub, obj, onlyCheckTypes);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.allowRemove is false, msg: " + s;
			}

			if (obj instanceof OAObject) {
				eq = callObjectCallbackGetVerifyRemoveObjectCallback(thisHub, obj, onlyCheckTypes);
				if (eq != null && !eq.getAllowed()) {
					String s = eq.getResponse();
					s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
					return "ObjectCallback.verifyRemove is false, msg: " + s;
				}
			}
		}

		if (thisHub.getSharedHub() != null) {
			return getCantRemoveMessage(thisHub.getSharedHub(), obj, onlyCheckTypes);
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
	public String getCantRemoveAllMessage(final Hub<?> thisHub, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = callHubDetailGetDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}
		
		if (onlyCheckTypes != null) {
			OAObjectCallback eq = callObjectCallbackGetAllowRemoveAllObjectCallback(thisHub, onlyCheckTypes);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.allowRemoveAll is false, msg: " + s;
			}
			eq = callObjectCallbackGetVerifyRemoveAllObjectCallback(thisHub, onlyCheckTypes);
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
				return "ObjectCallback.verifyRemoveAll is false, msg: " + s;
			}
		}

		Hub<?> hubx = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (hubx != null) {
			return getCantRemoveAllMessage(hubx, onlyCheckTypes);
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
	public void clear(final Hub<?> thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		if (thisHub == null) return;
		if (!callRemoteThreadIsRemoteThread() && bSendNewList) {
			OAObjectCallback eq = callObjectCallbackGetVerifyRemoveAllObjectCallback(thisHub, OAObjectCallback.getCallbackOnlyCheckType());
			if (eq != null && !eq.getAllowed()) {
				String s = eq.getResponse();
				if (OAString.isEmpty(s)) {
					s = "Cant clear, OAObjectCallback verifyRemoveAll retured false";
				}
				throw new RuntimeException(s);
			}

			eq = callObjectCallbackGetAllowRemoveAllObjectCallback(thisHub, OAObjectCallback.getCallbackOnlyCheckType());
			if (eq != null && !eq.getAllowed()) {
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
			callThreadLocalLock(thisHub);
			b = _clear(thisHub, bSetAOtoNull, bSendNewList);
		} finally {
			callThreadLocalUnlock(thisHub);
		}
		if (b) {
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
	private <T extends OAObject> boolean _clear(final Hub<T> thisHub, final boolean bSetAOtoNull, final boolean bSendNewList) {
		if (thisHub.getSharedHub() != null) {
			return _clear(thisHub.getSharedHub(), bSetAOtoNull, bSendNewList);
		}

		if (!thisHub.getEnabled()) {
			return false;
		}

		if (callRemoteThreadIsRemoteThread()) {
			callRemoteThreadSetStartedNextThread(true); // keep it from being started
		}
		
		if (bSetAOtoNull) {
			thisHub.setAO(null);
		}
		callHubSelectCancelSelect(thisHub, false);

		// 20140616 moved this here since other objects (ex: HubMerger) uses the
		//   to fire new events, etc.
		callHubEventFireBeforeRemoveAllEvent(thisHub);

		//int x = HubDataDelegate.getCurrentSize(thisHub);

		// 20120627 need to send event to clients if there is a masterObject
		boolean bSendEvent = faHub.getHubDataMaster(thisHub).getMasterObject() != null;

		if (bSendEvent) {
			callHubCSRemoveAllFromHub(thisHub);
		}

		// 20160615
		T[] objs = thisHub.toArray();
		faHub.getHubData(thisHub).getVector().removeAllElements();
		
		boolean bIsDeleting = callThreadLocalIsDeleting(thisHub);
		if (!bIsDeleting && (faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges())) {
			Vector<T> vecRemove = faHub.getHubData(thisHub).getVecRemove();
			final boolean bWasEmpty = vecRemove == null ? true : vecRemove.size() == 0;
			for (T obj : objs) {
				Vector<T> vx = faHub.getHubData(thisHub).getVecAdd();
				if (vx != null && vx.removeElement(obj)) {
					// no-op
				} else {
					if (vecRemove == null) {
						vecRemove = callHubDataCreateVecRemove(thisHub);
					}
					if (bWasEmpty || vecRemove.indexOf(obj) < 0) {
						vecRemove.addElement(obj);
					}
				}
			}
			boolean b = (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0)
					|| (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0);

			callHubStatusSetChanged(thisHub, b); 
		} else {
			callHubStatusSetChanged(thisHub, true);
		}

		// if this is OAClientThread, so that OAClientMessageHandler can continue with next message
		if (callRemoteThreadIsRemoteThread()) {
			callRemoteThreadSetStartedNextThread(false);
		}
		callRemoteThreadStartNextThread();

		// need to now have the object ref to hub removed.
		if (!bIsDeleting) {
			for (OAObject obj : objs) {
				remove(thisHub, obj, false, false, bIsDeleting, bSetAOtoNull, true, true);
			}
		}
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
			callHubShareSetSharedHubsAfterRemoveAll(thisHub);
		}

		if (bSendNewList) {
			callHubEventFireOnNewListEvent(thisHub, true);
		}
		callHubEventFireAfterRemoveAllEvent(thisHub);
	}

	/**
	 * Determines whether the specified object can be added to the hub.
	 * Delegates to {@link #canAddMsg(Hub, Object)}.
	 *
	 * @param thisHub the hub to evaluate
	 * @param obj     the object to test
	 * @return {@code true} if the object can be added, otherwise {@code false}
	 */
	public <T extends OAObject> boolean canAdd(final Hub<T> thisHub, final T obj) {
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
	public boolean canAdd(final Hub<?> thisHub) {
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
	public String canAddMsg(final Hub<?> thisHub) {
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
	@SuppressWarnings("unchecked")
	public <T extends OAObject> String canAddMsg(final Hub<T> thisHub, final T obj) {
		if (thisHub == null) {
			return "hub is null";
		}

		if (!thisHub.getEnabled()) {
			return "hub is disabled";
		}

		if (obj != null) {
			final Class<T> c = (Class<T>) obj.getClass();
			
			if (faHub.getHubData(thisHub).getObjClass() == null) {
				callHubDataSetObjectClass(thisHub, c);
			}
			if (!faHub.getHubData(thisHub).getObjClass().isAssignableFrom(c)) {
				return "class not assignable, class=" + c.getSimpleName();
			}
		}

		// if there is a masterHub, then make sure that this Hub is active/valid
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && thisHub.getCurrentSize() == 0) {
			HubDataMaster dm = callHubDetailGetDataMaster(thisHub, true);
			if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
				return "has masterHub, but masterObject is null";
			}
		}

		OAObjectCallback eq = callObjectCallbackGetAllowAddObjectCallback(thisHub, obj, OAObjectCallback.getCallbackOnlyCheckType());
		if (eq != null && !eq.getAllowed()) {
			String s = eq.getResponse();
			s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
			return "ObjectCallback.allowAdd is false, msg: " + s;
		}

		eq = callObjectCallbackGetVerifyAddObjectCallback(thisHub, obj, OAObjectCallback.getCallbackOnlyCheckType());
		if (eq != null && !eq.getAllowed()) {
			String s = eq.getResponse();
			s = OAString.concat(s, eq.getThrowable().getMessage(), ", ");
			return "ObjectCallback.verifyAdd is false, msg: " + s;
		}

		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			return canAddMsg(faHub.getHubDataUnique(thisHub).getSharedHub(), obj);
		}

		if (obj != null && (faHub.getHubData(thisHub).getUniqueProperty() != null || faHub.getHubDataMaster(thisHub).getUniqueProperty() != null)) {
			if (!callHubVerifyUniqueProperty(thisHub, obj)) {
				return "verifyUniqueProperty returned false for property " + faHub.getHubDataMaster(thisHub).getUniqueProperty();
			}
		}

		// 20140731 recursive hub check
		if (obj != null && callHubDetailIsRecursiveMasterDetail(thisHub)) {
			final Class<T> c = (Class<T>) obj.getClass();
			// cant add a recursive object to its children Hub
			// cant make a recursive object have one of its children as the parent

			// was:
			// OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(c);
			// OALinkInfo li = oi.getRecursiveLinkInfo(OALinkInfo.ONE);

			OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
			if (li != null) {
				Object master = callHubDetailGetMasterObject(thisHub);
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
    public <T extends OAObject> boolean add(final Hub<T> thisHub, final T obj) {
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
    public <T extends OAObject> boolean add(final Hub<T> thisHub, final T obj, final boolean bAlreadyCalledContains) {
		if (thisHub == null || obj == null) {
			return false;
		}
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			if (thisHub.getEnabled()) {
				return add(faHub.getHubDataUnique(thisHub).getSharedHub(), obj, bAlreadyCalledContains);
			}
		}

		final boolean bIsLoading = callThreadLocalIsLoading();
		if (!bIsLoading && faHub.getHubData(thisHub).getSortListener() != null) {
			// use getCurrentSize to "guess" that it will go at the end, in
			//  cases where this is loaded in order.
			return insert(thisHub, obj, thisHub.getCurrentSize());
		}

		if (!bIsLoading && !callRemoteThreadIsRemoteThread()) {
			String s = canAddMsg(thisHub, obj);
			if (s != null) {
				throw new RuntimeException(
						"Cant add object, can add returned false, hub=" + thisHub + ", add object=" + obj + ", Reason: " + s);
			}
		}

		boolean b = false;
		try {
			if (!bIsLoading) {
				callThreadLocalLock(thisHub);
			}
			b = _add(thisHub, obj, bIsLoading, bAlreadyCalledContains);
		} finally {
			if (!bIsLoading) {
				callThreadLocalUnlock(thisHub);
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
	private <T extends OAObject> boolean _add(final Hub<T> thisHub, final T obj, final boolean bIsLoading, final boolean bAlreadyCalledContains) {
		if (thisHub.getObjectClass() == null || thisHub.getObjectClass().equals(OAObject.class)) {
			Class<T> c = (Class<T>) obj.getClass();
			if (thisHub.getObjectClass() == null || !c.equals(OAObject.class)) {
				callHubDataSetObjectClass(thisHub, c);
			}
		}

		// need to check even if isLoading=true, since datasource could autoadd to a cache hub
		if (!bAlreadyCalledContains && thisHub.contains(obj)) {
			return false;
		}

		if (!bIsLoading) {
			callHubEventFireBeforeAddEvent(thisHub, obj, thisHub.getCurrentSize());
		}

		callHubCSAddToHub(thisHub, obj); // use OARuntime.threadService().setSuppressCSMessages(true) to not have add sent to other clients/server
		if (!internalAdd(thisHub, obj, true, false)) {
			//LOG.warning(" NOT ADDED <<<<<");
			return false;
		}

		if (obj instanceof OAObject) {
			if (callHubDataContains(thisHub, obj, true)) {
				// this code has been moved before the listeners are notified.  Else listeners could ask for more objects

				if (!bIsLoading) {
					if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
						OALinkInfo lix = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
						if (lix != null) {
							if (lix.getType() == OALinkInfo.ONE) {
								callHubDetailSetPropertyToMasterHub(thisHub, obj, faHub.getHubDataMaster(thisHub).getMasterObject());
							} else if (lix.getType() == OALinkInfo.MANY) {
								// 20210326 M2M
								Hub hubx = (Hub) lix.getValue(obj);
								if (hubx != null) {
									hubx.add(faHub.getHubDataMaster(thisHub).getMasterObject());
								}
							}
						}
					} else if (obj.isNew()) {
						Hub hubx = callHubSelectGetSelectWhereHub(thisHub);
						if (hubx != null) {
							Object objx = hubx.getAO();
							if (objx != null) {
								String ppx = callHubSelectGetSelectWhereHubPath(thisHub);
								OALinkInfo lix = hubx.getOAObjectInfo().getLinkInfo(ppx);
								if (lix != null) {
									lix = lix.getReverseLinkInfo();
									if (lix != null) {
										if (obj.getProperty(lix.getName()) == null) {
											obj.setProperty(lix.getName(), objx);
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
							OALinkInfo liRecursive = callObjectInfoGetRecursiveLinkInfo(thisHub.getOAObjectInfo(), OALinkInfo.ONE);
							if (liRecursive != null) {
								callObjectReflectSetProperty(obj, liRecursive.getName(), null, null);
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
	private <T extends OAObject> void _afterAdd(final Hub<T> thisHub, final T obj) {
		callHubEventFireAfterAddEvent(thisHub, obj, thisHub.getCurrentSize() - 1);
		
		if (!callThreadLocalIsLoading()) {
			callHubStatusSetReferenceable(thisHub, true);
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
	public <T extends OAObject>  boolean internalAdd(final Hub<T> thisHub, final T obj, final boolean bHasLock, final boolean bCheckContains) {
		if (obj == null) {
			return false;
		}

		// this will lock, sync(data), and startNextThread
		if (!callHubData_add(thisHub, obj, bHasLock, bCheckContains)) {
			return false;
		}

		if (obj instanceof OAObject) {
			callObjectHubAddHub(obj, thisHub);
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
	public void sortMove(final Hub<?> thisHub, final Object obj) {
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
	public <T extends OAObject> void move(final Hub<T> thisHub, final int posFrom, int posTo) {
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

		T objFrom = thisHub.elementAt(posFrom);
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
				T cobj = thisHub.elementAt(i);
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

		callHubEventFireBeforeMoveEvent(thisHub, posFrom, posTo);

		//  OAClient must send message to OAServer before continuing
		callHubCSMoveObjectInHub(thisHub, posFrom, posTo);

		// this will lock
		callHubData_move(thisHub, objFrom, posFrom, posTo);

		callHubEventFireAfterMoveEvent(thisHub, posFrom, posTo);
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
	public <T extends OAObject> boolean insert(final Hub<T> thisHub, final T obj, final int pos) {
		if (thisHub == null || obj == null) {
			return false;
		}
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			return insert(faHub.getHubDataUnique(thisHub).getSharedHub(), obj, pos);
		}

		if (!callThreadLocalIsLoading()) {
			if (!callRemoteThreadIsRemoteThread()) {
				String s = canAddMsg(thisHub, obj);
				if (s != null) {
					throw new RuntimeException(
							"Cant insert object, can add returned false, hub=" + thisHub + ", object=" + obj + ", Reason: " + s);
				}
			}
		}
		int newPos = pos;
		try {
			callThreadLocalLock(thisHub);
			newPos = _insert(thisHub, obj, pos);
		} finally {
			callThreadLocalUnlock(thisHub);
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
	private <T extends OAObject> int _insert(final Hub<T> thisHub, final T obj, int pos) {
		if (thisHub.getObjectClass() == null || thisHub.getObjectClass().equals(OAObject.class)) {
			Class<T> c = (Class<T>) obj.getClass();
			if (thisHub.getObjectClass() == null || !c.equals(OAObject.class)) {
				callHubDataSetObjectClass(thisHub, c);
			}
		}
		
		if (faHub.getHubData(thisHub).getSortListener() != null) {
		    // 20240118 need to make sure object is not already loaded
		    //    the Id could match, but the sort prop does not match
            if (thisHub.contains(obj)) {
                return -1;
            }
		    
			// 20170608 quicksort
			int head = -1;
			int tail = faHub.getHubData(thisHub).getVector().size();
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

				T cobj = thisHub.elementAt(i);
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

		callHubEventFireBeforeInsertEvent(thisHub, obj, pos);

		// send message to OAServer
		//  OAClient must send message to OAServer before continuing
		if (callHubCSInsertInHub(thisHub, obj, pos)) {
			if (thisHub.contains(obj)) {
				return -1; // already loaded (another thread)
			}
		}
		//was: 20140826 removed to make faster.  Another object could have the same objectId.  (should use contains instead of getObj)
		// if (HubDataDelegate.getObject(thisHub, key) != null) return false;

		// this will lock, sync(data), and startNextThread
		//was: boolean b = HubDataDelegate._insert(thisHub, key, obj, pos, false);  // false=dont lock, since this method is locked
		boolean b = callHubData_insert(thisHub, obj, pos, true);
		if (!b) {
			return -1;
		}

		/* 20140904 this is moved before setPropertyToMasterHub, so that
		 * hub.contains(obj) will return true.
		 */
		callObjectHubAddHub(obj, thisHub);

		// moved before listeners are notified.  Else listeners could ask for it.
		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getType() == OALinkInfo.ONE) {
				callHubDetailSetPropertyToMasterHub(thisHub, obj, faHub.getHubDataMaster(thisHub).getMasterObject());
			} else if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getType() == OALinkInfo.MANY) {
				// 20210326 M2M
				Hub hubx = (Hub) faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getValue(obj);
				if (hubx != null) {
					hubx.add(faHub.getHubDataMaster(thisHub).getMasterObject());
				}
			}
		} else if (obj instanceof OAObject && obj.isNew()) {
			// 20201212
			Hub hubx = callHubSelectGetSelectWhereHub(thisHub);
			if (hubx != null) {
				Object objx = hubx.getAO();
				if (objx != null) {
					String ppx = callHubSelectGetSelectWhereHubPath(thisHub);
					OALinkInfo lix = hubx.getOAObjectInfo().getLinkInfo(ppx);
					if (lix != null) {
						lix = lix.getReverseLinkInfo();
						if (lix != null) {
							if (obj.getProperty(lix.getName()) == null) {
								obj.setProperty(lix.getName(), objx);
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
				OALinkInfo liRecursive = callObjectInfoGetRecursiveLinkInfo(thisHub.getOAObjectInfo(), OALinkInfo.ONE);
				if (liRecursive != null) {
					callObjectReflectSetProperty(obj, liRecursive.getName(), null, null);
				}
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
	private <T extends OAObject> void _afterInsert(final Hub<T> thisHub, final T obj, final int pos) {
		callHubEventFireAfterInsertEvent(thisHub, obj, pos);
		
		if (!callThreadLocalIsLoading()) {
			callHubStatusSetReferenceable(thisHub, true);
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
	public void swap(final Hub<?> thisHub, int pos1, int pos2) {
		if (thisHub == null) return;
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
		OAObject obj1 = thisHub.elementAt(pos1);
		OAObject obj2 = thisHub.elementAt(pos2);

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
	public <T extends OAObject>  T[] getAddedObjects(Hub<T> thisHub) {
		if (thisHub == null) return null;
		return callHubDataGetAddedObjects(thisHub);
	}

	/**
	 * Retrieves the list of objects tracked as removed from the hub.
	 *
	 * @param thisHub the hub to inspect
	 * @return an array of removed {@link OAObject} instances
	 */
	public <T extends OAObject> T[] getRemovedObjects(Hub<T> thisHub) {
		if (thisHub == null) return null;
		return callHubDataGetRemovedObjects(thisHub);
	}

	/**
	 * Determines whether the hub permits duplicate add/remove operations based on
	 * its configuration.
	 *
	 * @param thisHub the hub to check
	 * @return {@code true} if duplicate add/remove operations are allowed
	 */
	public boolean isAllowAddRemove(Hub<?> thisHub) {
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
	public boolean isAllowRemove(Hub<?> thisHub) {
		if (thisHub == null) {
			return false;
		}
		if (!faHub.getHubData(thisHub).isDupAllowAddRemove()) {
			return false;
		}

		// see if fkeys is also pkey
		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(thisHub);
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
	public <T extends OAObject> void unsafeAddAll(Hub<T> hub, List<T> list) {
		if (hub == null || list == null) return;
		faHub.getHubData(hub).getVector().addAll(list);
	}

	/**
	 * Replaces all objects in the hub with those contained in another hub.
	 * Clears internal structures, removes old hub references, adds new objects,
	 * and fires a new-list event.
	 *
	 * @param hub    the hub being updated
	 * @param hubNew the hub providing the new objects
	 */
	public <T extends OAObject> void refresh(Hub<T> hub, Hub<T> hubNew) {
		if (hub == null) return;
		for (T objx : hub) {
			callObjectHubRemoveHub(objx, hub, false);
		}
		faHub.getHubData(hub).getVector().clear();
		
		faHub.getHubDataActive(hubNew).clear();

		for (T objx : hubNew) {
			faHub.getHubData(hub).getVector().add(objx);
			callObjectHubAddHub(objx, hub);
		}
		callHubEventFireOnNewListEvent(hub, true);
	}

	
	/**
	 * Updates link relationships for objects added to or removed from this hub.
	 * When objects are removed, the method determines whether the reverse link
	 * requires deletion, reference removal, or persistence based on the link type,
	 * master relationship, and cascade rules. Many-to-many links are updated when
	 * needed. New objects are skipped because they do not yet exist in the data
	 * source.
	 *
	 * @param thisHub       the hub whose add/remove state is being processed
	 * @param iCascadeRule  the cascade rule for save/delete operations
	 * @param cascade       the cascade tracker for preventing reprocessing
	 * @param bIsSaving     whether the caller is performing a save operation
	 */
	
		
	public void _updateHubAddsAndRemoves(final Hub thisHub, final int iCascadeRule, final OACascade cascade,
			final boolean bIsSaving) {
		//qqqqqqqq method was protected
		// removed Objects need to be saved if reference = null.
		HubDataMaster dm = callHubDetailGetDataMaster(thisHub);
		
		boolean bM2M = (dm != null && dm.getDetailToMasterLinkInfo() != null && dm.getDetailToMasterLinkInfo().getType() == OALinkInfo.MANY);
		OALinkInfo liRev = null;
		if (dm != null && dm.getDetailToMasterLinkInfo() != null) {
			liRev = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
		}

		boolean bHasMethod = true;
		if (dm == null) {
		} else if (bM2M) {
			bHasMethod = false;
			if (dm.getMasterObject() != null && dm.getDetailToMasterLinkInfo() != null) {
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		} else {
			// 20120907 cases where there is not a public method created, and would use a link table.
			Method method = callObjectInfoGetMethod(dm.getDetailToMasterLinkInfo());
			if (method == null || ((method.getModifiers() & (Modifier.PRIVATE)) != 0)) {
				bHasMethod = false;
				updateMany2ManyLinks(thisHub, dm); // update any link tables
			}
		}

		Object[] objs = callHubDataGetRemovedObjects(thisHub);
		if (objs == null) {
			return;
		}

		for (int i = 0; i < objs.length; i++) {
			OAObject obj = (OAObject) objs[i];
			if (obj.getNew()) {
				continue; // does not exist in DS
			}
			if (liRev != null && liRev.isOwner()) {
				if (dm.getDetailToMasterLinkInfo() != null) {
					Object ox = callObjectReflectGetProperty(obj, dm.getDetailToMasterLinkInfo().getName());
					if (ox == null) {
						callObjectDeleteDelete(obj, cascade);
					}
				}
			} else if (dm != null && dm.getDetailToMasterLinkInfo() != null && bHasMethod) {
				Object ox = callObjectReflectGetProperty(obj, dm.getDetailToMasterLinkInfo().getName());
				if (ox == null) { // else property has been reassigned
					// 20120925
					callObjectDSRemoveReference(obj, dm.getDetailToMasterLinkInfo());
					//was: OAObjectSaveDelegate._saveObjectOnly(obj, cascade);
				}
			} else if (bIsSaving && dm != null && dm.getDetailToMasterLinkInfo() != null && !bHasMethod && !callSyncIsClient() && !obj.isDeleted()) {
				// 20181126 if it is a removed object from ServerRoot, need to save now
				callObjectSaveSave(obj, iCascadeRule, cascade);
			}
		}
	}

	
	
	
	/**
	 * Synchronizes many-to-many link table entries for this hub. Added and removed
	 * objects are examined and cross-updated on the opposite hub. When changes
	 * occur, the link table is updated using the master object's reverse link
	 * property.
	 *
	 * @param thisHub the hub whose many-to-many links are being updated
	 * @param dm      the master relationship information for this hub
	 */
	private void updateMany2ManyLinks(Hub thisHub, HubDataMaster dm) {
		if (dm == null || dm.getDetailToMasterLinkInfo() == null) {
			return;
		}
		OAObject[] adds = callHubAddRemoveGetAddedObjects(thisHub);
		OAObject[] removes = callHubAddRemoveGetRemovedObjects(thisHub);

		boolean b = false;
		// cross update opposite hub vecAdd/Remove
		for (int i = 0; adds != null && i < adds.length; i++) {
			b = true;
			if (adds[i] == null) continue;
			OAObject obj = adds[i];
			if (obj.getNew()) continue;
			Object objx = callObjectReflectGetRawReference(obj, dm.getDetailToMasterLinkInfo().getName());
			if (objx instanceof Hub) {
				callHubDataRemoveFromAddedList((Hub) objx, dm.getMasterObject());
			}
		}
		for (int i = 0; removes != null && i < removes.length; i++) {
			b = true;
			if (removes[i] == null) continue;
			OAObject obj = (OAObject) removes[i];
			Object objx = callObjectReflectGetRawReference(obj, dm.getDetailToMasterLinkInfo().getName());
			if (objx instanceof Hub) {
				callHubDataRemoveFromRemovedList((Hub) objx, dm.getMasterObject());
			}
		}
		if (b) {
			String propFromMaster = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo()).getName();
			callHubDSUpdateMany2ManyLinks(dm.getMasterObject(), adds, removes, propFromMaster);
		}
	}
	
	
	// ObjectCallback
	/**
	 * Dependency hook used by this service for ObjectCallbackGetAllowRemoveObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param objRemove method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowRemoveObjectCallback(final Hub<T> hub, final T objRemove, final OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Dependency hook used by this service for ObjectCallbackGetVerifyRemoveObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param objRemove method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(final Hub<T> hub, final T objRemove, final OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Dependency hook used by this service for ObjectCallbackGetAllowRemoveAllObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract OAObjectCallback callObjectCallbackGetAllowRemoveAllObjectCallback(final Hub<?> hub, final OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Dependency hook used by this service for ObjectCallbackGetVerifyRemoveAllObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(final Hub<?> hub, final OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Dependency hook used by this service for ObjectCallbackGetAllowAddObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param objAdd method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetAllowAddObjectCallback(final Hub<T> hub, T objAdd, final OAObjectCallback.CheckType[] onlyCheckTypes);
	/**
	 * Dependency hook used by this service for ObjectCallbackGetVerifyAddObjectCallback behavior.
	 *
	 * @param hub method input
	 * @param oaObj method input
	 * @param onlyCheckTypes method input
	 * @return result value
	 */
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(final Hub<T> hub, final T oaObj, final OAObjectCallback.CheckType[] onlyCheckTypes);
	
	// ObjectDelete
	/**
	 * Dependency hook used by this service for ObjectDeleteDelete behavior.
	 *
	 * @param oaObj method input
	 * @param cascade method input
	 */
	public abstract void callObjectDeleteDelete(final OAObject oaObj, OACascade cascade);

	// ObjectDS
	/**
	 * Dependency hook used by this service for ObjectDSRemoveReference behavior.
	 *
	 * @param oaObj method input
	 * @param li method input
	 */
	public abstract void callObjectDSRemoveReference(OAObject oaObj, OALinkInfo li);
	
	// ObjectHub
	/**
	 * Dependency hook used by this service for ObjectHubAddHub behavior.
	 *
	 * @param oaObj method input
	 * @param hub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub);
	/**
	 * Dependency hook used by this service for ObjectHubRemoveHub behavior.
	 *
	 * @param oaObj method input
	 * @param hub method input
	 * @param bIsOnHubFinalize method input
	 */
	public abstract <T extends OAObject> void callObjectHubRemoveHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize);

	// ObjectInfo
	/**
	 * Dependency hook used by this service for ObjectInfoGetReverseLinkInfo behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for ObjectInfoGetRecursiveLinkInfo behavior.
	 *
	 * @param thisOI method input
	 * @param type method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type);
	/**
	 * Dependency hook used by this service for ObjectInfoGetMethod behavior.
	 *
	 * @param li method input
	 * @return result value
	 */
	public abstract Method callObjectInfoGetMethod(OALinkInfo li);
	/**
	 * Dependency hook used by this service for ObjectInfoGetMethod behavior.
	 *
	 * @param clazz method input
	 * @param methodName method input
	 * @return result value
	 */
	public abstract Method callObjectInfoGetMethod(Class<?> clazz, String methodName);
	
	// ObjectReflect
	/**
	 * Dependency hook used by this service for ObjectReflectSetProperty behavior.
	 *
	 * @param oaObj method input
	 * @param propName method input
	 * @param value method input
	 * @param fmt method input
	 */
	public abstract void callObjectReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);
	/**
	 * Dependency hook used by this service for ObjectReflectGetProperty behavior.
	 *
	 * @param oaObj method input
	 * @param propPath method input
	 * @return result value
	 */
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String propPath);
	/**
	 * Dependency hook used by this service for ObjectReflectGetRawReference behavior.
	 *
	 * @param oaObj method input
	 * @param name method input
	 * @return result value
	 */
	public abstract Object callObjectReflectGetRawReference(OAObject oaObj, String name);

	// ObjectSave
	/**
	 * Dependency hook used by this service for ObjectSaveSave behavior.
	 *
	 * @param oaObj method input
	 * @param iCascadeRule method input
	 * @param cascade method input
	 */
	public abstract void callObjectSaveSave(OAObject oaObj, int iCascadeRule, OACascade cascade);

	// HubAddRemove
	/**
	 * Dependency hook used by this service for HubAddRemoveGetAddedObjects behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject>  T[] callHubAddRemoveGetAddedObjects(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubAddRemoveGetRemovedObjects behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T[] callHubAddRemoveGetRemovedObjects(Hub<T> thisHub);
	
	// HubCS
	/**
	 * Dependency hook used by this service for HubCSRemoveFromHub behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubCSRemoveFromHub(Hub<T> thisHub, T obj, int pos);
	/**
	 * Dependency hook used by this service for HubCSRemoveAllFromHub behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubCSRemoveAllFromHub(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubCSAddToHub behavior.
	 *
	 * @param thisHub method input
	 * @param thisObj method input
	 */
	public abstract <T extends OAObject> void callHubCSAddToHub(final Hub<T> thisHub, final T thisObj);
	/**
	 * Dependency hook used by this service for HubCSMoveObjectInHub behavior.
	 *
	 * @param thisHub method input
	 * @param posFrom method input
	 * @param posTo method input
	 */
	public abstract void callHubCSMoveObjectInHub(Hub<?> thisHub, int posFrom, int posTo);
	/**
	 * Dependency hook used by this service for HubCSInsertInHub behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubCSInsertInHub(Hub<T> thisHub, T obj, int pos);
	
	// HubData
	/**
	 * Dependency hook used by this service for HubDataGetObjectAt behavior.
	 *
	 * @param thisHub method input
	 * @param pos method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos);
	/**
	 * Dependency hook used by this service for HubDataGetPos behavior.
	 *
	 * @param thisHub method input
	 * @param object method input
	 * @param adjustMaster method input
	 * @param bUpdateLink method input
	 * @return result value
	 */
	public abstract <T extends OAObject> int callHubDataGetPos(final Hub<T> thisHub, T object, final boolean adjustMaster, final boolean bUpdateLink);
	/**
	 * Dependency hook used by this service for HubData_remove behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param bDeleting method input
	 * @param bIsRemovingAll method input
	 * @return result value
	 */
	public abstract <T extends OAObject> int callHubData_remove(Hub<T> thisHub, T obj, boolean bDeleting, boolean bIsRemovingAll);
	/**
	 * Dependency hook used by this service for HubDataSetObjectClass behavior.
	 *
	 * @param thisHub method input
	 * @param objClass method input
	 */
	public abstract <T extends OAObject> void callHubDataSetObjectClass(Hub<T> thisHub, Class<T> objClass);
	/**
	 * Dependency hook used by this service for HubDataCreateVecRemove behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> Vector<T> callHubDataCreateVecRemove(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubDataContains behavior.
	 *
	 * @param hub method input
	 * @param obj method input
	 * @param bJustAdded method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubDataContains(Hub<T> hub, T obj, final boolean bJustAdded);
	/**
	 * Dependency hook used by this service for HubData_add behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param bHasLock method input
	 * @param bCheckContains method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubData_add(Hub<T> thisHub, T obj, boolean bHasLock, boolean bCheckContains);
	/**
	 * Dependency hook used by this service for HubData_move behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param posFrom method input
	 * @param posTo method input
	 */
	public abstract <T extends OAObject> void callHubData_move(Hub<T> thisHub, T obj, int posFrom, int posTo);
	/**
	 * Dependency hook used by this service for HubData_insert behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 * @param bIsLocked method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubData_insert(Hub<T> thisHub, T obj, int pos, boolean bIsLocked);
	/**
	 * Dependency hook used by this service for HubDataGetAddedObjects behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T[] callHubDataGetAddedObjects(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubDataGetRemovedObjects behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T[] callHubDataGetRemovedObjects(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubDataRemoveFromAddedList behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 */
	public abstract <T extends OAObject> void callHubDataRemoveFromAddedList(Hub<T> thisHub, T obj);
	/**
	 * Dependency hook used by this service for HubDataRemoveFromRemovedList behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 */
	public abstract <T extends OAObject> void callHubDataRemoveFromRemovedList(Hub<T> thisHub, T obj);

	// HubDetail
	/**
	 * Dependency hook used by this service for HubDetailGetDataMaster behavior.
	 *
	 * @param thisHub method input
	 * @param bIncludedFilteredHub method input
	 * @return result value
	 */
	public abstract HubDataMaster callHubDetailGetDataMaster(final Hub<?> thisHub, boolean bIncludedFilteredHub);
	/**
	 * Dependency hook used by this service for HubDetailGetDataMaster behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract HubDataMaster callHubDetailGetDataMaster(final Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDetailSetPropertyToMasterHub behavior.
	 *
	 * @param thisHub method input
	 * @param detailObject method input
	 * @param objMaster method input
	 */
	public abstract <T extends OAObject> void callHubDetailSetPropertyToMasterHub(Hub<T> thisHub, T detailObject, OAObject objMaster);
	/**
	 * Dependency hook used by this service for HubDetailIsRecursiveMasterDetail behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract boolean callHubDetailIsRecursiveMasterDetail(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDetailGetMasterObject behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract OAObject callHubDetailGetMasterObject(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDetailGetLinkInfoFromDetailToMaster behavior.
	 *
	 * @param hub method input
	 * @return result value
	 */
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	
	// HubDS
	/**
	 * Dependency hook used by this service for HubDSUpdateMany2ManyLinks behavior.
	 *
	 * @param masterObject method input
	 * @param adds method input
	 * @param removes method input
	 * @param propFromMaster method input
	 */
	public abstract void callHubDSUpdateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster);
	
	// HubEvent
	/**
	 * Dependency hook used by this service for HubEventFireBeforeRemoveEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireBeforeRemoveEvent(Hub<T> thisHub, T obj, int pos);
	/**
	 * Dependency hook used by this service for HubEventFireAfterRemoveEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireAfterRemoveEvent(Hub<T> thisHub, final T obj, int pos);
	/**
	 * Dependency hook used by this service for HubEventFireBeforeRemoveAllEvent behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubEventFireBeforeRemoveAllEvent(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubEventFireOnNewListEvent behavior.
	 *
	 * @param thisHub method input
	 * @param bAll method input
	 */
	public abstract void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll);
	/**
	 * Dependency hook used by this service for HubEventFireAfterRemoveAllEvent behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubEventFireAfterRemoveAllEvent(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubEventFireBeforeAddEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireBeforeAddEvent(Hub<T> thisHub, T obj, int pos);
	/**
	 * Dependency hook used by this service for HubEventFireAfterAddEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireAfterAddEvent(Hub<T> thisHub, final T obj, int pos);
	/**
	 * Dependency hook used by this service for HubEventFireBeforeMoveEvent behavior.
	 *
	 * @param thisHub method input
	 * @param fromPos method input
	 * @param toPos method input
	 */
	public abstract void callHubEventFireBeforeMoveEvent(Hub<?> thisHub, int fromPos, int toPos);
	/**
	 * Dependency hook used by this service for HubEventFireAfterMoveEvent behavior.
	 *
	 * @param thisHub method input
	 * @param fromPos method input
	 * @param toPos method input
	 */
	public abstract void callHubEventFireAfterMoveEvent(Hub<?> thisHub, int fromPos, int toPos);
	/**
	 * Dependency hook used by this service for HubEventFireBeforeInsertEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireBeforeInsertEvent(Hub<T> thisHub, T obj, int pos);
	/**
	 * Dependency hook used by this service for HubEventFireAfterInsertEvent behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param pos method input
	 */
	public abstract <T extends OAObject> void callHubEventFireAfterInsertEvent(Hub<T> thisHub, final T obj, int pos);

	// HubFind
	/**
	 * Dependency hook used by this service for HubFindGetRealObject behavior.
	 *
	 * @param hub method input
	 * @param object method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callHubFindGetRealObject(Hub<T> hub, Object object); //qqqqqqq Object can be T, OAObjectKey, pkey value
	
	// HubShare
	/**
	 * Dependency hook used by this service for HubShareSetSharedHubsAfterRemove behavior.
	 *
	 * @param thisHub method input
	 * @param objRemoved method input
	 * @param posRemoved method input
	 */
	public abstract <T extends OAObject> void callHubShareSetSharedHubsAfterRemove(Hub<T> thisHub, T objRemoved, int posRemoved);
	/**
	 * Dependency hook used by this service for HubShareSetSharedHubsAfterRemoveAll behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubShareSetSharedHubsAfterRemoveAll(Hub<?> thisHub);
	
	// HubStatus
	/**
	 * Dependency hook used by this service for HubStatusSetReferenceable behavior.
	 *
	 * @param hub method input
	 * @param bReferenceable method input
	 */
	public abstract void callHubStatusSetReferenceable(Hub<?> hub, boolean bReferenceable);
	/**
	 * Dependency hook used by this service for HubStatusSetChanged behavior.
	 *
	 * @param thisHub method input
	 * @param bChanged method input
	 */
	public abstract void callHubStatusSetChanged(Hub<?> thisHub, boolean bChanged);
	
	// HubSelect
	/**
	 * Dependency hook used by this service for HubSelectCancelSelect behavior.
	 *
	 * @param thisHub method input
	 * @param bRemoveSelect method input
	 */
	public abstract void callHubSelectCancelSelect(Hub<?> thisHub, boolean bRemoveSelect);
	/**
	 * Dependency hook used by this service for HubSelectGetSelectWhereHub behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> Hub<T> callHubSelectGetSelectWhereHub(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubSelectGetSelectWhereHubPath behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract String callHubSelectGetSelectWhereHubPath(Hub<?> thisHub);
	
	// HubVerify
	/**
	 * Dependency hook used by this service for HubVerifyUniqueProperty behavior.
	 *
	 * @param thisHub method input
	 * @param object method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubVerifyUniqueProperty(final Hub<T> thisHub, final T object);

	// ThreadLocal
	/**
	 * Dependency hook used by this service for ThreadLocalIsDeleting behavior.
	 *
	 * @param obj method input
	 * @return result value
	 */
	public abstract boolean callThreadLocalIsDeleting(Object obj);
	/**
	 * Dependency hook used by this service for ThreadLocalIsLoading behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service for ThreadLocalLock behavior.
	 *
	 * @param object method input
	 */
	public abstract void callThreadLocalLock(Object object);
	/**
	 * Dependency hook used by this service for ThreadLocalUnlock behavior.
	 *
	 * @param object method input
	 */
	public abstract void callThreadLocalUnlock(Object object);
	
	// RemoteThread
	/**
	 * Dependency hook used by this service for RemoteThreadIsRemoteThread behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callRemoteThreadIsRemoteThread();
	/**
	 * Dependency hook used by this service for RemoteThreadStartNextThread behavior.
	 */
	public abstract void callRemoteThreadStartNextThread();
	/**
	 * Dependency hook used by this service for RemoteThreadSetStartedNextThread behavior.
	 *
	 * @param b method input
	 */
	public abstract void callRemoteThreadSetStartedNextThread(boolean b);
	
	// Sync
	/**
	 * Dependency hook used by this service for SyncIsClient behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callSyncIsClient();
}
