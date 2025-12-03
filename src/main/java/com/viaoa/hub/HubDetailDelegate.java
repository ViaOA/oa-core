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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Internal delegate for Master/Detail wiring in {@link Hub}: creates, maintains,
 * and re-syncs detail Hubs from a master Hub’s active object and link metadata.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Establish master→detail relationships using property paths or link info
 *       (see {@link #getDetailHub(Hub, String)} and overloads).</li>
 *   <li>Keep detail hubs “pointed” at the correct collection/object whenever the
 *       master Hub’s active object (AO) changes ({@link #updateAllDetail}).</li>
 *   <li>Rebind detail hubs to shared or merged hubs, including reconnect logic
 *       for recursive/self-referential models ({@link #updateDetail}).</li>
 *   <li>Keep reference properties in sync when adds/removes happen in the detail
 *       hub ({@link #setPropertyToMasterHub}).</li>
 *   <li>Compute and expose relationship metadata (master hub/object, link info,
 *       property names, “owned” semantics, recursion checks).</li>
 * </ul>
 *
 * <h3>Key APIs</h3>
 * <ul>
 *   <li>{@link #setMasterHub(Hub, Hub, String, boolean, String)} — define/replace the master of a hub.</li>
 *   <li>{@link #getDetailHub(Hub, String)} — resolve or build a detail hub via property path,
 *       routing through {@code HubMerger} when the path requires fan-out.</li>
 *   <li>{@link #updateDetail(Hub, HubDetail, Hub, boolean)} — (re)targets the detail hub’s data
 *       and AO after master AO or link changes.</li>
 *   <li>{@link #getLinkInfoFromMasterToDetail(Hub)} / {@link #getPropertyFromMasterToDetail(Hub)} — metadata helpers.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Supports many-to-many, one-to-many, and recursive graphs (uses reverse link info and
 *       {@code OAPropertyPath} decomposition).</li>
 *   <li>Shares underlying {@code HubData} when detail is a Hub reference; otherwise populates
 *       from arrays/objects with duplicate-allow toggling and newList events.</li>
 *   <li>Integrates with linking/sharing delegates and selection/order settings.</li>
 * </ul>
 */
public class HubDetailDelegate {
	private static Logger LOG = Logger.getLogger(HubDetailDelegate.class.getName());

	/**
	 * Sets the master hub for this hub using the provided property path.
	 * <p>
	 * If this hub already has a master hub defined, the existing master/detail
	 * configuration is removed. If {@code masterHub} is non-null, this method
	 * resolves or creates the corresponding detail hub using the supplied
	 * property path and sharing options.
	 *
	 * @param thisHub     the hub whose master relationship is being set
	 * @param masterHub   the new master hub
	 * @param path        the property path from the master hub to this hub
	 * @param bShared     whether the detail hub should share underlying data
	 * @param selectOrder the select order to assign to the detail hub
	 */
	public static void setMasterHub(Hub thisHub, Hub masterHub, String path, boolean bShared, String selectOrder) {
		if (thisHub.datau.getSharedHub() != null) {
			if (masterHub == null) {
				throw new RuntimeException("sharedHub cant have a master hub");
			}
		}

		if (thisHub.datam.getMasterHub() != null) {
			// this will set all props back to default values
			thisHub.datam.getMasterHub().removeDetailHub(thisHub);
		}

		if (masterHub != null) {
			getDetailHub(masterHub, path, null, thisHub.getObjectClass(), thisHub, bShared, selectOrder);
		}
	}

	/**
	 * Returns whether this hub participates in a recursive master/detail
	 * relationship. The method checks link metadata from the hub's detail-to-master
	 * link and evaluates the reverse link for recursion.
	 *
	 * @param thisHub the hub to test
	 * @return true if the relationship is recursive, otherwise false
	 */
	public static boolean isRecursiveMasterDetail(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}

		OALinkInfo li = thisHub.datam.liDetailToMaster;
		if (li == null) {
			HubDataMaster dm = getDataMaster(thisHub);
			if (dm == null) {
				return false;
			}
			li = dm.liDetailToMaster;
			if (li == null) {
				return false;
			}
		}

		li = OAObjectInfoDelegate.getReverseLinkInfo(li);
		if (li == null) {
			return false;
		}
		return li.getRecursive();
	}

	/**
	 * Attempts to align the master hub's active object with the specified
	 * detail object's reference back to the master. Handles MANY–MANY links,
	 * reverse-link resolution, and active-object adjustment.
	 *
	 * @param thisHub     the detail hub
	 * @param detailObject the detail object whose master reference is checked
	 * @param bUpdateLink  whether linked hubs should update link properties
	 * @return true if the master hub's active object was adjusted, otherwise false
	 */
	protected static boolean setMasterHubActiveObject(Hub thisHub, Object detailObject, boolean bUpdateLink) {
		// make sure none of these have a linkHub
		// and find the sharedHub that has a masterHub
		HubDataMaster dm = getDataMaster(thisHub);
		boolean result = false;
		if (dm.getMasterHub() != null && dm.liDetailToMaster != null) {
			if (dm.liDetailToMaster.getType() == OALinkInfo.MANY) {
				OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster);
				if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
					// Many2Many link
					Hub h = (Hub) OAObjectReflectDelegate.getProperty((OAObject) detailObject, dm.liDetailToMaster.getName());
					dm.getMasterHub().setSharedHub(h, false);
					HubAODelegate.setActiveObject(dm.getMasterHub(), 0, false, false, false); // pick any one, so that detailObject will be in it.
					return true;
				}
			}
			Object obj = OAObjectReflectDelegate.getProperty((OAObject) detailObject, dm.liDetailToMaster.getName());
			// 20121010 if obj==null then dont adjust:  ex: hi5  employeeAward.awardType that was from program.awardTypes, and now the list is in location.awardTpes
			if (obj != null && dm.getMasterHub().getActiveObject() != obj && !(obj instanceof Hub)) {
				//was: if (dm.masterHub.getActiveObject() != obj) {
				if (dm.getMasterHub().datau.isUpdatingActiveObject()) {
					return false;
					// see if masterHub (or a share of it) has a link
					//  if it does, then dont allow it to adjustMaster
				}

				if (OAThreadLocalDelegate.getCanAdjustHub(dm.getMasterHub())) {
					HubAODelegate.setActiveObject(dm.getMasterHub(), obj, true, bUpdateLink, false); // adjustMaster, updateLink, force
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Updates the reference property on a detail object to reflect the
	 * current master object. Handles ONE and MANY link types, reverse-link
	 * processing, Hub-based references, and array-based membership updates.
	 *
	 * @param thisHub      the detail hub
	 * @param detailObject the detail object to update
	 * @param objMaster    the master object used for reference assignment
	 */
	protected static void setPropertyToMasterHub(Hub thisHub, Object detailObject, Object objMaster) {
		if (thisHub == null || detailObject == null) {
			return;
		}
		if (!(detailObject instanceof OAObject)) {
			return;
		}

		// 20160920 this needs to run even if loading.
		///   ex: using copy, loading from xml, etc
		// if (OAThreadLocalDelegate.isLoading()) return;

		HubDataMaster dm;
		if (objMaster != null) {
			dm = getDataMaster(thisHub, objMaster.getClass(), false);
		} else {
			dm = thisHub.datam;
			if (dm == null) {
				return;
			}
		}
		if (dm.liDetailToMaster == null) {
			return;
		}

		// 20120920 if thisHub is a detailHub of type=One, then need to update the masterObj.linkProp
		OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster);
		if (liRev != null && liRev.getType() == OALinkInfo.ONE) {
			if (objMaster == null) {
				// remove was called
				Object objx = HubDetailDelegate.getMasterObject(thisHub);
				if (objx != null) {
					OAObjectReflectDelegate.setProperty((OAObject) objx, liRev.getName(), null, null);
				}
			} else {
				// add was called
				OAObjectReflectDelegate.setProperty((OAObject) objMaster, liRev.getName(), detailObject, null);
				// the AO will also be set, and thisHub.datau.dupAllowAddRemove = false;
			}
		}

		Method method = OAObjectInfoDelegate.getMethod(thisHub.getObjectClass(), "get" + dm.liDetailToMaster.getName());
		if (method == null) {
			// LOG.warning("liDetailToMaster invalid, method not found, hub="+thisHub+", method=get"+dm.liDetailToMaster.getName());
			return;
		}

		if (Hub.class.isAssignableFrom(method.getReturnType())) {
			//if (detailObject instanceof OAObjectKey) return;

			// 20140616 if hub is not loaded and isClient, then dont need to load
			if (!OASyncDelegate.isServer(thisHub)) {
				if (!OAObjectReflectDelegate.isReferenceHubLoaded((OAObject) detailObject, dm.liDetailToMaster.getName())) {
					return;
				}
			}

			Object obj = OAObjectReflectDelegate.getProperty((OAObject) detailObject, dm.liDetailToMaster.getName());
			if (objMaster == null) { // remove
				if (thisHub.datam.getMasterObject() != null) {
					objMaster = thisHub.datam.getMasterObject();
				} else if (dm.getMasterObject() != null) {
					objMaster = dm.getMasterObject();
				} else {
					if (dm.getMasterHub() != null) {
						objMaster = thisHub.getActiveObject();
					}
				}

				// 20101228 pos() could cause the master hub AO to be changed
				//was: if (objMaster != null && ((Hub)obj).getPos(objMaster) >= 0) {
				if (objMaster != null && ((Hub) obj).contains(objMaster)) {
					((Hub) obj).remove(objMaster);
				}
			} else if (obj != null) { // add
				// 20101228
				//was: if ( ((Hub)obj).getPos(objMaster) < 0 ) {
				if (!((Hub) obj).contains(objMaster)) {
					((Hub) obj).add(objMaster);
				}
			}
		} else {
			method = OAObjectInfoDelegate.getMethod(thisHub.getObjectClass(), "set" + dm.liDetailToMaster.getName());
			if (method == null) {
				// LOG.warning("liDetailToMaster invalid, method not found, hub="+thisHub+", method=set"+dm.liDetailToMaster.getName());
				return;
			}
			Object currentValue = OAObjectReflectDelegate.getProperty((OAObject) detailObject, dm.liDetailToMaster.getName());
			if (currentValue == objMaster) {
				return;
			}

			if (objMaster == null) { // must have been called by remove()
				// if "real" current master == obj, then set new value to null
				//   otherwise then the remove is being done by OAObject during
				//   a propertyChange and the object is being moved from one hub to another
				if (dm.getMasterObject() != null) {
					if (currentValue != dm.getMasterObject()) {
						return;
					}
				} else if (dm.getMasterHub() != null) {
					if (currentValue != dm.getMasterHub().getActiveObject()) {
						return;
					}
				}
			}

			OAObjectReflectDelegate.setProperty((OAObject) detailObject, dm.liDetailToMaster.getName(), objMaster, null);
		}
	}

	/**
	 * Updates all detail hubs under the specified hub. Each registered
	 * {@code HubDetail} is processed to ensure its underlying data and
	 * active object align with the current master's active object.
	 *
	 * @param thisHub     the hub whose detail hubs should be updated
	 * @param bUpdateLink whether link-based updates should be propagated
	 */
	protected static void updateAllDetail(Hub thisHub, boolean bUpdateLink) {
		int x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();
		// get objects that go with detail hub
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) thisHub.datau.getVecHubDetail().elementAt(i);
			Hub h = hd.hubDetail;
			if (h == null) {
				thisHub.datau.getVecHubDetail().removeElementAt(i);
				x--;
				i--;
			} else {
				updateDetail(thisHub, hd, h, bUpdateLink);
			}
		}
	}

	/**
	 * Preloads detail data for the object at the specified position in the
	 * master hub. Touches each detail hub’s property getter to ensure data
	 * is loaded or initialized.
	 *
	 * @param thisHub the master hub
	 * @param pos     the index of the master object whose detail data is preloaded
	 */
	public static void preloadDetailData(final Hub thisHub, final int pos) {
		if (thisHub == null || pos < 0) {
			return;
		}
		int x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();

		Object objMaster = thisHub.getAt(pos);
		if (objMaster == null) {
			return;
		}

		// get objects that go with detail hub
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) thisHub.datau.getVecHubDetail().elementAt(i);
			Hub h = hd.hubDetail;
			if (h == null) {
				thisHub.datau.getVecHubDetail().removeElementAt(i);
				x--;
				i--;
				continue;
			}
			OAObjectReflectDelegate.getProperty((OAObject) thisHub.dataa.activeObject, hd.liMasterToDetail.getName());
		}
	}

	/**
	 * Internal method used to refresh the contents and active object of a
	 * detail hub after changes to the master hub’s active object or link
	 * property. Handles Hub, OAObject, Object, and array-based detail types,
	 * as well as shared-hub state.
	 *
	 * @param thisHub     the master hub
	 * @param detail      the hub-detail metadata
	 * @param detailHub   the hub being updated
	 * @param bUpdateLink whether link-based updates should be propagated
	 */
	protected static void updateDetail(final Hub thisHub, final HubDetail detail, final Hub detailHub, final boolean bUpdateLink) {
		/* get Hub, Object, OAObject or Array value from property
		   ex:  Emp
		          String name;
		          Dept[] depts;  or
		          Hub depts;     or
		          Dept dept;
		   then add to dHub.vector
		*/
		if (detail == null || detail.type == detail.HUBMERGER) {
			return;
		}

		if (detail.bIgnoreUpdate) { // set by hubDetail.setup()
			// this is called by hubListener in HubDetail, to make sure that the detailHub is "reconnected" to the masterHub.
			//    it can get disconnected when it is changed to point (shared) to a child hub.
			// in case detailHub was set/shared to a recursive child hub.  This will set it back to be off of the masterHub (thisHub)
			if (detailHub.datam.getMasterObject() == (OAObject) thisHub.dataa.activeObject) {
				// 20160204
				if (detailHub.datam.getMasterHub() != thisHub) {
					Hub hx = detailHub.datau.getSharedHub();
					boolean b = (hx != null && hx.datam == detailHub.datam); // this happens by setting sharedHub - when it was sharing with a child hub
					if (b) {
						// this will reconnect detailHub to the masterHub (thisHub)
						detailHub.datam = new HubDataMaster();
						detailHub.datam.liDetailToMaster = OAObjectInfoDelegate.getReverseLinkInfo(detail.liMasterToDetail);
						detailHub.datam.setMasterHub(thisHub);
						detailHub.datam.setMasterObject((OAObject) thisHub.dataa.activeObject);
						HubShareDelegate.syncSharedHubs(detailHub, detail.bShareActiveObject, detailHub.dataa, hx.dataa, bUpdateLink);
					}
				}

				/*was
				detailHub.datam.liDetailToMaster = OAObjectInfoDelegate.getReverseLinkInfo(detail.liMasterToDetail);
				detailHub.datam.masterHub = thisHub;
				*/
			}
			return;
		}

		if (detailHub.datau.getSharedHub() != null) {
			if (detailHub.datau.getSharedHub().datam == detailHub.datam) {
				detailHub.datam = new HubDataMaster();
			}
		}
		detailHub.datam.setMasterObject((OAObject) thisHub.dataa.activeObject);
		detailHub.datam.liDetailToMaster = OAObjectInfoDelegate.getReverseLinkInfo(detail.liMasterToDetail);
		detailHub.datam.setMasterHub(thisHub);

		Object obj = null; // reference property
		try {
			if (thisHub.dataa.activeObject == null) {
				obj = null;
			} else {
				obj = OAObjectReflectDelegate.getProperty((OAObject) thisHub.dataa.activeObject, detail.liMasterToDetail.getName());
			}
		} catch (Exception e) {
			throw new RuntimeException("error calling get method for master to detail: " + detail.liMasterToDetail.getName());
		}

		boolean wasShared = false;
		if (detail.type == HubDetail.HUB) {
			if (detailHub.datau.getSharedHub() != null) {
				HubShareDelegate.removeSharedHub(detailHub.datau.getSharedHub(), detailHub);
				detailHub.datau.setSharedHub(null);
				wasShared = true;
			}
		} else {
			// see if the detail list needs changed
			if (obj == detailHub.dataa.activeObject) {
				// 20120720 need to send newList event, in case master object was previously null
				if (obj == null) {
					HubEventDelegate.fireOnNewListEvent(detailHub, false); // notifies all of this hub's shared hubs
				}
				return;
			}

			if (detailHub.isOAObject()) {
				for (int i = 0;; i++) {
					Object objx = HubDataDelegate.getObjectAt(detailHub, i);
					if (objx == null) {
						break;
					}
					OAObjectHubDelegate.removeHub((OAObject) objx, detailHub, true); // 20160713 changed to true so that it wont add to server cache
				}
			}
			detailHub.data.vector.removeAllElements();
		}

		detailHub.data.setDupAllowAddRemove(true);
		if (obj == null) {
			HubDataActive daOld = detailHub.dataa;

			if (wasShared) {
				// have to create its own since it might have been sharing the current one
				detailHub.data = new HubData(detailHub.data.objClass);
				if (detail.bShareActiveObject) {
					detailHub.dataa = new HubDataActive();
				}
			}
			detailHub.data.setDupAllowAddRemove(false); // 2004/08/23
			//was: if (detail.type != HubDetail.HUB) dHub.datau.dupAllowAddRemove = false;
			HubShareDelegate.syncSharedHubs(detailHub, true, daOld, detailHub.dataa, bUpdateLink);
		} else if (detail.type == HubDetail.HUB) { // Hub
			// share oaObject info and activeObject info
			// dont share listeners and links ("datau")
			// dont share activeObject ("dataa")
			//     unless DetailHub.bShareActiveObject is true then set it after events
			Hub h = (Hub) obj;

			if (HubSortDelegate.isSorted(detailHub)) {
				String s = HubSortDelegate.getSortProperty(detailHub);
				if (s != null) {
					String s2 = HubSortDelegate.getSortProperty(h);
					if (!OAString.equals(s, s2, true)) {
						boolean b = HubSortDelegate.getSortAsc(detailHub);
						h.sort(s, b);
					}
				}
			}

			// need to select before assigning to detail hub so that add events wont
			//            be sent to detail hubs listeners
			detailHub.data = h.data;
			detailHub.datau.setSharedHub(h);
			HubShareDelegate.addSharedHub(h, detailHub);

			// 20120926 "h" could be a shared/calc Hub.
			// 20160204 this can happen for recursive, where the detailHub is pointing/shared to a childHub.
			//     this will reconnect it to the parent
			if (detailHub.datam.getMasterObject() != (OAObject) h.datam.getMasterObject()) {
				Hub hx = detailHub.datau.getSharedHub();
				if (hx != null && hx.datam == detailHub.datam) {
					detailHub.datam = new HubDataMaster();
				}

				if (h.datam.getMasterObject() != null) {
					detailHub.datam.setMasterObject((OAObject) h.datam.getMasterObject());
				}
				if (h.datam.liDetailToMaster != null) {
					detailHub.datam.liDetailToMaster = h.datam.liDetailToMaster;
				}

				// 20160204
				detailHub.datam.setMasterHub(detail.hubMaster);
				//was: detailHub.datam.masterHub = h.datam.masterHub;
			}
			HubShareDelegate.syncSharedHubs(detailHub, detail.bShareActiveObject, detailHub.dataa, h.dataa, bUpdateLink);

			if (detailHub.datam.getMasterObject() != null && h.datam.getMasterObject() == null) {
				HubDetailDelegate.setMasterObject(h, detailHub.datam.getMasterObject(), detailHub.datam.liDetailToMaster);
			}
		} else if (detail.type == HubDetail.OAOBJECT || detail.type == HubDetail.OBJECT) {
			HubAddRemoveDelegate.internalAdd(detailHub, (OAObject) obj, false, true);
			detailHub.data.setDupAllowAddRemove(false);
		} else {
			// HubDetail.OBJECTARRAY || HubDetail.OAOBJECTARRAY
			int j = Array.getLength(obj);
			for (int k = 0; k < j; k++) {
				Object objx = Array.get(obj, k);
				HubAddRemoveDelegate.internalAdd(detailHub, objx, false, true);
			}
			detailHub.data.setDupAllowAddRemove(false);
		}

		HubDataDelegate.incChangeCount(detailHub);
		Object aoHold = detailHub.dataa.activeObject;
		HubData hd = detailHub.data;
		detailHub.dataa.activeObject = null;

		HubEventDelegate.fireOnNewListEvent(detailHub, false); // notifies all of this hub's shared hubs
		if (detailHub.data == hd && detailHub.dataa.activeObject == null) {
			detailHub.dataa.activeObject = aoHold;
		}

		// 20140421 moved to after newList
		HubDetailDelegate.updateDetailActiveObject(detailHub, detailHub, bUpdateLink, detail.bShareActiveObject);

		if (detail.type == HubDetail.OAOBJECT || detail.type == HubDetail.OBJECT) {
			detailHub.setPos(0);
		}
	}

	/**
	 * Initializes or adjusts the active object for a detail hub based on
	 * master hub state, link-hub constraints, or shared active-object rules.
	 *
	 * @param thisHub           the hub whose active object drives updates
	 * @param hubDetailHub      the detail hub being updated
	 * @param bUpdateLink       whether linked hubs should update link properties
	 * @param bShareActiveObject whether the hubs share active-object state
	 */
	protected static void updateDetailActiveObject(final Hub thisHub, final Hub hubDetailHub, final boolean bUpdateLink,
			final boolean bShareActiveObject) {
		boolean bUseCurrent = (bShareActiveObject && thisHub.dataa == hubDetailHub.dataa); // if hubs are sharing active object then dont change it.
		if (!bUseCurrent || (thisHub == hubDetailHub)) {
			Hub hubWithLink = HubLinkDelegate.getHubWithLink(thisHub, true);

			if (hubWithLink == null) {
				// if there is not a linkHub, then go to default object
				int pos;
				if (bUseCurrent) {
					pos = thisHub.getPos();
				} else {
					pos = thisHub.datau.getDefaultPos(); // default is -1
				}
				HubAODelegate.setActiveObject(thisHub, pos, bUpdateLink, true, false); // bForce=true,bCalledByShareHub=false
			} else if (bUpdateLink) {
				int pos;
				if (bUseCurrent) {
					pos = thisHub.getPos();
				} else {
					pos = -1;
				}
				HubAODelegate.setActiveObject(thisHub, pos, bUpdateLink, true, false); // bForce=true, this will recursivly notify this links HubDetails
			} else {
				// if linkHub & !bUpdateLink, then retreive value from linked property
				// and make that the activeObject in this Hub
				try {
					Object obj = hubWithLink.datau.getLinkToHub().getActiveObject();
					if (obj != null) {
						obj = hubWithLink.datau.getLinkToGetMethod().invoke(obj, (Object[]) null);
					}
					if (hubWithLink.datau.isLinkPos()) {
						int x = -1;
						if (obj != null && obj instanceof Number) {
							x = ((Number) obj).intValue();
						}
						if (thisHub.getPos() != x) {
							HubAODelegate.setActiveObject(thisHub, thisHub.elementAt(x), x, bUpdateLink, false, false);//bUpdateLink,bForce,bCalledByShareHub
						}
					} else if (hubWithLink.datau.getLinkFromPropertyName() != null) { // 20110116 ex: Breed.name linked to Pet.breed (string)
						Object objx;
						if (obj != null) {
							objx = hubWithLink.find(hubWithLink.datau.getLinkFromPropertyName(), obj);
						} else {
							objx = null;
						}
						HubAODelegate.setActiveObject(thisHub, objx, bUpdateLink, false, false);
					} else {
						int pos = thisHub.getPos(obj);
						if (obj != null && pos < 0) {
							obj = null;
						}
						HubAODelegate.setActiveObject(thisHub, obj, pos, bUpdateLink, false, false);//bUpdateLink,bForce,bCalledByShareHub
					}
				} catch (Exception e) {
					throw new RuntimeException(thisHub.datau.getLinkToGetMethod().getName(), e); // wrap orig exception
				}
			}
		}

		// 20120715
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

			// only update sharedHubs with diff dataa, setActiveObject will do others
			if (h2.dataa != hubDetailHub.dataa) {
				updateDetailActiveObject(h2, hubDetailHub, false, bShareActiveObject); // dont update link properties
			}
		}

		/* was
		Hub[] hubs = HubShareDelegate.getSharedHubs(thisHub);
		for (int i=0; i<hubs.length; i++) {
		    Hub h2 = hubs[i];
		    if (h2 == null) continue;
		    // only update sharedHubs with diff dataa, setActiveObject will do others
		    if (h2.dataa != hubDetailHub.dataa) {
		        updateDetailActiveObject(h2, hubDetailHub,false,bShareActiveObject); // dont update link properties
		    }
		}
		*/
	}

	/**
	 * Returns the {@code HubDataMaster} associated with the hub or one of
	 * its shared hubs. If no shared hub contains master information, the
	 * hub's own {@code datam} is returned.
	 *
	 * @param thisHub the hub whose master data is resolved
	 * @return the master-data descriptor
	 */
	protected static HubDataMaster getDataMaster(final Hub thisHub) {
		return getDataMaster(thisHub, null, false);
	}

	/**
	 * Returns the hub's master-data descriptor, optionally searching filtered
	 * shared hubs. Delegates to the internal {@code getDataMaster} variant.
	 *
	 * @param thisHub               the hub whose master data is resolved
	 * @param bIncludedFilteredHub  whether filtered shared hubs should be considered
	 * @return the resolved {@code HubDataMaster}
	 */
	protected static HubDataMaster getDataMaster(final Hub thisHub, boolean bIncludedFilteredHub) {
		return getDataMaster(thisHub, null, bIncludedFilteredHub);
	}

	/**
	 * Internal implementation for resolving the {@code HubDataMaster} from
	 * this hub or its shared hubs that match the optional master class.
	 *
	 * @param thisHub              the hub to evaluate
	 * @param masterClass          optional class constraint for the master hub
	 * @param bIncludedFilteredHub whether filtered hubs are eligible
	 * @return the resolved {@code HubDataMaster}, or null if not found
	 */
	private static HubDataMaster getDataMaster(final Hub thisHub, final Class masterClass, boolean bIncludedFilteredHub) {
		if (thisHub == null) {
			return null;
		}

		if (thisHub.datam.getMasterHub() != null) {
			return thisHub.datam;
		}

		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				if (h.datam.getMasterHub() != null) {
					if (masterClass == null || masterClass.equals(h.datam.getMasterHub().getObjectClass())) {
						return true;
					}
				}
				return false;
			}
		};
		Hub hubx = HubShareDelegate.getFirstSharedHub(thisHub, filter, bIncludedFilteredHub, false);
		if (hubx != null) {
			return hubx.datam;
		}
		return thisHub.datam;
	}

	/**
	 * Returns this hub or a shared hub that has a master hub defined. Searches
	 * the hub and its shared hubs until one with a non-null master hub is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master hub, or null if none exists
	 */
	public static Hub getHubWithMasterHub(final Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		if (thisHub.datam.getMasterHub() != null) {
			return thisHub;
		}

		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				if (h.datam.getMasterHub() != null) {
					// 20130916 make sure it has the same masterObject
					//    since it could be a recursive hub, that points
					//    to the root hub, and not just it's parent
					return true;
				}
				return false;
			}
		};
		Hub hubx = HubShareDelegate.getFirstSharedHub(thisHub, filter, true, false);
		return hubx;
	}

	/**
	 * Returns this hub or a shared hub that has a master object defined. Searches
	 * the hub and its shared hubs until one with a non-null master object is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master object, or null if none exists
	 */
	public static Hub getHubWithMasterObject(final Hub thisHub) {
		if (thisHub.datam == null) {
			return null; // could be deserializing and not fully loaded
		}
		if (thisHub.datam.getMasterObject() != null) {
			return thisHub;
		}

		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				if (h.datam.getMasterHub() != null) {
					// 20130916 make sure it has the same masterObject
					//    since it could be a recursive hub, that points
					//    to the root hub, and not just it's parent
					if (h.datam.getMasterObject() != null) {
						return true;
					}
				}
				return false;
			}
		};
		Hub hubx = HubShareDelegate.getFirstSharedHub(thisHub, filter, true, false);
		return hubx;
	}

	/**
	 * Returns the master hub for this hub or any shared hub that carries
	 * master-hub metadata.
	 *
	 * @param thisHub the hub whose master hub is requested
	 * @return the master hub, or null if none exists
	 */
	public static Hub getMasterHub(Hub thisHub) {
		Hub h = getHubWithMasterHub(thisHub);
		if (h != null) {
			h = h.datam.getMasterHub();
		}
		return h;
	}

	/**
	 * Returns the master object associated with this hub or a shared hub.
	 *
	 * @param thisHub the hub whose master object is requested
	 * @return the master object, or null if not defined
	 */
	public static OAObject getMasterObject(Hub thisHub) {
		thisHub = getHubWithMasterObject(thisHub);
		if (thisHub == null) {
			return null;
		}
		return thisHub.datam.getMasterObject();
	}

	/**
	 * Returns the class of the master object or master hub associated with
	 * this hub. If none is found, returns null.
	 *
	 * @param thisHub the hub whose master class is requested
	 * @return the master class, or null if unavailable
	 */
	public static Class getMasterClass(Hub thisHub) {
		if (thisHub.datam.getMasterObject() != null) {
			return thisHub.datam.getMasterObject().getClass();
		}
		if (thisHub.datam.getMasterHub() != null) {
			return thisHub.datam.getMasterHub().getObjectClass();
		}
		Hub h = getHubWithMasterObject(thisHub);
		if (h != null) {
			return h.getObjectClass();
		}

		h = getHubWithMasterHub(thisHub);
		if (h != null) {
			return h.getObjectClass();
		}
		return null;
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation to create
	 * or resolve a detail hub using the specified class array.
	 *
	 * @param thisHub the master hub
	 * @param clazz   the class path used to derive the property path
	 * @return the resolved or newly created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, Class[] clazz) {
		return getDetailHub(thisHub, null, clazz, null, null, false, null);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using a
	 * single class, with optional shared active-object and select-order settings.
	 *
	 * @param thisHub      the master hub
	 * @param clazz        the target class for the detail relationship
	 * @param bShareActive whether the detail hub shares active-object state
	 * @param selectOrder  optional select-order string
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, Class clazz, boolean bShareActive, String selectOrder) {
		return getDetailHub(thisHub, null, new Class[] { clazz }, null, null, bShareActive, selectOrder);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * supplied property path and optional object class.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path to the detail hub
	 * @param objectClass  the class of the detail objects
	 * @param bShareActive whether the detail hub shares active-object state
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, Class objectClass, boolean bShareActive) {
		return getDetailHub(thisHub, path, null, objectClass, null, bShareActive, null);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path.
	 *
	 * @param thisHub the master hub
	 * @param path    the property path
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path) {
		return getDetailHub(thisHub, path, null, null, null, false, null);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path and select-order setting.
	 *
	 * @param thisHub     the master hub
	 * @param path        the property path
	 * @param selectOrder optional select-order for the detail hub
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, String selectOrder) {
		return getDetailHub(thisHub, path, null, null, null, false, selectOrder);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * provided property path and active-object sharing flag.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path
	 * @param bShareActive whether the detail hub shares active-object state
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, boolean bShareActive) {
		return getDetailHub(thisHub, path, null, null, null, bShareActive, null);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path, active-object sharing flag, and select-order.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path
	 * @param bShareActive whether active-object state is shared
	 * @param selectOrder  optional select-order for the detail hub
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, boolean bShareActive, String selectOrder) {
		return getDetailHub(thisHub, path, null, null, null, bShareActive, selectOrder);
	}

	/**
	 * Core implementation for resolving or creating a detail hub based on a
	 * property path or class sequence. Handles HubMerger creation, discovery
	 * of existing HubDetail entries, link resolution, and recursion through
	 * multi-segment property paths.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path (may be null for class-based lookup)
	 * @param classes      optional class array to derive a property path
	 * @param lastClass    optional class constraint for the final segment
	 * @param detailHub    optionally supplied hub to populate
	 * @param bShareActive whether the detail hub shares active-object state
	 * @param selectOrder  optional select-order for the detail hub
	 * @return the resolved or newly created detail hub
	 */
	protected static Hub getDetailHub(final Hub thisHub, String path, Class[] classes, Class lastClass, Hub detailHub, boolean bShareActive,
			String selectOrder) {
		// linkHub is Hub that is the detail hub, it is supplied by setMaster()
		// lastClass can be the class to use for the last class in the path

		if (path != null && path.length() > 0 && thisHub.data.objClass == null) {
			return null;
		}

		if (path == null) {
			Class[] c = classes;
			if (c == null && lastClass != null) {
				c = new Class[1];
				c[0] = lastClass;
			}
			if (c != null) {
				path = HubDelegate.getPropertyPathforClasses(thisHub, c);
			}
			if (path == null) {
				throw new RuntimeException("cant find path.");
			}
		} else if (path.length() == 0) {
			return thisHub; // since this is a recursive method
		}

		// support for using HubMerger if property path has more then one ending object/hub
		Class clazz = thisHub.getObjectClass();

		final OAPropertyPath ppx = new OAPropertyPath(clazz, path);
		OALinkInfo[] lis = ppx.getLinkInfos();
		boolean bLastMany = false;
		int cntMany = 0;

		for (int i = 0; i < lis.length; i++) {
			OALinkInfo li = lis[i];

			if (li.getType() == OALinkInfo.MANY) {
				bLastMany = true;
				cntMany++;
			} else {
				bLastMany = false;
			}
			clazz = li.getToClass();
		}

		if (cntMany > 1 || (cntMany > 0 && !bLastMany)) {
			// use HubMerger to create DetailHub
			// see if HubDetail is already created
			if (detailHub == null) {
				HubDetail hd = null;
				int x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();
				for (int i = 0; i < x; i++) {
					hd = (HubDetail) thisHub.datau.getVecHubDetail().elementAt(i);
					if (hd.type == hd.HUBMERGER && path.equalsIgnoreCase(hd.path)) {
						hd.referenceCount++;
						return hd.hubDetail;
					}
				}
			}

			if (detailHub == null) {
				detailHub = new Hub(clazz);
			}

			HubMerger hm = new HubMerger(thisHub, detailHub, path,
					bShareActive, selectOrder, false);

			HubDetail hd = new HubDetail(path, detailHub);
			hd.referenceCount = 1;
			if (thisHub.datau.getVecHubDetail() == null) {
				thisHub.datau.setVecHubDetail(new Vector<HubDetail>(3, 5));
			}
			thisHub.datau.getVecHubDetail().addElement(hd);

			return detailHub;
		}

		final String propertyName = ppx.getProperties()[0];
		final Class newClass = ppx.getClasses()[0];

		// get LinkInfo
		final OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(thisHub.data.getObjectInfo(), propertyName);
		if (linkInfo == null) {
			throw new RuntimeException(
					"cant find linkInfo, hub=" + thisHub + ", propertyPath=" + path + ", property not found=" + propertyName);
		}

		// see what type of object the property returns: Array, Hub, OAObject, Object

		int type = -1; // must be assign < 0
		/* removed support for array
		if (returnClass.isArray()) { // see if it is an Array
			type = HubDetail.ARRAY;
			returnClass = returnClass.getComponentType();
		}
		if (Hub.class.isAssignableFrom(returnClass)) {
			if (type != HubDetail.ARRAY) {
				type = HubDetail.HUB;
			}
		} else if (OAObject.class.isAssignableFrom(returnClass)) {
			if (type == HubDetail.ARRAY) {
				type = HubDetail.OAOBJECTARRAY;
			} else {
				type = HubDetail.OAOBJECT;
			}
		} else {
			if (type == HubDetail.ARRAY) {
				type = HubDetail.OBJECTARRAY;
			} else {
				type = HubDetail.OBJECT;
			}
		}
		*/

		if (linkInfo.getType() == OALinkInfo.TYPE_MANY) {
			type = HubDetail.HUB;
		} else {
			type = HubDetail.OAOBJECT;
		}

		//  see if HubDetail is already created
		Hub hub = null;
		HubDetail hd = null;
		int x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			hd = (HubDetail) thisHub.datau.getVecHubDetail().elementAt(i);
			if (hd.liMasterToDetail != null && hd.liMasterToDetail.equals(linkInfo) && hd.hubDetail != null) {
				if (detailHub == null || detailHub == hd.hubDetail) {
					hub = hd.hubDetail;
					break;
				}
			}
		}

		final int pos2 = path.indexOf(')');
		final int pos = path.indexOf(".", pos2 > 0 ? pos2 + 1 : 0);

		boolean bFound = false;
		if (hub == null) {
			if (pos > 0 || detailHub == null) {
				hub = new Hub(newClass); // create new hub to reference objects
				hd = new HubDetail(thisHub, hub, linkInfo, type, propertyName);
			} else {
				hd = new HubDetail(thisHub, null, linkInfo, type, propertyName); // from call to "setMaster()"
			}
			if (thisHub.datau.getVecHubDetail() == null) {
				thisHub.datau.setVecHubDetail(new Vector(3, 5));
			}
			thisHub.datau.getVecHubDetail().addElement(hd);
		} else {
			bFound = true;
		}

		if (pos < 0 && bShareActive) {
			hd.bShareActiveObject = true;
		}

		if (pos < 0) {
			if (detailHub != null) {
				if (detailHub.getObjectClass() == null) {
					HubDelegate.setObjectClass(detailHub, newClass);
				}
				if (hub != null && !hub.getObjectClass().equals(detailHub.getObjectClass())) {
					if (!hub.getObjectClass().isAssignableFrom(detailHub.getObjectClass())) {
						throw new RuntimeException("ObjectClass is different, hub=" + hub + ", path=" + path);
					}
				}
				hub = detailHub;
				hd.hubDetail = hub;
			}
			if (selectOrder != null) {
				hub.setSelectOrder(selectOrder);
			}
			hd.referenceCount++;

			path = "";
		} else {
			path = path.substring(pos + 1);
		}
		hub.datam.setMasterHub(thisHub);

		if (type == HubDetail.OAOBJECT || type == HubDetail.OBJECT) {
			hub.datau.setDefaultPos(0);

			if (type == HubDetail.OAOBJECT && linkInfo.getCalculated() && linkInfo.getCalcDependentProperties() != null
					&& linkInfo.getCalcDependentProperties().length > 0) {
				// need to use a hub listener if it's a calculated link that has dependent PPs
				thisHub.addHubListener(new HubListenerAdapter() {
					// no-op, just need to have it generate a property change whenever a dependent prop changes so that link hub is updated.
				}, propertyName);
			}
		}

		if (!bFound) {
			updateDetail(thisHub, hd, hd.hubDetail, false);
		}

		return getDetailHub(hub, path, null, lastClass, detailHub, bShareActive, selectOrder);
	}

	/**
	 * Sets the master object for this hub and assigns the associated
	 * detail-to-master link information. Updates the hub’s master object
	 * reference when changed.
	 *
	 * @param thisHub          the hub whose master is being set
	 * @param masterObject     the new master object
	 * @param liDetailToMaster the link information from detail to master
	 */
	public static void setMasterObject(Hub thisHub, OAObject masterObject, OALinkInfo liDetailToMaster) {
		// OAObject needs to know which hubs are under it
		if (thisHub.datam == null) {
			return; // could be deserializing and not fully loaded
		}
		thisHub.datam.liDetailToMaster = liDetailToMaster;
		if (masterObject == thisHub.datam.getMasterObject()) {
			return;
		}
		thisHub.datam.setMasterObject(masterObject);
	}

	/**
	 * Convenience wrapper that sets the master object using the hub’s
	 * existing detail-to-master link information.
	 *
	 * @param thisHub      the hub whose master object is assigned
	 * @param masterObject the master object to set
	 */
	public static void setMasterObject(Hub thisHub, OAObject masterObject) {
		setMasterObject(thisHub, masterObject, thisHub.datam.liDetailToMaster);
	}

	/**
	 * Returns the {@code OALinkInfo} that links a detail hub to its master.
	 * Searches this hub and any shared hubs that carry master metadata.
	 *
	 * @param hub the detail hub
	 * @return the detail-to-master link information, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromDetailToMaster(Hub hub) {
		if (hub == null) {
			return null;
		}
		Hub h = getHubWithMasterHub(hub);
		if (h == null) {
			h = getHubWithMasterObject(hub);
			if (h == null) {
				return null;
			}
		}
		return h.datam.liDetailToMaster;
	}

	/**
	 * Returns true if any master hub in the hierarchy above this hub has an
	 * active object marked as new. Walks upward through master hubs or master
	 * objects until the chain terminates.
	 *
	 * @param thisHub the hub to evaluate
	 * @return true if a master active object is new, otherwise false
	 */
	public static boolean isMasterNew(Hub thisHub) {
		thisHub = getHubWithMasterObject(thisHub);
		if (thisHub == null) {
			return false;
		}

		Hub h = thisHub;
		for (; h != null;) {
			HubDataMaster dm = HubDetailDelegate.getDataMaster(h, true);

			Object obj = null;
			if (dm.getMasterHub() != null) {
				h = dm.getMasterHub();
				obj = h.getActiveObject();
			} else {
				if (dm.getMasterObject() != null) {
					obj = dm.getMasterObject();
				}
				h = null;
			}

			if (obj == null) {
				break;
			}
			if (!(obj instanceof OAObject)) {
				break;
			}
			if (((OAObject) obj).getNew()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes or decrements the reference count for a registered detail hub.
	 * If no more references remain and the detail hub has no children, its
	 * data and master information are reset.
	 *
	 * @param thisHub   the master hub
	 * @param hubDetail the detail hub to remove
	 * @return true if the hub was removed entirely, otherwise false
	 */
	public static boolean removeDetailHub(Hub thisHub, Hub hubDetail) {
		// remove HubDetail if it does not have any more listeners or links
		if (hubDetail == thisHub) {
			return false;
		}

		int x = thisHub.datau.getVecHubDetail() == null ? 0 : thisHub.datau.getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) thisHub.datau.getVecHubDetail().elementAt(i);
			Hub h = hd.hubDetail;
			if (h == hubDetail) {
				hd.referenceCount--;
				if (hd.referenceCount <= 0) {
					if (h.datau.getVecHubDetail() == null || h.datau.getVecHubDetail().size() == 0) {
						thisHub.datau.getVecHubDetail().removeElementAt(i);
						hubDetail.data = new HubData(hubDetail.data.objClass);
						hubDetail.datam = new HubDataMaster();
						hubDetail.dataa = new HubDataActive();
						return true;
					}
					hd.referenceCount = 0;
				}
				return false;
			}
			// if not found, this will recursively look to find hub in other linked hubDetails
			if (h != null) {
				boolean b = removeDetailHub(h, hubDetail);
				if (b && hd.referenceCount <= 0) {
					if (h.datau.getVecHubDetail() != null || h.datau.getVecHubDetail().size() == 0) {
						removeDetailHub(thisHub, h);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns the name of the property on the master object or hub that leads
	 * to this detail hub. Attempts resolution via link metadata, OAObjectInfo,
	 * and HubDetail entries.
	 *
	 * @param thisHub the detail hub
	 * @return the master-to-detail property name, or null if unavailable
	 */
	public static String getPropertyFromMasterToDetail(Hub thisHub) {
		Hub h = HubDetailDelegate.getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return null;
			}
		}
		thisHub = h;
		if (thisHub.datam.liDetailToMaster != null) {
			String name = thisHub.datam.liDetailToMaster.getReverseName();
			if (name != null) {
				return name;
			}
		}

		OAObject master = thisHub.datam.getMasterObject();
		if (master != null) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(master.getClass());
			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, master, thisHub);
			if (li != null) {
				return li.getName();
			}
		}

		// see if it can be found using detailHub info
		Hub hubMaster = thisHub.datam.getMasterHub();
		if (hubMaster != null) {
			int x = hubMaster.datau.getVecHubDetail() == null ? 0 : hubMaster.datau.getVecHubDetail().size();
			for (int i = 0; i < x; i++) {
				HubDetail hd = (HubDetail) hubMaster.datau.getVecHubDetail().elementAt(i);
				if (hd.hubDetail == thisHub) {
					OALinkInfo li = hd.liMasterToDetail;
					if (li != null) {
						return li.getName();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the link information from a master hub to this detail hub.
	 * Delegates to {@link #getLinkInfoFromMasterToDetail(Hub)}.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the link info from master to detail, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterHubToDetail(Hub thisDetailHub) {
		return getLinkInfoFromMasterToDetail(thisDetailHub);
	}

	/**
	 * Determines whether a recursive one-to-many relationship is valid for
	 * this hub based on link metadata and object-class comparisons.
	 *
	 * @param hub the hub to evaluate
	 * @return true if the recursive structure is valid, otherwise false
	 */
	public static boolean getIsValidRecursive(final Hub hub) {
		if (hub == null) {
			return true;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterToDetail(hub);
		if (li == null) {
			return true;
		}

		if (li.getRecursive()) {
			return true;
		}

		OALinkInfo liRev = li.getReverseLinkInfo();
		if (liRev == null) {
			return true;
		}

		if (li.getType() != OALinkInfo.TYPE_MANY || liRev.getType() != OALinkInfo.TYPE_ONE) {
			return true;
		}

		if (!li.getToObjectInfo().equals(liRev.getToObjectInfo())) {
			return true;
		}

		// same class and not recursive

		Hub hubMaster = hub.getMasterHub();
		if (hubMaster != null) {
			OALinkInfo lix = HubDetailDelegate.getLinkInfoFromMasterToDetail(hubMaster);
			if (lix == null) {
				return true;
			}
			OALinkInfo lixRev = lix.getReverseLinkInfo();
			if (lixRev == null) {
				return true;
			}
			if (lix.getType() != OALinkInfo.TYPE_MANY || lixRev.getType() != OALinkInfo.TYPE_ONE) {
				return true;
			}
			if (lix.getToObjectInfo().equals(lixRev.getToObjectInfo())) {
				return false;
			}
			return true;
		}

		OAObject obj = hub.getMasterObject();
		if (obj == null) {
			return true;
		}

		OAObject obj2 = (OAObject) liRev.getValue(obj);
		if (obj2 == null) {
			return true;
		}

		if (obj2.getClass().equals(liRev.getToClass())) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether both hubs originate from the same master hub based on
	 * link-info equality from their respective master hubs.
	 *
	 * @param hub1 the first hub
	 * @param hub2 the second hub
	 * @return true if both hubs share the same master link info, otherwise false
	 */
	public static boolean getIsFromSameMasterHub(Hub hub1, Hub hub2) {
		// if (HubDetailDelegate.getLinkInfoFromMasterToDetail(getOriginalHub().getMasterHub()) == HubDetailDelegate.getLinkInfoFromMasterToDetail(getPlatformCampaigns())) {
		if (hub1 == null || hub2 == null) {
			return false;
		}

		Hub h1 = hub1.getMasterHub();
		if (h1 == null) {
			return false;
		}
		OALinkInfo li1 = HubDetailDelegate.getLinkInfoFromMasterToDetail(h1);
		if (li1 == null) {
			return false;
		}

		OALinkInfo li2 = HubDetailDelegate.getLinkInfoFromMasterToDetail(hub2);
		if (li2 == null) {
			return false;
		}

		return li1 == li2;
	}

	/**
	 * Resolves the link information from the master hub or master object
	 * to this detail hub. Searches shared hubs, link metadata, and registered
	 * HubDetail entries.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the master-to-detail link information, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterToDetail(Hub thisDetailHub) {
		if (thisDetailHub == null) {
			return null;
		}
		Hub h = HubShareDelegate.getMainSharedHub(thisDetailHub);

		if (h == null) {
			h = getHubWithMasterObject(thisDetailHub);
			if (h == null) {
				return null;
			}
		}

		thisDetailHub = h;

		Hub hubMaster = thisDetailHub.datam.getMasterHub();
		OAObject master = thisDetailHub.datam.getMasterObject();

		if (thisDetailHub.datam.liDetailToMaster != null) {
			OALinkInfo li = thisDetailHub.datam.liDetailToMaster.getReverseLinkInfo();
			if (li != null) {
				if (master == null) {
					return li;
				}
				if (hubMaster == null) {
					return li;
				}

				if (hubMaster.getObjectClass().equals(master.getClass())) {
					return li;
				}
			}
		} else if (master != null) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(master.getClass());
			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, master, thisDetailHub);
			if (li != null) {
				return li;
			}
		}

		// see if it can be found using detailHub info
		if (hubMaster == null) {
			return null;
		}
		int x = hubMaster.datau.getVecHubDetail() == null ? 0 : hubMaster.datau.getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) hubMaster.datau.getVecHubDetail().elementAt(i);
			if (hd.hubDetail == thisDetailHub) {
				OALinkInfo li = hd.liMasterToDetail;
				if (li != null) {
					return li;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the link information from the master object to this detail hub.
	 * Searches master hubs, shared hubs, and HubDetail records to locate the
	 * appropriate link metadata.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the link info from master object to detail, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub thisDetailHub) {

		// 20181231 needs to also check copied hubs
		Hub h = getHubWithMasterHub(thisDetailHub);

		if (h == null) {
			h = HubShareDelegate.getMainSharedHub(thisDetailHub);
		}

		if (h == null) {
			h = getHubWithMasterObject(thisDetailHub);
			if (h == null) {
				return null;
			}
		}

		thisDetailHub = h;
		if (thisDetailHub.datam.liDetailToMaster != null) {
			OALinkInfo li = thisDetailHub.datam.liDetailToMaster.getReverseLinkInfo();
			if (li != null) {
				return li;
			}
		}

		OAObject master = thisDetailHub.datam.getMasterObject();
		if (master != null) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(master.getClass());
			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, master, thisDetailHub);
			if (li != null) {
				return li;
			}
		}

		Hub hubMaster = thisDetailHub.datam.getMasterHub();

		// see if it can be found using detailHub info
		if (hubMaster == null) {
			return null;
		}

		int x = hubMaster.datau.getVecHubDetail() == null ? 0 : hubMaster.datau.getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) hubMaster.datau.getVecHubDetail().elementAt(i);
			if (hd.hubDetail == thisDetailHub) {
				OALinkInfo li = hd.liMasterToDetail;
				if (li != null) {
					return li;
				}
			}
		}
		return null;
	}

	/**
	 * Builds a dot-separated property path representing the sequence of
	 * detail-to-master relationships from this hub upward through its
	 * master hierarchy.
	 *
	 * @param thisHub the starting hub
	 * @return the property path to all masters, or an empty string if none
	 */
	public static String getPropertyPathToMasters(Hub thisHub) {
		if (thisHub == null) {
			return null;
		}

		String pp = "";
		Hub h = thisHub;

		for (;;) {
			String s = getPropertyFromDetailToMaster(h);
			if (OAString.isEmpty(s)) {
				break;
			}
			pp = OAString.concat(pp, "", ".");
			h = h.getMasterHub();
			if (h == null) {
				break;
			}
		}
		return pp;
	}

	/**
	 * Returns the property name on the detail object that refers to the
	 * master object, based on the hub’s detail-to-master link information.
	 *
	 * @param thisHub the detail hub
	 * @return the detail-to-master property name, or null if unavailable
	 */
	public static String getPropertyFromDetailToMaster(Hub thisHub) {
		Hub h = getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return null;
			}
		}
		thisHub = h;
		if (thisHub.datam.liDetailToMaster != null) {
			return thisHub.datam.liDetailToMaster.getName();
		}
		return null;
	}

	/**
	 * Returns whether this hub represents an owned relationship, determined
	 * by evaluating the reverse link info of its detail-to-master link.
	 *
	 * @param thisHub the hub to evaluate
	 * @return true if the detail objects are owned by the master, otherwise false
	 */
	public static boolean isOwned(Hub thisHub) {
		Hub h = getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return false;
			}
		}
		thisHub = h;
		HubDataMaster dm = thisHub.datam;
		if (dm.getMasterObject() != null && dm.liDetailToMaster != null) {
			OALinkInfo li = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster);
			if (li != null) {
				return li.getOwner();
			}
		}
		return false;
	}

	/**
	 * Returns the actual hub instance that should be used based on the
	 * current master object’s property value. If the master object’s
	 * detail property points to a different hub, that hub is returned.
	 *
	 * @param thisHub the hub to resolve
	 * @return the appropriate hub instance
	 */
	public static Hub getRealHub(Hub thisHub) {
		Hub hubMaster = HubDetailDelegate.getMasterHub(thisHub);
		if (hubMaster == null) {
			return thisHub;
		}

		Hub h = thisHub;
		OAObject o = HubDetailDelegate.getMasterObject(thisHub);
		if (o != null && o != hubMaster.getAO()) {
			h = (Hub) OAObjectReflectDelegate.getProperty(o, getPropertyFromMasterToDetail(hubMaster));
			if (h == null) {
				h = thisHub; // should not happen
			}
		}
		return h;
	}

	/*20180305 was:   not sure why this was
	public static Hub getRealHub(Hub thisHub) {
	    return _getRealHub(thisHub, 0);
	}
	public static Hub _getRealHub(Hub thisHub, int cnt) {
	    Hub hubMaster = HubDetailDelegate.getMasterHub(thisHub);
	    if (hubMaster == null) return thisHub;
	
	    if (cnt > 10) {
	        LOG.log(Level.WARNING, "", new Exception("possible stackoverflow, thisHub="+thisHub+", masterHub="+hubMaster));
	    }
	    else {
	        hubMaster = _getRealHub(hubMaster, cnt+1);
	    }
	
	    Hub h = thisHub;
	    OAObject o = HubDetailDelegate.getMasterObject(thisHub);
	    if (o != null && o != hubMaster.getAO()) {
	        h = (Hub) OAObjectReflectDelegate.getProperty(o, getPropertyFromMasterToDetail(hubMaster));
	        if (h == null) {
	            h = thisHub; // should not happen
	        }
	    }
	    return h;
	}
	*/

	/**
	 * Returns whether this hub has any registered detail hubs.
	 *
	 * @param thisHub the hub to inspect
	 * @return true if detail hubs are present, otherwise false
	 */
	public static boolean hasDetailHubs(Hub thisHub) {
		if (thisHub == null || thisHub.datau == null) {
			return false;
		}
		return thisHub.datau.getVecHubDetail() != null && thisHub.datau.getVecHubDetail().size() > 0;
	}

	/**
	 * 20111008 finish if/when needed public static HubDetail getHubDetail(Hub hubDetail) { Hub hubMaster = hubDetail.getMasterHub();
	 * Vector<HubDetail> vec = hubMaster.datau.vecHubDetail; if (vec == null) return null; for (HubDetail hd : vec) { if (hd.) } }
	 */

}
