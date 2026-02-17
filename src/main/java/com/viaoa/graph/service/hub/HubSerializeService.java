package com.viaoa.graph.service.hub;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.*;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;

public abstract class HubSerializeService {
	private final Logger LOG = Logger.getLogger(HubSerializeService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubSerializeService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Used by serialization to store Hub.
	 */
	public void _writeObject(Hub thisHub, java.io.ObjectOutputStream stream) throws IOException {
		if (callHubSelectIsMoreData(thisHub)) {
			try {
				callThreadLocalSetSuppressCSMessages(true);
				callHubSelectLoadAllData(thisHub); // otherwise, client will not have the correct datasource
			} finally {
				callThreadLocalSetSuppressCSMessages(false);
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
		if (thisHub == null || !thisHub.isOAObject()) return thisHub; 
		for (int i = 0;; i++) {
			Object obj = thisHub.getAt(i);
			if (obj == null) break;

			if (i == 0) {
				if (obj instanceof OAObject) {
					// dont initialize this hub if the master object is a duplicate.
					// check by looking to see if this object already belongs to a hub that has the
					// same masterObject/linkinfo
					if (callObjectHubIsAlreadyInHub((OAObject) obj, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo())) {
						break; // this hub is a dup and wont be used
					}
				}
			}
			callObjectHubAddHub((OAObject) obj, thisHub);
		}
		return thisHub;
	}

	@OAParentProvided (example = "srvcObject.getOAObjectHubService().isAlreadyInHub")
	public abstract boolean callObjectHubIsAlreadyInHub(OAObject oaObj, OALinkInfo li);
	
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().addHub")
	public abstract boolean callObjectHubAddHub(OAObject oaObj, Hub hub);

	@OAParentProvided (example = "srvcHub.getHubSelectService().isMoreData")
	public abstract boolean callHubSelectIsMoreData(Hub thisHub);

	@OAParentProvided (example = "srvcHub.getHubSelectService().loadAllData")
	public abstract void callHubSelectLoadAllData(Hub thisHub);

	@OAParentProvided (example = "srvcThreadLocal.setSuppressCSMessages")
	public abstract void callThreadLocalSetSuppressCSMessages(boolean b);
}

