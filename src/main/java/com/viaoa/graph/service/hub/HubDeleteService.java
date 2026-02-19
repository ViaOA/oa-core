package com.viaoa.graph.service.hub;

import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;

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
    public void deleteAll(Hub<?> thisHub) {
        // 20150206 send to server
        if (thisHub.getSize() == 0) return;
        if (!callHubCSDeleteAll(thisHub)) {
            return; // sent to server to be done.
        }

        try {
            callThreadLocalSetDeleting(thisHub, true);
            callRemoteThreadSendMessages(true);
            _runDeleteAll(thisHub);
        }
        finally {
            callRemoteThreadSendMessages(false);
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
    private <T extends OAObject> void _runDeleteAll(Hub<T> thisHub) {
        T[] objs = thisHub.toArray();

        callHubAddRemoveClear(thisHub); // single event to remove all from hub (sent to clients)
        callHubDataClearHubChanges(thisHub);

        if (objs != null) {
            OACascade cascade = new OACascade();
            for (Object obj : objs) {
                callObjectDeleteDelete((OAObject) obj, cascade);
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
                if (masterObj != null) dataSource = OADataSource.getDataSource(masterObj.getClass());
            }
        }

        // 20160615
        final T[] objs = thisHub.toArray();
        
        faHub.getHubData(thisHub).getVector().removeAllElements();

        if ((faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges())) {
            Vector vecRemove = faHub.getHubData(thisHub).getVecRemove();
            int x = vecRemove == null ? 0 : vecRemove.size();
            for (Object obj : objs) {
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
            callHubDataSetChanged(thisHub,
                (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0) || (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0));
        }
        else {
        	callHubDataSetChanged(thisHub, true);
        }

        for (T obj : objs) {
            // 20240125
            // since thisHub.data.vector.removeAllElements was called (above), need to call remove for thisHub
            callHubAddRemoveRemove(thisHub, obj, false, true, true, true, true, true);

            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] { (OAObject) obj }, liRev.getName());
            }

            callObjectDeleteDelete((OAObject) obj, cascade);
        }

        callHub_updateHubAddsAndRemoves(thisHub, -1, cascade, false);

        thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    }

	@OAParentProvided (example = "srvcObject.getOAObjectDeleteService().delete")
	public abstract void callObjectDeleteDelete(final OAObject oaObj, OACascade cascade);
    
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);

	@OAParentProvided (example = "srvcHub.getHubCSService().deleteAll")
	public abstract boolean callHubCSDeleteAll(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubAddRemoveService().clear")
	public abstract void callHubAddRemoveClear(final Hub<?> thisHub);
	
	@OAParentProvided (example = "srvcHub.getHubDataService().clearHubChanges")
	public abstract void callHubDataClearHubChanges(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubAddRemoveService().remove")
	public abstract <T extends OAObject> boolean callHubAddRemoveRemove(final Hub<T> thisHub, T obj, final boolean bForce,
			final boolean bSendEvent, final boolean bDeleting, final boolean bSetAO,
			final boolean bSetPropToMaster, final boolean bIsRemovingAll);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster")
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getMasterObject")
	public abstract OAObject callHubDetailGetMasterObject(Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubDataService().createVecRemove")
	public abstract <T extends OAObject> Vector<T> callHubDataCreateVecRemove(Hub<T> thisHub);

	@OAParentProvided (example = "srvcHub.getHubDataService().setChanged")
	public abstract void callHubDataSetChanged(Hub<?> thisHub, boolean bChanged);

	@OAParentProvided (example = "srvcHub._updateHubAddsAndRemoves")
	public abstract void callHub_updateHubAddsAndRemoves(final Hub<?> thisHub, final int iCascadeRule, final OACascade cascade, final boolean bIsSaving);



	@OAParentProvided (example = "srvcThreadLocal.setDeleting")
	public abstract void callThreadLocalSetDeleting(Hub<?> hub, boolean b);

	@OAParentProvided (example = "srvcThreadLocal.isDeleting")
	public abstract boolean callThreadLocalIsDeleting(Hub<?> hub);

	@OAParentProvided (example = "srvcThreadLocal.lock")
	public abstract void callThreadLocalLock(Hub<?> hub);

	@OAParentProvided (example = "srvcThreadLocal.unlock")
	public abstract void callThreadLocalUnlock(Hub<?> hub);

	@OAParentProvided (example = "srvcRemoteThread.sendMessages")
	public abstract void callRemoteThreadSendMessages(boolean b);


}
