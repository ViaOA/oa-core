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

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.runtime.OARuntime;

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

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubRootService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubRootService().?(?);
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
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return null;
		return g.hubs().getHubRootService().getRootHub(thisHub);
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
		OAGraph g = getGraph(thisHub, null);
		if (g == null) return;
		g.hubs().getHubRootService().setRootHub(thisHub, b);
	}

}
