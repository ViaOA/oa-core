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


import java.io.*;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncDelegate;

/**
 * Delegate for serializing and deserializing {@link Hub} instances.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Serialize Hub state safely, forcing completion of pending selects.</li>
 *   <li>Restore Hub membership references after deserialization.</li>
 *   <li>Replace master or contained objects while maintaining referential integrity.</li>
 * </ul>
 *
 * <p>Used heavily in client/server synchronization via {@link OASyncDelegate}
 * and {@link OAObjectSerializeDelegate}.
 */
public class HubSerializeDelegate {
    private static Logger LOG = Logger.getLogger(HubSerializeDelegate.class.getName());

	/*
	OAGraph g = getGraph(hub, null);
	if (g == null) return;
	g.hubs().getHubSerializeService().?(?);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.hubs().getHubSerializeService().?(?);
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
        Used by serialization to store Hub.
    */
    protected static void _writeObject(Hub thisHub, java.io.ObjectOutputStream stream) throws IOException {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.hubs().getHubSerializeService()._writeObject(thisHub, stream);
    }
    
    public static int replaceObject(Hub thisHub, OAObject objFrom, OAObject objTo) {
    	OAGraph g = getGraph(thisHub, objFrom);
    	if (g == null) return 0;
    	return g.hubs().getHubSerializeService().replaceObject(thisHub, objFrom, objTo);
    }

    public static void replaceMasterObject(Hub thisHub, OAObject objFrom, OAObject objTo) {
    	OAGraph g = getGraph(thisHub, objFrom);
    	if (g == null) return;
    	g.hubs().getHubSerializeService().replaceMasterObject(thisHub, objFrom, objTo);
    }
    
    /** 
     * Used by OAObjectSerializeDelegate
     */
    public static boolean isResolved(Hub thisHub) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.hubs().getHubSerializeService().isResolved(thisHub);
    }

    /**
        Used by serialization when reading objects from stream.
        This needs to add the hub to OAObject.hubs, but only if it is not a duplicate (and is not needed)
    */
    protected static Object _readResolve(Hub thisHub) throws ObjectStreamException {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return null;
    	return g.hubs().getHubSerializeService()._readResolve(thisHub);
    }
}
