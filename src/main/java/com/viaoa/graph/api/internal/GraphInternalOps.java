package com.viaoa.graph.api.internal;


public interface GraphInternalOps {

	public ObjectsOps objects();

	public HubsOps hubs();

	
	public SyncInternalOps sync();

	public ReplicationInternalOps replication();
	
	public TriggersOps triggers();
	
}
