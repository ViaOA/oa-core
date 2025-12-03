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

import java.util.logging.Logger;

import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.util.OAFilter;

/**
 * Utility delegate that locates or assigns the root {@link Hub} in a
 * recursive object hierarchy.
 *
 * <p>A recursive hub is one whose {@link OAObject OAObject} type has a
 * self-referencing link (for example, an {@code Employee} with a
 * {@code getEmployees()} method for subordinates).  The root Hub is the
 * one that represents objects whose parent reference is {@code null}.
 * Other recursive Hubs reference this same root so that recursive trees
 * share a common top-level container.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Find the correct root Hub for a recursive Hub by examining
 *       {@link OALinkInfo} relationships, master/detail links, and owner
 *       flags.</li>
 *   <li>Support {@link #setRootHub(Hub, boolean)} to explicitly assign or
 *       remove a Hub as the root for its recursive class.</li>
 *   <li>Handle complex cases where the Hub is shared, linked through a
 *       master object, or part of an ownership chain.</li>
 * </ul>
 *
 * <h3>Typical Usage</h3>
 * <pre>{@code
 * Hub<Employee> hubAllEmployees = new Hub<>(Employee.class);
 * Hub<Employee> hubReports = hubAllEmployees.getDetailHub("employees");
 *
 * Hub root = HubRootDelegate.getRootHub(hubReports);
 * // Returns hubAllEmployees, since it is the top of the recursion
 * }</pre>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Relies on {@link OAObjectInfoDelegate} for metadata about recursive
 *       links and ownership.</li>
 *   <li>Used internally by {@link HubDetailDelegate} and other wiring
 *       components to ensure that all recursive detail Hubs point to the
 *       same root.</li>
 *   <li>Does not modify Hub contents—only metadata about which Hub is the
 *       recursion root.</li>
 * </ul>
 */
public class HubRootDelegate {
	private static Logger LOG = Logger.getLogger(HubRootDelegate.class.getName());

