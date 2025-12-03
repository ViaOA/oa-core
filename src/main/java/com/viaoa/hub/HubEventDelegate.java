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
package com.viaoa.hub;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAString;

/**
 * Creates and dispatches {@link HubEvent}s for structural and selection
 * operations, routing them through registered {@link HubListener}s.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Construct and fire before/after events for add, remove, clear, and AO changes.</li>
 *   <li>Coordinate verification callbacks using {@code OAObjectCallbackDelegate}.</li>
 *   <li>Support queued dispatch for remote or asynchronous event contexts.</li>
 *   <li>Trigger OAObject triggers and maintain referential updates for master/detail links.</li>
 * </ul>
 *
 * <p>Ensures event ordering and prevents reentrancy issues by maintaining
 * per-thread event stacks in {@code OAThreadLocalDelegate}.
 */
public class HubEventDelegate {

	// 20120827 might be used later, if we need to have hub changes notify masterobject
	/**
	 * Placeholder for future support to notify that a Hub's master object
	 * has changed. Currently unused and contains no implementation.
	 *
	 * @param thisHub       the Hub whose master object would be reported
	 * @param bRefreshFlag  whether the change is associated with a refresh
	 */
	protected static void fireMasterObjectChangeEvent(Hub thisHub, boolean bRefreshFlag) {
		// OAObjectHubDelegate.fireMasterObjectHubChangeEvent(thisHub, bRefreshFlag);
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
	public static void fireBeforeRemoveEvent(Hub thisHub, Object obj, int pos) {
		// verify with objectCallback
		if (!OARemoteThreadDelegate.isRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = OAObjectCallbackDelegate.getVerifyRemoveObjectCallback(	thisHub, (OAObject) obj,
																								OAObjectCallback.CHECK_CallbackMethod);
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
		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeRemove(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static <T> void fireAfterRemoveEvent(Hub<T> thisHub, final T obj, int pos) {
		if (OAThreadLocalDelegate.isLoading()) {
			return;
		}

		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterRemove(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterRemove(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			OAObjectCacheDelegate.fireAfterRemoveEvent( (Hub<OAObject>) thisHub, (OAObject) obj);
		}
		//OAObjectCacheDelegate.fireAfterRemoveEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		if (obj instanceof OAObject && !((OAObject) obj).isLoading()) {
			OAObject objx = thisHub.datam.getMasterObject();
			if (objx != null) {
				String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeRemoveAllEvent(Hub thisHub) {
		// verify with objectCallback
		if (!OARemoteThreadDelegate.isRemoteThread()) {
			OAObjectCallback em = OAObjectCallbackDelegate.getVerifyRemoveAllObjectCallback(thisHub, OAObjectCallback.CHECK_CallbackMethod);
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
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeRemoveAll(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterRemoveAllEvent(Hub thisHub) {
		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub);
			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterRemoveAll(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterRemoveAll(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
				}
			}
		}
		//fireMasterObjectChangeEvent(thisHub, true);

		// 20160304 created
		// 20220124 might be a filtered hub
		final OAObject objx = thisHub.datam.getMasterObject();
		// was: final OAObject objx = thisHub.getMasterObject();
		if (objx != null) {
			final String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
			if (s != null) {
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objx.getClass());
				if (oi.getHasTriggers()) {
					final HubEvent hubEvent = new HubEvent(thisHub);
					try {
						OAThreadLocalDelegate.addHubEvent(hubEvent);
						oi.onChange(objx, s, hubEvent);
					} finally {
						OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeAddEvent(Hub thisHub, Object obj, int pos) {
		// verify with objectCallback
		if (!OARemoteThreadDelegate.isRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = OAObjectCallbackDelegate.getVerifyAddObjectCallback(	thisHub, (OAObject) obj,
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
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeAdd(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static <T> void fireAfterAddEvent(Hub<T> thisHub, final T obj, int pos) {
		if (OAThreadLocalDelegate.isLoading()) {
			return;
		}

		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterAdd(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterAdd(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			OAObjectCacheDelegate.fireAfterAddEvent((Hub<OAObject>) thisHub, (OAObject) obj);
		}
		//OAObjectCacheDelegate.fireAfterAddEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		// 20160304
		if (obj instanceof OAObject) {
			OAObject objx = thisHub.datam.getMasterObject();
			if (objx != null) {
				String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						OAThreadLocalDelegate.addHubEvent(hubEvent);
						try {
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeInsertEvent(Hub thisHub, Object obj, int pos) {
		// verify with objectCallback
		if (!OARemoteThreadDelegate.isRemoteThread()) {
			if (obj instanceof OAObject) {
				OAObjectCallback em = OAObjectCallbackDelegate.getVerifyAddObjectCallback(	thisHub, (OAObject) obj,
																							OAObjectCallback.CHECK_CallbackMethod);
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
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeInsert(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static <T> void fireAfterInsertEvent(Hub<T> thisHub, final T obj, int pos) {
		if (OAThreadLocalDelegate.isLoading()) {
			return;
		}

		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterInsert(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterInsert(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
				}
			}
		}
		if (obj instanceof OAObject) {
			OAObjectCacheDelegate.fireAfterAddEvent((Hub<OAObject>) thisHub, (OAObject) obj);
		}

		//OAObjectCacheDelegate.fireAfterInsertEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		if (obj instanceof OAObject) {
			OAObject objx = thisHub.datam.getMasterObject();
			if (objx != null) {
				String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
				if (s != null) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objx.getClass());
					if (oi.getHasTriggers()) {
						HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							oi.onChange(thisHub.getMasterObject(), s, hubEvent);
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared) {
		HubListener[] hl = getAllListeners(thisHub, bAllShared ? 1 : 3);
		int x = hl.length;
		if (x > 0) {
			Exception exception = null;
			final HubEvent hubEvent = new HubEvent(thisHub, obj, pos);
			OAThreadLocalDelegate.addHubEvent(hubEvent);
			for (int i = 0; i < x; i++) {
				try {
					hl[i].afterChangeActiveObject(hubEvent);
				} catch (Exception e) {
					if (e != null) {
						exception = e;
					}
				}
			}
			OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeRefreshEvent(Hub thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			OAThreadLocalDelegate.addHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].beforeRefresh(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires a before-select event for the Hub. Notifies all registered
	 * listeners via their {@code beforeSelect} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireBeforeSelectEvent(Hub thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			OAThreadLocalDelegate.addHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].beforeSelect(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
			}
		}
	}

	/**
	 * Fires an after-sort event for the Hub. Notifies all registered listeners
	 * via their {@code afterSort} method.
	 *
	 * @param thisHub the Hub generating the event
	 */
	public static void fireAfterSortEvent(Hub thisHub) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub);
			OAThreadLocalDelegate.addHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].afterSort(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeDeleteEvent(Hub thisHub, Object obj) {
		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforeDelete(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterDeleteEvent(Hub thisHub, Object obj) {
		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, obj);

			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterDelete(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterDelete(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeSaveEvent(Hub thisHub, OAObject obj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].beforeSave(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterSaveEvent(Hub thisHub, OAObject obj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, obj);
			OAThreadLocalDelegate.addHubEvent(hubEvent);
			try {
				for (int i = 0; i < x; i++) {
					hl[i].afterSave(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforeMoveEvent(Hub thisHub, int fromPos, int toPos) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, fromPos, toPos);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].beforeMove(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterMoveEvent(Hub thisHub, int fromPos, int toPos) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, fromPos, toPos);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterMove(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireCalcPropertyChange(Hub thisHub, final Object object, final String propertyName) {
		// 20180304
		if (OAThreadLocalDelegate.hasSentCalcPropertyChange(thisHub, (OAObject) object, propertyName)) {
			return;
		}

		// 20210506 could be used by link
		if (object instanceof OAObject) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo((OAObject) object);
			OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
			if (linkInfo != null) {
				propertyChangeUpdateDetailHubs(thisHub, (OAObject) object, propertyName);
			}
		}

		HubListener[] hl = HubEventDelegate.getAllListeners(thisHub);
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, object, propertyName, null, null);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterPropertyChange(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireBeforePropertyChange(Hub thisHub, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
		HubListener[] hls = getAllListeners(thisHub);
		int x = hls.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, oaObj, propertyName, oldValue, newValue);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hls[i].beforePropertyChange(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
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
	public static void fireAfterPropertyChange(final Hub thisHub, final OAObject oaObj, final String propertyName, final Object oldValue,
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

		if (!OASync.isRemoteThread()) {
			String s = thisHub.data.getUniqueProperty();
			if (s == null) {
				s = thisHub.datam.getUniqueProperty();
			}

			if (s != null && newValue != null && s.equalsIgnoreCase(propertyName)) {
				if (!HubDelegate.verifyUniqueProperty(thisHub, oaObj)) {
					throw new RuntimeException("Property " + s + " already exists in " + oaObj.getClass().getSimpleName());
				}
			}
		}

		final HubListener[] hl = getAllListeners(thisHub);
		final int x = hl.length;
		if (x > 0) {
			final HubEvent hubEvent = new HubEvent(thisHub, oaObj, propertyName, oldValue, newValue);

			if (OARemoteThreadDelegate.shouldEventsBeQueued()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							OAThreadLocalDelegate.addHubEvent(hubEvent);
							for (int i = 0; i < x; i++) {
								hl[i].afterPropertyChange(hubEvent);
							}
						} finally {
							OAThreadLocalDelegate.removeHubEvent(hubEvent);
						}
					}
				};
				OARemoteThreadDelegate.queueEvent(r);
			} else {
				try {
					OAThreadLocalDelegate.addHubEvent(hubEvent);
					for (int i = 0; i < x; i++) {
						hl[i].afterPropertyChange(hubEvent);
					}
				} finally {
					OAThreadLocalDelegate.removeHubEvent(hubEvent);
				}
			}
		}

		/* 20160827 removed, since it is done when obj is changed, or when a Hub has a add/insert/remove
		// 20160110
		if (linkInfo != null && oaObj != null && !oaObj.isLoading() && OASync.isServer()) {
		    HubDelegate.setReferenceable(thisHub, true);
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
	private static void propertyChangeUpdateDetailHubs(Hub thisHub, OAObject object, String propertyName) {
		int i, x;

		if (object == thisHub.dataa.activeObject) {
			x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();
			for (i = 0; i < x; i++) {
				HubDetail detail = (HubDetail) (thisHub.datau.getVecHubDetail().elementAt(i));

				Hub dHub = detail.hubDetail;
				if (dHub != null && detail.liMasterToDetail != null && detail.liMasterToDetail.getName().equalsIgnoreCase(propertyName)) {
					HubDetailDelegate.updateDetail(thisHub, detail, dHub, false); // ex: from activeObject.setDept(dept), dont updateLinkProperty
				}
			}
		}

		WeakReference<Hub>[] refs = HubShareDelegate.getSharedWeakHubs(thisHub);
		for (i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
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
	public static void fireOnNewListEvent(Hub thisHub, boolean bAll) {
		if (thisHub == null) {
			return;
		}
		HubListener[] hl = getAllListeners(thisHub, (bAll ? 0 : 2));
		int x = hl.length;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, null);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].onNewList(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
			}

			hubEvent = new HubEvent(thisHub, null);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (int i = 0; i < x; i++) {
					hl[i].afterNewList(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
			}
		}
		// 20160118 use this instead of newListCount
		HubDataDelegate.incChangeCount(thisHub);
		//was:  thisHub.data.setNewListCount(thisHub.data.getNewListCount()+1);
	}

	/**
	 * Returns the {@link HubListenerTree} associated with the Hub, creating
	 * it if necessary.
	 *
	 * @param thisHub the Hub whose listener tree is requested
	 * @return the HubListenerTree instance, or null if Hub is null
	 */
	private static HubListenerTree getHubListenerTree(Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		if (thisHub.datau.getListenerTree() == null) {
			synchronized (thisHub.datau) {
				if (thisHub.datau.getListenerTree() == null) {
					thisHub.datau.setListenerTree(new HubListenerTree(thisHub));
				}
			}
		}
		return thisHub.datau.getListenerTree();
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
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths) {
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
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
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
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
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
	public static void addHubListener(Hub thisHub, HubListener hl, String property) {
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
	public static void addHubListener(Hub thisHub, HubListener hl, String property, boolean bActiveObjectOnly) {
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
	public static void addHubListener(Hub thisHub, HubListener hl, boolean bActiveObjectOnly) {
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
	public static void addHubListener(Hub thisHub, HubListener hl) {
		getHubListenerTree(thisHub).addListener(hl);
		clearGetAllListenerCache(thisHub);
	}

	public static int TotalHubListeners;

	/**
	 * Removes a HubListener from the Hub’s listener tree if present. Clears
	 * the listener cache afterward.
	 *
	 * @param thisHub the Hub to remove the listener from
	 * @param l       the listener to remove
	 */
	protected static void removeHubListener(Hub thisHub, HubListener l) {
		if (thisHub == null || l == null) {
			return;
		}
		if (thisHub.datau.getListenerTree() == null) {
			return;
		}
		thisHub.datau.getListenerTree().removeListener(l);
		//was: thisHub.datau.getListenerTree().removeListener(thisHub, l);
		clearGetAllListenerCache(thisHub);
	}

	private final static HubListener[] hlEmpty = new HubListener[0];

	/**
	 * Returns all listeners registered directly on this Hub (not including
	 * shared or duplicate Hubs). Returns an empty array when none exist.
	 *
	 * @param thisHub the Hub whose direct listeners are requested
	 * @return an array of HubListeners
	 */
	protected static HubListener[] getHubListeners(Hub thisHub) {
		if (thisHub.datau.getListenerTree() == null) {
			return hlEmpty;
		}
		HubListener[] hl = thisHub.datau.getListenerTree().getHubListeners();
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
	public static int getListenerCount(Hub thisHub) {
		return getAllListeners(thisHub).length;
	}

	/**
	 * Returns all listeners for this Hub, delegating to the type-0
	 * variant of {@code getAllListeners(Hub,int)}.
	 *
	 * @param thisHub the Hub to inspect
	 * @return all listeners associated with the Hub
	 */
	public static HubListener[] getAllListeners(Hub thisHub) {
		return getAllListeners(thisHub, 0);
	}

	// 20160606 cache for getAllListeners
	static final int maxCacheGetAllListeners = 12;

	private static class CacheGetAllListeners {
		Hub hub;
		HubListener[] hl;
	}

	private static final ReentrantReadWriteLock rwCacheGetAllListeners = new ReentrantReadWriteLock();
	private static final CacheGetAllListeners[] cacheGetAllListeners = new CacheGetAllListeners[maxCacheGetAllListeners];

	static {
		for (int i = 0; i < maxCacheGetAllListeners; i++) {
			cacheGetAllListeners[i] = new CacheGetAllListeners();
		}
	}
	private static final AtomicInteger aiGetAllListeners = new AtomicInteger();

	/**
	 * Returns listeners for the Hub according to the lookup type, which
	 * controls whether shared or duplicate Hubs are included. Uses a small
	 * cache for type-0 lookups.
	 *
	 * @param thisHub the Hub to inspect
	 * @param type    lookup type selector
	 * @return an array of listeners matching the lookup criteria
	 */
	protected static HubListener[] getAllListeners(final Hub thisHub, int type) {
		if (thisHub == null) {
			return null;
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
		if (type < 2) {
			for (; h.datau.getSharedHub() != null;) {
				h = h.datau.getSharedHub();
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
	public static void clearGetAllListenerCache(Hub hub) {
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
	protected static HubListener[] getAllListenersRecursive(Hub thisHub, Hub hub, int type) {
		ArrayList<HubListener> al = _getAllListenersRecursive(thisHub, null, hub, type, false, false);

		HubListener[] hl = new HubListener[al == null ? 0 : al.size()];
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
	private static ArrayList<HubListener> _getAllListenersRecursive(Hub thisHub, ArrayList<HubListener> al, Hub hub, int type,
			boolean bHasLastChecked, boolean bHasLast) {
		if (type == 0 || type == 2 || thisHub.dataa == hub.dataa) {
			HubListener[] hls = getHubListeners(thisHub);
			if (hls != null && hls.length > 0) {
				int x;
				if (al == null) {
					al = new ArrayList<HubListener>(Math.max(hls.length * 2, 10));
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

		WeakReference<Hub>[] refs = HubShareDelegate.getSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
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
	public static void fireAfterLoadEvent(Hub thisHub, OAObject oaObj) {
		HubListener[] hl = getAllListeners(thisHub);
		int x = hl.length;
		int i;
		if (x > 0) {
			HubEvent hubEvent = new HubEvent(thisHub, oaObj);
			try {
				OAThreadLocalDelegate.addHubEvent(hubEvent);
				for (i = 0; i < x; i++) {
					hl[i].afterLoad(hubEvent);
				}
			} finally {
				OAThreadLocalDelegate.removeHubEvent(hubEvent);
			}
		}
	}

	/**
	 * qqqqq these are in Hub public static boolean canAdd(Hub thisHub) { return canAdd(thisHub, null); } public static boolean canAdd(Hub
	 * thisHub, OAObject obj) { if (obj == null) return OAObjectCallbackDelegate.getAllowAdd(thisHub); return
	 * OAObjectCallbackDelegate.getVerifyAdd(thisHub, obj); } public static boolean canRemove(Hub thisHub) { return canRemove(thisHub,
	 * null); } public static boolean canRemove(Hub thisHub, OAObject obj) { if (obj == null) return
	 * OAObjectCallbackDelegate.getAllowRemove(thisHub); return OAObjectCallbackDelegate.getVerifyRemove(thisHub, obj); } public static
	 * boolean canRemoveAll(Hub thisHub) { return OAObjectCallbackDelegate.getAllowRemoveAll(thisHub); }
	 */
}
