/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.hub;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

import com.viaoa.datasource.OADataSource;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.*;

/**
 * Delegate that handles delete operations for {@link Hub} objects.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Perform full or selective deletions on Hub contents.</li>
 *   <li>Route deletions to the server when running in distributed mode.</li>
 *   <li>Maintain transactional cascade logic through {@link OACascade}.</li>
 *   <li>Coordinate with {@link HubAddRemoveDelegate} and {@link HubDataDelegate}
 *       to keep local and remote state synchronized.</li>
 * </ul>
 *
 * <p>Implements both client-side and server-side delete strategies, ensuring
 * correct removal from master/detail relationships and data sources.
 */
public class HubDeleteDelegate {

	/**
	 * Deletes all objects in the hub. If running in client/server mode and the
	 * delete must occur on the server, the request is delegated to the server and
	 * no further action is taken locally. Otherwise, this method marks the hub as
	 * deleting, enables remote message forwarding, and invokes the internal delete
	 * routine. Remote messaging and delete flags are restored afterward.
	 *
	 * @param thisHub the hub whose contents will be deleted
	 */
    public static void deleteAll(Hub thisHub) {
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
    private static void _runDeleteAll(Hub thisHub) {
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
    public static boolean isDeletingAll(Hub thisHub) {
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
    public static void deleteAll(Hub thisHub, OACascade cascade) {
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
    private static void _deleteAll(Hub thisHub, OACascade cascade) {
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
        thisHub.data.vector.removeAllElements();

        if ((thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()) && thisHub.isOAObject()) {
            Vector vecRemove = thisHub.data.getVecRemove();
            int x = vecRemove == null ? 0 : vecRemove.size();
            for (Object obj : objs) {
                if (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().removeElement(obj)) {
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
                (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0) || (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0));
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
