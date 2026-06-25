package com.viaoa.oa.service.facade;

import com.viaoa.oa.api.services.ServicesOps;
import com.viaoa.oa.api.services.HubsOps;
import com.viaoa.oa.api.services.ObjectsOps;
import com.viaoa.oa.api.services.TriggersOps;

public class ServicesOpsImpl implements ServicesOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	
	public ServicesOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers) {
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

