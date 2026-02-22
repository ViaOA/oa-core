package com.viaoa.graph.service.hub;

import com.viaoa.annotation.OAParentProvided;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

public abstract class HubDetailService {
	private final Logger LOG = Logger.getLogger(HubDetailService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubDetailService(Hub.FriendAccess faHub ) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}
	
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
	@SuppressWarnings("unchecked")
	public <T extends OAObject, U extends OAObject> void setMasterHub(Hub<T> thisHub, Hub<U> masterHub, String path, boolean bShared, String selectOrder) {
		final HubDataMaster hdm = faHub.getHubDataMaster(thisHub);
		final HubDataUnique<?> hdu = faHub.getHubDataUnique(thisHub);
		
		if (hdu.getSharedHub() != null) {
			if (masterHub == null) {
				throw new RuntimeException("sharedHub cant have a master hub");
			}
		}

		if (hdm.getMasterHub() != null) {
			// this will set all props back to default values
			hdm.getMasterHub().removeDetailHub(thisHub);
		}

		if (masterHub != null) {
			getDetailHub(masterHub, path, null, (Class<T>) thisHub.getObjectClass(), thisHub, bShared, selectOrder);
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
	public boolean isRecursiveMasterDetail(Hub<?> thisHub) {
		if (thisHub == null) {
			return false;
		}

		final HubDataMaster hdm = faHub.getHubDataMaster(thisHub);
		
		OALinkInfo li = hdm.getDetailToMasterLinkInfo();
		if (li == null) {
			HubDataMaster dm = getDataMaster(thisHub);
			if (dm == null) {
				return false;
			}
			li = dm.getDetailToMasterLinkInfo();
			if (li == null) {
				return false;
			}
		}

		li = callObjectInfoGetReverseLinkInfo(li);
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
	@SuppressWarnings({ "unchecked"})
	public <T extends OAObject, U extends OAObject> boolean setMasterHubActiveObject(Hub<T> thisHub, T detailObject, boolean bUpdateLink) {
		// make sure none of these have a linkHub
		// and find the sharedHub that has a masterHub
		HubDataMaster dm = getDataMaster(thisHub);
		boolean result = false;
		if (dm.getMasterHub() != null && dm.getDetailToMasterLinkInfo() != null) {
			if (dm.getDetailToMasterLinkInfo().getType() == OALinkInfo.MANY) {
				OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
				if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
					// Many2Many link
					Hub h = (Hub) callObjectReflectGetProperty(detailObject, dm.getDetailToMasterLinkInfo().getName());
					dm.getMasterHub().setSharedHub(h, false);
					callHubAOSetActiveObject(dm.getMasterHub(), 0, false, false, false); // pick any one, so that detailObject will be in it.
					return true;
				}
			}
			Object obj = callObjectReflectGetProperty(detailObject, dm.getDetailToMasterLinkInfo().getName());
			// 20121010 if obj==null then dont adjust:  ex: hi5  employeeAward.awardType that was from program.awardTypes, and now the list is in location.awardTpes
			if (obj != null && dm.getMasterHub().getActiveObject() != obj && !(obj instanceof Hub)) {
				//was: if (dm.masterHub.getActiveObject() != obj) {
				
				if (faHub.getHubDataUnique(dm.getMasterHub()).isUpdatingActiveObject()) {
					return false;
					// see if masterHub (or a share of it) has a link
					//  if it does, then dont allow it to adjustMaster
				}

				if (callThreadLocalGetCanAdjustHub(dm.getMasterHub())) {
					callHubAOSetActiveObject( (Hub<U>) dm.getMasterHub(), (U) obj, true, bUpdateLink, false); // adjustMaster, updateLink, force
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
	public <T extends OAObject> void setPropertyToMasterHub(Hub<T> thisHub, T detailObject, OAObject objMaster) {
		if (thisHub == null || detailObject == null) {
			return;
		}
		if (!(detailObject instanceof OAObject)) {
			return;
		}

		// 20160920 this needs to run even if loading.
		///   ex: using copy, loading from xml, etc
		// if (OARuntime.threadService().isLoading()) return;

		HubDataMaster dm;
		if (objMaster != null) {
			dm = getDataMaster(thisHub, objMaster.getClass(), false);
		} else {
			dm = faHub.getHubDataMaster(thisHub);
			if (dm == null) {
				return;
			}
		}
		if (dm.getDetailToMasterLinkInfo() == null) {
			return;
		}

		// 20120920 if thisHub is a detailHub of type=One, then need to update the masterObj.linkProp
		OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
		if (liRev != null && liRev.getType() == OALinkInfo.ONE) {
			if (objMaster == null) {
				// remove was called
				OAObject objx = getMasterObject(thisHub);
				if (objx != null) {
					callObjectReflectSetProperty(objx, liRev.getName(), null, null);
				}
			} else {
				// add was called
				callObjectReflectSetProperty(objMaster, liRev.getName(), detailObject, null);
				// the AO will also be set, and thisHub.datau.dupAllowAddRemove = false;
			}
		}

		Method method = callObjectInfoGetMethod(thisHub.getObjectClass(), "get" + dm.getDetailToMasterLinkInfo().getName());
		if (method == null) {
			// LOG.warning("liDetailToMaster invalid, method not found, hub="+thisHub+", method=get"+dm.getDetailToMasterLinkInfo().getName());
			return;
		}

		if (Hub.class.isAssignableFrom(method.getReturnType())) {
			//if (detailObject instanceof OAObjectKey) return;

			// 20140616 if hub is not loaded and isClient, then dont need to load
			if (!callSyncIsServer()) {
				if (!callObjectReflectIsReferenceHubLoaded((OAObject) detailObject, dm.getDetailToMasterLinkInfo().getName())) {
					return;
				}
			}

			Object obj = callObjectReflectGetProperty((OAObject) detailObject, dm.getDetailToMasterLinkInfo().getName());
			if (objMaster == null) { // remove
				
				final HubDataMaster hdm = faHub.getHubDataMaster(thisHub);
				
				if (hdm.getMasterObject() != null) {
					objMaster = hdm.getMasterObject();
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
			method = callObjectInfoGetMethod(thisHub.getObjectClass(), "set" + dm.getDetailToMasterLinkInfo().getName());
			if (method == null) {
				// LOG.warning("liDetailToMaster invalid, method not found, hub="+thisHub+", method=set"+dm.getDetailToMasterLinkInfo().getName());
				return;
			}
			Object currentValue = callObjectReflectGetProperty((OAObject) detailObject, dm.getDetailToMasterLinkInfo().getName());
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

			callObjectReflectSetProperty((OAObject) detailObject, dm.getDetailToMasterLinkInfo().getName(), objMaster, null);
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
	public void updateAllDetail(Hub<?> thisHub, boolean bUpdateLink) {
		final HubDataUnique<?> hdu = faHub.getHubDataUnique(thisHub);
		int x = hdu.getVecHubDetail() == null ? 0 : hdu.getVecHubDetail().size();
		// get objects that go with detail hub
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) hdu.getVecHubDetail().elementAt(i);
			
			Hub<?> h = hd.getDetailHub();
			if (h == null) {
				hdu.getVecHubDetail().removeElementAt(i);
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
	public void preloadDetailData(final Hub<?> thisHub, final int pos) {
		if (thisHub == null || pos < 0) {
			return;
		}
		final HubDataUnique hdu = faHub.getHubDataUnique(thisHub);
		int x = hdu.getVecHubDetail() == null ? 0 : hdu.getVecHubDetail().size();

		Object objMaster = thisHub.getAt(pos);
		if (objMaster == null) {
			return;
		}

		// get objects that go with detail hub
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) hdu.getVecHubDetail().elementAt(i);
			Hub<?> h = hd.getDetailHub();
			if (h == null) {
				hdu.getVecHubDetail().removeElementAt(i);
				x--;
				i--;
				continue;
			}
			
			callObjectReflectGetProperty((OAObject) faHub.getHubDataActive(thisHub).getActiveObject(), hd.getMasterToDetailLinkInfo().getName());
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
	public <T extends OAObject, U extends OAObject> void updateDetail(final Hub<T> thisHub, final HubDetail detail, final Hub<U> detailHub, final boolean bUpdateLink) {
		/* get Hub, Object, OAObject or Array value from property
		   ex:  Emp
		          String name;
		          Dept[] depts;  or
		          Hub depts;     or
		          Dept dept;
		   then add to dHub.vector
		*/
		if (detail == null || detail.getType() == detail.HUBMERGER) {
			return;
		}

		if (detail.getIgnoreUpdate()) { // set by hubDetail.setup()
			// this is called by hubListener in HubDetail, to make sure that the detailHub is "reconnected" to the masterHub.
			//    it can get disconnected when it is changed to point (shared) to a child hub.
			// in case detailHub was set/shared to a recursive child hub.  This will set it back to be off of the masterHub (thisHub)
			if (faHub.getHubDataMaster(detailHub).getMasterObject() == (OAObject) faHub.getHubDataActive(thisHub).getActiveObject()) {
				// 20160204
				if (faHub.getHubDataMaster(detailHub).getMasterHub() != thisHub) {
					Hub<?> hx = faHub.getHubDataUnique(detailHub).getSharedHub();
					boolean b = (hx != null && faHub.getHubDataMaster(hx) == faHub.getHubDataMaster(detailHub)); // this happens by setting sharedHub - when it was sharing with a child hub
					if (b) {
						// this will reconnect detailHub to the masterHub (thisHub)
						
						HubDataMaster dm = new HubDataMaster();
						faHub.setHubDataMaster(detailHub, dm);
						
						dm.setDetailToMasterLinkInfo(callObjectInfoGetReverseLinkInfo(detail.getMasterToDetailLinkInfo()));
						dm.setMasterHub(thisHub);
						dm.setMasterObject((OAObject) faHub.getHubDataActive(thisHub).getActiveObject());
						
						callHubShareSyncSharedHubs(detailHub, detail.getShareActiveObject(), faHub.getHubDataActive(detailHub), faHub.getHubDataActive(hx), bUpdateLink);
					}
				}

				/*was
				detailHub.datam.getDetailToMasterLinkInfo() = callObjectINfoGetReverseLinkInfo(detail.liMasterToDetail);
				detailHub.datam.masterHub = thisHub;
				*/
			}
			return;
		}

		HubDataUnique hdu = faHub.getHubDataUnique(detailHub);
		if (hdu.getSharedHub() != null) {
			if (faHub.getHubDataMaster(hdu.getSharedHub()) == faHub.getHubDataMaster(detailHub)) {
				faHub.setHubDataMaster(detailHub, new HubDataMaster());
			}
		}
		faHub.getHubDataMaster(detailHub).setMasterObject((OAObject) faHub.getHubDataActive(thisHub).getActiveObject());
		faHub.getHubDataMaster(detailHub).setDetailToMasterLinkInfo( callObjectInfoGetReverseLinkInfo(detail.getMasterToDetailLinkInfo()));
		faHub.getHubDataMaster(detailHub).setMasterHub(thisHub);

		Object obj = null; // reference property
		try {
			if (faHub.getHubDataActive(thisHub).getActiveObject() == null) {
				obj = null;
			} else {
				obj = callObjectReflectGetProperty((OAObject) faHub.getHubDataActive(thisHub).getActiveObject(), detail.getMasterToDetailLinkInfo().getName());
			}
		} catch (Exception e) {
			throw new RuntimeException("error calling get method for master to detail: " + detail.getMasterToDetailLinkInfo().getName());
		}

		boolean wasShared = false;
		if (detail.getType() == HubDetail.HUB) {
			if (faHub.getHubDataUnique(detailHub).getSharedHub() != null) {
				callHubShareRemoveSharedHub(faHub.getHubDataUnique(detailHub).getSharedHub(), detailHub);
				faHub.getHubDataUnique(detailHub).setSharedHub(null);
				wasShared = true;
			}
		} else {
			// see if the detail list needs changed
			if (obj == faHub.getHubDataActive(detailHub).getActiveObject()) {
				// 20120720 need to send newList event, in case master object was previously null
				if (obj == null) {
					callHubEventFireOnNewListEvent(detailHub, false); // notifies all of this hub's shared hubs
				}
				return;
			}

			for (int i = 0;; i++) {
				Object objx = callHubDataGetObjectAt(detailHub, i);
				if (objx == null) {
					break;
				}
				callObjectHubRemoveHub((OAObject) objx, (Hub<OAObject>) detailHub, true); // 20160713 changed to true so that it wont add to server cache
			}

			faHub.getHubData(detailHub).getVector().removeAllElements();
		}

		faHub.getHubData(detailHub).setDupAllowAddRemove(true);
		if (obj == null) {
			HubDataActive daOld = faHub.getHubDataActive(detailHub);

			if (wasShared) {
				// have to create its own since it might have been sharing the current one
				faHub.setHubData(detailHub, new HubData(faHub.getHubData(detailHub).getObjClass()));
				if (detail.getShareActiveObject()) {
					faHub.setHubDataActive(detailHub, new HubDataActive());
				}
			}
			faHub.getHubData(detailHub).setDupAllowAddRemove(false); // 2004/08/23
			//was: if (detail.type != HubDetail.HUB) dHub.datau.dupAllowAddRemove = false;
			callHubShareSyncSharedHubs(detailHub, true, daOld, faHub.getHubDataActive(detailHub), bUpdateLink);
		} else if (detail.getType() == HubDetail.HUB) { // Hub
			// share oaObject info and activeObject info
			// dont share listeners and links ("datau")
			// dont share activeObject ("dataa")
			//     unless DetailHub.bShareActiveObject is true then set it after events
			Hub<U> h = (Hub<U>) obj;

			if (callHubSortIsSorted(detailHub)) {
				String s = callHubSortGetSortProperty(detailHub);
				if (s != null) {
					String s2 = callHubSortGetSortProperty(h);
					if (!OAString.equals(s, s2, true)) {
						boolean b = callHubSortGetSortAsc(detailHub);
						h.sort(s, b);
					}
				}
			}

			// need to select before assigning to detail hub so that add events wont
			//            be sent to detail hubs listeners
			faHub.setHubData(detailHub, faHub.getHubData(h));
			faHub.getHubDataUnique(detailHub).setSharedHub(h);
			callHubShareAddSharedHub(h, detailHub);

			// 20120926 "h" could be a shared/calc Hub.
			// 20160204 this can happen for recursive, where the detailHub is pointing/shared to a childHub.
			//     this will reconnect it to the parent
			if (faHub.getHubDataMaster(detailHub).getMasterObject() != (OAObject) faHub.getHubDataMaster(h).getMasterObject()) {
				Hub<?> hx = faHub.getHubDataUnique(detailHub).getSharedHub();
				if (hx != null && faHub.getHubDataMaster(hx) == faHub.getHubDataMaster(detailHub)) {
					faHub.setHubDataMaster(detailHub, new HubDataMaster());
				}

				if (faHub.getHubDataMaster(h).getMasterObject() != null) {
					faHub.getHubDataMaster(detailHub).setMasterObject((OAObject) faHub.getHubDataMaster(h).getMasterObject());
				}
				if (faHub.getHubDataMaster(h).getDetailToMasterLinkInfo() != null) {
					faHub.getHubDataMaster(detailHub).setDetailToMasterLinkInfo(faHub.getHubDataMaster(h).getDetailToMasterLinkInfo());
				}

				// 20160204
				faHub.getHubDataMaster(detailHub).setMasterHub(detail.getHubMaster());
				//was: detailHub.datam.masterHub = h.datam.masterHub;
			}
			callHubShareSyncSharedHubs(detailHub, detail.getShareActiveObject(), faHub.getHubDataActive(detailHub), faHub.getHubDataActive(h), bUpdateLink);

			if (faHub.getHubDataMaster(detailHub).getMasterObject() != null && faHub.getHubDataMaster(h).getMasterObject() == null) {
				setMasterObject(h, faHub.getHubDataMaster(detailHub).getMasterObject(), faHub.getHubDataMaster(detailHub).getDetailToMasterLinkInfo());
			}
		} else if (detail.getType() == HubDetail.OAOBJECT || detail.getType() == HubDetail.OBJECT) {
			callHubAddRemoveInternalAdd(detailHub, (U) obj, false, true);
			faHub.getHubData(detailHub).setDupAllowAddRemove(false);
		} else {
			// HubDetail.OBJECTARRAY || HubDetail.OAOBJECTARRAY
			int j = Array.getLength(obj);
			for (int k = 0; k < j; k++) {
				Object objx = Array.get(obj, k);
				callHubAddRemoveInternalAdd(detailHub, (U) objx, false, true);
			}
			faHub.getHubData(detailHub).setDupAllowAddRemove(false);
		}

		callHubDataIncChangeCount(detailHub);
		OAObject aoHold = faHub.getHubDataActive(detailHub).getActiveObject();
		HubData hd = faHub.getHubData(detailHub);
		faHub.getHubDataActive(detailHub).setActiveObject(null);
		
		callHubEventFireOnNewListEvent(detailHub, false); // notifies all of this hub's shared hubs
		if (faHub.getHubData(detailHub) == hd && faHub.getHubDataActive(detailHub).getActiveObject() == null) {
			faHub.getHubDataActive(detailHub).setActiveObject(aoHold);
		}

		// 20140421 moved to after newList
		updateDetailActiveObject(detailHub, detailHub, bUpdateLink, detail.getShareActiveObject());

		if (detail.getType() == HubDetail.OAOBJECT || detail.getType() == HubDetail.OBJECT) {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends OAObject> void updateDetailActiveObject(final Hub<T> thisHub, final Hub<?> hubDetailHub, final boolean bUpdateLink,
			final boolean bShareActiveObject) {
		boolean bUseCurrent = (bShareActiveObject && faHub.getHubDataActive(thisHub) == faHub.getHubDataActive(hubDetailHub)); // if hubs are sharing active object then dont change it.
		if (!bUseCurrent || (thisHub == hubDetailHub)) {
			Hub<?> hubWithLink = callHubLinkGetHubWithLink(thisHub, true);

			if (hubWithLink == null) {
				// if there is not a linkHub, then go to default object
				int pos;
				if (bUseCurrent) {
					pos = thisHub.getPos();
				} else {
					pos = faHub.getHubDataUnique(thisHub).getDefaultPos(); // default is -1
				}
				callHubAOSetActiveObject(thisHub, pos, bUpdateLink, true, false); // bForce=true,bCalledByShareHub=false
			} else if (bUpdateLink) {
				int pos;
				if (bUseCurrent) {
					pos = thisHub.getPos();
				} else {
					pos = -1;
				}
				callHubAOSetActiveObject(thisHub, pos, bUpdateLink, true, false); // bForce=true, this will recursivly notify this links HubDetails
			} else {
				// if linkHub & !bUpdateLink, then retreive value from linked property
				// and make that the activeObject in this Hub
				try {
					Object obj = faHub.getHubDataUnique(hubWithLink).getLinkToHub().getActiveObject();
					if (obj != null) {
						obj = faHub.getHubDataUnique(hubWithLink).getLinkToGetMethod().invoke(obj, (Object[]) null);
					}
					if (faHub.getHubDataUnique(hubWithLink).isLinkPos()) {
						int x = -1;
						if (obj != null && obj instanceof Number) {
							x = ((Number) obj).intValue();
						}
						if (thisHub.getPos() != x) {
							callHubAOSetActiveObject(thisHub, thisHub.elementAt(x), x, bUpdateLink, false, false);//bUpdateLink,bForce,bCalledByShareHub
						}
					} else if (faHub.getHubDataUnique(hubWithLink).getLinkFromPropertyName() != null) { // 20110116 ex: Breed.name linked to Pet.breed (string)
						Object objx;
						if (obj != null) {
							objx = hubWithLink.find(faHub.getHubDataUnique(hubWithLink).getLinkFromPropertyName(), obj);
						} else {
							objx = null;
						}
						callHubAOSetActiveObject(thisHub, (T) objx, bUpdateLink, false, false);
					} else {
						int pos = thisHub.getPos(obj);
						if (obj != null && pos < 0) {
							obj = null;
						}
						callHubAOSetActiveObject(thisHub, (T) obj, pos, bUpdateLink, false, false);//bUpdateLink,bForce,bCalledByShareHub
					}
				} catch (Exception e) {
					throw new RuntimeException(faHub.getHubDataUnique(thisHub).getLinkToGetMethod().getName(), e); // wrap orig exception
				}
			}
		}

		// 20120715
		WeakReference<Hub<T>>[] refs = callHubShareGetSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub<T>> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub<?> h2 = ref.get();
			if (h2 == null) {
				continue;
			}

			// only update sharedHubs with diff dataa, setActiveObject will do others
			if (faHub.getHubDataActive(h2) != faHub.getHubDataActive(hubDetailHub)) {
				updateDetailActiveObject(h2, hubDetailHub, false, bShareActiveObject); // dont update link properties
			}
		}

		/* was
		Hub[] hubs = srvcHub.getHubShareService().getSharedHubs(thisHub);
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
	public HubDataMaster getDataMaster(final Hub<?> thisHub) {
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
	public HubDataMaster getDataMaster(final Hub<?> thisHub, boolean bIncludedFilteredHub) {
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
	private HubDataMaster getDataMaster(final Hub<?> thisHub, final Class<? extends OAObject> masterClass, boolean bIncludedFilteredHub) {
		if (thisHub == null) {
			return null;
		}

		if (faHub.getHubDataMaster(thisHub).getMasterHub() != null) {
			return faHub.getHubDataMaster(thisHub);
		}

		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				if (faHub.getHubDataMaster(h).getMasterHub() != null) {
					if (masterClass == null || masterClass.equals(faHub.getHubDataMaster(h).getMasterHub().getObjectClass())) {
						return true;
					}
				}
				return false;
			}
		};
		Hub<?> hubx = callHubShareGetFirstSharedHub(thisHub, filter, bIncludedFilteredHub, false);
		if (hubx != null) {
			return faHub.getHubDataMaster(hubx);
		}
		return faHub.getHubDataMaster(thisHub);
	}

	/**
	 * Returns this hub or a shared hub that has a master hub defined. Searches
	 * the hub and its shared hubs until one with a non-null master hub is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master hub, or null if none exists
	 */
	public <T extends OAObject> Hub<T> getHubWithMasterHub(final Hub<T> thisHub) {
		if (thisHub == null) {
			return null;
		}
		if (faHub.getHubDataMaster(thisHub).getMasterHub() != null) {
			return thisHub;
		}

		OAFilter<Hub> filter = new OAFilter<>() {
			@Override
			public boolean isUsed(Hub h) {
				if (faHub.getHubDataMaster(h).getMasterHub() != null) {
					// 20130916 make sure it has the same masterObject
					//    since it could be a recursive hub, that points
					//    to the root hub, and not just it's parent
					return true;
				}
				return false;
			}
		};
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, filter, true, false);
		return hubx;
	}

	/**
	 * Returns this hub or a shared hub that has a master object defined. Searches
	 * the hub and its shared hubs until one with a non-null master object is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master object, or null if none exists
	 */
	public <T extends OAObject> Hub<T> getHubWithMasterObject(final Hub<T> thisHub) {
		if (faHub.getHubDataMaster(thisHub) == null) {
			return null; // could be deserializing and not fully loaded
		}
		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			return thisHub;
		}

		OAFilter<Hub> filter = new OAFilter<>() {
			@Override
			public boolean isUsed(Hub h) {
				if (faHub.getHubDataMaster(h).getMasterHub() != null) {
					// 20130916 make sure it has the same masterObject
					//    since it could be a recursive hub, that points
					//    to the root hub, and not just it's parent
					if (faHub.getHubDataMaster(h).getMasterObject() != null) {
						return true;
					}
				}
				return false;
			}
		};
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, filter, true, false);
		return hubx;
	}

	/**
	 * Returns the master hub for this hub or any shared hub that carries
	 * master-hub metadata.
	 *
	 * @param thisHub the hub whose master hub is requested
	 * @return the master hub, or null if none exists
	 */
	public Hub<?> getMasterHub(Hub<?> thisHub) {
		Hub<?> h = getHubWithMasterHub(thisHub);
		if (h != null) {
			h = faHub.getHubDataMaster(h).getMasterHub();
		}
		return h;
	}

	/**
	 * Returns the master object associated with this hub or a shared hub.
	 *
	 * @param thisHub the hub whose master object is requested
	 * @return the master object, or null if not defined
	 */
	public OAObject getMasterObject(Hub<?> thisHub) {
		thisHub = getHubWithMasterObject(thisHub);
		if (thisHub == null) {
			return null;
		}
		return faHub.getHubDataMaster(thisHub).getMasterObject();
	}

	/**
	 * Returns the class of the master object or master hub associated with
	 * this hub. If none is found, returns null.
	 *
	 * @param thisHub the hub whose master class is requested
	 * @return the master class, or null if unavailable
	 */
	public Class<? extends OAObject> getMasterClass(Hub<?> thisHub) {
		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			return faHub.getHubDataMaster(thisHub).getMasterObject().getClass();
		}
		if (faHub.getHubDataMaster(thisHub).getMasterHub() != null) {
			return faHub.getHubDataMaster(thisHub).getMasterHub().getObjectClass();
		}
		Hub<?> h = getHubWithMasterObject(thisHub);
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
	public Hub<?> getDetailHub(Hub<?> thisHub, Class[] clazz) {
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
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> thisHub, Class<T> clazz, boolean bShareActive, String selectOrder) {
		return getDetailHub(thisHub, null, new Class[] { clazz }, clazz, null, bShareActive, selectOrder);
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
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> thisHub, String path, Class<T> objectClass, boolean bShareActive) {
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
	public Hub<?> getDetailHub(Hub<?> thisHub, String path) {
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
	public Hub<?> getDetailHub(Hub<?> thisHub, String path, String selectOrder) {
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
	public Hub<?> getDetailHub(Hub<?> thisHub, String path, boolean bShareActive) {
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
	public Hub<?> getDetailHub(Hub<?> thisHub, String path, boolean bShareActive, String selectOrder) {
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
	public <T extends OAObject> Hub<T> getDetailHub(final Hub<?> thisHub, String path, Class<? extends OAObject>[] classes, Class<T> lastClass, Hub<T> detailHub, boolean bShareActive, String selectOrder) {
		// linkHub is Hub that is the detail hub, it is supplied by setMaster()
		// lastClass can be the class to use for the last class in the path

		if (path != null && path.length() > 0 && faHub.getHubData(thisHub).getObjClass() == null) {
			return null;
		}

		if (path == null) {
			Class[] c = classes;
			if (c == null && lastClass != null) {
				c = new Class[1];
				c[0] = lastClass;
			}
			if (c != null) {
				path = callHubGetPropertyPathforClasses(thisHub, c);
			}
			if (path == null) {
				throw new RuntimeException("cant find path.");
			}
		} else if (path.length() == 0) {
			return (Hub<T>) thisHub; // since this is a recursive method
		}

		// support for using HubMerger if property path has more then one ending object/hub
		Class<? extends OAObject> clazz = thisHub.getObjectClass();

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
				int x = faHub.getHubDataUnique(thisHub).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(thisHub).getVecHubDetail().size();
				for (int i = 0; i < x; i++) {
					hd = (HubDetail) faHub.getHubDataUnique(thisHub).getVecHubDetail().elementAt(i);
					if (hd.getType() == hd.HUBMERGER && path.equalsIgnoreCase(hd.getPath())) {
						hd.incrementReferenceCount();
						return hd.getHubDetail();
					}
				}
			}

			if (detailHub == null) {
				detailHub = new Hub(clazz);
			}

			HubMerger hm = new HubMerger(thisHub, detailHub, path,
					bShareActive, selectOrder, false);

			HubDetail hd = new HubDetail(path, detailHub);
			hd.setReferenceCount(1);
			if (faHub.getHubDataUnique(thisHub).getVecHubDetail() == null) {
				faHub.getHubDataUnique(thisHub).setVecHubDetail(new Vector<HubDetail>(3, 5));
			}
			faHub.getHubDataUnique(thisHub).getVecHubDetail().addElement(hd);

			return detailHub;
		}

		final String propertyName = ppx.getProperties()[0];
		final Class newClass = ppx.getClasses()[0];


		// get LinkInfo
		final OALinkInfo linkInfo = callObjectInfoGetLinkInfo(faHub.getHubData(thisHub).getObjectInfo(), propertyName);
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
		Hub<?> hub = null;
		HubDetail hd = null;
		int x = faHub.getHubDataUnique(thisHub).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(thisHub).getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			hd = (HubDetail) faHub.getHubDataUnique(thisHub).getVecHubDetail().elementAt(i);
			if (hd.getMasterToDetailLinkInfo() != null && hd.getMasterToDetailLinkInfo().equals(linkInfo) && hd.getDetailHub() != null) {
				if (detailHub == null || detailHub == hd.getHubDetail()) {
					hub = hd.getHubDetail();
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
			if (faHub.getHubDataUnique(thisHub).getVecHubDetail() == null) {
				faHub.getHubDataUnique(thisHub).setVecHubDetail(new Vector(3, 5));
			}
			faHub.getHubDataUnique(thisHub).getVecHubDetail().addElement(hd);
		} else {
			bFound = true;
		}

		if (pos < 0 && bShareActive) {
			hd.setShareActiveObject(true);
		}

		if (pos < 0) {
			if (detailHub != null) {
				if (detailHub.getObjectClass() == null) {
					callHubGetObjectClass(detailHub, newClass);
				}
				if (hub != null && !hub.getObjectClass().equals(detailHub.getObjectClass())) {
					if (!hub.getObjectClass().isAssignableFrom(detailHub.getObjectClass())) {
						throw new RuntimeException("ObjectClass is different, hub=" + hub + ", path=" + path);
					}
				}
				hub = detailHub;
				hd.setHubDetail(hub);
			}
			if (selectOrder != null) {
				hub.setSelectOrder(selectOrder);
			}
			hd.incrementReferenceCount();

			path = "";
		} else {
			path = path.substring(pos + 1);
		}
		faHub.getHubDataMaster(hub).setMasterHub(thisHub);

		if (type == HubDetail.OAOBJECT || type == HubDetail.OBJECT) {
			faHub.getHubDataUnique(hub).setDefaultPos(0);

			if (type == HubDetail.OAOBJECT && linkInfo.getCalculated() && linkInfo.getCalcDependentProperties() != null
					&& linkInfo.getCalcDependentProperties().length > 0) {
				// need to use a hub listener if it's a calculated link that has dependent PPs
				thisHub.addHubListener(new HubListenerAdapter() {
					// no-op, just need to have it generate a property change whenever a dependent prop changes so that link hub is updated.
				}, propertyName);
			}
		}

		if (!bFound) {
			updateDetail(thisHub, hd, hd.getHubDetail(), false);
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
	public void setMasterObject(Hub<?> thisHub, OAObject masterObject, OALinkInfo liDetailToMaster) {
		// OAObject needs to know which hubs are under it
		if (faHub.getHubDataMaster(thisHub) == null) {
			return; // could be deserializing and not fully loaded
		}
		faHub.getHubDataMaster(thisHub).setDetailToMasterLinkInfo(liDetailToMaster);
		if (masterObject == faHub.getHubDataMaster(thisHub).getMasterObject()) {
			return;
		}
		faHub.getHubDataMaster(thisHub).setMasterObject(masterObject);
	}

	/**
	 * Convenience wrapper that sets the master object using the hub’s
	 * existing detail-to-master link information.
	 *
	 * @param thisHub      the hub whose master object is assigned
	 * @param masterObject the master object to set
	 */
	public void setMasterObject(Hub<?> thisHub, OAObject masterObject) {
		setMasterObject(thisHub, masterObject, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo());
	}

	/**
	 * Returns the {@code OALinkInfo} that links a detail hub to its master.
	 * Searches this hub and any shared hubs that carry master metadata.
	 *
	 * @param hub the detail hub
	 * @return the detail-to-master link information, or null if not found
	 */
	public OALinkInfo getLinkInfoFromDetailToMaster(Hub<?> hub) {
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
		return faHub.getHubDataMaster(h).getDetailToMasterLinkInfo();
	}

	/**
	 * Returns true if any master hub in the hierarchy above this hub has an
	 * active object marked as new. Walks upward through master hubs or master
	 * objects until the chain terminates.
	 *
	 * @param thisHub the hub to evaluate
	 * @return true if a master active object is new, otherwise false
	 */
	public boolean isMasterNew(Hub<?> thisHub) {
		thisHub = getHubWithMasterObject(thisHub);
		if (thisHub == null) {
			return false;
		}

		Hub<?> h = thisHub;
		for (; h != null;) {
			HubDataMaster dm = getDataMaster(h, true);

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
	public <T extends OAObject, U extends OAObject> boolean removeDetailHub(Hub<T> thisHub, Hub<U> hubDetail) {
		// remove HubDetail if it does not have any more listeners or links
		if (hubDetail == thisHub) {
			return false;
		}

		int x = faHub.getHubDataUnique(thisHub).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(thisHub).getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) faHub.getHubDataUnique(thisHub).getVecHubDetail().elementAt(i);
			Hub<?> h = hd.getHubDetail();
			if (h == hubDetail) {
				hd.decrementReferenceCount();
				if (hd.getReferenceCount() <= 0) {
					if (faHub.getHubDataUnique(h).getVecHubDetail() == null || faHub.getHubDataUnique(h).getVecHubDetail().size() == 0) {
						faHub.getHubDataUnique(thisHub).getVecHubDetail().removeElementAt(i);
						faHub.setHubData(hubDetail, new HubData<U>(faHub.getHubData(hubDetail).getObjClass()));
						faHub.setHubDataMaster(hubDetail, new HubDataMaster());
						faHub.setHubDataActive(hubDetail, new HubDataActive<U>());
						return true;
					}
					hd.setReferenceCount(0);
				}
				return false;
			}
			// if not found, this will recursively look to find hub in other linked hubDetails
			if (h != null) {
				boolean b = removeDetailHub(h, hubDetail);
				if (b && hd.getReferenceCount() <= 0) {
					if (faHub.getHubDataUnique(h).getVecHubDetail() != null || faHub.getHubDataUnique(h).getVecHubDetail().size() == 0) {
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
	public <T extends OAObject> String getPropertyFromMasterToDetail(Hub<T> thisHub) {
		Hub<T> h = getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return null;
			}
		}
		thisHub = h;
		if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null) {
			String name = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getReverseName();
			if (name != null) {
				return name;
			}
		}

		OAObject master = faHub.getHubDataMaster(thisHub).getMasterObject();
		if (master != null) {
			OAObjectInfo oi = callObjectInfoGetObjectInfo(master.getClass());
			OALinkInfo li = callObjectInfoGetLinkInfo(oi, master, thisHub);
			if (li != null) {
				return li.getName();
			}
		}

		// see if it can be found using detailHub info
		Hub<?> hubMaster = faHub.getHubDataMaster(thisHub).getMasterHub();
		if (hubMaster != null) {
			int x = faHub.getHubDataUnique(hubMaster).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(hubMaster).getVecHubDetail().size();
			for (int i = 0; i < x; i++) {
				HubDetail hd = (HubDetail) faHub.getHubDataUnique(hubMaster).getVecHubDetail().elementAt(i);
				if (hd.getHubDetail() == thisHub) {
					OALinkInfo li = hd.getMasterToDetailLinkInfo();
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
	public OALinkInfo getLinkInfoFromMasterHubToDetail(Hub thisDetailHub) {
		return getLinkInfoFromMasterToDetail(thisDetailHub);
	}

	/**
	 * Determines whether a recursive one-to-many relationship is valid for
	 * this hub based on link metadata and object-class comparisons.
	 *
	 * @param hub the hub to evaluate
	 * @return true if the recursive structure is valid, otherwise false
	 */
	public boolean getIsValidRecursive(final Hub<?> hub) {
		if (hub == null) {
			return true;
		}

		OALinkInfo li = getLinkInfoFromMasterToDetail(hub);
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
			OALinkInfo lix = getLinkInfoFromMasterToDetail(hubMaster);
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
	public <T extends OAObject> boolean getIsFromSameMasterHub(Hub<T> hub1, Hub<T> hub2) {
		// if (getLinkInfoFromMasterToDetail(getOriginalHub().getMasterHub()) == getLinkInfoFromMasterToDetail(getPlatformCampaigns())) {
		if (hub1 == null || hub2 == null) {
			return false;
		}

		Hub<?> h1 = hub1.getMasterHub();
		if (h1 == null) {
			return false;
		}
		OALinkInfo li1 = getLinkInfoFromMasterToDetail(h1);
		if (li1 == null) {
			return false;
		}

		OALinkInfo li2 = getLinkInfoFromMasterToDetail(hub2);
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
	public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> thisDetailHub) {
		if (thisDetailHub == null) {
			return null;
		}
		Hub<?> h = callHubShareGetMainSharedHub(thisDetailHub);

		if (h == null) {
			h = getHubWithMasterObject(thisDetailHub);
			if (h == null) {
				return null;
			}
		}

		thisDetailHub = h;

		Hub<?> hubMaster = faHub.getHubDataMaster(thisDetailHub).getMasterHub();
		OAObject master = faHub.getHubDataMaster(thisDetailHub).getMasterObject();

		if (faHub.getHubDataMaster(thisDetailHub).getDetailToMasterLinkInfo() != null) {
			OALinkInfo li = faHub.getHubDataMaster(thisDetailHub).getDetailToMasterLinkInfo().getReverseLinkInfo();
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
			OAObjectInfo oi = callObjectInfoGetObjectInfo(master.getClass());
			OALinkInfo li = callObjectInfoGetLinkInfo(oi, master, thisDetailHub);
			if (li != null) {
				return li;
			}
		}

		// see if it can be found using detailHub info
		if (hubMaster == null) {
			return null;
		}
		int x = faHub.getHubDataUnique(hubMaster).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(hubMaster).getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) faHub.getHubDataUnique(hubMaster).getVecHubDetail().elementAt(i);
			if (hd.getHubDetail() == thisDetailHub) {
				OALinkInfo li = hd.getMasterToDetailLinkInfo();
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
	public OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub<?> thisDetailHub) {

		// 20181231 needs to also check copied hubs
		Hub<?> h = getHubWithMasterHub(thisDetailHub);

		if (h == null) {
			h = callHubShareGetMainSharedHub(thisDetailHub);
		}

		if (h == null) {
			h = getHubWithMasterObject(thisDetailHub);
			if (h == null) {
				return null;
			}
		}

		thisDetailHub = h;
		if (faHub.getHubDataMaster(thisDetailHub).getDetailToMasterLinkInfo() != null) {
			OALinkInfo li = faHub.getHubDataMaster(thisDetailHub).getDetailToMasterLinkInfo().getReverseLinkInfo();
			if (li != null) {
				return li;
			}
		}

		OAObject master = faHub.getHubDataMaster(thisDetailHub).getMasterObject();
		if (master != null) {
			OAObjectInfo oi = callObjectInfoGetObjectInfo(master.getClass());
			OALinkInfo li = callObjectInfoGetLinkInfo(oi, master, thisDetailHub);
			if (li != null) {
				return li;
			}
		}

		Hub<?> hubMaster = faHub.getHubDataMaster(thisDetailHub).getMasterHub();

		// see if it can be found using detailHub info
		if (hubMaster == null) {
			return null;
		}

		int x = faHub.getHubDataUnique(hubMaster).getVecHubDetail() == null ? 0 : faHub.getHubDataUnique(hubMaster).getVecHubDetail().size();
		for (int i = 0; i < x; i++) {
			HubDetail hd = (HubDetail) faHub.getHubDataUnique(hubMaster).getVecHubDetail().elementAt(i);
			if (hd.getHubDetail() == thisDetailHub) {
				OALinkInfo li = hd.getMasterToDetailLinkInfo();
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
	public String getPropertyPathToMasters(Hub<?> thisHub) {
		if (thisHub == null) {
			return null;
		}

		String pp = "";
		Hub<?> h = thisHub;

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
	public String getPropertyFromDetailToMaster(Hub<?> thisHub) {
		Hub<?> h = getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return null;
			}
		}
		thisHub = h;
		if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null) {
			return faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getName();
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
	public boolean isOwned(Hub thisHub) {
		Hub<?> h = getHubWithMasterHub(thisHub);
		if (h == null) {
			h = getHubWithMasterObject(thisHub);
			if (h == null) {
				return false;
			}
		}
		thisHub = h;
		HubDataMaster dm = faHub.getHubDataMaster(thisHub);
		if (dm.getMasterObject() != null && dm.getDetailToMasterLinkInfo() != null) {
			OALinkInfo li = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
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
	public <T extends OAObject> Hub<T> getRealHub(Hub<T> thisHub) {
		Hub<?> hubMaster = getMasterHub(thisHub);
		if (hubMaster == null) {
			return thisHub;
		}

		Hub<T> h = thisHub;
		OAObject o = getMasterObject(thisHub);
		if (o != null && o != hubMaster.getAO()) {
			h = (Hub<T>) callObjectReflectGetProperty(o, getPropertyFromMasterToDetail(hubMaster));
			if (h == null) {
				h = thisHub; // should not happen
			}
		}
		return h;
	}

	/**
	 * Returns whether this hub has any registered detail hubs.
	 *
	 * @param thisHub the hub to inspect
	 * @return true if detail hubs are present, otherwise false
	 */
	public boolean hasDetailHubs(Hub<?> thisHub) {
		if (thisHub == null || faHub.getHubDataUnique(thisHub) == null) {
			return false;
		}
		return faHub.getHubDataUnique(thisHub).getVecHubDetail() != null && faHub.getHubDataUnique(thisHub).getVecHubDetail().size() > 0;
	}

	/**
	 * 20111008 finish if/when needed public HubDetail getHubDetail(Hub hubDetail) { Hub hubMaster = hubDetail.getMasterHub();
	 * Vector<HubDetail> vec = hubMaster.datau.vecHubDetail; if (vec == null) return null; for (HubDetail hd : vec) { if (hd.) } }
	 */




	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getProperty")
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String propPath);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().setProperty")
	public abstract void callObjectReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getMethod")
	public abstract Method callObjectInfoGetMethod(Class<?> clazz, String methodName);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().isReferenceHubLoaded")
	public abstract boolean callObjectReflectIsReferenceHubLoaded(OAObject oaObj, String propertyName);

	@OAParentProvided (example = "srvcObject.getOAObjectHubService().removeHub")
	public abstract <T extends OAObject> void callObjectHubRemoveHub(final T oaObj, Hub<T> hub, boolean bIsOnHubFinalize);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo")
	public abstract OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, String propertyName);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo")
	public abstract OALinkInfo callObjectInfoGetLinkInfo(OAObjectInfo oi, OAObject fromObject, Hub<?> hub);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class<? extends OAObject> clazz);


	@OAParentProvided (example = "srvcSync.isServer")
	public abstract boolean callSyncIsServer();
	

	@OAParentProvided (example = "srvcThreadLocal.getCanAdjustHub")
	public abstract boolean callThreadLocalGetCanAdjustHub(Hub<?> hub);
	
	
	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject")
	public abstract <T extends OAObject> T callHubAOSetActiveObject(Hub<T> thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub);
	
	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject")
	public abstract <T extends OAObject> void callHubAOSetActiveObject(final Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub);

	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject")
	public abstract <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);
	
	@OAParentProvided (example = "srvcHub.getHubShareService().getSharedWeakHubs")
	public abstract <T extends OAObject> WeakReference<Hub<T>>[] callHubShareGetSharedWeakHubs(Hub<T> thisHub);

	@OAParentProvided (example = "srvcHub.getHubShareService().getFirstSharedHub")
	public abstract <T extends OAObject> Hub<T> callHubShareGetFirstSharedHub(Hub<T> thisHub, OAFilter<Hub> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO);

	@OAParentProvided (example = "srvcHub.getPropertyPathforClasses")
	public abstract String callHubGetPropertyPathforClasses(Hub<?> hub, Class<? extends OAObject>[] classes);

	@OAParentProvided (example = "srvcHub.setObjectClass")
	public abstract <T extends OAObject> void callHubGetObjectClass(Hub<T> thisHub, Class<T> objClass);

	@OAParentProvided (example = "srvcHub.getHubShareService().getMainSharedHub")
	public abstract <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub);

	@OAParentProvided (example = "srvcHub.getHubShareService().syncSharedHubs")
	public abstract void callHubShareSyncSharedHubs(Hub<?> thisHub, boolean bShareActiveObject, HubDataActive daOld, HubDataActive daNew,
			boolean bUpdateLink);
	
	@OAParentProvided (example = "srvcHub.getHubShareService().removeSharedHub")
	public abstract <T extends OAObject> void callHubShareRemoveSharedHub(Hub<T> sharedHub, Hub<T> hub);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireOnNewListEvent")
	public abstract void callHubEventFireOnNewListEvent(Hub<?> thisHub, boolean bAll);

	@OAParentProvided (example = "srvcHub.getHubDataService().getObjectAt")
	public abstract <T extends OAObject> T callHubDataGetObjectAt(Hub<T> thisHub, int pos);

	@OAParentProvided (example = "srvcHub.getHubSortService().isSorted")
	public abstract boolean callHubSortIsSorted(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubSortService().getSortProperty")
	public abstract String callHubSortGetSortProperty(Hub<?> thisHub);
	
	@OAParentProvided (example = "srvcHub.getHubSortService().getSortAsc")
	public abstract boolean callHubSortGetSortAsc(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubShareService().addSharedHub")
	public abstract <T extends OAObject> void callHubShareAddSharedHub(Hub<T> thisHub, Hub<T> hub);
	
	@OAParentProvided (example = "srvcHub.getHubAddRemoveService().internalAdd")
	public abstract <T extends OAObject> boolean callHubAddRemoveInternalAdd(final Hub<T> thisHub, final T obj, final boolean bHasLock, final boolean bCheckContains);

	@OAParentProvided (example = "srvcHub.getHubDataService().incChangeCount")
	public abstract void callHubDataIncChangeCount(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubLinkService().getHubWithLink")
	public abstract <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(final Hub<T> thisHub, boolean bIncludeCopiedHubs);

}


