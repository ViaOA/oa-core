package com.viaoa.oa.service.facade;

import com.viaoa.oa.api.services.ServicesOps;
import com.viaoa.oa.api.services.HubsOps;
import com.viaoa.oa.api.services.ObjectsOps;
import com.viaoa.oa.api.services.RulesOps;
import com.viaoa.oa.api.services.TriggersOps;

public class ServicesOpsImpl implements ServicesOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	private RulesOps rules;
	
	public ServicesOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers, RulesOps rules) {
		this.hubs = hubs;
		this.objects = objects;
		this.triggers = triggers;
		this.rules = rules;
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
	public RulesOps rules() {
		return rules;
	}
}

