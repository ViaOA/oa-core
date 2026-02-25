package com.viaoa.graph.service.hub;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.*;

public abstract class HubEventService {
	private final Logger LOG = Logger.getLogger(HubEventService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubEventService(Hub.FriendAccess faHub ) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;

	
		for (int i = 0; i < maxCacheGetAllListeners; i++) {
			cacheGetAllListeners[i] = new CacheGetAllListeners();
		}
	}

	
	// 20120827 might be used later, if we need to have hub changes notify masterobject
	/**
	 * Placeholder for future support to notify that a Hub's master object
	 * has changed. Currently unused and contains no implementation.
	 *
	 * @param thisHub       the Hub whose master object would be reported
	 * @param bRefreshFlag  whether the change is associated with a refresh
	 */
	public void fireMasterObjectChangeEvent(Hub<?> thisHub, boolean bRefreshFlag) {
		// srvcObject.getOAObjectHubService().fireMasterObjectHubChangeEvent(thisHub, bRefreshFlag);
	}

	/**
	 * Fires a before-remove event for the specified object and position.
	 * Verifies removal through {@code OAObjectCallbackDelegate} and then
	 * notifies all registered listeners via their {@code beforeRemove} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being removed
	 * @param pos     the position of the object within the Hub
	 */
	public <T extends OAObject> void fireBeforeRemoveEvent(Hub<T> thisHub, T obj, int pos) {
		// verify with objectCallback
		if (!callRemoteThreadIsRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = callObjectCallbackGetVerifyRemoveObjectCallback(	thisHub, obj, OAObjectCallback.CHECK_CallbackMethod);
				if (!em.getAllowed()) {
					String s = em.getResponse();
					if (OAString.isEmpty(s)) {
						s = "edit query returned false for remove, Hub=" + thisHub;
					}
					throw new RuntimeException(s, em.getThrowable());
				}
			}
		}

