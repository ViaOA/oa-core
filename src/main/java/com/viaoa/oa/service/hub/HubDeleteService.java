package com.viaoa.oa.service.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;


/*qqqqqqqqqqqqqqqqqq
CODEX

#6 — bug
  file/class/method: src/main/java/com/viaoa/oa/service/hub/HubDeleteService.java:61, _runDeleteAll; src/main/
  java/com/viaoa/oa/service/hub/HubDeleteService.java:127, _deleteAll
  exact concern: Both delete-all paths clear the Hub/vector before deleting the objects. If object delete fails mid-
  loop, the in-memory Hub has already been emptied and remote/listener remove-all may already have been sent.
  why it matters: A failed delete can leave runtime OA model state inconsistent with datasource state and with clients.
  severity: invariant risk
  minimal fix: Define failure semantics. Minimal hardening is to preserve the object snapshot and restore
  membership/change state on delete failure, or delete first and clear only after successful delete cascade.
  suggested invariant ID/name: HUB-DELETE-STATE-001: failed deleteAll does not silently lose Hub membership
  suggested test coverage: Force object delete failure during Hub.deleteAll() and verify Hub membership, change
  state, and events.

 #8
  file/class/method: src/main/java/com/viaoa/oa/service/hub/HubDeleteService.java:91, src/main/java/com/viaoa/
  oa/service/hub/HubDeleteService.java:143

  exact execution path that triggers the bug: Hub.deleteAll() -> snapshot objects -> clear Hub/vector before
  deleting objects -> one object delete fails -> Hub membership is already gone, change tracking may already be
  updated, and remove-all effects may already have been sent.

  why it is a real correctness risk: failed delete can leave in-memory OA model state inconsistent with datasource
  state. Objects that still exist in storage are no longer in the Hub.

  severity: high-risk bug

  minimal fix: delete first and clear only after successful delete, or preserve enough snapshot/change state to
  restore membership on failure.

  suggested test case: Hub with multiple objects; force delete failure on second object; assert Hub membership and
  change state remain consistent with storage.
  
see: comment in code for #8  
Invariant:
HUB-DELETEALL-STOPS-ON-FIRST-FAILURE
deleteAll processes objects in snapshot order. Each successful delete is committed to Hub state. If a delete fails, 
processing stops, the failed object and all unprocessed objects remain in the Hub, and the exception is propagated.

HUB-DELETEALL-USES-SNAPSHOT:
deleteAll operates only on the Hub contents captured at start. Objects added during deleteAll are not part of that delete operation and must remain in the Hub.

*/


/**
 * Coordinates delete operations for Hub contents.
 */


