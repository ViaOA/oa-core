package com.viaoa.oa.internal.facade;

import com.viaoa.oa.api.internal.GraphInternalOps;
import com.viaoa.oa.api.internal.HubsOps;
import com.viaoa.oa.api.internal.ObjectsOps;
import com.viaoa.oa.api.internal.ReplicationInternalOps;
import com.viaoa.oa.api.internal.SyncInternalOps;
import com.viaoa.oa.api.internal.TriggersOps;

public class GraphInternalOpsImpl implements GraphInternalOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	private SyncInternalOps opsSync;
	private ReplicationInternalOps opsReplication;
	
	public GraphInternalOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers, SyncInternalOps opsSync, ReplicationInternalOps opsReplication) {
		this.hubs = hubs;
		this.objects = objects;
		this.triggers = triggers;
		this.opsSync = opsSync;
		this.opsReplication = opsReplication;
	}

	@Override
	public ObjectsOps objects() {
		return objects;
	}

	@Override
	public HubsOps hubs() {
		return hubs;
	}

	@Override
	public TriggersOps triggers() {
		return triggers;
	}
	
	@Override
	public SyncInternalOps sync() {
		return opsSync;
	}

	@Override
	public ReplicationInternalOps replication() {
		return opsReplication;
	}
}
