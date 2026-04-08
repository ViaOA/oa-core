package com.viaoa.graph;

import com.viaoa.graph.api.*;

public interface OAGraph {
    
	public ObjectsOps objects();

	public HubsOps hubs();
    
	public SyncOps sync();

	public ReplOps repl();
	
	public String getPackageName();
	
	
}