		// call listeners
		HubListener<T>[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent<T> hubEvent = new HubEvent<T>(thisHub, obj, pos);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeRemove(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-remove event for the specified object and position.
	 * Supports queued delivery for remote threads and triggers referential
	 * updates and OAObject triggers when appropriate.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the removed object
	 * @param pos     the position the object occupied
	 */
	public <T extends OAObject> void fireAfterRemoveEvent(Hub<T> thisHub, final T obj, int pos) {
		if (callThreadLocalIsLoading()) {
			return;
		}

		final HubListener<T>[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent<T> hubEvent = new HubEvent<T>(thisHub, obj, pos);
			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterRemove(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterRemove(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			callObjectCacheFireAfterRemoveEvent(thisHub, obj);
		}
		//callObjectCacheFireAfterRemoveEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		if (obj instanceof OAObject && !((OAObject) obj).isLoading()) {
			OAObject objx = faHub.getHubDataMaster(thisHub).getMasterObject();
			if (objx != null) {
				String s = callHubDetailGetPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = callObjectInfoGetObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						try {
							callThreadLocalAddHubEvent(hubEvent);
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				}
			}
		}
	}

	/**
	 * Fires a before-remove-all event for the Hub. Verifies permission via
	 * {@code OAObjectCallbackDelegate} and notifies listeners through their
	 * {@code beforeRemoveAll} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public void fireBeforeRemoveAllEvent(Hub<?> thisHub) {
		// verify with objectCallback
		if (!callRemoteThreadIsRemoteThread()) {
			OAObjectCallback em = callObjectCallbackGetVerifyRemoveAllObjectCallback(thisHub, OAObjectCallback.CHECK_CallbackMethod);
			if (!em.getAllowed()) {
				String s = em.getResponse();
				if (OAString.isEmpty(s)) {
					s = "edit query returned false for removeAll, Hub=" + thisHub;
				}
				throw new RuntimeException(s, em.getThrowable());
			}
		}

		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeRemoveAll(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-remove-all event for the Hub. Supports queued delivery,
	 * notifies listeners through {@code afterRemoveAll}, and triggers master
	 * object onChange processing when applicable.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public void fireAfterRemoveAllEvent(Hub<?> thisHub) {
		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub);
			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterRemoveAll(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterRemoveAll(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}
		//fireMasterObjectChangeEvent(thisHub, true);

		// 20160304 created
		// 20220124 might be a filtered hub
		final OAObject objx = faHub.getHubDataMaster(thisHub).getMasterObject();
		// was: final OAObject objx = thisHub.getMasterObject();
		if (objx != null) {
			final String s = callHubDetailGetPropertyFromMasterToDetail(thisHub);
			if (s != null) {
				OAObjectInfo oi = callObjectInfoGetObjectInfo(objx.getClass());
				if (oi.getHasTriggers()) {
					final HubEvent hubEvent = new HubEvent(thisHub);
					try {
						callThreadLocalAddHubEvent(hubEvent);
						oi.onChange(objx, s, hubEvent);
					} finally {
						callThreadLocalRemoveHubEvent(hubEvent);
					}
				}
			}
		}
	}

	/**
	 * Fires a before-add event for an object being added to the Hub. Verifies
	 * the addition through {@code OAObjectCallbackDelegate} and notifies all
	 * listeners via {@code beforeAdd}.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being added
	 * @param pos     the position at which the object will be added
	 */
	public <T extends OAObject> void fireBeforeAddEvent(Hub<T> thisHub, T obj, int pos) {
		// verify with objectCallback
		if (!callRemoteThreadIsRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = callObjectCallbackGetVerifyAddObjectCallback(	thisHub, obj,
																							OAObjectCallback.CHECK_CallbackMethod);
				if (!em.getAllowed()) {
					String s = em.getResponse();
					if (OAString.isEmpty(s)) {
						s = "edit query returned false for add, Hub=" + thisHub;
					}
					throw new RuntimeException(s, em.getThrowable());
				}
			}
		}

		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeAdd(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-add event for an object added to the Hub. Supports queued
	 * event dispatch, triggers OAObject cache notifications, and processes
	 * master-object triggers when applicable.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the added object
	 * @param pos     the position of the added object
	 */
	public <T extends OAObject> void fireAfterAddEvent(Hub<T> thisHub, final T obj, int pos) {
		if (callThreadLocalIsLoading()) {
			return;
		}

		final HubListener<T>[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent<T> hubEvent = new HubEvent(thisHub, obj, pos);
			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterAdd(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterAdd(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			callObjectCacheFireAfterAddEvent((Hub<OAObject>) thisHub, (OAObject) obj);
		}
		//callObjectCacheFireAfterAddEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		// 20160304
		if (obj instanceof OAObject) {
			OAObject objx = faHub.getHubDataMaster(thisHub).getMasterObject();
			if (objx != null) {
				String s = callHubDetailGetPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = callObjectInfoGetObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						callThreadLocalAddHubEvent(hubEvent);
						try {
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				}
			}
		}
	}

	/**
	 * Fires a before-insert event for an object being inserted into the Hub.
	 * Performs callback verification and notifies listeners via
	 * {@code beforeInsert}.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being inserted
	 * @param pos     the target insertion position
	 */
	public <T extends OAObject> void fireBeforeInsertEvent(Hub<T> thisHub, T obj, int pos) {
		// verify with objectCallback
		if (!callRemoteThreadIsRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = callObjectCallbackGetVerifyAddObjectCallback(	thisHub, obj, OAObjectCallback.CHECK_CallbackMethod);
				if (!em.getAllowed()) {
					String s = em.getResponse();
					if (OAString.isEmpty(s)) {
						s = "edit query returned false for add/insert, Hub=" + thisHub;
					}
					throw new RuntimeException(s, em.getThrowable());
				}
			}
		}

		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeInsert(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-insert event for an object inserted into the Hub.
	 * Supports queued event dispatch, updates OAObject caches, and triggers
	 * master-object change processing when appropriate.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the inserted object
	 * @param pos     the position of the inserted object
	 */
	public <T extends OAObject> void fireAfterInsertEvent(Hub<T> thisHub, final T obj, int pos) {
		if (callThreadLocalIsLoading()) {
			return;
		}

		final HubListener<T>[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent<T> hubEvent = new HubEvent<T>(thisHub, obj, pos);
			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterInsert(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterInsert(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			callObjectCacheFireAfterAddEvent((Hub<OAObject>) thisHub, (OAObject) obj);
		}

		//srvcObject.getOAObjectCacheService().fireAfterInsertEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		if (obj instanceof OAObject) {
			OAObject objx = faHub.getHubDataMaster(thisHub).getMasterObject();
			if (objx != null) {
				String s = callHubDetailGetPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = callObjectInfoGetObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						try {
							callThreadLocalAddHubEvent(hubEvent);
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				}
			}
		}
	}

	/**
	 * Fires an after-change-active-object event for the Hub. Notifies all
	 * applicable listeners and propagates any exceptions encountered.
	 *
	 * @param thisHub    the Hub generating the event
	 * @param obj        the new active object
	 * @param pos        the position associated with the change
	 * @param bAllShared whether shared Hubs should also receive the event
	 */
	public <T extends OAObject> void fireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared) {
		HubListener[] hl = getAllListeners(thisHub, bAllShared ? 1 : 3);
		int x = hl.length;
		if (x > 0) {
			Exception exception = null;
			final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			callThreadLocalAddHubEvent(hubEvent);
			for (int i = 0; i < x; i++) {
				try {
					hl[i].afterChangeActiveObject(hubEvent);
				} catch (Exception e) {
					if (e != null) {
						exception = e;
					}
				}
			}
			callThreadLocalRemoveHubEvent(hubEvent);
			if (exception != null) {
				throw new RuntimeException("Exception while calling fireAfterChangeActiveObjectEvent", exception);
			}
		}
	}

	/**
	 * Fires a before-refresh event for the Hub. Notifies all registered
	 * listeners via their {@code beforeRefresh} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public void fireBeforeRefreshEvent(Hub<?> thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			callThreadLocalAddHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].beforeRefresh(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires a before-select event for the Hub. Notifies all registered
	 * listeners via their {@code beforeSelect} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public void fireBeforeSelectEvent(Hub<?> thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			callThreadLocalAddHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].beforeSelect(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-sort event for the Hub. Notifies all registered listeners
	 * via their {@code afterSort} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public void fireAfterSortEvent(Hub<?> thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			callThreadLocalAddHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].afterSort(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
		//fireMasterObjectChangeEvent(thisHub, false);
	}

	/**
	 * Fires a before-delete event for the specified object. Notifies listeners
	 * via their {@code beforeDelete} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being deleted
	 */
	public <T extends OAObject> void fireBeforeDeleteEvent(Hub<T> thisHub, T obj) {
		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeDelete(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-delete event for the specified object. Supports queued
	 * dispatch when remote threading is active and notifies listeners via
	 * their {@code afterDelete} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the deleted object
	 */
	public <T extends OAObject> void fireAfterDeleteEvent(Hub<T> thisHub, T obj) {
		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, obj);

			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterDelete(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterDelete(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}
		//fireMasterObjectChangeEvent(thisHub, false);
	}

	/**
	 * Fires a before-save event for the specified object. Notifies listeners
	 * via their {@code beforeSave} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object being saved
	 */
	public void fireBeforeSaveEvent(Hub<?> thisHub, OAObject obj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].beforeSave(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-save event for the specified object. Notifies all
	 * registered listeners via their {@code afterSave} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param obj     the object that was saved
	 */
	public void fireAfterSaveEvent(Hub<?> thisHub, OAObject obj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			callThreadLocalAddHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].afterSave(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires a before-move event for an object being repositioned within the Hub.
	 *
	 * @param thisHub the Hub generating the event
	 * @param fromPos the original position
	 * @param toPos   the destination position
	 */
	public void fireBeforeMoveEvent(Hub<?> thisHub, int fromPos, int toPos) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, fromPos, toPos);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].beforeMove(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-move event for an object repositioned within the Hub.
	 * Notifies listeners via their {@code afterMove} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param fromPos the original position
	 * @param toPos   the new position
	 */
	public void fireAfterMoveEvent(Hub<?> thisHub, int fromPos, int toPos) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, fromPos, toPos);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterMove(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
		//fireMasterObjectChangeEvent(thisHub, false);
	}

	/**
	 * Fires a calculated-property-change event for the given object and
	 * property. Updates affected detail Hubs when the property corresponds
	 * to a link and notifies all listeners via {@code afterPropertyChange}.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param object       the object whose property changed
	 * @param propertyName the name of the property that changed
	 */
	public <T extends OAObject> void fireCalcPropertyChange(Hub<T> thisHub, final T object, final String propertyName) {
		// 20210506 could be used by link
		if (object instanceof OAObject) {
			// 20180304
			if (callThreadLocalHasSentCalcPropertyChange(thisHub, object, propertyName)) {
				return;
			}
			
			OAObjectInfo oi = callObjectInfoGetObjectInfo((OAObject) object);
			OALinkInfo linkInfo = callObjectInfoGetLinkInfo(oi, propertyName);
			if (linkInfo != null) {
				propertyChangeUpdateDetailHubs(thisHub, object, propertyName);
			}
		}

		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, object, propertyName, null, null);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterPropertyChange(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}

	}

	/**
	 * Fires a before-property-change event for the given object. Notifies all
	 * listeners via their {@code beforePropertyChange} method.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param oaObj        the object whose property is changing
	 * @param propertyName the name of the property
	 * @param oldValue     the previous value
	 * @param newValue     the new value
	 */
	public void fireBeforePropertyChange(Hub<?> thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, oaObj, propertyName, oldValue, newValue);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforePropertyChange(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-property-change event for the given object and property.
	 * Updates detail Hubs when the property corresponds to a link, validates
	 * unique-property constraints, and notifies all listeners via their
	 * {@code afterPropertyChange} method. Supports queued dispatch when
	 * remote-thread queuing is enabled.
	 *
	 * @param thisHub      the Hub generating the event
	 * @param oaObj        the object whose property changed
	 * @param propertyName the name of the changed property
	 * @param oldValue     the previous value
	 * @param newValue     the new value
	 * @param linkInfo     link metadata for the property, or null
	 */
	public <T extends OAObject> void fireAfterPropertyChange(final Hub<T> thisHub, final T oaObj, final String propertyName, final Object oldValue,
			final Object newValue, final OALinkInfo linkInfo) {
		// 2007/01/03 need to call propertyChangeDupChain() first, since propertyChange
		//            could need to change a detail hub(s), before a HubLinkEventListener is called, which
		//            could have needed the detail hubs to be changed.
		if (thisHub == null) {
			return;
		}
		if (linkInfo != null) {
			propertyChangeUpdateDetailHubs(thisHub, oaObj, propertyName);
		}

		if (!callRemoteThreadIsRemoteThread()) {
			String s = faHub.getHubData(thisHub).getUniqueProperty();
			if (s == null) {
				s = faHub.getHubDataMaster(thisHub).getUniqueProperty();
			}

			if (s != null && newValue != null && s.equalsIgnoreCase(propertyName)) {
				if (!callHubVerifyUniqueProperty(thisHub, oaObj)) {
					throw new RuntimeException("Property " + s + " already exists in " + oaObj.getClass().getSimpleName());
				}
			}
		}

		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, oaObj, propertyName, oldValue, newValue);

			if (callRemoteThreadShouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							callThreadLocalAddHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterPropertyChange(hubEvent);
							}
						} finally {
							callThreadLocalRemoveHubEvent(hubEvent);
						}
					}
				};
				callRemoteThreadQueueEvent(r);
			} else {
				try {
					callThreadLocalAddHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterPropertyChange(hubEvent);
					}
				} finally {
					callThreadLocalRemoveHubEvent(hubEvent);
				}
			}
		}

		/* 20160827 removed, since it is done when obj is changed, or when a Hub has a add/insert/remove
		// 20160110
		if (linkInfo != null && oaObj != null && !oaObj.isLoading() && OASync.isServer()) {
		    srvcHub.setReferenceable(thisHub, true);
		}
		*/
	}

	/**
	 * Updates detail Hubs whose master-to-detail link corresponds to the
	 * specified property change. Recursively follows shared Hubs to ensure
	 * all dependent detail Hubs are updated.
	 *
	 * @param thisHub      the Hub whose detail Hubs may be affected
	 * @param object       the object whose property changed
	 * @param propertyName the name of the changed property
	 */
	private <T extends OAObject> void propertyChangeUpdateDetailHubs(Hub<T> thisHub, T object, String propertyName) {
		int i, x;

		if (object == faHub.getHubDataActive(thisHub).getActiveObject()) {
			x = faHub.getHubDataUnique(thisHub).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(thisHub).getVecHubDetail().size();
			for (i = 0; i < x; i++) {
				HubDetail detail = (HubDetail) (faHub.getHubDataUnique(thisHub).getVecHubDetail().elementAt(i));

				Hub dHub = detail.getHubDetail();
				if (dHub != null && detail.getMasterToDetailLinkInfo() != null && detail.getMasterToDetailLinkInfo().getName().equalsIgnoreCase(propertyName)) {
					callHubDetailUpdateDetail(thisHub, detail, dHub, false); // ex: from activeObject.setDept(dept), dont updateLinkProperty
				}
			}
		}

		WeakReference<Hub<T>>[] refs = callHubShareGetSharedWeakHubs(thisHub);
		for (i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub<T>> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}
			propertyChangeUpdateDetailHubs(h2, object, propertyName);
		}
	}

	/**
	 * Fires new-list and after-new-list events for the Hub to notify listeners
	 * that a new collection has been established. Also increments the Hub’s
	 * change count.
	 *
	 * @param thisHub the Hub generating the event
	 * @param bAll    whether to notify all listeners or a subset
	 */
	public void fireOnNewListEvent(Hub<?> thisHub, boolean bAll) {
		if (thisHub == null) {
			return;
		}
		HubListener[] hl = getAllListeners(thisHub, (bAll ? 0 : 2));
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, null);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].onNewList(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}

