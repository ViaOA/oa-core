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
import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
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

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubDeleteService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubDeleteService().?(?);
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
	 * Deletes all objects in the hub. If running in client/server mode and the
	 * delete must occur on the server, the request is delegated to the server and
	 * no further action is taken locally. Otherwise, this method marks the hub as
	 * deleting, enables remote message forwarding, and invokes the internal delete
	 * routine. Remote messaging and delete flags are restored afterward.
	 *
	 * @param thisHub the hub whose contents will be deleted
	 */
    public static void deleteAll(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubDeleteService().deleteAll(thisHub);
    }

    /**
     * Indicates whether the specified hub is currently in the process of having all
     * its objects deleted. This flag is maintained using thread-local tracking.
     *
     * @param thisHub the hub being checked
     * @return {@code true} if the hub is currently deleting all objects
     */
    public static boolean isDeletingAll(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubDeleteService().isDeletingAll(thisHub);
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
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubDeleteService().deleteAll(thisHub, cascade);
    }

}
