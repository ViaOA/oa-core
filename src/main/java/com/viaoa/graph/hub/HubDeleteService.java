package com.viaoa.graph.hub;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAddRemoveDelegate;
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
import com.viaoa.object.OACascade;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectDeleteDelegate;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThreadDelegate;

public class HubDeleteService {
	private final Logger LOG = Logger.getLogger(HubDeleteService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	private final HubData.FriendAccess faHubData;
	private final HubDataUnique.FriendAccess faHubDataUnique;
	private final HubDataActive.FriendAccess faHubDataActive;
	
	
	public HubDeleteService(HubService srvcHub, 
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
        if (!HubCSDelegate.deleteAll(thisHub)) {
            return; // sent to server to be done.
        }

        try {
            OAThreadLocalDelegate.setDeleting(thisHub, true);
            OARemoteThreadDelegate.sendMessages(true);
            _runDeleteAll(thisHub);
        }
        finally {
            OARemoteThreadDelegate.sendMessages(false);
            OAThreadLocalDelegate.setDeleting(thisHub, false);
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

        HubAddRemoveDelegate.clear(thisHub); // single event to remove all from hub (sent to clients)
        HubDataDelegate.clearHubChanges(thisHub);

        if (objs != null) {
            OACascade cascade = new OACascade();
            for (Object obj : objs) {
                OAObjectDeleteDelegate.delete((OAObject) obj, cascade);
            }
            for (Object obj : objs) {
                HubAddRemoveDelegate.remove(thisHub, obj, false, false, true, false, false, true);
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
        return OAThreadLocalDelegate.isDeleting(thisHub);
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
            OAThreadLocalDelegate.setDeleting(thisHub, true);
            OAThreadLocalDelegate.lock(thisHub);
            _deleteAll(thisHub, cascade);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
            OAThreadLocalDelegate.setDeleting(thisHub, false);
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
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
        OALinkInfo liRev = null;
        OAObject masterObj = null;
        OADataSource dataSource = null;
        if (bIsOa && li != null && li.getType() == li.ONE) {
            if (li.getPrivateMethod()) {
                // uses a link table, need to delete from link table first
                liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);

                masterObj = HubDetailDelegate.getMasterObject(thisHub);
                if (masterObj != null) dataSource = OADataSource.getDataSource(masterObj.getClass());
            }
        }

        // 20160615
        final Object[] objs = thisHub.toArray();
        
        faHubData.getVector(thisHub).removeAllElements();

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
                        if (vecRemove == null) vecRemove = HubDataDelegate.createVecRemove(thisHub);
                        vecRemove.addElement(obj);
                    }
                }
            }
            HubDataDelegate.setChanged(thisHub,
                (faHub.getHubData(thisHub).getVecAdd() != null && faHub.getHubData(thisHub).getVecAdd().size() > 0) || (faHub.getHubData(thisHub).getVecRemove() != null && faHub.getHubData(thisHub).getVecRemove().size() > 0));
        }
        else {
            HubDataDelegate.setChanged(thisHub, true);
        }

        for (Object obj : objs) {
            // 20240125
            // since thisHub.data.vector.removeAllElements was called (above), need to call remove for thisHub
            HubAddRemoveDelegate.remove(thisHub, obj, false, true, true, true, true, true);

            if (dataSource != null) {
                dataSource.updateMany2ManyLinks(masterObj, null, new OAObject[] { (OAObject) obj }, liRev.getName());
            }

            if (bIsOa) {
                OAObjectDeleteDelegate.delete((OAObject) obj, cascade);
            }

        }

        HubDelegate._updateHubAddsAndRemoves(thisHub, -1, cascade, false);

        thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    }


}
