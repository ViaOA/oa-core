package com.viaoa.graph.service.hub;

import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.OAObject;

public abstract class HubMasterService {
	private final Logger LOG = Logger.getLogger(HubMasterService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubMasterService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Returns the master OAObject associated with this hub. If no master
	 * relationship exists or the hub is null, {@code null} is returned.
	 *
	 * @param hub the hub whose master object is requested
	 * @return the master OAObject, or {@code null} if none exists
	 */
	public OAObject getMasterObject(Hub<?> hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = callHubDetailGetDataMaster(hub, true);
		if (dm == null) {
			return null;
		}
		return dm.getMasterObject();
	}

	/**
	 * Returns the class of the hub's master OAObject. If the master object exists,
	 * its class is returned; otherwise, if a master hub exists, that hub's object
	 * class is used. If neither is available, {@code null} is returned.
	 *
	 * @param hub the hub whose master object's class is requested
	 * @return the master class, or {@code null} if unavailable
	 */
	public Class<? extends OAObject> getMasterClass(Hub<?> hub) {
		if (hub == null) {
			return null;
		}
		HubDataMaster dm = callHubDetailGetDataMaster(hub, true);
		OAObject obj = dm.getMasterObject();
		if (obj != null) {
			return obj.getClass();
		}
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub().getObjectClass();
		}
		return null;
	}

    /**
     * Determines which hub controls this hub’s validity. If the hub has a master
     * hub, that master hub is returned. If a linked shared hub exists, its link
     * target or its controlling hub is returned. If an addHub is present, its
     * controlling hub is evaluated. Otherwise, this hub is returned.
     *
     * @param thisHub the hub whose controlling hub is requested
     * @return the controlling hub
     */
	public Hub<?> getControllingHub(Hub<?> thisHub) {
		if (thisHub == null) return null;
		HubDataMaster dm = callHubDetailGetDataMaster(thisHub, true);
		if (dm.getMasterHub() != null) {
			return dm.getMasterHub();
		}

		// 20181119 find shared hub with link
		Hub<?> hubWithLink = callHubLinkGetHubWithLink(thisHub, true);
		
		if (hubWithLink != null) {
			HubDataUnique hdu = faHub.getHubDataUnique(hubWithLink);			
			if (hdu.getLinkToHub() != null) {
				if (hdu.isAutoCreate()) {
					return getControllingHub(hdu.getLinkToHub());
				}
				return hdu.getLinkToHub();
			}
		}
		HubDataUnique hdu = faHub.getHubDataUnique(thisHub);			
		if (hdu.getAddHub() != null) {
			return getControllingHub(hdu.getAddHub());
		}
		return thisHub;
	}

	public abstract HubDataMaster callHubDetailGetDataMaster(final Hub<?> thisHub, boolean bIncludedFilteredHub);
	public abstract <T extends OAObject> Hub<?> callHubLinkGetHubWithLink(final Hub<T> thisHub, boolean bIncludeCopiedHubs);
}