			hubEvent = new HubEvent(thisHub, null);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterNewList(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
		// 20160118 use this instead of newListCount
		callHubDataIncChangeCount(thisHub);
		//was:  thisHub.data.setNewListCount(thisHub.data.getNewListCount()+1);
	}

	/**
	 * Returns the {@link HubListenerTree} associated with the Hub, creating
	 * it if necessary.
	 *
	 * @param thisHub the Hub whose listener tree is requested
	 * @return the HubListenerTree instance, or null if Hub is null
	 */
	private HubListenerTree getHubListenerTree(Hub<?> thisHub) {
		if (thisHub == null) {
			return null;
		}
		if (faHub.getHubDataUnique(thisHub).getListenerTree() == null) {
			synchronized (faHub.getHubDataUnique(thisHub)) {
				if (faHub.getHubDataUnique(thisHub).getListenerTree() == null) {
					faHub.getHubDataUnique(thisHub).setListenerTree(new HubListenerTree(thisHub));
				}
			}
		}
		return faHub.getHubDataUnique(thisHub).getListenerTree();
	}

	/**
	 * Registers a HubListener for a specific property name with optional
	 * dependent property paths. Clears the listener-cache afterward.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, String property, String[] dependentPropertyPaths) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, false);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a HubListener for a property with optional dependent paths
	 * and an option to receive events only for the active object. Clears the
	 * listener cache after registration.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 * @param bActiveObjectOnly       true to notify only for active-object events
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a HubListener for a property with options for dependent
	 * paths, active-object filtering, and background-thread execution.
	 * Clears the listener cache after registration.
	 *
	 * @param thisHub                 the Hub to attach the listener to
	 * @param hl                      the listener to add
	 * @param property                the property name
	 * @param dependentPropertyPaths  dependent properties to monitor
	 * @param bActiveObjectOnly       true for active-object-only events
	 * @param bUseBackgroundThread    true to dispatch events in a background thread
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a HubListener for a specific property. Clears the listener
	 * cache after registration.
	 *
	 * @param thisHub the Hub to attach the listener to
	 * @param hl      the listener to add
	 * @param property the property name
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, String property) {
		getHubListenerTree(thisHub).addListener(hl, property);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a listener for a property with an option to receive only
	 * active-object-related events. Clears the listener cache afterward.
	 *
	 * @param thisHub           the Hub to attach the listener to
	 * @param hl                the listener to add
	 * @param property          the property name
	 * @param bActiveObjectOnly true to restrict events to active-object changes
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, String property, boolean bActiveObjectOnly) {
		getHubListenerTree(thisHub).addListener(hl, property, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a listener for all properties with an option to restrict
	 * events to active-object changes. Clears the listener cache afterward.
	 *
	 * @param thisHub           the Hub to attach the listener to
	 * @param hl                the listener to add
	 * @param bActiveObjectOnly true to restrict notifications
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl, boolean bActiveObjectOnly) {
		getHubListenerTree(thisHub).addListener(hl, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Registers a listener to receive all Hub and OAObject events. Clears
	 * the listener cache afterward.
	 *
	 * @param thisHub the Hub to attach the listener to
	 * @param hl      the listener to add
	 */
	public void addHubListener(Hub<?> thisHub, HubListener hl) {
		getHubListenerTree(thisHub).addListener(hl);
		clearGetAllListenerCache(thisHub);
	}

