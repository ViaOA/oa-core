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

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.sync.*;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;

/**
 * Handles all Client/Server synchronization logic for {@link Hub} operations.
 * <p>
 * The HubCSDelegate is invoked by Hub internals whenever objects are added,
 * removed, inserted, moved, sorted, or otherwise modified so that the same
 * change can be propagated to all connected systems.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Translate Hub events into remote synchronization commands.</li>
 *   <li>Route commands through {@link RemoteSyncInterface} (server → clients)
 *       or {@link RemoteClientInterface} (client → server).</li>
 *   <li>Respect suppression flags and skip calculated or local-only objects.</li>
 *   <li>Ensure that distributed Hubs remain state-identical without causing
 *       feedback loops or redundant updates.</li>
 * </ul>
 *
 * <h3>Supported Remote Actions</h3>
 * <ul>
 *   <li>{@link #addToHub(Hub, OAObject)} — replicate an add event.</li>
 *   <li>{@link #insertInHub(Hub, OAObject, int)} — replicate insert at index.</li>
 *   <li>{@link #removeFromHub(Hub, OAObject, int)} and {@link #removeAllFromHub(Hub)}.</li>
 *   <li>{@link #moveObjectInHub(Hub, int, int)} — reorder synchronization.</li>
 *   <li>{@link #sort(Hub, String, boolean, Comparator)} — remote sort propagation.</li>
 *   <li>{@link #deleteAll(Hub)} — instructs remote delete on server.</li>
 *   <li>{@link #clearHubChanges(Hub)} — resets change tracking on clients.</li>
 *   <li>{@link #sendRefresh(Hub)} — triggers remote refresh request.</li>
 * </ul>
 *
 * <h3>Behavioral Safeguards</h3>
 * <ul>
 *   <li>All methods no-op in single-user mode or when
 *       {@code OAThreadLocalDelegate.isSuppressCSMessages()} is true.</li>
 *   <li>Skips propagation for calculated or local-only relationships.</li>
 *   <li>Uses {@link OARemoteThreadDelegate#shouldSendMessages()} to avoid
 *       feedback loops during replication.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Hub<Order> hubOrders = new Hub<>(Order.class);
 * hubOrders.add(order);  // transparently propagated to all clients
 * }</pre>
 *
 * <p>This delegate underpins OA’s distributed object graph consistency.</p>
 */
public class HubCSDelegate {
    private static Logger LOG = Logger.getLogger(HubCSDelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubCSService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubCSService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
    
    
    /**
     * Removes all objects from the same hub on connected systems by sending
     * a remote "remove all" command, if synchronization is enabled and the
     * hub has a master object. No-op when in single-user mode or when
     * client/server message suppression flags are active.
     *
     * @param thisHub the hub whose remote counterparts should remove all items
     */
    public static void removeAllFromHub(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().removeAllFromHub(thisHub);
    }
    
    /**
     * Removes the specified object from the same hub on connected systems.
     * Skips calculated or local-only objects, and does not execute when
     * synchronization is suppressed or when the master object is absent.
     *
     * @param thisHub the hub originating the removal
     * @param obj     the object being removed
     * @param pos     the position from which the object was removed
     */
	public static void removeFromHub(Hub thisHub, OAObject obj, int pos) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().removeFromHub(thisHub, obj, pos);
	}

	/**
	 * Adds an object to the same hub on connected systems. Sends the object
	 * itself if necessary so remote clients can instantiate it. No-op for
	 * local-only or calculated objects, or when synchronization is suppressed.
	 *
	 * @param thisHub the hub originating the add operation
	 * @param thisObj the object being added
	 */
	public static void addToHub(final Hub thisHub, final OAObject thisObj) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().addToHub(thisHub, thisObj);
	}	

	/**
	 * Inserts an object at the specified position in the same hub on
	 * connected systems. Returns {@code false} when synchronization is
	 * suppressed, when local-only or calculated rules block propagation,
	 * or when no master object exists.
	 *
	 * @param thisHub the hub originating the insert
	 * @param obj     the object being inserted
	 * @param pos     the target index
	 * @return {@code true} if a remote insert command was sent; otherwise {@code false}
	 */
	public static boolean insertInHub(Hub thisHub, OAObject obj, int pos) {
    	OAGraph g = getGraph(thisHub, obj);
    	if (g == null) return false;
    	return g.hubs().getHubCSService().insertInHub(thisHub, obj, pos);
	}	
	
	/**
	 * Moves an object from one index to another in the same hub on
	 * connected systems. Skips propagation when synchronization is
	 * suppressed or when operating on local-only or calculated links.
	 *
	 * @param thisHub the hub originating the move request
	 * @param posFrom the starting index
	 * @param posTo   the destination index
	 */
	public static void moveObjectInHub(Hub thisHub, int posFrom, int posTo) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().moveObjectInHub(thisHub, posFrom, posTo);
	}

	/**
	 * Determines whether the specified hub is operating on the server.
	 *
	 * @param h the hub to check
	 * @return {@code true} if this is the server; otherwise {@code false}
	 */
	public static boolean isServer(Hub h) {
    	OAGraph g = getGraph(h, null);
    	if (g == null) return false;
    	return g.hubs().getHubCSService().isServer(h);
	}		

	/**
	 * Returns whether the current thread is executing as a remote
	 * synchronization thread.
	 *
	 * @return {@code true} if the thread is a remote thread; otherwise {@code false}
	 */
	public static boolean isRemoteThread() {
		return (OARemoteThreadDelegate.isRemoteThread());
	}		
	
	/**
	 * Sorts objects in the hub on connected systems by sending a remote
	 * sort command. Skips propagation if synchronization is suppressed,
	 * the master object is missing or local-only, or the link is calculated.
	 *
	 * @param thisHub       the hub being sorted
	 * @param propertyPaths the property paths to sort by
	 * @param bAscending    whether sorting is ascending
	 * @param comp          optional comparator used for sorting
	 */
	public static void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().sort(thisHub, propertyPaths, bAscending, comp);
	}
	
	/**
	 * Deletes all objects in the hub, either locally or by sending a
	 * remote delete-all request, depending on whether this system is the
	 * server. Returns {@code true} if the deletion should occur locally,
	 * or {@code false} if it was delegated to a remote server.
	 *
	 * @param thisHub the hub whose contents should be deleted
	 * @return {@code true} if deletion is local; otherwise {@code false}
	 */
    public static boolean deleteAll(Hub thisHub) {
    	//qqqqqqq method was protected
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubCSService().deleteAll(thisHub);
    }
    
    /**
     * Clears hub change tracking on connected clients by sending a
     * "clear changes" event, when synchronization is enabled. Returns
     * {@code false} when propagation cannot occur due to suppression,
     * missing master object, or local-only/calculated relationships.
     *
     * @param thisHub the hub whose change state should be cleared remotely
     * @return {@code true} if a clear request was sent; otherwise {@code false}
     */
    public static boolean clearHubChanges(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubCSService().clearHubChanges(thisHub);
    }   

    /**
     * Sends a remote refresh request for the specified hub’s master object,
     * if synchronization is available and the link information can be obtained.
     *
     * @param thisHub the hub requesting refresh
     */
    public static void sendRefresh(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubCSService().sendRefresh(thisHub);
    }
}
