package com.viaoa.graph.hub;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.HubService;
import com.viaoa.hub.*;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectSaveDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

public class HubSerializeService {
	private final Logger LOG = Logger.getLogger(HubSerializeService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;

	public HubSerializeService(HubService srvcHub, Hub.FriendAccess faHub) {
		if (srvcHub == null)
			throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
		if (faHub == null)
			throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Used by serialization to store Hub.
	 */
	public void _writeObject(Hub thisHub, java.io.ObjectOutputStream stream) throws IOException {
		if (HubSelectDelegate.isMoreData(thisHub)) {
			try {
				OAThreadLocalDelegate.setSuppressCSMessages(true);
				HubSelectDelegate.loadAllData(thisHub); // otherwise, client will not have the correct datasource
			} finally {
				OAThreadLocalDelegate.setSuppressCSMessages(false);
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
					if (OAObjectHubDelegate.isAlreadyInHub((OAObject) obj, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo())) {
						break; // this hub is a dup and wont be used
					}
				}
			}
			OAObjectHubDelegate.addHub((OAObject) obj, thisHub);
		}
		return thisHub;
	}

}