	/**
	 * Determines and returns the root Hub for a recursive Hub hierarchy.
	 *
	 * <p>Behavior includes:</p>
	 * <ul>
	 *   <li>Checks whether the Hub’s object type has a recursive link.</li>
	 *   <li>If a root Hub is already registered via {@link OAObjectInfoDelegate}, returns it.</li>
	 *   <li>Examines shared Hubs, master/detail links, and ownership flags to
	 *       determine the correct root for the recursion chain.</li>
	 *   <li>Handles complex cases such as owner-based recursion, multiple master hubs,
	 *       or when the parent link is not part of the recursive relationship.</li>
	 *   <li>Returns {@code null} if no root Hub can be determined.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub whose recursive root is being requested
	 * @return the root Hub for this recursive Hub, or {@code null} if not recursive
	 */
	public static Hub getRootHub(final Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		OALinkInfo liRecursive = OAObjectInfoDelegate.getRecursiveLinkInfo(thisHub.data.getObjectInfo(), OALinkInfo.ONE);
		// 1: must be recursive
		if (liRecursive == null) {
			return null;
		}

		// 2: check for root hub
		Hub h = OAObjectInfoDelegate.getRootHub(thisHub.data.getObjectInfo());
		if (h != null) {
			return h;
		}

		// 3: get dm
		// 20120717 could be more then one master hub available, find the one that owns this object
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub hx) {
				return (hx.datam.getMasterHub() != null);
			}
		};
		Hub[] hubs = HubShareDelegate.getAllSharedHubs(thisHub, filter);
		HubDataMaster dm = null;
		for (int i = 0; hubs != null && i < hubs.length; i++, dm = null) {
			dm = hubs[i].datam;
			if (dm.liDetailToMaster == null) {
				continue;
			}
			OALinkInfo rev = OAObjectInfoDelegate.getReverseLinkInfo(hubs[i].datam.liDetailToMaster);
			if (rev != null && rev.isOwner()) {
				if (rev.getType() == OALinkInfo.TYPE_MANY && rev.getToClass().equals(thisHub.getObjectClass())) {
					break;
				}
			}
		}
		if (dm == null) {
			dm = thisHub.datam;
			// was: HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
		}

		// 20120304 added other cases on how to find the root hub
		if (dm.liDetailToMaster == null) {
			return OAObjectInfoDelegate.getRootHub(thisHub.data.getObjectInfo());
		}
		if (thisHub.datam.getMasterObject() == null && thisHub.datam.getMasterHub() == null) {
			return OAObjectInfoDelegate.getRootHub(thisHub.data.getObjectInfo());
		}
		if (thisHub.datam.getMasterObject() == null) {
			if (thisHub.datam.getMasterHub() != null) {
				Class mc = thisHub.datam.getMasterHub().getObjectClass();
				if (mc != null) {
					if (mc.equals(thisHub.getObjectClass())) {
						h = getRootHub(thisHub.datam.getMasterHub());
						if (h != null) {
							return h;
						}
					} else {
						// could be owner / master Hub
						if (OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster).getOwner()) {
							return thisHub; // thisHub is a detail from the owner.  When the owner hub AO is changed, then thisHub will have root
						}
					}
				}
			}
			return OAObjectInfoDelegate.getRootHub(thisHub.data.getObjectInfo());
		}
		// End 20120304

		/*was
		// 4: check to see if there is a valid masterObject - must have a link to it
		if (thisHub.datam.masterObject == null || dm.liDetailToMaster == null) {
		    // does not belong to a owner or master object.
		    // The root hub needs to be manually set by calling Hub.setRootHub,
		    //     since the recursive hub does not have an owner object
		    return OAObjectInfoDelegate.getRootHub(thisHub.datau.objectInfo);
		}
		*/

		// 5: if parent is not recursive - if the LinkInfos are different
		if (dm.liDetailToMaster != OAObjectInfoDelegate.getRecursiveLinkInfo(thisHub.data.getObjectInfo(), OALinkInfo.ONE)) {
			// if dm.masterObject is owner, then it is owner
			OALinkInfo rli = OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster);
			if (rli == null) {
				LOG.warning("cant find reverse linkInfo, hub=" + thisHub);
			}

			if (rli != null && rli.getOwner()) {
				// found the root hub and owner
				// cant use the masterHub, need to get the "real" detail hub of master object
				//   For recursive hubs that are linked, the master (owner) might not be using the root hub.
				//   By getting the hub value of the masterObject, it will call its hub getMethod, which will be the root hub
				return (Hub) OAObjectReflectDelegate.getProperty(	(OAObject) dm.getMasterObject(),
																	OAObjectInfoDelegate.getReverseLinkInfo(dm.liDetailToMaster).getName());
			}

			// the linkInfo for the parent is not the owner or a recursive parent
			// The root hub needs to be manually set by calling Hub.setRootHub,
			//     since the recursive hub does not have an owner object
			return OAObjectInfoDelegate.getRootHub(thisHub.data.getObjectInfo());
		}

		// 6: dm.masterObject is the same as this class - recursive parent hub
		//    use it to get the owner object and then the root hub (from owner object)
		// find owner link
		OALinkInfo linkOwner = OAObjectInfoDelegate.getLinkToOwner(thisHub.data.getObjectInfo());
		if (linkOwner != null) {
			OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(linkOwner);
			if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
				// get owner object:
				Object owner = OAObjectReflectDelegate.getProperty((OAObject) dm.getMasterObject(), linkOwner.getName());
				if (owner != null) {
					Object root = OAObjectReflectDelegate.getProperty((OAObject) owner, liRev.getName());
					if (!(root instanceof Hub)) {
						throw new RuntimeException("Hub.getRootHub() method from owner object not returning a Hub.");
					}
					return (Hub) root;
				}
			}
		}

		return null;
	}

	/**
	 * Explicitly assigns or removes the root Hub designation for a recursive
	 * Hub class.
	 *
	 * <p>If {@code b} is {@code true}, the supplied Hub becomes the root Hub
	 * for all recursive Hubs of its object class. If {@code false}, any
	 * previously registered root is cleared.</p>
	 *
	 * <p>Used when recursive relationships do not have an owner object to
	 * automatically determine the root Hub.</p>
	 *
	 * @param thisHub the Hub to set or clear as the root
	 * @param b       {@code true} to set thisHub as root, {@code false} to remove it
	 */
	public static void setRootHub(Hub thisHub, boolean b) {
		OAObjectInfoDelegate.setRootHub(thisHub.data.getObjectInfo(), b ? thisHub : null);
	}

}
