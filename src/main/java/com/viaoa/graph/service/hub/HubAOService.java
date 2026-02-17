package com.viaoa.graph.service.hub;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.*;

public abstract class HubAOService {
	private final Logger LOG = Logger.getLogger(HubAOService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubAOService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	
	/**
	 * Sets the active object based on the specified position. If the position is
	 * outside the valid range, the active object is set to {@code null}. Delegates
	 * to the full active-object update method using default update and force flags.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param pos     the position of the object to make active
	 * @return the object at the specified position, or {@code null} if none
	 */
	public <T> T setActiveObject(Hub<T> thisHub, int pos) {
		return setActiveObject(thisHub, pos, true, false, false); //bUpdateLink,bForce,bCalledByShareHub
	}

	/**
	 * Sets the active object to the specified object. If the object is not found
	 * in the hub, the active object is set to {@code null}. Resolves proxies using
	 * {@link HubDelegate#getRealObject} before updating.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to make active, or {@code null} to clear
	 */
	public void setActiveObject(Hub thisHub, Object object) {
		if (object != null) {
			/* not needed, used for debugging 20150920
			if (object instanceof Hub) {
			    LOG.warning("trying to set active object using a AO=hub, thisHub="+thisHub+", AO="+object);
			    return;
			}
			*/
			object = callHubGetRealObject(thisHub, object);
		}
		setActiveObject(thisHub, object, true, true, false);
	}

	/**
	 * Forces the active object to be updated even if it is already the current
	 * active object. Resolves proxies before updating.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to force as active, or {@code null} to clear
	 */
	public void setActiveObjectForce(Hub thisHub, Object object) {
		if (object != null) {
			object = callHubGetRealObject(thisHub, object);
		}
		setActiveObject(thisHub, object, true, true, true);
	}

	/**
	 * Sets the active object using master-adjust logic. If the object is not in
	 * the hub, the hub attempts to adjust the master hub so that the object becomes
	 * available in this hub.
	 *
	 * @param thisHub      the hub whose active object is being updated
	 * @param object       the object to make active
	 * @param adjustMaster whether to adjust the master hub if the object is not found
	 */
	public void setActiveObject(Hub thisHub, Object object, boolean adjustMaster) {
		if (object != null) {
			object = callHubGetRealObject(thisHub, object);
		}
		setActiveObject(thisHub, object, adjustMaster, true, false); // adjMaster, updateLink, force
	}

	/**
	 * Sets the active object with control over master adjustment, link updates, and
	 * force behavior. Resolves proxies before updating, adjusts master if required,
	 * computes the object position, and delegates to the full active-object update.
	 *
	 * @param thisHub      the hub whose active object is being updated
	 * @param object       the object to make active
	 * @param adjustMaster whether to adjust the master hub if needed
	 * @param bUpdateLink  whether to update link properties
	 * @param bForce       whether to force the update even if unchanged
	 */
	public void setActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
		// for detailHub where link.type=ONE
		OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
		OALinkInfo liRev;
		if (li != null) {
			liRev = callObjectInfoGetReverseLinkInfo(li);
		} else {
			liRev = callHubDetailGetLinkInfoFromMasterObjectToDetail(thisHub);
		}
		if (liRev != null) {
			if (liRev.getType() == li.ONE && bUpdateLink) { // 20171117
				//was: if (liRev.getType() == li.ONE) {
				Object objMaster = callHubDetailGetMasterObject(thisHub);
				if (objMaster != null) {
					Object value = callObjectReflectGetProperty((OAObject) objMaster, liRev.getName());
					if (value != object) {
						if (objMaster != null) {
							callObjectReflectSetProperty((OAObject) objMaster, liRev.getName(), object, null);
						}
					}
				}
			}
		}

		int pos = callHubDataGetPos(thisHub, object, adjustMaster, bUpdateLink);
		setActiveObject(thisHub, (pos < 0 ? null : object), pos, bUpdateLink, bForce, false);
	}

	/**
	 * Sets the active object and position using default link-update and force
	 * behavior. Delegates to the full active-object update method.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to make active
	 * @param pos     the position of the object in the hub
	 */
	public void setActiveObject(Hub thisHub, Object object, int pos) {
		setActiveObject(thisHub, object, pos, true, false, false); // bUpdateLink,bForce
	}

	/**
	 * Sets the active object based on a position and delegates to the full
	 * active-object update method. If the position is invalid, clears the active
	 * object.
	 *
	 * @param thisHub           the hub whose active object is being updated
	 * @param pos               the position of the object to activate
	 * @param bUpdateLink       whether to update link properties
	 * @param bForce            whether to force the update
	 * @param bCalledByShareHub whether this call originated from a shared hub
	 * @return the object at the specified position, or {@code null} if none
	 */
	public <T> T setActiveObject(Hub<T> thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
		T ho;
		if (pos < 0) {
			ho = null;
		} else {
			ho = callHubDataGetObjectAt(thisHub, pos);
		}

		if (ho == null) {
			setActiveObject(thisHub, null, -1, bUpdateLink, bForce, bCalledByShareHub);
		} else {
			setActiveObject(thisHub, ho, pos, bUpdateLink, bForce, bCalledByShareHub);
		}
		return ho;
	}

