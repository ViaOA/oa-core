package com.viaoa.oa.api.internal;

public interface InternalOps {

	public ObjectsOps objects();

	public HubsOps hubs();

	public SyncInternalOps sync();

	public ReplicationInternalOps replication();
	
	public TriggersOps triggers();
	
}