public abstract class HubDeleteService {
	private final Logger LOG = Logger.getLogger(HubDeleteService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubDeleteService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Deletes all objects in the hub. If running in client/server mode and the
	 * delete must occur on the server, the request is delegated to the server and
	 * no further action is taken locally. Otherwise, this method marks the hub as
	 * deleting, enables remote message forwarding, and invokes the internal delete
	 * routine. Remote messaging and delete flags are restored afterward.
	 *
	 * @param thisHub the hub whose contents will be deleted
	 */
    public <T extends OAObject> void deleteAll(Hub<T> thisHub) {
    	if (thisHub == null) return;
        // 20150206 send to server
        if (thisHub.getSize() == 0) return;
        if (callHubCSDeleteAll(thisHub)) {
            return; // sent to server to be done.
        }

        boolean bWas = callThreadLocalGetSendSyncMessages();
//qqqqqqqqqqqqq todo: #8 codex         
// 20260512 add new array to track which objs were deleted/removed ... will need to change Hub msg and sync remoting for this        
        List<T> alDeletedObjects = new ArrayList<>();            
        try {
            callThreadLocalSetDeleting(thisHub, true);
            callThreadLocalSetSendSyncMessages(true);
            
            _runDeleteAll(thisHub, alDeletedObjects);
        }
        finally {
        	callThreadLocalSetSendSyncMessages(bWas);
            callThreadLocalSetDeleting(thisHub, false);
        }
    }


    /**
     * Server-side implementation that performs a complete deletion of all hub
     * objects. Change tracking is cleared, and all objects are removed using a
     * single bulk event. Each OAObject is deleted using a shared cascade, and then
     * explicitly removed from the hub for synchronization with listeners.
     *
     * @param thisHub the hub whose contents are being deleted
     */
    private <T extends OAObject> void _runDeleteAll(Hub<T> thisHub, List<T> alDeletedObjects) {
    	if (thisHub == null) return;
        T[] objs = thisHub.toArray();

		callHubAddRemoveClear(thisHub); // single event to remove all from hub (sent to clients)
		callHubDataClearHubChanges(thisHub);

        if (objs != null) {
            OACascade cascade = new OACascade();
            for (T obj : objs) {
        		callObjectDeleteDelete(obj, cascade);
            	alDeletedObjects.add(obj);
            }
            for (T obj : objs) {
                callHubAddRemoveRemove(thisHub, obj, false, false, true, false, false, true);
            }
        }
    }
    
    /**
     * Indicates whether the specified hub is currently in the process of having all
     * its objects deleted. This flag is maintained using thread-local tracking.
     *
     * @param thisHub the hub being checked
     * @return {@code true} if the hub is currently deleting all objects
     */
    public boolean isDeletingAll(Hub<?> thisHub) {
    	if (thisHub == null) return false;
        return callThreadLocalIsDeleting(thisHub);
    }

    /**
     * Deletes all objects in the hub using the supplied cascade. If the hub is
     * empty or has already been processed in the cascade, no action is taken. The
     * hub is locked during deletion and the deleting state is enabled for the
     * duration of the operation.
     *
     * @param thisHub the hub whose contents will be deleted
     * @param cascade the cascade tracker used to avoid repeated processing
     */
    public void deleteAll(Hub<?> thisHub, OACascade cascade) {
    	if (thisHub == null) return;
        if (thisHub.size() == 0) return;
        if (cascade.wasCascaded(thisHub, true)) return;
        
        try {
            callThreadLocalSetDeleting(thisHub, true);
            callThreadLocalLock(thisHub);
            _deleteAll(thisHub, cascade);
        }
        finally {
            callThreadLocalUnlock(thisHub);
            callThreadLocalSetDeleting(thisHub, false);
            
        }
    }

    /**
     * Internal deletion routine that removes all objects from the hub, updates
     * change-tracking lists, and deletes each OAObject using the provided cascade.
     * Handles special cases for one-to-many link tables, many-to-many link cleanup,
     * master/detail updates, and ensures HubAddRemoveDelegate receives removal
     * events for each object.
     *
     * @param thisHub the hub being cleared
     * @param cascade the cascade used for recursive delete operations
     */
    private <T extends OAObject> void _deleteAll(Hub<T> thisHub, OACascade cascade) {
    	if (thisHub == null) return;
        Object objLast = null;

        // 20121005 need to check to see if a link table was used for a 1toM, where createMethod for One is false
        OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(thisHub);
        OALinkInfo liRev = null;
        OAObject masterObj = null;
        OADataSource dataSource = null;
        if (li != null && li.getType() == li.ONE) {
            if (li.getPrivateMethod()) {
                // uses a link table, need to delete from link table first
                liRev = callObjectInfoGetReverseLinkInfo(li);

                masterObj = callHubDetailGetMasterObject(thisHub);
                if (masterObj != null) dataSource = OARuntime.datasource().get(masterObj.getClass());
            }
        }

        // 20160615
        final T[] objs = thisHub.toArray();
        
        faHub.getHubData(thisHub).getVector().removeAllElements();

        if ((faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges())) {
            Vector<T> vecRemove = faHub.getHubData(thisHub).getVecRemove();
            int x = vecRemove == null ? 0 : vecRemove.size();
            for (T obj : objs) {
                if (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().removeElement(obj)) {
                    // no-op
                }
                else {
                    boolean b = false;
                    for (int i = 0; i < x; i++) {
                        if (obj == vecRemove.elementAt(i)) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        if (vecRemove == null) vecRemove = callHubDataCreateVecRemove(thisHub);
                        vecRemove.addElement(obj);
                    }
                }
            }
            callHubStatusSetChanged(thisHub,
                (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0) || (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0));
        }
        else {
        	callHubStatusSetChanged(thisHub, true);
        }

        for (T obj : objs) {
            // 20240125
            // since thisHub.data.vector.removeAllElements was called (above), need to call remove for thisHub
            callHubAddRemoveRemove(thisHub, obj, false, true, true, true, true, true);

            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] { obj }, liRev.getName());
            }

