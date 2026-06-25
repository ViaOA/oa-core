package com.viaoa.oa.service.hub;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

/*qqqqqqqqqqqq
CODEX


 #10 — invariant risk
  file/class/method: src/main/java/com/viaoa/graph/service/hub/HubSerializeService.java:25, _writeObject
  exact concern: Hub serialization mutates runtime state by loading all remaining select data before
  defaultWriteObject. Sync messages are suppressed, but normal load/select side effects can still occur.
  why it matters: Serialization should have clear ownership: either it snapshots already-loaded Hub state or
  explicitly guarantees full materialization. Current behavior can turn serialization into a datasource-loading
  operation.
  severity: invariant risk
  minimal fix: Document/test this as intentional, or gate full materialization behind serializer policy. At minimum,
  assert loading does not fire Hub events or sync messages.
  suggested invariant ID/name: HUB-SER-LOAD-001: Hub serialization materialization is deterministic and side-effect
  bounded
  suggested test coverage: Serialize partially loaded Hub; assert loaded count, event count, sync suppression, and
  select state.


*/

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
	public void _writeObject(Hub<?> thisHub, java.io.ObjectOutputStream stream) throws IOException {
		if (callHubSelectIsMoreData(thisHub)) {
			boolean bWas = callThreadLocalGetSendSyncMessages();
			try {
				callThreadLocalSetSendSyncMessages(false);
				callHubSelectLoadAllData(thisHub); // otherwise, client will not have the correct datasource
			} finally {
				callThreadLocalSetSendSyncMessages(bWas);
			}
		}
		stream.defaultWriteObject();
	}

	public <T extends OAObject> int replaceObject(Hub<T> thisHub, T objFrom, T objTo) {
		if (thisHub == null) return -1;
		//qqqqq not thread safe
		HubData<T> hd = faHub.getHubData(thisHub);
		if (hd == null) return -1;
		Vector<T> vec = hd.getVector(); 
		if (vec == null) return -1;
		int pos = vec.indexOf(objFrom);
		if (pos >= 0) vec.setElementAt(objTo, pos);
		return pos;
	}

	public <T extends OAObject> void replaceMasterObject(Hub<T> thisHub, T objFrom, T objTo) {
		if (thisHub == null) return;
		HubDataMaster dm = faHub.getHubDataMaster(thisHub); 
		if (dm.getMasterObject() == objFrom) {
			dm.setMasterObject(objTo);
		}
	}

	/**
	 * Used by OAObjectSerializeDelegate
	 */
	public <T extends OAObject> boolean isResolved(Hub<T> thisHub) {
		if (thisHub == null) return false;
		HubData<T> hd = faHub.getHubData(thisHub);
		return (hd != null && hd.getVector() != null);
	}

	/**
	 * Used by serialization when reading objects from stream. This needs to add the
	 * hub to OAObject.hubs, but only if it is not a duplicate (and is not needed)
	 */
	public <T extends OAObject> Object _readResolve(Hub<T> thisHub) throws ObjectStreamException {
		if (thisHub == null) return thisHub; 
		for (int i = 0;; i++) {
			T obj = thisHub.getAt(i);
			if (obj == null) break;

			if (i == 0) {
				// dont initialize this hub if the master object is a duplicate.
				// check by looking to see if this object already belongs to a hub that has the
				// same masterObject/linkinfo
				if (callObjectHubIsAlreadyInHub((OAObject) obj, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo())) {
					break; // this hub is a dup and wont be used
				}
			}
			callObjectHubAddHub(obj, thisHub);
		}
		return thisHub;
	}

	public abstract boolean callObjectHubIsAlreadyInHub(OAObject oaObj, OALinkInfo li);
	public abstract <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub);
	public abstract boolean callHubSelectIsMoreData(Hub<?> thisHub);
	public abstract void callHubSelectLoadAllData(Hub<?> thisHub);
	public abstract boolean callThreadLocalGetSendSyncMessages();
	public abstract void callThreadLocalSetSendSyncMessages(boolean b);
}

