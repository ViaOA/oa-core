package com.viaoa.graph.hub;

import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.HubService;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
//import com.viaoa.hub.HubDataDelegate;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectDeleteDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;

public class HubDeleteService {
	private final Logger LOG = Logger.getLogger(HubDeleteService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	
	public HubDeleteService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
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
    public void deleteAll(Hub thisHub) {
        // 20150206 send to server
        if (thisHub.getSize() == 0) return;
        if (!srvcHub.getHubCSService().deleteAll(thisHub)) {
            return; // sent to server to be done.
        }

        try {
            OARuntime.get().threadService().setDeleting(thisHub, true);
            OARemoteThreadDelegate.sendMessages(true);
            _runDeleteAll(thisHub);
        }
        finally {
            OARemoteThreadDelegate.sendMessages(false);
            OARuntime.get().threadService().setDeleting(thisHub, false);
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
    private void _runDeleteAll(Hub thisHub) {
        Object[] objs;
        if (thisHub.isOAObject()) objs = thisHub.toArray();
        else objs = null;

        srvcHub.getHubAddRemoveService().clear(thisHub); // single event to remove all from hub (sent to clients)
        srvcHub.getHubDataService().clearHubChanges(thisHub);

        if (objs != null) {
            OACascade cascade = new OACascade();
            for (Object obj : objs) {
                srvcObject.getOAObjectDeleteService().delete((OAObject) obj, cascade);
            }
            for (Object obj : objs) {
                srvcHub.getHubAddRemoveService().remove(thisHub, obj, false, false, true, false, false, true);
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
    public boolean isDeletingAll(Hub thisHub) {
        return OARuntime.get().threadService().isDeleting(thisHub);
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
    public void deleteAll(Hub thisHub, OACascade cascade) {
        if (thisHub.size() == 0) return;
        if (cascade.wasCascaded(thisHub, true)) return;
        try {
            OARuntime.get().threadService().setDeleting(thisHub, true);
            OARuntime.get().threadService().lock(thisHub);
            _deleteAll(thisHub, cascade);
        }
        finally {
            OARuntime.get().threadService().unlock(thisHub);
            OARuntime.get().threadService().setDeleting(thisHub, false);
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
    private void _deleteAll(Hub thisHub, OACascade cascade) {
        final boolean bIsOa = thisHub.isOAObject();
        Object objLast = null;

        // 20121005 need to check to see if a link table was used for a 1toM, where createMethod for One is false
        OALinkInfo li = srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(thisHub);
        OALinkInfo liRev = null;
        OAObject masterObj = null;
        OADataSource dataSource = null;
        if (bIsOa && li != null && li.getType() == li.ONE) {
            if (li.getPrivateMethod()) {
                // uses a link table, need to delete from link table first
                liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);

                masterObj = srvcHub.getHubDetailService().getMasterObject(thisHub);
                if (masterObj != null) dataSource = OADataSource.getDataSource(masterObj.getClass());
            }
        }

        // 20160615
        final Object[] objs = thisHub.toArray();
        
        faHub.getHubData(thisHub).getVector().removeAllElements();

        if ((faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges()) && thisHub.isOAObject()) {
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
                        if (vecRemove == null) vecRemove = srvcHub.getHubDataService().createVecRemove(thisHub);
                        vecRemove.addElement(obj);
                    }
                }
            }
            srvcHub.getHubDataService().setChanged(thisHub,
                (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0) || (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0));
        }
        else {
        	srvcHub.getHubDataService().setChanged(thisHub, true);
        }

        for (Object obj : objs) {
            // 20240125
            // since thisHub.data.vector.removeAllElements was called (above), need to call remove for thisHub
            srvcHub.getHubAddRemoveService().remove(thisHub, obj, false, true, true, true, true, true);

            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] { (OAObject) obj }, liRev.getName());
            }

            if (bIsOa) {
                srvcObject.getOAObjectDeleteService().delete((OAObject) obj, cascade);
            }

        }

        srvcHub._updateHubAddsAndRemoves(thisHub, -1, cascade, false);

        thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    }


}
