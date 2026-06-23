package com.viaoa.graph.service.facade;

import com.viaoa.graph.api.services.GraphServicesOps;
import com.viaoa.graph.api.services.HubsOps;
import com.viaoa.graph.api.services.ObjectsOps;
import com.viaoa.graph.api.services.TriggersOps;

public class GraphServicesOpsImpl implements GraphServicesOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	
	public GraphServicesOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers) {
		this.hubs = hubs;
		this.objects = objects;
		this.triggers = triggers;
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
}