	/**
	 * Sets the active object using the specified position and update options.
	 * Delegates to the full active-object update method with shared-hub updates enabled.
	 *
	 * @param thisHub           the hub whose active object is being updated
	 * @param object            the object to make active
	 * @param pos               the position of the object
	 * @param bUpdateLink       whether to update link properties
	 * @param bForce            whether to force the update
	 * @param bCalledByShareHub whether this call originated from a shared hub
	 */
	public void setActiveObject(final Hub thisHub, Object object, int pos, boolean bUpdateLink, boolean bForce,
			boolean bCalledByShareHub) {
		setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub, true);
	}

	protected final HashSet<Hub> hsWarnOnSettingAO = new HashSet<>();

	/**
	 * Registers the hub to emit warnings when its active object is set directly.
	 * Warnings are only applied when the hub is not the master hub of a master/detail
	 * relationship.
	 *
	 * @param thisHub the hub that should warn on active-object updates
	 */
	public void warnOnSettingAO(Hub thisHub) {
		if (thisHub == null) {
			return;
		}
		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			if (faHub.getHubDataMaster(thisHub).getMasterHub() == null) {
				return; // already will warn if AO is set
			}
		}
		hsWarnOnSettingAO.add(thisHub);
	}

	/**
	 * Main routine for setting the active object. Updates the hub’s active object,
	 * propagates changes to shared hubs, updates detail hubs, updates link
	 * properties, and fires after-change events. Prevents recursion and respects
	 * force and share-hub flags.
	 *
	 * @param thisHub                 the hub whose active object is being updated
	 * @param object                  the object to make active
	 * @param pos                     the position of the active object
	 * @param bUpdateLink             whether to update link properties
	 * @param bForce                  whether to force the update
	 * @param bCalledByShareHub       whether this update was triggered by a shared hub
	 * @param bUpdateSharedHubDetail  whether to update detail hubs on shared hubs
	 */
	public void setActiveObject(final Hub thisHub, Object object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail) {
		if (thisHub == null) {
			return;
		}

		if (faHub.getHubDataActive(thisHub).getActiveObject() == object && !bForce) {
			return;
		}
		if (faHub.getHubDataUnique(thisHub).isUpdatingActiveObject()) {
			return;
		}

		// 20180328 check to see if thisHub has masterObject and no masterHub, which is the real hub and should not setAO on it since other "users" could be doing the same
		if (OAObject.getDebugMode()) {
			if (faHub.getHubDataMaster(thisHub).getMasterObject() != null && thisHub.getSharedHub() == null) {
				if (faHub.getHubDataMaster(thisHub).getMasterHub() == null) {
					if (!thisHub.getOAObjectInfo().getLocalOnly()) {
						if (faHub.getHubDataActive(thisHub).getActiveObject() != object || !bForce) {
							if (!(faHub.getHubDataMaster(thisHub).getMasterObject() instanceof OAGroupBy)) {
								LOG.log(Level.WARNING,
										"Note/FYI only: should not setAO on thisHub=" + thisHub + " (use sharedHub), will continue",
										new Exception("showing thread stack"));
							}
						}
					}
				}
			}
			if (hsWarnOnSettingAO.contains(thisHub) && thisHub.getSharedHub() == null) {
				if (faHub.getHubDataActive(thisHub).getActiveObject() != object || !bForce) {
					LOG.log(Level.WARNING, "Note/FYI only: should not setAO on thisHub=" + thisHub + " (use sharedHub), will continue",
							new Exception("showing thread stack"));
				}
			}
		}

		callThreadLocalLock(thisHub);
		Object origActiveObject = faHub.getHubDataActive(thisHub).getActiveObject();
		faHub.getHubDataActive(thisHub).setActiveObject(object);
		callThreadLocalUnlock(thisHub);

		faHub.getHubDataUnique(thisHub).setUpdatingActiveObject(true);

		callHubDetailUpdateAllDetail(thisHub, bUpdateLink);

		if (bUpdateLink) {
			callHubLinkUpdateLinkProperty(thisHub, object, pos);
		}
		faHub.getHubDataUnique(thisHub).setUpdatingActiveObject(false);

		// Now call for all sharedHubs with same "dataa"
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				return faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub); 
			}
		};

		final Hub[] hubs = callHubShareGetAllSharedHubs(thisHub, filter);

		for (int i = 0; i < hubs.length; i++) {
			Hub h = hubs[i];
			if (h != thisHub && faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				faHub.getHubDataUnique(h).setUpdatingActiveObject(true);

				if (bUpdateSharedHubDetail) {
					callHubDetailUpdateAllDetail(h, bUpdateLink);
				}
				if (bUpdateLink) {
					callHubLinkUpdateLinkProperty(h, object, pos);
				}
				faHub.getHubDataUnique(h).setUpdatingActiveObject(false);
			}
		}

		// must send event After updateAllDetail()
		// this will send event to all sharedHubs with same "dataa" only
		callHubEventFireAfterChangeActiveObjectEvent(thisHub, object, pos, !bCalledByShareHub);

		for (int i = 0; object != null && i < hubs.length; i++) {
			Hub h = hubs[i];
			if (faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				Hub hx = faHub.getHubDataUnique(h).getAddHub();
				if (hx != null) {
					if (hx.getObject(object) == null) {
						hx.add(object);
					}
					setActiveObject(hx, object);
				}
			}
		}
	}

	/**
	 * Updates all detail hubs associated with shared hubs that share the same
	 * active-object data structure. Calls {@code updateAllDetail} on each relevant
	 * hub.
	 *
	 * @param thisHub the hub whose detail hubs should be updated
	 */
	public void updateDetailHubs(final Hub thisHub) {
		if (thisHub == null) {
			return;
		}

		// Now call for all sharedHubs with same "dataa"
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				return faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub); 
			}
		};

		Hub[] hubs = callHubShareGetAllSharedHubs(thisHub, filter);

		for (int i = 0; i < hubs.length; i++) {
			Hub h = hubs[i];
			if (h != thisHub && faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				faHub.getHubDataUnique(h).setUpdatingActiveObject(true);
				callHubDetailUpdateAllDetail(h, true);
				faHub.getHubDataUnique(h).setUpdatingActiveObject(false);
			}
		}
	}

	/**
	 * Ensures that the hub always maintains its first object as the active object.
	 * Registers listeners to reset the active position to zero after add, remove,
	 * insert, new-list, remove-all, and active-object changes.
	 *
	 * @param thisHub the hub whose active object should always be the first object
	 */
	public void keepActiveObject(final Hub thisHub) {
		if (thisHub == null) {
			return;
		}
		thisHub.addHubListener(new HubListenerAdapter() {
			@Override
			public void afterChangeActiveObject(HubEvent e) {
				update();
			}

			@Override
			public void afterNewList(HubEvent e) {
				update();
			}

			@Override
			public void afterAdd(HubEvent e) {
				update();
			}

			@Override
			public void afterRemove(HubEvent e) {
				update();
			}

			@Override
			public void afterInsert(HubEvent e) {
				update();
			}

			@Override
			public void afterRemoveAll(HubEvent e) {
				update();
			}

			void update() {
				thisHub.setPos(0);
			}
		});
		thisHub.setPos(0);
	}

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi); 

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getProperty")
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String propPath);
		
	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().setProperty")
	public abstract void callObjectReflectSetProperty(final OAObject oaObj, String propName, Object value, final String fmt);


	@OAParentProvided (example = "srvcHub.getHubDataService().getPos")
	public abstract int callHubDataGetPos(final Hub thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink);

	@OAParentProvided (example = "srvcHub.getRealObject")
	public abstract Object callHubGetRealObject(Hub hub, Object object);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getLinkInfoFromMasterObjectToDetail")
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub thisDetailHub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getMasterObject")
	public abstract OAObject callHubDetailGetMasterObject(Hub thisHub);

	@OAParentProvided (example = "srvcHub.getHubDataService().getObjectAt")
	public abstract <T> T callHubDataGetObjectAt(Hub<T> thisHub, int pos);

	@OAParentProvided (example = "srvcHub.getHubDetailService().updateAllDetail")
	public abstract void callHubDetailUpdateAllDetail(Hub thisHub, boolean bUpdateLink);
	
	@OAParentProvided (example = "srvcHub.getHubLinkService().updateLinkProperty")
	public abstract void callHubLinkUpdateLinkProperty(Hub thisHub, Object fromObject, int pos);

	@OAParentProvided (example = "srvcHub.getHubShareService().getAllSharedHubs")
	public abstract Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireAfterChangeActiveObjectEvent")
	public abstract void callHubEventFireAfterChangeActiveObjectEvent(Hub thisHub, Object obj, int pos, boolean bAllShared);


	@OAParentProvided (example = "srvcThreadLocal.lock")
	public abstract void callThreadLocalLock(Object object);
	
	@OAParentProvided (example = "srvcThreadLocal.unlock")
	public abstract void callThreadLocalUnlock(Object object);
	
}
