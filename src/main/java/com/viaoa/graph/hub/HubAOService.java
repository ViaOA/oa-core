package com.viaoa.graph.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCSDelegate;
import com.viaoa.hub.HubData;
import com.viaoa.hub.HubDataActive;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubDataUnique;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubLinkDelegate;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OAGroupBy;
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

public class HubAOService {
	private final Logger LOG = Logger.getLogger(HubAOService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	private final HubData.FriendAccess faHubData;
	private final HubDataUnique.FriendAccess faHubDataUnique;
	private final HubDataActive.FriendAccess faHubDataActive;
	
	
	public HubAOService(HubService srvcHub, 
			Hub.FriendAccess faHub,
			HubData.FriendAccess faHubData,
			HubDataUnique.FriendAccess faHubDataUnique,
			HubDataActive.FriendAccess faHubDataActive
			) {
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
    	if (faHubData == null) throw new IllegalArgumentException("HubData.FriendAccess can not be null");
    	this.faHubData = faHubData;
    	if (faHubDataUnique == null) throw new IllegalArgumentException("HubDataUnique.FriendAccess can not be null");
    	this.faHubDataUnique = faHubDataUnique;
    	if (faHubDataActive == null) throw new IllegalArgumentException("HubDataActive.FriendAccess can not be null");
    	this.faHubDataActive = faHubDataActive;
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
	public Object setActiveObject(Hub thisHub, int pos) {
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
			object = HubDelegate.getRealObject(thisHub, object);
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
			object = HubDelegate.getRealObject(thisHub, object);
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
			object = HubDelegate.getRealObject(thisHub, object);
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
			liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
		} else {
			liRev = HubDetailDelegate.getLinkInfoFromMasterObjectToDetail(thisHub);
		}
		if (liRev != null) {
			if (liRev.getType() == li.ONE && bUpdateLink) { // 20171117
				//was: if (liRev.getType() == li.ONE) {
				Object objMaster = HubDetailDelegate.getMasterObject(thisHub);
				if (objMaster != null) {
					Object value = OAObjectReflectDelegate.getProperty((OAObject) objMaster, liRev.getName());
					if (value != object) {
						if (objMaster != null) {
							OAObjectReflectDelegate.setProperty((OAObject) objMaster, liRev.getName(), object, null);
						}
					}
				}
			}
		}

		int pos = HubDataDelegate.getPos(thisHub, object, adjustMaster, bUpdateLink);
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
	public Object setActiveObject(Hub thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
		Object ho;
		if (pos < 0) {
			ho = null;
		} else {
			ho = HubDataDelegate.getObjectAt(thisHub, pos);
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

		if (faHubDataActive.getActiveObject(thisHub) == object && !bForce) {
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
						if (faHubDataActive.getActiveObject(thisHub) != object || !bForce) {
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
				if (faHubDataActive.getActiveObject((thisHub)) != object || !bForce) {
					LOG.log(Level.WARNING, "Note/FYI only: should not setAO on thisHub=" + thisHub + " (use sharedHub), will continue",
							new Exception("showing thread stack"));
				}
			}
		}

		OAThreadLocalDelegate.lock(thisHub);
		Object origActiveObject = faHubDataActive.getActiveObject((thisHub));
		faHubDataActive.setActiveObject(thisHub, object);
		OAThreadLocalDelegate.unlock(thisHub);

		faHub.getHubDataUnique(thisHub).setUpdatingActiveObject(true);

		HubDetailDelegate.updateAllDetail(thisHub, bUpdateLink);

		if (bUpdateLink) {
			HubLinkDelegate.updateLinkProperty(thisHub, object, pos);
		}
		faHub.getHubDataUnique(thisHub).setUpdatingActiveObject(false);

		// Now call for all sharedHubs with same "dataa"
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub h) {
				return faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub); 
			}
		};

		final Hub[] hubs = HubShareDelegate.getAllSharedHubs(thisHub, filter);

		for (int i = 0; i < hubs.length; i++) {
			Hub h = hubs[i];
			if (h != thisHub && faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				faHub.getHubDataUnique(h).setUpdatingActiveObject(true);

				if (bUpdateSharedHubDetail) {
					HubDetailDelegate.updateAllDetail(h, bUpdateLink);
				}
				if (bUpdateLink) {
					HubLinkDelegate.updateLinkProperty(h, object, pos);
				}
				faHub.getHubDataUnique(h).setUpdatingActiveObject(false);
			}
		}

		// must send event After updateAllDetail()
		// this will send event to all sharedHubs with same "dataa" only
		HubEventDelegate.fireAfterChangeActiveObjectEvent(thisHub, object, pos, !bCalledByShareHub);

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

		Hub[] hubs = HubShareDelegate.getAllSharedHubs(thisHub, filter);

		for (int i = 0; i < hubs.length; i++) {
			Hub h = hubs[i];
			if (h != thisHub && faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				faHub.getHubDataUnique(h).setUpdatingActiveObject(true);
				HubDetailDelegate.updateAllDetail(h, true);
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


	
}
