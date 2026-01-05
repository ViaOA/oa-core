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

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.runtime.OARuntime;


/**
 * Delegate that connects a {@link Hub} to its {@link com.viaoa.datasource.OADataSource}.
 * <p>
 * Provides helper methods for managing persistence and link-table updates
 * (e.g., many-to-many relationships).
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Retrieve the proper {@link OADataSource} for a given class.</li>
 *   <li>Update or remove many-to-many link-table records.</li>
 *   <li>Integrate with {@link HubAddRemoveDelegate} to persist relationship changes.</li>
 * </ul>
 */
public class HubDSDelegate {

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
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	
	/**
	 * Returns the {@link OADataSource} associated with the specified class.
	 * Delegates directly to {@link OADataSource#getDataSource(Class)}.
	 *
	 * @param c the class used to look up its data source
	 * @return the data source for the class, or null if none exists
	 */
	protected static OADataSource getDataSource(Class c) {
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.hubs().getHubDSService().getDataSource(c);
	}
    
	/**
	 * Updates many-to-many link-table records for the specified master object.
	 * Retrieves the appropriate data source and forwards the request to its
	 * {@code updateMany2ManyLinks} method.
	 *
	 * @param masterObject   the master object whose link table is updated
	 * @param adds           objects to add to the link table
	 * @param removes        objects to remove from the link table
	 * @param propFromMaster the name of the master-side property for the link
	 */
	public static void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
		OAGraph g = getGraph(null, masterObject);
		if (g == null) return;
		g.hubs().getHubDSService().updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
	}

	/**
	 * Removes many-to-many link-table records associated with the removed
	 * objects in the given hub. Only applies when the hub represents a
	 * many-to-many relationship and has removed objects tracked.
	 *
	 * @param hub the hub whose removed objects should have link records deleted
	 */
    public static void removeMany2ManyLinks(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.hubs().getHubDSService().removeMany2ManyLinks(hub);
    }

}
