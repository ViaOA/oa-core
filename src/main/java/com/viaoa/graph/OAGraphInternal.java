package com.viaoa.graph;

import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;

public interface OAGraphInternal extends OAGraph {

	
	public ObjectsInternalOps objectsInternal();

	public HubsInternalOps hubsInternal();
    
	public SyncInternalOps syncInternal();

}