	public int TotalHubListeners;

	/**
	 * Removes a HubListener from the Hub’s listener tree if present. Clears
	 * the listener cache afterward.
	 *
	 * @param thisHub the Hub to remove the listener from
	 * @param l       the listener to remove
	 */
	public void removeHubListener(Hub<?> thisHub, HubListener l) {
		if (thisHub == null || l == null) {
			return;
		}
		if (faHub.getHubDataUnique(thisHub).getListenerTree() == null) {
			return;
		}
		faHub.getHubDataUnique(thisHub).getListenerTree().removeListener(l);
		//was: thisHub.datau.getListenerTree().removeListener(thisHub, l);
		clearGetAllListenerCache(thisHub);
	}

	private final HubListener[] hlEmpty = new HubListener[0];

	/**
	 * Returns all listeners registered directly on this Hub (not including
	 * shared or duplicate Hubs). Returns an empty array when none exist.
	 *
	 * @param thisHub the Hub whose direct listeners are requested
	 * @return an array of HubListeners
	 */
	public HubListener[] getHubListeners(Hub<?> thisHub) {
		if (faHub.getHubDataUnique(thisHub).getListenerTree() == null) {
			return hlEmpty;
		}
		HubListener[] hl = faHub.getHubDataUnique(thisHub).getListenerTree().getHubListeners();
		if (hl == null) {
			hl = hlEmpty;
		}
		return hl;
	}

