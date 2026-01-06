package com.viaoa.graph.hub;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.*;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class HubSerializeService {
	private final Logger LOG = Logger.getLogger(HubSerializeService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;

	public HubSerializeService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
		if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Used by serialization to store Hub.
	 */
	public void _writeObject(Hub thisHub, java.io.ObjectOutputStream stream) throws IOException {
		if (srvcHub.getHubSelectService().isMoreData(thisHub)) {
			try {
				OARuntime.get().threadService().setSuppressCSMessages(true);
				srvcHub.getHubSelectService().loadAllData(thisHub); // otherwise, client will not have the correct datasource
			} finally {
				OARuntime.get().threadService().setSuppressCSMessages(false);
			}
		}
		stream.defaultWriteObject();
	}

	public int replaceObject(Hub thisHub, OAObject objFrom, OAObject objTo) {
		if (thisHub == null)
			return -1;
		if (faHub.getHubData(thisHub) == null)
			return -1;
		if (faHub.getHubData(thisHub).getVector() == null)
			return -1;
		int pos = faHub.getHubData(thisHub).getVector().indexOf(objFrom);
		if (pos >= 0)
			faHub.getHubData(thisHub).getVector().setElementAt(objTo, pos);
		return pos;
	}

	public void replaceMasterObject(Hub thisHub, OAObject objFrom, OAObject objTo) {
		if (thisHub == null)
			return;
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == objFrom) {
			faHub.getHubDataMaster(thisHub).setMasterObject(objTo);
		}
	}

	/**
	 * Used by OAObjectSerializeDelegate
	 */
	public boolean isResolved(Hub thisHub) {
		return (thisHub != null && faHub.getHubData(thisHub) != null && faHub.getHubData(thisHub).getVector() != null);
	}

	/**
	 * Used by serialization when reading objects from stream. This needs to add the
	 * hub to OAObject.hubs, but only if it is not a duplicate (and is not needed)
	 */
	public Object _readResolve(Hub thisHub) throws ObjectStreamException {
		for (int i = 0;; i++) {
			Object obj = thisHub.getAt(i);
			if (obj == null)
				break;

			if (i == 0) {
				if (obj instanceof OAObject) {
					// dont initialize this hub if the master object is a duplicate.
					// check by looking to see if this object already belongs to a hub that has the
					// same masterObject/linkinfo
					if (srvcObject.getOAObjectHubService().isAlreadyInHub((OAObject) obj, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo())) {
						break; // this hub is a dup and wont be used
					}
				}
			}
			srvcObject.getOAObjectHubService().addHub((OAObject) obj, thisHub);
		}
		return thisHub;
	}

}
