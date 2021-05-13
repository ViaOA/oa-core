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
 * Delegate used to register Hub listeners, get Listeners and to send Events to Hub listeners.
 */
public class HubEventDelegate {

	// 20120827 might be used later, if we need to have hub changes notify masterobject
	protected static void fireMasterObjectChangeEvent(Hub thisHub, boolean bRefreshFlag) {
		// OAObjectHubDelegate.fireMasterObjectHubChangeEvent(thisHub, bRefreshFlag);
	}

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

	public static void fireAfterRemoveEvent(Hub thisHub, final Object obj, int pos) {
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
		OAObjectCacheDelegate.fireAfterRemoveEvent(thisHub, obj);
		//OAObjectCacheDelegate.fireAfterRemoveEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		// 20160304
		if (obj instanceof OAObject && !((OAObject) obj).isLoading()) {
			OAObject objx = thisHub.getMasterObject();
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

		// 20160304
		OAObject objx = thisHub.getMasterObject();
		if (objx != null) {
			String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
			if (s != null) {
				OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(objx.getClass());
				if (oi.getHasTriggers()) {
					final HubEvent hubEvent = new HubEvent(thisHub);
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

	public static void fireAfterAddEvent(Hub thisHub, final Object obj, int pos) {
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
		OAObjectCacheDelegate.fireAfterRemoveEvent(thisHub, obj);
		//OAObjectCacheDelegate.fireAfterAddEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		// 20160304
		if (obj instanceof OAObject) {
			OAObject objx = thisHub.getMasterObject();
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

	public static void fireAfterInsertEvent(Hub thisHub, final Object obj, int pos) {
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
		OAObjectCacheDelegate.fireAfterAddEvent(thisHub, obj);
		//OAObjectCacheDelegate.fireAfterInsertEvent(thisHub, obj, pos);
		//fireMasterObjectChangeEvent(thisHub, false);

		// 20160304
		if (obj instanceof OAObject) {
			OAObject objx = thisHub.getMasterObject();
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
	 * Used by OAObjects to notify all listeners of a property change. If the property involves a reference to another object, then other
	 * objects and Hubs will automaticially be updated.
	 * <p>
	 * Example:<br>
	 * If the Department is changed for an Employee, then the Employee will be removed from the previous Department's Hub of Employees and
	 * moved to the new Department's Hub of Employees.
	 * <p>
	 * If this Hub is linked to a property in another Hub and that property is changed, this Hub will changed it's active object to match
	 * the same value as the new property value.
	 *
	 * @param propertyName name of property that changed. This is case insensitive
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
	 * Called by OAObject and Hub, used to notify all listeners of a property change.
	 *
	 * @param oaObj        OAObject that was changed
	 * @param propertyName name of property that changed. This is case insensitive
	 * @param oldValue     previous value of property
	 * @param newValue     new value of property
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
	 * Called by OAObject and Hub, used to notify all listeners of a property change.
	 *
	 * @param oaObj        OAObject that was changed
	 * @param propertyName name of property that changed. This is case insensitive
	 * @param oldValue     previous value of property
	 * @param newValue     new value of property
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
	 * If property change affects the property used for a detail Hub, then update detail Hub.
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
	 * Used to notify listeners that a new collection has been established. Called by select() and when a detail Hub's source of data is
	 * changed.
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
	 * Add a Listener to this hub specifying a specific property name. If property is a calculated property, then the Hub will automatically
	 * set up internal listeners to know when the calculated property changes.
	 *
	 * @param property name to listen for
	 */
	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, false);
		clearGetAllListenerCache(thisHub);
	}

	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	public static void addHubListener(Hub thisHub, HubListener hl, String property, String[] dependentPropertyPaths,
			boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
		if (property != null && property.indexOf('.') >= 0) {
			throw new RuntimeException(
					"dont use a property path for listener, use addHubListener(h,hl,propertyName, String[path]) instead");
		}
		getHubListenerTree(thisHub).addListener(hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
		clearGetAllListenerCache(thisHub);
	}

	public static void addHubListener(Hub thisHub, HubListener hl, String property) {
		getHubListenerTree(thisHub).addListener(hl, property);
		clearGetAllListenerCache(thisHub);
	}

	public static void addHubListener(Hub thisHub, HubListener hl, String property, boolean bActiveObjectOnly) {
		getHubListenerTree(thisHub).addListener(hl, property, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	public static void addHubListener(Hub thisHub, HubListener hl, boolean bActiveObjectOnly) {
		getHubListenerTree(thisHub).addListener(hl, bActiveObjectOnly);
		clearGetAllListenerCache(thisHub);
	}

	/**
	 * Add a new Hub Listener, that receives all Hub and OAObject events.
	 */
	public static void addHubListener(Hub thisHub, HubListener hl) {
		getHubListenerTree(thisHub).addListener(hl);
		clearGetAllListenerCache(thisHub);
	}

	public static int TotalHubListeners;

	/**
	 * Remove HubListener from list.
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
	 * Returns list of registered listeners for this Hub only.
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
	 * Returns a count of all of the listeners for this Hub and all of Hubs that are shared with it.
	 */
	public static int getListenerCount(Hub thisHub) {
		return getAllListeners(thisHub).length;
	}

	/**
	 * Returns an array of HubListeners for all of the listeners for this Hub and all of Hubs that are shared with it.
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
		if (type < 2 && type != 3) {
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

	protected static HubListener[] getAllListenersRecursive(Hub thisHub, Hub hub, int type) {
		ArrayList<HubListener> al = _getAllListenersRecursive(thisHub, null, hub, type, false, false);

		HubListener[] hl = new HubListener[al == null ? 0 : al.size()];
		if (al != null) {
			al.toArray(hl);
		}
		return hl;
	}

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