	/**
	 * Returns the count of listeners registered for this Hub, including
	 * shared and duplicate Hubs.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the total listener count
	 */
	public int getListenerCount(Hub<?> thisHub) {
		return getAllListeners(thisHub).length;
	}

	/**
	 * Returns all listeners for this Hub, delegating to the type-0
	 * variant of {@code getAllListeners(Hub,int)}.
	 *
	 * @param thisHub the Hub to inspect
	 * @return all listeners associated with the Hub
	 */
	public <T extends OAObject> HubListener<T>[] getAllListeners(Hub<T> thisHub) {
		return getAllListeners(thisHub, 0);
	}

	// 20160606 cache for getAllListeners
	static final int maxCacheGetAllListeners = 12;

	private static class CacheGetAllListeners {
		Hub<?> hub;
		HubListener[] hl;
	}

	private final ReentrantReadWriteLock rwCacheGetAllListeners = new ReentrantReadWriteLock();
	private final CacheGetAllListeners[] cacheGetAllListeners = new CacheGetAllListeners[maxCacheGetAllListeners];
	private final AtomicInteger aiGetAllListeners = new AtomicInteger();

	/**
	 * Returns listeners for the Hub according to the lookup type, which
	 * controls whether shared or duplicate Hubs are included. Uses a small
	 * cache for type-0 lookups.
	 *
	 * @param thisHub the Hub to inspect
	 * @param type    lookup type selector
	 * @return an array of listeners matching the lookup criteria
	 */
	public HubListener[] getAllListeners(final Hub<?> thisHub, int type) {
		if (thisHub == null) {
			return new HubListener[0];
		}
		/* 0: get all
		   1: get all that are duplicates (dataa == dataa)
		   2: get all that are shared with this hub only
		   3: get all that are duplicates (dataa == dataa), dont go to beginning
		*/
		// 20160606
		if (type == 0) {
			try {
				rwCacheGetAllListeners.readLock().lock();
				for (int i = 0; i < maxCacheGetAllListeners; i++) {
					CacheGetAllListeners cl = cacheGetAllListeners[i];
					HubListener[] hl = cl.hl;
					if (cl.hub == thisHub) {
						return hl;
					}
				}
			} finally {
				rwCacheGetAllListeners.readLock().unlock();
			}
		}

		Hub h = thisHub;

		// go to beginning of shared hub chain
		if (type < 2 && type != 3) {
			for (; faHub.getHubDataUnique(h).getSharedHub() != null;) {
				h = faHub.getHubDataUnique(h).getSharedHub();
			}
		}
		if (type == 3) {
			type = 1;
		}
		HubListener[] hl = getAllListenersRecursive(h, thisHub, type);

		if (type == 0) {
			try {
				rwCacheGetAllListeners.writeLock().lock();
				CacheGetAllListeners cl = cacheGetAllListeners[aiGetAllListeners.getAndIncrement() % maxCacheGetAllListeners];
				cl.hub = thisHub;
				cl.hl = hl;
			} finally {
				rwCacheGetAllListeners.writeLock().unlock();
			}
		}

		return hl;
	}

