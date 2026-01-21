package com.viaoa.graph;

import com.viaoa.graph.api.*;

public interface OAGraph {
    
	public OAObjectOps objects();

	public HubOps hubs();
    
	public OASyncOps sync();
	
}

