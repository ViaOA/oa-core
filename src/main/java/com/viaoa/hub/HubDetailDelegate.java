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

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Internal delegate for Master/Detail wiring in {@link Hub}: creates, maintains,
 * and re-syncs detail Hubs from a master Hub’s active object and link metadata.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Establish master→detail relationships using property paths or link info
 *       (see {@link #getDetailHub(Hub, String)} and overloads).</li>
 *   <li>Keep detail hubs “pointed” at the correct collection/object whenever the
 *       master Hub’s active object (AO) changes ({@link #updateAllDetail}).</li>
 *   <li>Rebind detail hubs to shared or merged hubs, including reconnect logic
 *       for recursive/self-referential models ({@link #updateDetail}).</li>
 *   <li>Keep reference properties in sync when adds/removes happen in the detail
 *       hub ({@link #setPropertyToMasterHub}).</li>
 *   <li>Compute and expose relationship metadata (master hub/object, link info,
 *       property names, “owned” semantics, recursion checks).</li>
 * </ul>
 *
 * <h3>Key APIs</h3>
 * <ul>
 *   <li>{@link #setMasterHub(Hub, Hub, String, boolean, String)} — define/replace the master of a hub.</li>
 *   <li>{@link #getDetailHub(Hub, String)} — resolve or build a detail hub via property path,
 *       routing through {@code HubMerger} when the path requires fan-out.</li>
 *   <li>{@link #updateDetail(Hub, HubDetail, Hub, boolean)} — (re)targets the detail hub’s data
 *       and AO after master AO or link changes.</li>
 *   <li>{@link #getLinkInfoFromMasterToDetail(Hub)} / {@link #getPropertyFromMasterToDetail(Hub)} — metadata helpers.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Supports many-to-many, one-to-many, and recursive graphs (uses reverse link info and
 *       {@code OAPropertyPath} decomposition).</li>
 *   <li>Shares underlying {@code HubData} when detail is a Hub reference; otherwise populates
 *       from arrays/objects with duplicate-allow toggling and newList events.</li>
 *   <li>Integrates with linking/sharing delegates and selection/order settings.</li>
 * </ul>
 */
public class HubDetailDelegate {
	private static Logger LOG = Logger.getLogger(HubDetailDelegate.class.getName());

	
	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubDetailService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubDetailService().?(?);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Sets the master hub for this hub using the provided property path.
	 * <p>
	 * If this hub already has a master hub defined, the existing master/detail
	 * configuration is removed. If {@code masterHub} is non-null, this method
	 * resolves or creates the corresponding detail hub using the supplied
	 * property path and sharing options.
	 *
	 * @param thisHub     the hub whose master relationship is being set
	 * @param masterHub   the new master hub
	 * @param path        the property path from the master hub to this hub
	 * @param bShared     whether the detail hub should share underlying data
	 * @param selectOrder the select order to assign to the detail hub
	 */
	public static void setMasterHub(Hub thisHub, Hub masterHub, String path, boolean bShared, String selectOrder) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) {
			g = getGraph(masterHub, null);
			if (g == null) return;
		}
		g.hubs().getHubDetailService().setMasterHub(thisHub, masterHub, path, bShared, selectOrder);
	}

	/**
	 * Returns whether this hub participates in a recursive master/detail
	 * relationship. The method checks link metadata from the hub's detail-to-master
	 * link and evaluates the reverse link for recursion.
	 *
	 * @param thisHub the hub to test
	 * @return true if the relationship is recursive, otherwise false
	 */
	public static boolean isRecursiveMasterDetail(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().isRecursiveMasterDetail(thisHub);
	}

	/**
	 * Attempts to align the master hub's active object with the specified
	 * detail object's reference back to the master. Handles MANY–MANY links,
	 * reverse-link resolution, and active-object adjustment.
	 *
	 * @param thisHub     the detail hub
	 * @param detailObject the detail object whose master reference is checked
	 * @param bUpdateLink  whether linked hubs should update link properties
	 * @return true if the master hub's active object was adjusted, otherwise false
	 */
	public static boolean setMasterHubActiveObject(Hub thisHub, Object detailObject, boolean bUpdateLink) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().setMasterHubActiveObject(thisHub, detailObject, bUpdateLink);
	}

	/**
	 * Updates the reference property on a detail object to reflect the
	 * current master object. Handles ONE and MANY link types, reverse-link
	 * processing, Hub-based references, and array-based membership updates.
	 *
	 * @param thisHub      the detail hub
	 * @param detailObject the detail object to update
	 * @param objMaster    the master object used for reference assignment
	 */
	public static void setPropertyToMasterHub(Hub thisHub, Object detailObject, Object objMaster) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().setPropertyToMasterHub(thisHub, detailObject, objMaster);
	}

	/**
	 * Updates all detail hubs under the specified hub. Each registered
	 * {@code HubDetail} is processed to ensure its underlying data and
	 * active object align with the current master's active object.
	 *
	 * @param thisHub     the hub whose detail hubs should be updated
	 * @param bUpdateLink whether link-based updates should be propagated
	 */
	public static void updateAllDetail(Hub thisHub, boolean bUpdateLink) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().updateAllDetail(thisHub, bUpdateLink);
	}

	/**
	 * Preloads detail data for the object at the specified position in the
	 * master hub. Touches each detail hub’s property getter to ensure data
	 * is loaded or initialized.
	 *
	 * @param thisHub the master hub
	 * @param pos     the index of the master object whose detail data is preloaded
	 */
	public static void preloadDetailData(final Hub thisHub, final int pos) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().preloadDetailData(thisHub, pos);
	}

	/**
	 * Internal method used to refresh the contents and active object of a
	 * detail hub after changes to the master hub’s active object or link
	 * property. Handles Hub, OAObject, Object, and array-based detail types,
	 * as well as shared-hub state.
	 *
	 * @param thisHub     the master hub
	 * @param detail      the hub-detail metadata
	 * @param detailHub   the hub being updated
	 * @param bUpdateLink whether link-based updates should be propagated
	 */
	public static void updateDetail(final Hub thisHub, final HubDetail detail, final Hub detailHub, final boolean bUpdateLink) {
		// qqqqqq method was protected
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().updateDetail(thisHub, detail, detailHub, bUpdateLink);
	}

	/**
	 * Initializes or adjusts the active object for a detail hub based on
	 * master hub state, link-hub constraints, or shared active-object rules.
	 *
	 * @param thisHub           the hub whose active object drives updates
	 * @param hubDetailHub      the detail hub being updated
	 * @param bUpdateLink       whether linked hubs should update link properties
	 * @param bShareActiveObject whether the hubs share active-object state
	 */
	public static void updateDetailActiveObject(final Hub thisHub, final Hub hubDetailHub, final boolean bUpdateLink,
			final boolean bShareActiveObject) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().updateDetailActiveObject(thisHub, hubDetailHub, bUpdateLink, bShareActiveObject);
	}

	/**
	 * Returns the {@code HubDataMaster} associated with the hub or one of
	 * its shared hubs. If no shared hub contains master information, the
	 * hub's own {@code datam} is returned.
	 *
	 * @param thisHub the hub whose master data is resolved
	 * @return the master-data descriptor
	 */
	public static HubDataMaster getDataMaster(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDataMaster(thisHub);
	}

	/**
	 * Returns the hub's master-data descriptor, optionally searching filtered
	 * shared hubs. Delegates to the internal {@code getDataMaster} variant.
	 *
	 * @param thisHub               the hub whose master data is resolved
	 * @param bIncludedFilteredHub  whether filtered shared hubs should be considered
	 * @return the resolved {@code HubDataMaster}
	 */
	public static HubDataMaster getDataMaster(final Hub thisHub, boolean bIncludedFilteredHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDataMaster(thisHub, bIncludedFilteredHub);
	}

	/**
	 * Returns this hub or a shared hub that has a master hub defined. Searches
	 * the hub and its shared hubs until one with a non-null master hub is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master hub, or null if none exists
	 */
	public static Hub getHubWithMasterHub(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getHubWithMasterHub(thisHub);
	}

	/**
	 * Returns this hub or a shared hub that has a master object defined. Searches
	 * the hub and its shared hubs until one with a non-null master object is found.
	 *
	 * @param thisHub the hub to inspect
	 * @return a hub with a master object, or null if none exists
	 */
	public static Hub getHubWithMasterObject(final Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getHubWithMasterObject(thisHub);
	}

	/**
	 * Returns the master hub for this hub or any shared hub that carries
	 * master-hub metadata.
	 *
	 * @param thisHub the hub whose master hub is requested
	 * @return the master hub, or null if none exists
	 */
	public static Hub getMasterHub(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getMasterHub(thisHub);
	}

	/**
	 * Returns the master object associated with this hub or a shared hub.
	 *
	 * @param thisHub the hub whose master object is requested
	 * @return the master object, or null if not defined
	 */
	public static OAObject getMasterObject(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getMasterObject(thisHub);
	}

	/**
	 * Returns the class of the master object or master hub associated with
	 * this hub. If none is found, returns null.
	 *
	 * @param thisHub the hub whose master class is requested
	 * @return the master class, or null if unavailable
	 */
	public static Class getMasterClass(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getMasterClass(thisHub);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation to create
	 * or resolve a detail hub using the specified class array.
	 *
	 * @param thisHub the master hub
	 * @param clazz   the class path used to derive the property path
	 * @return the resolved or newly created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, Class[] clazz) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, clazz);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using a
	 * single class, with optional shared active-object and select-order settings.
	 *
	 * @param thisHub      the master hub
	 * @param clazz        the target class for the detail relationship
	 * @param bShareActive whether the detail hub shares active-object state
	 * @param selectOrder  optional select-order string
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, Class clazz, boolean bShareActive, String selectOrder) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, clazz, bShareActive, selectOrder);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * supplied property path and optional object class.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path to the detail hub
	 * @param objectClass  the class of the detail objects
	 * @param bShareActive whether the detail hub shares active-object state
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, Class objectClass, boolean bShareActive) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path, objectClass, bShareActive);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path.
	 *
	 * @param thisHub the master hub
	 * @param path    the property path
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path and select-order setting.
	 *
	 * @param thisHub     the master hub
	 * @param path        the property path
	 * @param selectOrder optional select-order for the detail hub
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, String selectOrder) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path, selectOrder);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * provided property path and active-object sharing flag.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path
	 * @param bShareActive whether the detail hub shares active-object state
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, boolean bShareActive) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path, bShareActive);
	}

	/**
	 * Delegates to the full {@code getDetailHub} implementation using the
	 * specified property path, active-object sharing flag, and select-order.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path
	 * @param bShareActive whether active-object state is shared
	 * @param selectOrder  optional select-order for the detail hub
	 * @return the resolved or created detail hub
	 */
	public static Hub getDetailHub(Hub thisHub, String path, boolean bShareActive, String selectOrder) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path, bShareActive, selectOrder);
	}

	/**
	 * Core implementation for resolving or creating a detail hub based on a
	 * property path or class sequence. Handles HubMerger creation, discovery
	 * of existing HubDetail entries, link resolution, and recursion through
	 * multi-segment property paths.
	 *
	 * @param thisHub      the master hub
	 * @param path         the property path (may be null for class-based lookup)
	 * @param classes      optional class array to derive a property path
	 * @param lastClass    optional class constraint for the final segment
	 * @param detailHub    optionally supplied hub to populate
	 * @param bShareActive whether the detail hub shares active-object state
	 * @param selectOrder  optional select-order for the detail hub
	 * @return the resolved or newly created detail hub
	 */
	protected static Hub getDetailHub(final Hub thisHub, String path, Class[] classes, Class lastClass, Hub detailHub, 
			boolean bShareActive, String selectOrder) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getDetailHub(thisHub, path, classes, lastClass, detailHub, bShareActive, selectOrder); 
	}

	/**
	 * Sets the master object for this hub and assigns the associated
	 * detail-to-master link information. Updates the hub’s master object
	 * reference when changed.
	 *
	 * @param thisHub          the hub whose master is being set
	 * @param masterObject     the new master object
	 * @param liDetailToMaster the link information from detail to master
	 */
	public static void setMasterObject(Hub thisHub, OAObject masterObject, OALinkInfo liDetailToMaster) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().setMasterObject(thisHub, masterObject, liDetailToMaster); 
	}

	/**
	 * Convenience wrapper that sets the master object using the hub’s
	 * existing detail-to-master link information.
	 *
	 * @param thisHub      the hub whose master object is assigned
	 * @param masterObject the master object to set
	 */
	public static void setMasterObject(Hub thisHub, OAObject masterObject) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubDetailService().setMasterObject(thisHub, masterObject); 
	}

	/**
	 * Returns the {@code OALinkInfo} that links a detail hub to its master.
	 * Searches this hub and any shared hubs that carry master metadata.
	 *
	 * @param hub the detail hub
	 * @return the detail-to-master link information, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromDetailToMaster(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getLinkInfoFromDetailToMaster(hub); 
	}

	/**
	 * Returns true if any master hub in the hierarchy above this hub has an
	 * active object marked as new. Walks upward through master hubs or master
	 * objects until the chain terminates.
	 *
	 * @param thisHub the hub to evaluate
	 * @return true if a master active object is new, otherwise false
	 */
	public static boolean isMasterNew(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().isMasterNew(thisHub); 
	}

	/**
	 * Removes or decrements the reference count for a registered detail hub.
	 * If no more references remain and the detail hub has no children, its
	 * data and master information are reset.
	 *
	 * @param thisHub   the master hub
	 * @param hubDetail the detail hub to remove
	 * @return true if the hub was removed entirely, otherwise false
	 */
	public static boolean removeDetailHub(Hub thisHub, Hub hubDetail) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().removeDetailHub(thisHub, hubDetail); 
	}

	/**
	 * Returns the name of the property on the master object or hub that leads
	 * to this detail hub. Attempts resolution via link metadata, OAObjectInfo,
	 * and HubDetail entries.
	 *
	 * @param thisHub the detail hub
	 * @return the master-to-detail property name, or null if unavailable
	 */
	public static String getPropertyFromMasterToDetail(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getPropertyFromMasterToDetail(thisHub); 
	}

	/**
	 * Returns the link information from a master hub to this detail hub.
	 * Delegates to {@link #getLinkInfoFromMasterToDetail(Hub)}.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the link info from master to detail, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterHubToDetail(Hub thisDetailHub) {
		OAGraph g = getGraph(thisDetailHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getLinkInfoFromMasterHubToDetail(thisDetailHub); 
	}

	/**
	 * Determines whether a recursive one-to-many relationship is valid for
	 * this hub based on link metadata and object-class comparisons.
	 *
	 * @param hub the hub to evaluate
	 * @return true if the recursive structure is valid, otherwise false
	 */
	public static boolean getIsValidRecursive(final Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().getIsValidRecursive(hub); 
	}

	/**
	 * Returns whether both hubs originate from the same master hub based on
	 * link-info equality from their respective master hubs.
	 *
	 * @param hub1 the first hub
	 * @param hub2 the second hub
	 * @return true if both hubs share the same master link info, otherwise false
	 */
	public static boolean getIsFromSameMasterHub(Hub hub1, Hub hub2) {
		OAGraph g = getGraph(hub1, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().getIsFromSameMasterHub(hub1, hub2); 
	}

	/**
	 * Resolves the link information from the master hub or master object
	 * to this detail hub. Searches shared hubs, link metadata, and registered
	 * HubDetail entries.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the master-to-detail link information, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterToDetail(Hub thisDetailHub) {
		OAGraph g = getGraph(thisDetailHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getLinkInfoFromMasterToDetail(thisDetailHub); 
	}

	/**
	 * Returns the link information from the master object to this detail hub.
	 * Searches master hubs, shared hubs, and HubDetail records to locate the
	 * appropriate link metadata.
	 *
	 * @param thisDetailHub the detail hub
	 * @return the link info from master object to detail, or null if not found
	 */
	public static OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub thisDetailHub) {
		OAGraph g = getGraph(thisDetailHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisDetailHub); 
	}

	/**
	 * Builds a dot-separated property path representing the sequence of
	 * detail-to-master relationships from this hub upward through its
	 * master hierarchy.
	 *
	 * @param thisHub the starting hub
	 * @return the property path to all masters, or an empty string if none
	 */
	public static String getPropertyPathToMasters(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getPropertyPathToMasters(thisHub); 
	}

	/**
	 * Returns the property name on the detail object that refers to the
	 * master object, based on the hub’s detail-to-master link information.
	 *
	 * @param thisHub the detail hub
	 * @return the detail-to-master property name, or null if unavailable
	 */
	public static String getPropertyFromDetailToMaster(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getPropertyFromDetailToMaster(thisHub); 
	}

	/**
	 * Returns whether this hub represents an owned relationship, determined
	 * by evaluating the reverse link info of its detail-to-master link.
	 *
	 * @param thisHub the hub to evaluate
	 * @return true if the detail objects are owned by the master, otherwise false
	 */
	public static boolean isOwned(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().isOwned(thisHub); 
	}

	/**
	 * Returns the actual hub instance that should be used based on the
	 * current master object’s property value. If the master object’s
	 * detail property points to a different hub, that hub is returned.
	 *
	 * @param thisHub the hub to resolve
	 * @return the appropriate hub instance
	 */
	public static Hub getRealHub(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubDetailService().getRealHub(thisHub); 
	}

	/*20180305 was:   not sure why this was
	public static Hub getRealHub(Hub thisHub) {
	    return _getRealHub(thisHub, 0);
	}
	public static Hub _getRealHub(Hub thisHub, int cnt) {
	    Hub hubMaster = HubDetailDelegate.getMasterHub(thisHub);
	    if (hubMaster == null) return thisHub;
	
	    if (cnt > 10) {
	        LOG.log(Level.WARNING, "", new Exception("possible stackoverflow, thisHub="+thisHub+", masterHub="+hubMaster));
	    }
	    else {
	        hubMaster = _getRealHub(hubMaster, cnt+1);
	    }
	
	    Hub h = thisHub;
	    OAObject o = HubDetailDelegate.getMasterObject(thisHub);
	    if (o != null && o != hubMaster.getAO()) {
	        h = (Hub) OAObjectReflectDelegate.getProperty(o, getPropertyFromMasterToDetail(hubMaster));
	        if (h == null) {
	            h = thisHub; // should not happen
	        }
	    }
	    return h;
	}
	*/

	/**
	 * Returns whether this hub has any registered detail hubs.
	 *
	 * @param thisHub the hub to inspect
	 * @return true if detail hubs are present, otherwise false
	 */
	public static boolean hasDetailHubs(Hub thisHub) {
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return false;
		return g.hubs().getHubDetailService().hasDetailHubs(thisHub); 
	}

	/**
	 * 20111008 finish if/when needed public static HubDetail getHubDetail(Hub hubDetail) { Hub hubMaster = hubDetail.getMasterHub();
	 * Vector<HubDetail> vec = hubMaster.datau.vecHubDetail; if (vec == null) return null; for (HubDetail hd : vec) { if (hd.) } }
	 */

}