	/**
	 * Clears cached results used for {@code getAllListeners}. Removes cache
	 * entries for the specified Hub or all entries matching its object class.
	 *
	 * @param hub the Hub whose cache entries should be invalidated
	 */
	public void clearGetAllListenerCache(Hub<?> hub) {
		try {
			rwCacheGetAllListeners.writeLock().lock();
			for (int i = 0; i < maxCacheGetAllListeners; i++) {
				Hub h = cacheGetAllListeners[i].hub;
				if (h == null) {
					continue;
				}

				if (hub == null || hub == h) {
					cacheGetAllListeners[i].hub = null;
					cacheGetAllListeners[i].hl = null;
					continue;
				}
				Class c = hub.getObjectClass();
				Class c2 = h.getObjectClass();

				if (c == null || c2 == null || c.equals(c2)) {
					cacheGetAllListeners[i].hub = null;
					cacheGetAllListeners[i].hl = null;
				}
			}
		} finally {
			rwCacheGetAllListeners.writeLock().unlock();
		}
	}

	/**
	 * Recursively collects listeners from this Hub and appropriate shared or
	 * duplicate Hubs based on the lookup type.
	 *
	 * @param thisHub the Hub whose listeners are being collected
	 * @param hub     the reference Hub used for comparison
	 * @param type    lookup type selector
	 * @return an array of collected listeners
	 */
	public <T extends OAObject> HubListener<T>[] getAllListenersRecursive(Hub<T> thisHub, Hub<T> hub, int type) {
		ArrayList<HubListener<T>> al = _getAllListenersRecursive(thisHub, null, hub, type, false, false);

		HubListener<T>[] hl = new HubListener[al == null ? 0 : al.size()];
		if (al != null) {
			al.toArray(hl);
		}
		return hl;
	}

