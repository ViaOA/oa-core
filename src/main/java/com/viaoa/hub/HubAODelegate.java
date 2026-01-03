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

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;

/**
 * Delegate for managing Active Object (AO) state within a {@link Hub}.
 * <p>
 * Responsible for setting, validating, and propagating AO changes through linked
 * or shared Hubs while maintaining event integrity. Also prevents recursion
 * and enforces synchronization between master and detail Hubs.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Set and retrieve Active Object for the Hub and its dependents.</li>
 *   <li>Propagate AO changes across shared, linked, or detail Hubs.</li>
 *   <li>Prevent circular AO updates through thread-local flags.</li>
 *   <li>Fire {@link HubEvent}s to listeners after AO updates.</li>
 * </ul>
 */
public class HubAODelegate {
	private static Logger LOG = Logger.getLogger(HubAODelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubAOService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubAOService().?(?);
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
	 * Sets the active object based on the specified position. If the position is
	 * outside the valid range, the active object is set to {@code null}. Delegates
	 * to the full active-object update method using default update and force flags.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param pos     the position of the object to make active
	 * @return the object at the specified position, or {@code null} if none
	 */
	public static Object setActiveObject(Hub thisHub, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAOService().setActiveObject(thisHub, pos);
	}

	/**
	 * Sets the active object to the specified object. If the object is not found
	 * in the hub, the active object is set to {@code null}. Resolves proxies using
	 * {@link HubDelegate#getRealObject} before updating.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to make active, or {@code null} to clear
	 */
	public static void setActiveObject(Hub thisHub, Object object) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object);
	}

	/**
	 * Forces the active object to be updated even if it is already the current
	 * active object. Resolves proxies before updating.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to force as active, or {@code null} to clear
	 */
	public static void setActiveObjectForce(Hub thisHub, Object object) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObjectForce(thisHub, object);
	}

	/**
	 * Sets the active object using master-adjust logic. If the object is not in
	 * the hub, the hub attempts to adjust the master hub so that the object becomes
	 * available in this hub.
	 *
	 * @param thisHub      the hub whose active object is being updated
	 * @param object       the object to make active
	 * @param adjustMaster whether to adjust the master hub if the object is not found
	 */
	public static void setActiveObject(Hub thisHub, Object object, boolean adjustMaster) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object, adjustMaster);
	}

	/**
	 * Sets the active object with control over master adjustment, link updates, and
	 * force behavior. Resolves proxies before updating, adjusts master if required,
	 * computes the object position, and delegates to the full active-object update.
	 *
	 * @param thisHub      the hub whose active object is being updated
	 * @param object       the object to make active
	 * @param adjustMaster whether to adjust the master hub if needed
	 * @param bUpdateLink  whether to update link properties
	 * @param bForce       whether to force the update even if unchanged
	 */
	public static void setActiveObject(Hub thisHub, Object object, boolean adjustMaster, boolean bUpdateLink, boolean bForce) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object, adjustMaster, bUpdateLink, bForce);
	}

	/**
	 * Sets the active object and position using default link-update and force
	 * behavior. Delegates to the full active-object update method.
	 *
	 * @param thisHub the hub whose active object is being updated
	 * @param object  the object to make active
	 * @param pos     the position of the object in the hub
	 */
	public static void setActiveObject(Hub thisHub, Object object, int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object, pos);
	}

	/**
	 * Sets the active object based on a position and delegates to the full
	 * active-object update method. If the position is invalid, clears the active
	 * object.
	 *
	 * @param thisHub           the hub whose active object is being updated
	 * @param pos               the position of the object to activate
	 * @param bUpdateLink       whether to update link properties
	 * @param bForce            whether to force the update
	 * @param bCalledByShareHub whether this call originated from a shared hub
	 * @return the object at the specified position, or {@code null} if none
	 */
	protected static Object setActiveObject(Hub thisHub, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubAOService().setActiveObject(thisHub, pos, bUpdateLink, bForce, bCalledByShareHub);
	}

	/**
	 * Sets the active object using the specified position and update options.
	 * Delegates to the full active-object update method with shared-hub updates enabled.
	 *
	 * @param thisHub           the hub whose active object is being updated
	 * @param object            the object to make active
	 * @param pos               the position of the object
	 * @param bUpdateLink       whether to update link properties
	 * @param bForce            whether to force the update
	 * @param bCalledByShareHub whether this call originated from a shared hub
	 */
	public static void setActiveObject(final Hub thisHub, Object object, int pos, boolean bUpdateLink, boolean bForce,
			boolean bCalledByShareHub) {
		//qqqqqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub);
	}

	/**
	 * Registers the hub to emit warnings when its active object is set directly.
	 * Warnings are only applied when the hub is not the master hub of a master/detail
	 * relationship.
	 *
	 * @param thisHub the hub that should warn on active-object updates
	 */
	public static void warnOnSettingAO(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().warnOnSettingAO(thisHub);
	}

	/**
	 * Main routine for setting the active object. Updates the hub’s active object,
	 * propagates changes to shared hubs, updates detail hubs, updates link
	 * properties, and fires after-change events. Prevents recursion and respects
	 * force and share-hub flags.
	 *
	 * @param thisHub                 the hub whose active object is being updated
	 * @param object                  the object to make active
	 * @param pos                     the position of the active object
	 * @param bUpdateLink             whether to update link properties
	 * @param bForce                  whether to force the update
	 * @param bCalledByShareHub       whether this update was triggered by a shared hub
	 * @param bUpdateSharedHubDetail  whether to update detail hubs on shared hubs
	 */
	public static void setActiveObject(final Hub thisHub, Object object, final int pos, final boolean bUpdateLink, final boolean bForce,
			final boolean bCalledByShareHub, final boolean bUpdateSharedHubDetail) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().setActiveObject(thisHub, object, pos, bUpdateLink, bForce, bCalledByShareHub, bUpdateSharedHubDetail);
	}

	/**
	 * Updates all detail hubs associated with shared hubs that share the same
	 * active-object data structure. Calls {@code updateAllDetail} on each relevant
	 * hub.
	 *
	 * @param thisHub the hub whose detail hubs should be updated
	 */
	public static void updateDetailHubs(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().updateDetailHubs(thisHub);
	}

	/**
	 * Ensures that the hub always maintains its first object as the active object.
	 * Registers listeners to reset the active position to zero after add, remove,
	 * insert, new-list, remove-all, and active-object changes.
	 *
	 * @param thisHub the hub whose active object should always be the first object
	 */
	public static void keepActiveObject(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubAOService().keepActiveObject(thisHub);
	}

}