            callObjectDeleteDelete(obj, cascade);
        }

        callHub_updateHubAddsAndRemoves(thisHub, -1, cascade, false);

        thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    }

	/**
	 * Dependency hook used by this service for ObjectDeleteDelete behavior.
	 *
	 * @param oaObj method input
	 * @param cascade method input
	 */

	public abstract void callObjectDeleteDelete(final OAObject oaObj, OACascade cascade);
	/**
	 * Dependency hook used by this service for ObjectInfoGetReverseLinkInfo behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for HubCSDeleteAll behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract boolean callHubCSDeleteAll(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubAddRemoveClear behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubAddRemoveClear(final Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDataClearHubChanges behavior.
	 *
	 * @param thisHub method input
	 */
	public abstract void callHubDataClearHubChanges(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubAddRemoveRemove behavior.
	 *
	 * @param thisHub method input
	 * @param obj method input
	 * @param bForce method input
	 * @param bSendEvent method input
	 * @param bDeleting method input
	 * @param bSetAO method input
	 * @param bSetPropToMaster method input
	 * @param bIsRemovingAll method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubAddRemoveRemove(final Hub<T> thisHub, T obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll);
	/**
	 * Dependency hook used by this service for HubDetailGetLinkInfoFromDetailToMaster behavior.
	 *
	 * @param hub method input
	 * @return result value
	 */
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	/**
	 * Dependency hook used by this service for HubDetailGetMasterObject behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract OAObject callHubDetailGetMasterObject(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDataCreateVecRemove behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> Vector<T> callHubDataCreateVecRemove(Hub<T> thisHub);
	/**
	 * Dependency hook used by this service for HubStatusSetChanged behavior.
	 *
	 * @param thisHub method input
	 * @param bChanged method input
	 */
	public abstract void callHubStatusSetChanged(Hub<?> thisHub, boolean bChanged);
	/**
	 * Dependency hook used by this service for Hub_updateHubAddsAndRemoves behavior.
	 *
	 * @param thisHub method input
	 * @param iCascadeRule method input
	 * @param cascade method input
	 * @param bIsSaving method input
	 */
	public abstract void callHub_updateHubAddsAndRemoves(final Hub<?> thisHub, final int iCascadeRule, final OACascade cascade, final boolean bIsSaving);
	/**
	 * Dependency hook used by this service for ThreadLocalSetDeleting behavior.
	 *
	 * @param hub method input
	 * @param b method input
	 */
	public abstract void callThreadLocalSetDeleting(Hub<?> hub, boolean b);
	/**
	 * Dependency hook used by this service for ThreadLocalIsDeleting behavior.
	 *
	 * @param hub method input
	 * @return result value
	 */
	public abstract boolean callThreadLocalIsDeleting(Hub<?> hub);
	/**
	 * Dependency hook used by this service for ThreadLocalLock behavior.
	 *
	 * @param hub method input
	 */
	public abstract void callThreadLocalLock(Hub<?> hub);
	/**
	 * Dependency hook used by this service for ThreadLocalUnlock behavior.
	 *
	 * @param hub method input
	 */
	public abstract void callThreadLocalUnlock(Hub<?> hub);
	/**
	 * Dependency hook used by this service for ThreadLocalGetSendSyncMessages behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callThreadLocalGetSendSyncMessages();
	/**
	 * Dependency hook used by this service for ThreadLocalSetSendSyncMessages behavior.
	 *
	 * @param b method input
	 */
	public abstract void callThreadLocalSetSendSyncMessages(boolean b);
}