	/**
	 * Internal implementation for recursively collecting listeners. Handles
	 * insertion-location ordering and traversal of shared Hubs.
	 *
	 * @param thisHub         the Hub being inspected
	 * @param al              the accumulating list of listeners
	 * @param hub             the reference Hub
	 * @param type            lookup type selector
	 * @param bHasLastChecked internal state flag for ordering
	 * @param bHasLast        internal state flag for ordering
	 * @return the updated listener list
	 */
	private <T extends OAObject> ArrayList<HubListener<T>> _getAllListenersRecursive(Hub<T> thisHub, ArrayList<HubListener<T>> al, Hub<T> hub, int type,
			boolean bHasLastChecked, boolean bHasLast) {
		if (type == 0 || type == 2 || faHub.getHubDataActive(thisHub) == faHub.getHubDataActive(hub)) {
			HubListener[] hls = getHubListeners(thisHub);
			if (hls != null && hls.length > 0) {
				int x;
				if (al == null) {
					al = new ArrayList<HubListener<T>>(Math.max(hls.length * 2, 10));
					x = 0;
				} else {
					x = al.size();
				}

				for (int i = 0; i < hls.length; i++) {
					HubListener.InsertLocation loc = hls[i].getLocation();

					if (loc == HubListener.InsertLocation.LAST) {
						bHasLastChecked = bHasLast = true;
						al.add(hls[i]);
					} else if (x == 0) {
						bHasLastChecked = true;
						bHasLast = false;
						al.add(hls[i]);
					} else if (loc == HubListener.InsertLocation.FIRST) {
						al.add(0, hls[i]);
					} else if (bHasLastChecked && !bHasLast) {
						al.add(hls[i]);
					} else {
						// insert before any listeners that have location=LAST
						boolean bDone = false;
						for (int j = x - 1; j >= 0; j--) {
							HubListener hl2 = (HubListener) al.get(j);
							if (hl2.getLocation() == HubListener.InsertLocation.LAST) {
								bHasLast = true;
							} else {
								if (!bHasLast) {
									al.add(hls[i]);
								} else {
									al.add(j, hls[i]);
								}
								bDone = true;
								break;
							}
						}
						if (!bDone) {
							al.add(0, hls[i]); // all were last, need to add to front
						}
						bHasLastChecked = true;
					}
					x++;
				}
			}
		}

		WeakReference<Hub<T>>[] refs = callHubShareGetSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub<T>> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}
			al = _getAllListenersRecursive(h2, al, hub, type, bHasLastChecked, bHasLast);
		}
		return al;
	}

	/**
	 * Fires an after-load event for an OAObject added to the Hub's data.
	 * Notifies listeners via their {@code afterLoad} method.
	 *
	 * @param thisHub the Hub generating the event
	 * @param oaObj   the loaded object
	 */
	public void fireAfterLoadEvent(Hub<?> thisHub, OAObject oaObj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		int i;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, oaObj);
			try {
				callThreadLocalAddHubEvent(hubEvent);
				for (i = 0; i < x; i++) {
					hl[i].afterLoad(hubEvent);
				}
			} finally {
				callThreadLocalRemoveHubEvent(hubEvent);
			}
		}
	}

	/**
	 * qqqqq these are in Hub public boolean canAdd(Hub<?> thisHub) { return canAdd(thisHub, null); } public boolean canAdd(Hub
	 * thisHub, OAObject obj) { if (obj == null) return srvcObject.getOAObjectCallbackService().getAllowAdd(thisHub); return
	 * srvcObject.getOAObjectCallbackService().getVerifyAdd(thisHub, obj); } public boolean canRemove(Hub<?> thisHub) { return canRemove(thisHub,
	 * null); } public boolean canRemove(Hub<?> thisHub, OAObject obj) { if (obj == null) return
	 * srvcObject.getOAObjectCallbackService().getAllowRemove(thisHub); return srvcObject.getOAObjectCallbackService().getVerifyRemove(thisHub, obj); } public static
	 * boolean canRemoveAll(Hub<?> thisHub) { return srvcObject.getOAObjectCallbackService().getAllowRemoveAll(thisHub); }
	 */

	// @OAParentProvided (example = "srvcObject.getOAObjectCallbackService().getVerifyRemoveObjectCallback")
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyRemoveObjectCallback(final Hub<T> hub, final T objRemove, final int checkType);

	// @OAParentProvided (example = "srvcObject.getOAObjectCacheService().fireAfterRemoveEvent")
	public abstract <T extends OAObject> void callObjectCacheFireAfterRemoveEvent(Hub<T> hub, T obj);

	// @OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz);
	
	// @OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj);

	// @OAParentProvided (example = "srvcObject.getOAObjectCallbackService().getVerifyRemoveAllObjectCallback")
	public abstract OAObjectCallback callObjectCallbackGetVerifyRemoveAllObjectCallback(final Hub<?> hub, final int checkType);

	// @OAParentProvided (example = "srvcObject.getOAObjectCacheService().fireAfterAddEvent")
	public abstract <T extends OAObject> void callObjectCacheFireAfterAddEvent(Hub<T> hub, T obj);

	// @OAParentProvided (example = "srvcObject.getOAObjectCallbackService().getVerifyAddObjectCallback")
	public abstract <T extends OAObject> OAObjectCallback callObjectCallbackGetVerifyAddObjectCallback(final Hub<T> hub, final T oaObj, final int checkType);
	
	// @OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo")
	public abstract OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName);

	// @OAParentProvided (example = "srvcHub.getHubDetailService().getPropertyFromMasterToDetail")
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub);

	// @OAParentProvided (example = "srvcHub.verifyUniqueProperty")
	public abstract <T extends OAObject> boolean callHubVerifyUniqueProperty(final Hub<T> thisHub, final T object);

	// @OAParentProvided (example = "srvcHub.getHubDetailService().updateDetail")
	public abstract void callHubDetailUpdateDetail(final Hub<?> thisHub, final HubDetail detail, final Hub<?> detailHub, final boolean bUpdateLink);

	// @OAParentProvided (example = "srvcHub.getHubShareService().getSharedWeakHubs")
	public abstract <T extends OAObject> WeakReference<Hub<T>>[] callHubShareGetSharedWeakHubs(Hub<T> thisHub);

	// @OAParentProvided (example = "srvcHub.getHubDataService().incChangeCount")
	public abstract void callHubDataIncChangeCount(Hub<?> thisHub);

	// @OAParentProvided (example = "srvcRemoteThread.isRemoteThread")
	public abstract boolean callRemoteThreadIsRemoteThread();

	// @OAParentProvided (example = "srvcThreadLocal.addHubEvent")
	public abstract void callThreadLocalAddHubEvent(HubEvent he);

	// @OAParentProvided (example = "srvcThreadLocal.removeHubEvent")
	public abstract void callThreadLocalRemoveHubEvent(HubEvent<?> he);

	// @OAParentProvided (example = "srvcThreadLocal.isLoading")
	public abstract boolean callThreadLocalIsLoading();		

	// @OAParentProvided (example = "srvcRemoteThread.shouldEventsBeQueued")
	public abstract boolean callRemoteThreadShouldEventsBeQueued();

	// @OAParentProvided (example = "srvcRemoteThread.queueEvent")
	public abstract boolean callRemoteThreadQueueEvent(Runnable r);

	// @OAParentProvided (example = "srvcThreadLocal.hasSentCalcPropertyChange")
	public abstract <T extends OAObject> boolean callThreadLocalHasSentCalcPropertyChange(Hub<T> thisHub, T thisObj, String propertyName);
	
}

