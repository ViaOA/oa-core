package com.viaoa.graph;

import com.viaoa.graph.api.internal.HubsInternalOps;
import com.viaoa.graph.api.internal.ObjectsInternalOps;
import com.viaoa.graph.api.internal.ReplInternalOps;
import com.viaoa.graph.api.internal.SyncInternalOps;
import com.viaoa.graph.service.object.OAObjectCallbackService;
import com.viaoa.object.OAObject;

public interface OAGraphInternal extends OAGraph {
	
	public ObjectsInternalOps objectsInternal();

	public HubsInternalOps hubsInternal();
    
	public SyncInternalOps syncInternal();

	public ReplInternalOps replInternal();

}
